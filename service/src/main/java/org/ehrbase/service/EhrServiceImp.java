/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.service;

import static org.ehrbase.jooq.pg.Routines.partyUsage;
import static org.ehrbase.jooq.pg.Tables.PARTY_IDENTIFIED;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.Attestation;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.generic.RevisionHistoryItem;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.PartyRef;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.dao.access.interfaces.I_AttestationAccess;
import org.ehrbase.dao.access.interfaces.I_ConceptAccess;
import org.ehrbase.dao.access.interfaces.I_EhrAccess;
import org.ehrbase.dao.access.interfaces.I_StatusAccess;
import org.ehrbase.dao.access.jooq.AttestationAccess;
import org.ehrbase.dao.access.jooq.party.PersistedPartyProxy;
import org.ehrbase.dao.access.jooq.party.PersistedPartyRef;
import org.ehrbase.jooq.pg.Routines;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.EhrStatusDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredString;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.StructuredStringFormat;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.serialisation.xmlencoding.CanonicalXML;
import org.ehrbase.repository.EhrFolderRepository;
import org.ehrbase.util.PartyUtils;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service(value = "ehrService")
@Transactional()
public class EhrServiceImp extends BaseServiceImp implements EhrService {
    public static final String DESCRIPTION = "description";
    public static final String PARTY_ID_ALREADY_USED =
            "Supplied partyId[%s] is used by a different EHR in the same partyNamespace[%s].";
    private Logger logger = LoggerFactory.getLogger(getClass());
    private final ValidationService validationService;
    private final TenantService tenantService;

    private final EhrFolderRepository ehrFolderRepository;

    @Autowired
    public EhrServiceImp(
            KnowledgeCacheService knowledgeCacheService,
            ValidationService validationService,
            DSLContext context,
            ServerConfig serverConfig,
            TenantService tenantService,
            EhrFolderRepository ehrFolderRepository) {
        super(knowledgeCacheService, context, serverConfig);
        this.validationService = validationService;
        this.tenantService = tenantService;
        this.ehrFolderRepository = ehrFolderRepository;
    }

    @PostConstruct
    public void init() {
        // Create local system UUID
        getSystemUuid();
    }

    private UUID getEmptyPartyByTenant() {
        Short sysTenant = tenantService.getCurrentSysTenant();
        return new PersistedPartyProxy(getDataAccess()).getOrCreate(new PartySelf(), sysTenant);
    }

    @Override
    public UUID create(UUID ehrId, EhrStatus status) {

        check(status);

        if (status == null) { // in case of new status with default values
            status = new EhrStatus();
            status.setSubject(new PartySelf(null));
            status.setModifiable(true);
            status.setQueryable(true);
        }
        status.setUid(new HierObjectId(
                UuidGenerator.randomUUID().toString())); // server sets own new UUID in both cases (new or given status)

        UUID subjectUuid;
        if (PartyUtils.isEmpty(status.getSubject())) {
            subjectUuid = getEmptyPartyByTenant();
        } else {
            subjectUuid = new PersistedPartyProxy(getDataAccess())
                    .getOrCreate(status.getSubject(), tenantService.getCurrentSysTenant());

            if (I_EhrAccess.checkExist(getDataAccess(), subjectUuid)) {
                throw new StateConflictException(
                        "Specified party has already an EHR set (partyId=" + subjectUuid + ")");
            }
        }

        UUID systemId = getSystemUuid();
        UUID committerId = getCurrentUserId();

        try { // this try block sums up a bunch of operations that can throw errors in the following
            I_EhrAccess ehrAccess = I_EhrAccess.getInstance(
                    getDataAccess(), subjectUuid, systemId, null, ehrId, tenantService.getCurrentSysTenant());
            ehrAccess.setStatus(status);
            return ehrAccess.commit(committerId, systemId, DESCRIPTION);
        } catch (Exception e) {
            throw new InternalServerException("Could not create an EHR with given parameters.", e);
        }
    }

    @Override
    public Optional<EhrStatusDto> getEhrStatusEhrScape(UUID ehrUuid, CompositionFormat format) {

        if (!hasEhr(ehrUuid)) {
            return Optional.empty();
        }
        return Optional.of(getEhrStatus(ehrUuid)).map(s -> from(s, format));
    }

