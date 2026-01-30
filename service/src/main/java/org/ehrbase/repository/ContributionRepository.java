/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.repository;

import static org.ehrbase.jooq.pg.Tables.AUDIT_DETAILS;
import static org.ehrbase.jooq.pg.Tables.CONTRIBUTION;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.AuditDetails;
import org.ehrbase.jooq.pg.tables.Contribution;
import org.ehrbase.jooq.pg.tables.records.AuditDetailsRecord;
import org.ehrbase.jooq.pg.tables.records.ContributionRecord;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.service.TimeProvider;
import org.ehrbase.service.UserService;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles DB-Access to {@link Contribution} and {@link AuditDetails}
 */
@Repository
public class ContributionRepository {

    private final DSLContext context;
    private final SystemService systemService;
    private final UserService userService;
    private final PartyProxyRepository partyProxyRepository;

    private final TimeProvider timeProvider;

    public ContributionRepository(
            DSLContext context,
            SystemService systemService,
            UserService userService,
            PartyProxyRepository partyProxyRepository,
            TimeProvider timeProvider) {
        this.context = context;
        this.systemService = systemService;
        this.userService = userService;
        this.partyProxyRepository = partyProxyRepository;

        this.timeProvider = timeProvider;
    }

    /**
     * Create the default contribution in the DB for usage in case data is not saved via explicit provided contribution. Sets the committer from the auth context.
     *
     * @param ehrId
     * @param contributionType
     * @param contributionChangeType
     * @return {@link UUID} of the corresponding Database Record.
     */
    @Transactional
    public UUID createDefault(
            UUID ehrId, ContributionDataType contributionType, ContributionChangeType contributionChangeType) {

        UUID auditDetailsRecordId = createDefaultAudit(contributionChangeType, AuditDetailsTargetType.CONTRIBUTION);

        return createContribution(ehrId, UuidGenerator.randomUUID(), contributionType, auditDetailsRecordId);
    }

    /**
     * Create the default audit in the DB  for usage in case data is not saved via explicit provided contribution. Sets the committer from the auth context.
     *
     * @param contributionChangeType
     * @return {@link UUID} of the corresponding Database Record.
     */
    @Transactional
    public UUID createDefaultAudit(ContributionChangeType contributionChangeType, AuditDetailsTargetType targetType) {
        AuditDetailsRecord auditDetailsRecord = context.newRecord(AuditDetails.AUDIT_DETAILS);

        auditDetailsRecord.setId(UuidGenerator.randomUUID());
        auditDetailsRecord.setTimeCommitted(timeProvider.getNow());
        auditDetailsRecord.setTargetType(targetType.getAlias());

        auditDetailsRecord.setCommitter(null);
        auditDetailsRecord.setUserId(userService.getCurrentUserId());
        auditDetailsRecord.setChangeType(contributionChangeType);

        auditDetailsRecord.store();
        return auditDetailsRecord.getId();
    }

    @Transactional
    public UUID createContribution(
            UUID ehrId, UUID contributionUuid, ContributionDataType contributionType, UUID auditDetailsRecordId) {

        ContributionRecord contributionRecord = context.newRecord(Contribution.CONTRIBUTION);

        contributionRecord.setEhrId(ehrId);
        contributionRecord.setId(contributionUuid);
        contributionRecord.setContributionType(contributionType);
        contributionRecord.setHasAudit(auditDetailsRecordId);

        contributionRecord.store();

        return contributionRecord.getId();
    }

