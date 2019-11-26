/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School), Luis Marco-Ruiz (Hannover Medical School).

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

import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.dao.access.jooq.CompositionAccess;
import org.ehrbase.dao.access.util.ContributionDef;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.composition.EventContext;
import org.ehrbase.jooq.pg.tables.records.*;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.*;

/**
 * Composition Access Layer Interface<br>
 * Interface CRUD and specific methods
 */
public interface I_CompositionAccess extends I_SimpleCRUD<I_CompositionAccess, UUID> {

    //definitions of aliases used in joins
    String COMPOSITION_JOIN = "composition_join";
    String COMPOSER_JOIN = "composer_ref";
    String COMPOSER_ID = "composer_id";
    String FACILITY_JOIN = "facility_ref";
    String FACILITY_ID = "facility_id";
    String EVENT_CONTEXT_JOIN = "event_context_ref";
    String PARTICIPATION_JOIN = "participation_ref";
    String PERFORMER_JOIN = "performer_ref";
    String TERRITORY_JOIN = "territory_ref";
    String CONCEPT_JOIN = "concept_ref";

    String F_VERSION = "version";
    String F_COMPOSITION_ID = "composition_id";
    String F_ENTRY = "jsonb_entry";
    String F_ENTRY_TEMPLATE = "template_id";
    String F_LANGUAGE = "language";
    String F_TERRITORY = "territory";
    String F_TERRITORY_CODE = "territory_code";
    String F_COMPOSER_NAME = "composer_name";
    String F_COMPOSER_REF_VALUE = "composer_ref_value";
    String F_COMPOSER_REF_SCHEME = "composer_ref_scheme";
    String F_COMPOSER_REF_NAMESPACE = "composer_ref_namespace";
    String F_COMPOSER_REF_TYPE = "composer_ref_type";
    String F_COMPOSER_ID_VALUE = "composer_id_value";
    String F_COMPOSER_ID_ISSUER = "composer_id_issuer";
    String F_COMPOSER_ID_TYPE_NAME = "composer_id_type_name";
    String F_CONTEXT_START_TIME = "context_start_time";
    String F_CONTEXT_START_TIME_TZID = "context_start_time_tzid";
    String F_CONTEXT_END_TIME = "context_end_time";
    String F_CONTEXT_END_TIME_TZID = "context_end_time_tzid";
    String F_CONTEXT_LOCATION = "context_location";
    String F_CONTEXT_OTHER_CONTEXT = "context_other_context";
    String F_FACILITY_NAME = "facility_name";
    String F_FACILITY_REF_VALUE = "facility_ref_value";
    String F_FACILITY_REF_SCHEME = "facility_ref_scheme";
    String F_FACILITY_REF_NAMESPACE = "facility_ref_namespace";
    String F_FACILITY_REF_TYPE = "facility_ref_type";
    String F_FACILITY_ID_VALUE = "facility_id_value";
    String F_FACILITY_ID_ISSUER = "facility_id_issuer";
    String F_FACILITY_ID_TYPE_NAME = "facility_id_type_name";
    String F_PARTICIPATION_FUNCTION = "participation_function";
    String F_PARTICIPATION_MODE = "participation_mode";
    String F_PARTICIPATION_START_TIME = "participation_start_time";
    String F_PARTICIPATION_START_TIME_TZID = "participation_start_time_tzid";
    String F_PERFORMER_NAME = "performer_name";
    String F_PERFORMER_REF_VALUE = "performer_ref_value";
    String F_PERFORMER_REF_SCHEME = "performer_ref_scheme";
    String F_PERFORMER_REF_NAMESPACE = "performer_ref_namespace";
    String F_PERFORMER_REF_TYPE = "performer_ref_type";
    String F_PERFORMER_ID_VALUE = "performer_id_value";
    String F_PERFORMER_ID_ISSUER = "performer_id_issuer";
    String F_PERFORMER_ID_TYPE_NAME = "performer_id_type_name";
    String F_CONCEPT_ID = "concept_id";
    String F_CONCEPT_DESCRIPTION = "concept_description";


