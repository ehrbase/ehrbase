/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School).

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.dao.access.interfaces;

import com.nedap.archie.rm.datastructures.ItemStructure;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.dao.access.jooq.EhrAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import org.ehrbase.jooq.pg.tables.records.EhrRecord;
import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.ehr.EhrStatus;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.STATUS;

/**
 * Ehr access layer<br>
 * This interface deals with the main Ehr table as well as Status. Status provides the information
 * related to the actual Ehr owner (eg. patient or Named Subject generally).
 * Created by Christian Chevalley on 4/21/2015.
 */
public interface I_EhrAccess extends I_SimpleCRUD<I_EhrAccess, UUID> {

    String TAG_TEMPLATE_ID = "$TEMPLATE_ID$"; //used to serialize template id in json structure

    /**
     * get a new Ehr access layer instance
     *
     * @param domain      SQL access
     * @param partyId     owner UUID (patient)
     * @param systemId    system on which the Ehr is initiated (UUID)
     * @param directoryId optional directory structure Id
     * @param accessId    optional access strategy Id
     * @param ehrId       optional custom ehrID
     * @return I_EhrAccess
     * @throws InternalServerException if creating or retrieving system failed
     */
    static I_EhrAccess getInstance(I_DomainAccess domain, UUID partyId, UUID systemId, UUID directoryId, UUID accessId, UUID ehrId) {
        return new EhrAccess(domain.getContext(), domain.getServerConfig(), partyId, systemId, directoryId, accessId, ehrId);
    }

    /**
     * // TODO: keep! likely to be used by EHR controller
     * retrieve an Ehr for a subject identification with an identifier<br>
     * a subject identification consists of the issuer identification (ex. NHS) and an identification code
     *
     * @param domainAccess SQL access
     * @param subjectId    the subject code or number
     * @param issuerSpace  the issuer identifier
     * @return UUID of corresponding Ehr or null
     * @throws IllegalArgumentException if retrieving failed for given input
     */
    static UUID retrieveInstanceBySubject(I_DomainAccess domainAccess, String subjectId, String issuerSpace) {
        return EhrAccess.retrieveInstanceBySubject(domainAccess, subjectId, issuerSpace);
    }

    /**
     * // TODO: keep! likely to be used by EHR controller
     * retrieve an Ehr for a subject UUID<br>
     * a subject identification consists of the issuer identification (ex. NHS) and an identification code
     *
     * @param domainAccess SQL access
     * @param subjectUuid  the subject uuid
     * @return UUID of corresponding Ehr or null
     * @throws IllegalArgumentException if retrieving failed for given input
     */
    static UUID retrieveInstanceBySubject(I_DomainAccess domainAccess, UUID subjectUuid) {
        return EhrAccess.retrieveInstanceBySubject(domainAccess, subjectUuid);
    }

    /**
     * // TODO: keep! likely to be used by EHR controller
     * retrieve an Ehr for a subject identification by external reference<br>
     * a subject identification consists of the issuer identification (ex. NHS) and an identification code
     *
     * @param domainAccess SQL access
     * @param subjectId    the subject code or number
     * @param issuerSpace  the namespace
     * @return UUID of corresponding Ehr or null
     * @throws IllegalArgumentException if retrieving failed for given input
     */
    static UUID retrieveInstanceBySubjectExternalRef(I_DomainAccess domainAccess, String subjectId, String issuerSpace) {
        return EhrAccess.retrieveInstanceBySubjectExternalRef(domainAccess, subjectId, issuerSpace);
    }

    /**
     * retrieve an Ehr for a known status entry
     *
     * @param domainAccess SQL access
     * @param status       status UUID
     * @param version      optional version, will assume latest if null
     * @return UUID of corresponding Ehr or null
     * @throws IllegalArgumentException if retrieving failed for given input
     */
    static I_EhrAccess retrieveInstanceByStatus(I_DomainAccess domainAccess, UUID status, int version) {
        return EhrAccess.retrieveInstanceByStatus(domainAccess, status, version);
    }

    static boolean checkExist(I_DomainAccess domainAccess, UUID partyId) {
        return domainAccess.getContext().fetchExists(STATUS, STATUS.PARTY.eq(partyId));
    }