    /**
     * Creates a Audit in the Database
     *
     * @param auditDetails {@link AuditDetails} from which to take the data.
     * @param targetType
     * @return {@link UUID} of the corresponding Database Record.
     */
    @Transactional
    public UUID createAudit(com.nedap.archie.rm.generic.AuditDetails auditDetails, AuditDetailsTargetType targetType) {

        AuditDetailsRecord auditDetailsRecord = context.newRecord(AuditDetails.AUDIT_DETAILS);

        auditDetailsRecord.setId(UuidGenerator.randomUUID());
        auditDetailsRecord.setTimeCommitted(timeProvider.getNow());

        // save committer as json if not user
        if (!partyProxyRepository.fromUser(userService.getCurrentUserId()).equals(auditDetails.getCommitter())) {
            auditDetailsRecord.setCommitter(JSONB.jsonb(new CanonicalJson().marshal(auditDetails.getCommitter())));
        }

        auditDetailsRecord.setTargetType(targetType.getAlias());

        auditDetailsRecord.setChangeType(to(auditDetails.getChangeType()));
        // We just save the text here wich is not 100 % correct here.
        auditDetailsRecord.setDescription(Optional.ofNullable(auditDetails.getDescription())
                .map(DvText::getValue)
                .orElse(null));
        auditDetailsRecord.setUserId(userService.getCurrentUserId());

        auditDetailsRecord.store();
        return auditDetailsRecord.getId();
    }

    private ContributionChangeType to(DvCodedText changeType) {

        return switch (changeType.getDefiningCode().getCodeString()) {
            case "249" -> ContributionChangeType.creation;
            case "250" -> ContributionChangeType.amendment;
            case "251" -> ContributionChangeType.modification;
            case "252" -> ContributionChangeType.synthesis;
            case "253" -> ContributionChangeType.Unknown;
            case "523" -> ContributionChangeType.deleted;
            default -> throw new UnexpectedSwitchCaseException(changeType.toString());
        };
    }

    public ContributionRecord findById(UUID contibutionId) {

        return context.fetchOne(CONTRIBUTION, CONTRIBUTION.ID.eq(contibutionId));
    }

    public com.nedap.archie.rm.generic.AuditDetails findAuditDetails(UUID auditId) {
        AuditDetailsRecord auditDetailsRecord =
                context.fetchOne(AuditDetails.AUDIT_DETAILS, AUDIT_DETAILS.ID.eq(auditId));

        return mapToAuditDetails(auditDetailsRecord);
    }

    public com.nedap.archie.rm.generic.AuditDetails findAuditDetailsForContribution(UUID ehrId, UUID contributionId) {
        AuditDetailsRecord auditDetailsRecord = context.select(AuditDetails.AUDIT_DETAILS.fields())
                .from(Contribution.CONTRIBUTION)
                .join(AuditDetails.AUDIT_DETAILS)
                .on(AuditDetails.AUDIT_DETAILS.ID.eq(Contribution.CONTRIBUTION.HAS_AUDIT))
                .where(Contribution.CONTRIBUTION.EHR_ID.eq(ehrId).and(Contribution.CONTRIBUTION.ID.eq(contributionId)))
                .fetchOneInto(AuditDetailsRecord.class);
        return mapToAuditDetails(auditDetailsRecord);
    }

    private com.nedap.archie.rm.generic.AuditDetails mapToAuditDetails(AuditDetailsRecord auditDetailsRecord) {
        if (auditDetailsRecord == null) {
            return null;
        }

        com.nedap.archie.rm.generic.AuditDetails auditDetails = new com.nedap.archie.rm.generic.AuditDetails();

        auditDetails.setSystemId(systemService.getSystemId());

        if (auditDetailsRecord.getCommitter() != null) {
            auditDetails.setCommitter(new CanonicalJson()
                    .unmarshal(auditDetailsRecord.getCommitter().data(), PartyProxy.class));
        } else {
            auditDetails.setCommitter(partyProxyRepository.fromUser(auditDetailsRecord.getUserId()));
        }
        auditDetails.setDescription(new DvText(auditDetailsRecord.getDescription()));

        DvCodedText changeType = new DvCodedText(
                auditDetailsRecord.getChangeType().getLiteral(),
                new CodePhrase(
                        new TerminologyId("openehr"),
                        Integer.toString(ContributionService.ContributionChangeType.valueOf(auditDetailsRecord
                                        .getChangeType()
                                        .getLiteral()
                                        .toUpperCase())
                                .getCode())));
        auditDetails.setChangeType(changeType);

        DvDateTime time = new DvDateTime(auditDetailsRecord.getTimeCommitted());
        auditDetails.setTimeCommitted(time);
        return auditDetails;
    }
}