    private EhrStatusDto from(EhrStatus status, CompositionFormat format) {

        EhrStatusDto statusDto = new EhrStatusDto();
        if (status.getSubject().getExternalRef() != null) {
            statusDto.setSubjectId(status.getSubject().getExternalRef().getId().getValue());
            statusDto.setSubjectNamespace(status.getSubject().getExternalRef().getNamespace());
        }
        statusDto.setModifiable(status.isModifiable());
        statusDto.setQueryable(status.isQueryable());
        if (status.getOtherDetails() != null) {
            if (format.equals(CompositionFormat.XML)) {
                statusDto.setOtherDetails(new StructuredString(
                        new CanonicalXML().marshal(status.getOtherDetails()), StructuredStringFormat.XML));
            } else {
                statusDto.setOtherDetails(new StructuredString(
                        new CanonicalJson().marshal(status.getOtherDetails()), StructuredStringFormat.JSON));
            }
        }

        return statusDto;
    }

    @Override
    public EhrStatus getEhrStatus(UUID ehrUuid) {
        // pre-step: check for valid ehrId
        if (!hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrUuid.toString());
        }

        try {

            I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUuid);

            return ehrAccess.getStatus();

        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new InternalServerException(e);
        }
    }

    @Override
    public Optional<OriginalVersion<EhrStatus>> getEhrStatusAtVersion(
            UUID ehrUuid, UUID versionedObjectUid, int version) {
        // pre-step: check for valid ehrId
        if (!hasEhr(ehrUuid)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrUuid.toString());
        }

        if ((version == 0) || (I_StatusAccess.getLatestVersionNumber(getDataAccess(), versionedObjectUid) < version)) {
            throw new ObjectNotFoundException(
                    "versioned_ehr_status", "No VERSIONED_EHR_STATUS with given version: " + version);
        }

        I_StatusAccess statusAccess = I_StatusAccess.getVersionMapOfStatus(getDataAccess(), versionedObjectUid)
                .get(version);

        ObjectVersionId versionId = new ObjectVersionId(
                versionedObjectUid + "::" + getServerConfig().getNodename() + "::" + version);
        DvCodedText lifecycleState = new DvCodedText(
                "complete", new CodePhrase("532")); // TODO: once lifecycle state is supported, get it here dynamically
        AuditDetails commitAudit = statusAccess.getAuditDetailsAccess().getAsAuditDetails();
        ObjectRef<HierObjectId> contribution = new ObjectRef<>(
                new HierObjectId(
                        statusAccess.getStatusRecord().getInContribution().toString()),
                "openehr",
                "contribution");
        List<UUID> attestationIdList = I_AttestationAccess.retrieveListOfAttestationsByRef(
                getDataAccess(), statusAccess.getStatusRecord().getAttestationRef());
        List<Attestation> attestations = null; // as default, gets content if available in the following lines
        if (!attestationIdList.isEmpty()) {
            attestations = new ArrayList<>();
            for (UUID id : attestationIdList) {
                I_AttestationAccess a = new AttestationAccess(getDataAccess()).retrieveInstance(id);
                attestations.add(a.getAsAttestation());
            }
        }

        ObjectVersionId precedingVersionId = null;
        // check if there is a preceding version and set it, if available
        if (version > 1) {
            // in the current scope version is an int and therefore: preceding = current - 1
            precedingVersionId = new ObjectVersionId(
                    versionedObjectUid + "::" + getServerConfig().getNodename() + "::" + (version - 1));
        }

        OriginalVersion<EhrStatus> versionStatus = new OriginalVersion<>(
                versionId,
                precedingVersionId,
                statusAccess.getStatus(),
                lifecycleState,
                commitAudit,
                contribution,
                null,
                null,
                attestations);

        return Optional.of(versionStatus);
    }

    @Override
    public UUID updateStatus(UUID ehrId, EhrStatus status, UUID contributionId, UUID audit) {

        check(status);
        checkEhrExistForParty(ehrId, status);

        // pre-step: check for valid ehrId
        if (!hasEhr(ehrId)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId.toString());
        }

        I_EhrAccess ehrAccess;
        try {
            ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrId);
        } catch (Exception e) {
            throw new InternalServerException(e);
        }

        ehrAccess.setStatus(status);

        // execute actual update and check for success
        if (ehrAccess
                .update(
                        getCurrentUserId(),
                        getSystemUuid(),
                        contributionId,
                        null,
                        I_ConceptAccess.ContributionChangeType.MODIFICATION,
                        DESCRIPTION,
                        audit)
                .equals(false))
            throw new InternalServerException(
                    "Problem updating EHR_STATUS"); // unexpected problem. expected ones are thrown inside of update()

        return UUID.fromString(getEhrStatus(ehrId).getUid().getRoot().getValue());
    }

    private void checkEhrExistForParty(UUID ehrId, EhrStatus status) {
        Optional<PartyRef> partyRef =
                Optional.ofNullable(status).map(EhrStatus::getSubject).map(PartyProxy::getExternalRef);

        if (partyRef.isPresent()) {
            String subjectId = partyRef.get().getId().getValue();
            String namespace = partyRef.get().getNamespace();
            Optional<UUID> ehrIdOpt = findBySubject(subjectId, namespace);
            if (ehrIdOpt.isPresent() && !ehrIdOpt.get().equals(ehrId)) {
                throw new InvalidApiParameterException(String.format(PARTY_ID_ALREADY_USED, subjectId, namespace));
            }
        }
    }

    private void check(EhrStatus status) {
        try {
            validationService.check(status);
        } catch (Exception e) {
            // rethrow if this class, but wrap all others in InternalServerException
            if (e.getClass().equals(UnprocessableEntityException.class)) throw (UnprocessableEntityException) e;
            if (e.getClass().equals(IllegalArgumentException.class)) throw new ValidationException(e);
            if (e.getClass().equals(ValidationException.class)) throw e;
            else throw new InternalServerException(e);
        }
    }

    @Override
    public Optional<UUID> findBySubject(String subjectId, String nameSpace) {
        UUID subjectUuid = new PersistedPartyRef(getDataAccess()).findInDB(subjectId, nameSpace);
        return Optional.ofNullable(I_EhrAccess.retrieveInstanceBySubject(getDataAccess(), subjectUuid));
    }

    /**
     * Fetches time of creation of specific EHR record
     *
     * @param ehrId
     * @return LocalDateTime instance of timestamp from DB
     */
    public DvDateTime getCreationTime(UUID ehrId) {
        // pre-step: check for valid ehrId
        if (!hasEhr(ehrId)) {
            throw new ObjectNotFoundException("ehr", "No EHR found with given ID: " + ehrId.toString());
        }

        try {
            I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrId);
            OffsetDateTime offsetDateTime = OffsetDateTime.from(
                    LocalDateTime.from(ehrAccess.getEhrRecord().getDateCreated().toLocalDateTime())
                            .atZone(ZoneId.of(ehrAccess.getEhrRecord().getDateCreatedTzid())));
            return new DvDateTime(offsetDateTime);
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new InternalServerException(e);
        }
    }

    @Override
    public Integer getEhrStatusVersionByTimestamp(UUID ehrUid, Timestamp timestamp) {
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUid);
        return ehrAccess.getStatusAccess().getEhrStatusVersionFromTimeStamp(timestamp);
    }

    /**
     * Get latest version Uid of an EHR_STATUS by given versioned object UID.
     * @param ehrStatusId given versioned object UID
     * @return latest version Uid
     */
    public String getLatestVersionUidOfStatus(UUID ehrStatusId) {
        try {
            I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrStatusId);
            UUID statusId = ehrAccess.getStatusId();
            Integer version = I_StatusAccess.getLatestVersionNumber(getDataAccess(), statusId);

            return statusId.toString() + "::" + getServerConfig().getNodename() + "::" + version;
        } catch (Exception e) {
            throw new InternalServerException(e);
        }
    }

    public UUID getEhrStatusVersionedObjectUidByEhr(UUID ehrUid) {
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUid);
        return ehrAccess.getStatusId();
    }

    @Override
    public boolean hasEhr(UUID ehrId) {
        return I_EhrAccess.hasEhr(getDataAccess(), ehrId);
    }

    @Override
    /*TODO This method should be cached...
    For contributions it may be called n times where n is the number of versions in the contribution which will in turn mean
    n SQL queries are performed*/
    public Boolean isModifiable(UUID ehrId) {
        return I_EhrAccess.isModifiable(getDataAccess(), ehrId);
    }

    @Override
    public VersionedEhrStatus getVersionedEhrStatus(UUID ehrUid) {

        // FIXME VERSIONED_OBJECT_POC: Pre_has_ehr: has_ehr (an_ehr_id)
        // FIXME VERSIONED_OBJECT_POC: Pre_has_ehr_status_version: has_ehr_status_version (an_ehr_id,
        // a_version_uid)

        EhrStatus ehrStatus = getEhrStatus(ehrUid);

        VersionedEhrStatus versionedEhrStatus = new VersionedEhrStatus();

        versionedEhrStatus.setUid(new HierObjectId(ehrStatus.getUid().getRoot().getValue()));
        versionedEhrStatus.setOwnerId(new ObjectRef<>(new HierObjectId(ehrUid.toString()), "local", "EHR"));
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUid);
        versionedEhrStatus.setTimeCreated(new DvDateTime(OffsetDateTime.of(
                ehrAccess.getStatusAccess().getInitialTimeOfVersionedEhrStatus().toLocalDateTime(),
                OffsetDateTime.now().getOffset())));

        return versionedEhrStatus;
    }

    @Override
    public RevisionHistory getRevisionHistoryOfVersionedEhrStatus(UUID ehrUid) {
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrUid);

        // get number of versions
        int versions = I_StatusAccess.getLatestVersionNumber(getDataAccess(), ehrAccess.getStatusId());
        // fetch each version
        UUID versionedObjectUid = getEhrStatusVersionedObjectUidByEhr(ehrUid);
        RevisionHistory revisionHistory = new RevisionHistory();
        for (int i = 1; i <= versions; i++) {
            Optional<OriginalVersion<EhrStatus>> ehrStatus = getEhrStatusAtVersion(ehrUid, versionedObjectUid, i);

            // create RevisionHistoryItem for each version and append it to RevisionHistory
            if (ehrStatus.isPresent()) revisionHistory.addItem(revisionHistoryItemFromEhrStatus(ehrStatus.get(), i));
        }

        if (revisionHistory.getItems().isEmpty()) {
            throw new InternalServerException("Problem creating RevisionHistory"); // never should be empty; not valid
        }
        return revisionHistory;
    }

    private RevisionHistoryItem revisionHistoryItemFromEhrStatus(OriginalVersion<EhrStatus> ehrStatus, int version) {

        String statusId = ehrStatus.getUid().getValue().split("::")[0];
        ObjectVersionId objectVersionId =
                new ObjectVersionId(statusId + "::" + getServerConfig().getNodename() + "::" + version);

        // Note: is List but only has more than one item when there are contributions regarding this object of change
        // type attestation
        List<AuditDetails> auditDetailsList = new ArrayList<>();
        // retrieving the audits
        auditDetailsList.add(ehrStatus.getCommitAudit());

        // add retrieval of attestations, if there are any
        if (ehrStatus.getAttestations() != null) {
            for (Attestation a : ehrStatus.getAttestations()) {
                AuditDetails newAudit = new AuditDetails(
                        a.getSystemId(), a.getCommitter(), a.getTimeCommitted(), a.getChangeType(), a.getDescription());
                auditDetailsList.add(newAudit);
            }
        }

        return new RevisionHistoryItem(objectVersionId, auditDetailsList);
    }

    /**
     * {@inheritDoc}
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void adminDeleteEhr(UUID ehrId) {

        ehrFolderRepository.adminDelete(ehrId, null);
        I_EhrAccess ehrAccess = I_EhrAccess.retrieveInstance(getDataAccess(), ehrId);
        ehrAccess.adminDeleteEhr();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void adminPurgePartyIdentified() {
        getDataAccess()
                .getContext()
                .deleteFrom(PARTY_IDENTIFIED)
                .where(partyUsage(PARTY_IDENTIFIED.ID).eq(0L))
                .execute();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Override
    public void adminDeleteOrphanHistory() {
        Routines.deleteOrphanHistory(getDataAccess().getContext().configuration());
    }

    @Override
    public UUID getSubjectUuid(String ehrId) {
        return getSubjectUuids(List.of(ehrId)).get(0).getRight();
    }

    private List<Pair<String, UUID>> getSubjectUuids(Collection<String> ehrIds) {
        return ehrIds.stream()
                .map(ehrId -> Pair.of(ehrId, getEhrStatus(UUID.fromString(ehrId))))
                .map(p -> {
                    if (p.getRight() == null) return Pair.<String, UUID>of(p.getLeft(), null);
                    return Pair.of(
                            p.getLeft(),
                            new PersistedPartyProxy(getDataAccess())
                                    .getOrCreate(p.getRight().getSubject(), tenantService.getCurrentSysTenant()));
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getSubjectExtRefs(Collection<String> ehrIds) {
        List<UUID> nonNullVal = getSubjectUuids(ehrIds).stream()
                .filter(p -> p.getRight() != null)
                .map(p -> p.getRight())
                .collect(Collectors.toList());

        if (nonNullVal.size() == 0) return Collections.emptyList();

        return new PersistedPartyProxy(getDataAccess())
                .retrieveMany(nonNullVal).stream()
                        .map(p -> p.getExternalRef())
                        .filter(p -> p != null)
                        .map(p -> p.getId().getValue())
                        .collect(Collectors.toList());
    }

    @Override
    public String getSubjectExtRef(String ehrId) {
        List<String> extRefs = getSubjectExtRefs(List.of(ehrId));
        return extRefs.size() == 0 ? null : extRefs.get(0);
    }
}