    Table<CompositionRecord> compositionRef = COMPOSITION.as(COMPOSITION_JOIN);
    Table<PartyIdentifiedRecord> composerRef = PARTY_IDENTIFIED.as(COMPOSER_JOIN);
    Table<IdentifierRecord> composerId = IDENTIFIER.as(COMPOSER_ID);
    Table<PartyIdentifiedRecord> facilityRef = PARTY_IDENTIFIED.as(FACILITY_JOIN);
    Table<IdentifierRecord> facilityId = IDENTIFIER.as(FACILITY_ID);
    Table<EventContextRecord> eventContextRef = EVENT_CONTEXT.as(EVENT_CONTEXT_JOIN);
    Table<ParticipationRecord> participationRef = PARTICIPATION.as(PARTICIPATION_JOIN);
    Table<PartyIdentifiedRecord> performerRef = PARTY_IDENTIFIED.as(PERFORMER_JOIN);
    Table<TerritoryRecord> territoryRef = TERRITORY.as(TERRITORY_JOIN);
    Table<ConceptRecord> conceptRef = CONCEPT.as(CONCEPT_JOIN);

    /**
     * Get a new Composition Access Instance
     *
     * @param domain      SQL context, knowledge
     * @param composition a valid RM composition
     * @param ehrId       the EHR holding this instance
     * @return {@link I_CompositionAccess} object of the new access instance
     * @throws IllegalArgumentException when retrieval failed because of wrong input
     */
    static I_CompositionAccess getNewInstance(I_DomainAccess domain, Composition composition, UUID ehrId) {
        return new CompositionAccess(domain.getContext(), domain.getKnowledgeManager(), domain.getIntrospectService(), domain.getServerConfig(), composition, ehrId);
    }

    /**
     * Retrieve composition(s) for an identified version
     *
     * @param domainAccess SQL context
     * @param id           the versioned_object uuid
     * @param version      version number
     * @return {@link I_CompositionAccess} object of the matching access instance
     * @throws IllegalArgumentException when version number is not greater 0
     * @throws ObjectNotFoundException  when not matching composition can't be found
     */
    static I_CompositionAccess retrieveCompositionVersion(I_DomainAccess domainAccess, UUID id, int version) {
        return CompositionAccess.retrieveCompositionVersion(domainAccess, id, version);
    }

    /**
     * Calculate the version corresponding to a {@link com.nedap.archie.rm.ehr.VersionedComposition}  which is the closest in time (before) the {@link Timestamp} provided.
     *
     * @param domainAccess    The {@link I_DomainAccess} containing the persistence information and DB connection parameters for persisting a composition.
     * @param vCompositionUid The {@link UUID} corresponding to a {@link com.nedap.archie.rm.ehr.VersionedComposition}
     * @param timeCommitted   {@link Timestamp} of commit
     * @return version number
     * @throws IllegalArgumentException if no version is available for the {@link Timestamp} provided.
     * @throws InternalServerException  when SQL statements fail due to internal problems.
     */
    static int getVersionFromTimeStamp(I_DomainAccess domainAccess, UUID vCompositionUid, Timestamp timeCommitted) {
        return CompositionAccess.getVersionFromTimeStamp(domainAccess, vCompositionUid, timeCommitted);
    }

    /**
     * Returns the instance of a {@link com.nedap.archie.rm.ehr.VersionedComposition} corresponding to the version which is the closest in time before the timeCommitted provided.
     *
     * @param domainAccess    {@link I_DomainAccess} with the persistence SQL Context and knowledge cache
     * @param compositionUid {@link UUID} that identifies the composition.
     * @param timeCommitted   {java.sql.Timestamp} that indicates the point in time to search version for the composition backwards.
     * @return the number of the version that is the  closest  in time (before) the timeCommitted parameter provided. If a null timeCommitted is provided the latest composition will be returned.
     * @throws IllegalArgumentException
     * @throws InternalServerException
     * @throws ObjectNotFoundException
     */
    static I_CompositionAccess retrieveInstanceByTimestamp(I_DomainAccess domainAccess, UUID compositionUid, Timestamp timeCommitted) {
        return CompositionAccess.retrieveInstanceByTimestamp(domainAccess, compositionUid, timeCommitted);
    }

