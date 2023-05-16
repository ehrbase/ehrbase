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
package org.ehrbase.dao.access.interfaces;

import static org.ehrbase.jooq.pg.Tables.ENTRY;

import com.nedap.archie.rm.composition.Composition;
import java.util.UUID;
import org.ehrbase.dao.access.jooq.EntryAccess;
import org.ehrbase.jooq.pg.udt.records.DvCodedTextRecord;
import org.jooq.JSONB;

/**
 * Entry (Composition Content) access layer Created by Christian Chevalley on 4/21/2015.
 */
public interface I_EntryAccess extends I_SimpleCRUD {

    /**
     * create and get a new Entry commit
     *
     * @param domain        SQL context
     * @param templateId    the template Id for the composition
     * @param sequence      a sequence number ({@link Integer}) if applicable
     * @param compositionId the composition entry owning this content
     * @param composition   the actual {@link Composition} to store
     * @return an access layer instance
     * @see Composition
     */
    static I_EntryAccess getNewInstance(
            I_DomainAccess domain,
            String templateId,
            Integer sequence,
            UUID compositionId,
            Composition composition,
            Short sysTenant) {
        return new EntryAccess(domain, templateId, sequence, compositionId, composition, sysTenant);
    }

    /**
     * Retrieve the {@link I_EntryAccess} linked to given composition.
     *
     * @param domainAccess      SQL context
     * @param compositionAccess a composition access interface instance
     * @return the entry access
     * @throws IllegalArgumentException if DB is inconsistent or operation fails
     */
    static I_EntryAccess retrieveInstanceInComposition(
            I_DomainAccess domainAccess, I_CompositionAccess compositionAccess) {
        return EntryAccess.retrieveInstanceInComposition(domainAccess, compositionAccess);
    }

    /**
     * Retrieves the template ID from a composition entry using the given composition ID and domain access.
     *
     * @param domainAccess The domain access object used to retrieve the composition entry.
     * @param compositionId The UUID of the composition whose template ID is to be retrieved.
     * @return The template ID of the composition as a string.
     */
    static String getTemplateIdFromEntry(I_DomainAccess domainAccess, UUID compositionId) {
        return EntryAccess.fetchTemplateIdByCompositionId(domainAccess, compositionId);
    }

    /**
     * Retrieve the {@link I_EntryAccess} linked to given composition history.
     *
     * @param domainAccess             SQL context
     * @param compositionHistoryAccess the composition history access instance
     * @param version                  the version of the composition
     * @return the entry access
     */
    static I_EntryAccess retrieveInstanceInCompositionVersion(
            I_DomainAccess domainAccess, I_CompositionAccess compositionHistoryAccess, int version) {
        return EntryAccess.retrieveInstanceInCompositionVersion(domainAccess, compositionHistoryAccess, version);
    }

    /**
     * delete an entry
     *
     * @param domainAccess SQL access
     * @param id           {@link UUID} of entry to delete
     * @return count of deleted
     */
    static Integer delete(I_DomainAccess domainAccess, UUID id) {
        return domainAccess.getContext().delete(ENTRY).where(ENTRY.ID.eq(id)).execute();
    }

    /**
     * get the actual composition held in this entry
     *
     * @return {@link Composition}
     * @see Composition
     */
    Composition getComposition();

    /**
     * get the entry Id
     *
     * @return entry ID as {@link UUID}
     */
    UUID getId();

    /**
     * get the entry values as a JSON string
     *
     * @return JSON representation of entry values
     */
    JSONB getEntryJson();

    /**
     * get the entry category record id<br> Category attribute is a DvCodedText
     *
     * @return {@link org.ehrbase.jooq.pg.udt.DvCodedText} of category concept
     */
    DvCodedTextRecord getCategory();

    /**
     * get the composition Id owning this entry
     *
     * @return composition ID as {@link UUID}
     */
    UUID getCompositionId();

    /**
     * set the owner composition by its Id
     *
     * @param compositionId UUID
     */
    void setCompositionId(UUID compositionId);

    /**
     * get the template Id (a string) used to build the composition entry
     *
     * @return template ID as string
     */
    String getTemplateId();

    /**
     * set the template id to build the composition
     *
     * @param templateId a string
     */
    void setTemplateId(String templateId);

    /**
     * get the sequence number if applicable
     *
     * @return sequence number of entry
     */
    Integer getSequence();

    /**
     * set the sequence number of this entry
     *
     * @param sequence number of this entry
     */
    void setSequence(Integer sequence);

    /**
     * Get the root archetype to build the composition
     *
     * @return archetype ID as string
     */
    String getArchetypeId();

    String getRmVersion();

    /**
     * get the Item Type as a literal<br> Item type is one of
     * <ul>
     * <li>section</li>
     * <li>care_entry</li>
     * <li>admin</li>
     * </ul>
     *
     * @return item type as string
     */
    String getItemType();

    /**
     * set the composition data with an actual {@link Composition}
     *
     * @param composition Composition
     * @see Composition
     */
    void setCompositionData(Composition composition);
}
