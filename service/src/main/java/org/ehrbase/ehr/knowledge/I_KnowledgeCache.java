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
package org.ehrbase.ehr.knowledge;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.aql.containment.JsonPathQueryResult;
import org.ehrbase.openehr.sdk.webtemplate.parser.NodeId;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

public interface I_KnowledgeCache {

    String TEMPLATE_ID = "templateId";

    Set<String> getAllTemplateIds();

    /**
     * Adds operational template to system and also in current cache.
     *
     * @param content operational template input
     * @return resulting template ID, when successful
     * @throws InvalidApiParameterException when input can't be pared to OPT instance
     * @throws StateConflictException       when template with same template ID is already in the system
     * @throws InternalServerException      when an unspecified problem occurs
     */
    String addOperationalTemplate(InputStream content);

    String addOperationalTemplate(OPERATIONALTEMPLATE template);

    List<TemplateMetaData> listAllOperationalTemplates() throws IOException;

    /**
     * retrieve an operational template document instance
     *
     * @param key the name of the operational template
     * @return an OPERATIONALTEMPLATE document instance or null
     * @see org.openehr.schemas.v1.OPERATIONALTEMPLATE
     */
    Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(String key);

    /**
     * retrieve a <b>cached</b> operational template document instance using its unique Id
     *
     * @param uuid the name of the operational template
     * @return an OPERATIONALTEMPLATE document instance or null
     * @throws Exception
     * @see org.openehr.schemas.v1.OPERATIONALTEMPLATE
     */
    Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(UUID uuid);

    /**
     * Deletes a given operational template physically from cache and from template storage and from cache. Should only
     * be executed if the template is no longer referenced by any Composition. Make sure you check for references before
     * deleting a template otherwise this causes inconsistencies and no longer deliverable Composition entries.
     *
     * @param template - The template instance to delete
     * @return - Template has been deleted
     */
    boolean deleteOperationalTemplate(OPERATIONALTEMPLATE template);

    JsonPathQueryResult resolveForTemplate(String templateId, Collection<NodeId> jsonQueryExpression);

    ConceptValue getConceptByConceptId(int code, String language, BiFunction<Integer, String, ConceptValue> provider);

    ConceptValue getConceptById(UUID uuid, Function<UUID, ConceptValue> provider);

    ConceptValue getConceptByDescription(
            String description, String language, BiFunction<String, String, ConceptValue> provider);

    TerritoryValue getTerritoryCodeByTwoLetterCode(String territoryAsString, Function<String, TerritoryValue> provider);

    LanguageValue getLanguageByCode(String languageCode, Function<String, LanguageValue> provider);

    class ConceptValue implements Serializable {

        private final UUID id;
        private final int conceptId;
        private final String description;
        private final String language;

        public ConceptValue(UUID id, int conceptId, String description, String language) {
            this.id = id;
            this.conceptId = conceptId;
            this.description = description;
            this.language = language;
        }

        public UUID getId() {
            return id;
        }

        public int getConceptId() {
            return conceptId;
        }

        public String getDescription() {
            return description;
        }

        public String getLanguage() {
            return language;
        }
    }

    class LanguageValue implements Serializable {

        private final String code;
        private final String description;

        public LanguageValue(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    class TerritoryValue implements Serializable {

        private final int code;
        private final String twoletter;
        private final String threeletter;
        private final String text;

        public TerritoryValue(int code, String twoletter, String threeletter, String text) {
            this.code = code;
            this.twoletter = twoletter;
            this.threeletter = threeletter;
            this.text = text;
        }

        public int getCode() {
            return code;
        }

        public String getTwoletter() {
            return twoletter;
        }

        public String getThreeletter() {
            return threeletter;
        }

        public String getText() {
            return text;
        }
    }
}