    /**
     * retrieve the Ehr entry from its id
     *
     * @param domainAccess SQL access
     * @param ehrId        the Ehr UUID
     * @return UUID of corresponding Ehr or null
     * @throws IllegalArgumentException when either no EHR for ID, or problem with data structure of EHR, or DB inconsistency
     */
    static I_EhrAccess retrieveInstance(I_DomainAccess domainAccess, UUID ehrId) {
        return EhrAccess.retrieveInstance(domainAccess, ehrId);
    }

    /**
     * retrieve the list of identifiers for a subject owning an Ehr<br>
     * the identifiers are formatted as: "CODE:ISSUER"
     *
     * @param domainAccess SQL access
     * @param ehrId        the Ehr Id to search the subject from
     * @return a list of identifiers
     * @throws IllegalArgumentException when no EHR found for ID
     */
    static Map<String, Object> fetchSubjectIdentifiers(I_DomainAccess domainAccess, UUID ehrId) {
        return EhrAccess.fetchSubjectIdentifiers(domainAccess, ehrId);
    }

    /**
     * TODO: doc
     *
     * @param domainAccess
     * @param ehrId
     * @return
     * @throws IllegalArgumentException when no EHR found for ID
     */
    static Map<String, Map<String, String>> getCompositionList(I_DomainAccess domainAccess, UUID ehrId) {
        return EhrAccess.getCompositionList(domainAccess, ehrId);
    }

    void setModifiable(Boolean modifiable);

    void setQueryable(Boolean queryable);


    UUID commit(UUID committerId, UUID systemId, String description);

    /**
     * Updates the whole EHR access in the DB, e.g. to update the status. Embeds contribution and audit handling.
     * @param committerId ID of committer
     * @param systemId ID of committing system
     * @param state State of contribution
     * @param contributionChangeType Change type of contribution
     * @param description Description field
     * @return True for success
     * @throws InvalidApiParameterException when marshalling of EHR_STATUS / OTHER_DETAILS failed
     */
    Boolean update(UUID committerId, UUID systemId, ContributionDef.ContributionState state, I_ConceptAccess.ContributionChangeType contributionChangeType, String description);

    /**
     * set access id
     *
     * @param access UUID
     */
    void setAccess(UUID access);

    /**
     * set directory id
     *
     * @param directory UUID
     */
    void setDirectory(UUID directory);

    /**
     * set system Id
     *
     * @param system UUID
     */
    void setSystem(UUID system);

    /**
     * TODO: doc or is this one really not needed anymore? delete if so.
     *
     * @return
     * @throws IllegalArgumentException when instance's EHR ID can't be matched to existing one
     */
    UUID reload();

    /**
     * check if Ehr is newly created (uncommitted)
     *
     * @return true if new, false otherwise
     */
    boolean isNew();

    UUID getParty();

    void setParty(UUID partyId);

    UUID getId();

    Boolean isModifiable();

    Boolean isQueryable();

    UUID getSystemId();

    UUID getStatusId();

    UUID getDirectoryId();

    UUID getAccessId();

    void setContributionAccess(I_ContributionAccess contributionAccess);

    void setOtherDetails(ItemStructure otherDetails, String templateId);

    ItemStructure getOtherDetails();

    EhrRecord getEhrRecord();

    void setStatus(EhrStatus status);

    /**
     * Gets latest EHR_STATUS, which is attached to this EHR instance after retrieving it.
     * @return Latest EHR_STATUS
     */
    EhrStatus getStatus();

    /**
     * Get latest version number of EHR_STATUS by versioned object UID.
     * @param domainAccess access
     * @param ehrStatusId versioned object UID
     * @return version number
     */
    Integer getLastVersionNumberOfStatus(I_DomainAccess domainAccess, UUID ehrStatusId);

    /**
     * Get a specific version number of the associated EHR_STATUS of this EhrAccess by timestamp.
     * General idea behind the algorithm: 'what version was the top version at moment T?'
     * @param time Timestamp
     * @return version number
     */
    int getEhrStatusVersionFromTimeStamp(Timestamp time);
}
