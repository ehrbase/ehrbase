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
import org.ehrbase.dao.access.jooq.EntryAccess;
import com.nedap.archie.rm.composition.Composition;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.ENTRY;

/**
 * Entry (Composition Content) access layer
 * Created by Christian Chevalley on 4/21/2015.
 */
public interface I_EntryAccess extends I_SimpleCRUD<I_EntryAccess, UUID> {

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
    static I_EntryAccess getNewInstance(I_DomainAccess domain, String templateId, Integer sequence, UUID compositionId, Composition composition) {
        return new EntryAccess(domain, templateId, sequence, compositionId, composition);
    }

    /**
     * retrieve the list of entries for a composition
     *
     * @param domainAccess      SQL context
     * @param compositionAccess a composition access interface instance
     * @return a list of {@link I_EntryAccess}
     * @throws IllegalArgumentException if DB is inconsistent or operation fails
     */
    static List<I_EntryAccess> retrieveInstanceInComposition(I_DomainAccess domainAccess, I_CompositionAccess compositionAccess) {
        return EntryAccess.retrieveInstanceInComposition(domainAccess, compositionAccess);
    }

    // TODO Doc: appears to be explicitly used with versions other than the latest (because accessing only "entry_history" table)
    static List<I_EntryAccess> retrieveInstanceInCompositionVersion(I_DomainAccess domainAccess, I_CompositionAccess compositionHistoryAccess, int version) {
        return EntryAccess.retrieveInstanceInCompositionVersion(domainAccess, compositionHistoryAccess, version);
    }

    /**
     * delete all entries belonging to a composition
     *
     * @param domainAccess  SQL access
     * @param compositionId a composition id
     * @return count of deleted
     */
    static Integer deleteFromComposition(I_DomainAccess domainAccess, UUID compositionId) {
        return domainAccess.getContext().delete(ENTRY).where(ENTRY.COMPOSITION_ID.eq(compositionId)).execute();
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
     * perform an arbitrary SQL query on entries and return the result set as a JSON string
     *
     * @param domainAccess SQL access
     * @param query        a valid SQL queryJSON string
     * @return a JSON formatted result set
     * @throws InternalServerException when the query failed
     */
    static Map<String, Object> queryJSON(I_DomainAccess domainAccess, String query) {
        return EntryAccess.queryJSON(domainAccess, query);
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
    String getEntryJson();

    /**
     * get the entry category record id<br>
     * Category entry is a concept identified by a code and language in
     * the concept table. The concept is always identified for the English
     * language ('en')
     *
     * @return {@link UUID} of category concept
     */
    UUID getCategory();

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

    /**
     * get the Item Type as a literal<br>
     * Item type is one of
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