    /**
     * Retrieve a composition access instance from the persistence layer
     *
     * @param domainAccess SQL context, knowledge
     * @param id           a composition uuid
     * @return a valid {@link I_CompositionAccess}
     */
    static I_CompositionAccess retrieveInstance(I_DomainAccess domainAccess, UUID id) {
        return CompositionAccess.retrieveInstance(domainAccess, id);
    }

    /**
     * Retrieve a map of composition accesses for all compositions referencing a contribution
     *
     * @param domainAccess   SQL context, knowledge
     * @param contributionId contribution object uuid
     * @return a map of {@link I_CompositionAccess} referenced by their UUID, that match the condition
     * @throws IllegalArgumentException on DB inconsistency
     */
    static Map<UUID, I_CompositionAccess> retrieveInstancesInContributionVersion(I_DomainAccess domainAccess, UUID contributionId) {
        return CompositionAccess.retrieveCompositionsInContributionVersion(domainAccess, contributionId, 0); //FIXME: target methods doesn't use version parameter
    }

    /**
     * check if a composition has a previous version in history
     *
     * @param domainAccess
     * @param compositionId
     * @return
     */
    static boolean hasPreviousVersion(I_DomainAccess domainAccess, UUID compositionId) {
        return CompositionAccess.hasPreviousVersion(domainAccess, compositionId);
    }

    /**
     * retrieve the number of versions for this composition or 1 if no history present
     *
     * @param domainAccess
     * @param compositionId
     * @return
     */
    static Integer getLastVersionNumber(I_DomainAccess domainAccess, UUID compositionId) {
        return CompositionAccess.getLastVersionNumber(domainAccess, compositionId);
    }

    // TODO: doc! what's the logic behind the returned int code?
    static Integer fetchTerritoryCode(I_DomainAccess domainAccess, String territoryAsString) {
        Result<TerritoryRecord> result = domainAccess.getContext().selectFrom(TERRITORY).where(TERRITORY.TWOLETTER.equal(territoryAsString)).fetch();
        if (result.isEmpty())
            return -1;
        return result.get(0).getCode();
    }

    static boolean isValidLanguageCode(I_DomainAccess domainAccess, String languageCode) {
        return !domainAccess.getContext().selectFrom(LANGUAGE).where(LANGUAGE.CODE.equal(languageCode)).fetch().isEmpty();
    }

    /**
     * @throws InternalServerException  when contribution couldn't be created because of an internal problem
     * @throws IllegalArgumentException when content couldn't be committed
     */
    UUID commit(UUID committerId, UUID systemId, String description);

    /**
     * Unlike the basic commit it is necessary to set the contribution manually beforehand, here. This allows committing with a specific
     * contribution ID, which could be used to process several objects at once.
     * @param committerId Committer ID (PartyIdentified)
     * @param description  Description text
     * @param systemId System ID
     * @return ID of committed object
     * @throws IllegalArgumentException when content couldn't be committed
     */
    UUID commitWithCustomContribution(UUID committerId, UUID systemId, String description);

    Boolean update(UUID committerId, UUID systemId, ContributionDef.ContributionState state, I_ConceptAccess.ContributionChangeType contributionChangeType, String description);

    /**
     * Unlike the basic update it is necessary to set the contribution manually beforehand, here. This allows updating with a specific
     * contribution ID, which could be used to process several objects at once.
     * @param committerId Committer of this change
     * @param systemId System of origin of this change
     * @param contributionChangeType Change type of this change
     * @param description Updated description or NULL
     * @return Boolean to indicate success or failure of update
     */
    Boolean updateWithCustomContribution(UUID committerId, UUID systemId, I_ConceptAccess.ContributionChangeType contributionChangeType, String description);

    /**
     * Delete a composition<br>
     * Delete by simulating an SQL CASCADE wherever appropriate<br>
     * <ul>
     * <li>delete entry first</li>
     * <li>delete the corresponding version entry</li>
     * <li>delete the composition record</li>
     * </ul>
     *
     * @return
     */
    Integer delete(UUID committerId, UUID systemId, String description);

    /**
     * Unlike the basic delete it is necessary to set the contribution manually beforehand, here. This allows deleting with a specific
     * contribution ID, which could be used to process several objects at once.
     * @param committerId Committer of this change
     * @param systemId System of origin of this change
     * @param description Updated description or NUL
     * @return Rows affected by delete
     */
    Integer deleteWithCustomContribution(UUID committerId, UUID systemId, String description);

    Timestamp getSysTransaction();

    /**
     * Gets time_committed from contribution of composition.
     *
     * @return Timestamp of time of commitment
     */
    Timestamp getTimeCommitted();

    /**
     * get the composition Id
     *
     * @return
     */
    UUID getId();

    /**
     * get the EHR id to which this composition belongs to
     *
     * @return {@link UUID}
     */
    UUID getEhrid();

    /**
     * set the EHR id
     *
     * @param ehrId {@link UUID}
     */
    void setEhrid(UUID ehrId);

    /**
     * get the composer Id
     *
     * @return {@link UUID}
     */
    UUID getComposerId();

    /**
     * set the composer id
     *
     * @param composerId {@link UUID}
     */
    void setComposerId(UUID composerId);

    /**
     * get the event context id
     *
     * @return Optional with ID if it exists, otherwise empty Optional
     */
    Optional<UUID> getContextId();

    /**
     * get the contribution version id
     *
     * @return {@link UUID}
     */
    UUID getContributionVersionId();

    /**
     * get the language code for this composition (eg. 'en', 'fr' etc.)
     *
     * @return language code as string
     */
    String getLanguageCode();

    /**
     * set the language code
     *
     * @param code String
     */
    void setLanguageCode(String code);

    /**
     * FIXME: bug? comment says 2-letter while methods are saying Integer
     * get the 2-letters country code
     *
     * @return
     */
    Integer getTerritoryCode();

    /**
     * FIXME: bug? comment says 2-letter while methods are saying Integer
     * set the 2-letters territory code
     *
     * @param code String
     */
    void setTerritoryCode(Integer code);

    /**
     * get the list of entry Ids for this composition
     *
     * @return a list of entry {@link UUID}s
     */
    List<UUID> getContentIds();

    /**
     * set the event context id
     *
     * @param contextId {@link UUID}
     * @throws DataAccessException on problem updating context
     */
    void setContextCompositionId(UUID contextId);

    /**
     * add an entry to the composition
     *
     * @param entry {@link I_EntryAccess} instance
     * @return &gt;0 success
     */
    int addContent(I_EntryAccess entry);

    /**
     * get the list of entries for this composition
     *
     * @return the list of entry as {@link I_EntryAccess}
     * @see I_EntryAccess
     */
    List<I_EntryAccess> getContent();

    void setContent(List<I_EntryAccess> content);

    /**
     * set the contribution id for this composition
     *
     * @param contributionId
     */
    void setContributionId(UUID contributionId);

    void setCompositionRecord(CompositionRecord record);

    /**
     * @throws IllegalArgumentException when handling of record failed
     */
    void setCompositionRecord(Result<?> records);

    void setComposition(Composition composition);

    void setContributionAccess(I_ContributionAccess contributionAccess);

    void setAuditDetailsAccess(I_AuditDetailsAccess auditDetailsAccess);

    Integer getVersion();

    /**
     * @throws IllegalArgumentException when seeking language code, territory code or composer ID failed
     */
    void updateCompositionData(Composition newComposition);

    void setContext(EventContext historicalEventContext);

    UUID getAuditDetailsId();

    void setAuditDetailsId(UUID auditId);
}
