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

import static org.ehrbase.jooq.pg.Tables.CONCEPT;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.util.UUID;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.jooq.pg.tables.records.ConceptRecord;
import org.jooq.Condition;
import org.jooq.Field;

/**
 * access layer to Concepts
 * ETHERCIS Project ehrservice
 * Created by Christian Chevalley on 4/27/2015.
 */
public interface I_ConceptAccess {

    enum ContributionChangeType {
        CREATION(249),
        AMENDMENT(250),
        MODIFICATION(251),
        SYNTHESIS(252),
        UNKNOWN(253),
        DELETED(523);
        final int code;

        ContributionChangeType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * retrieve a concept
     *
     * @param domainAccess SQL context
     * @param conceptId    integer code
     * @param language     language code ('en', 'fr' etc.)
     * @return the record {@link UUID} or null if not found
     */
    static UUID fetchConcept(I_DomainAccess domainAccess, Integer conceptId, String language) {
        return getConceptByConceptId(domainAccess, conceptId, language).getId();
    }

    static DvCodedText fetchConceptText(I_DomainAccess domainAccess, UUID uuid) {
        ConceptRecord conceptRecord = domainAccess.getContext().fetchAny(CONCEPT, CONCEPT.ID.eq(uuid));
        I_KnowledgeCache.ConceptValue concept = getConceptByUuid(domainAccess, uuid);
        return new DvCodedText(
                concept.getDescription(),
                new CodePhrase(new TerminologyId("openehr"), Integer.toString(concept.getConceptId())));
    }

    static String fetchConceptLiteral(I_DomainAccess domainAccess, Integer conceptId, String language) {
        return getConceptByConceptId(domainAccess, conceptId, language).getDescription();
    }

    static String fetchConceptLiteral(I_DomainAccess domainAccess, UUID uuid) {
        return getConceptByUuid(domainAccess, uuid).getDescription();
    }

    /**
     * retrieve a concept
     *
     * @param domainAccess  SQL context
     * @param changeTypeStr String representation of change type
     * @return the record {@link UUID}
     * @throws IllegalArgumentException when the given change type can't be found
     */
    static UUID fetchContributionChangeType(I_DomainAccess domainAccess, String changeTypeStr) {
        ContributionChangeType contributionChangeType = ContributionChangeType.valueOf(changeTypeStr.toUpperCase());
        return fetchContributionChangeType(domainAccess, contributionChangeType);
    }

    static UUID fetchContributionChangeType(
            I_DomainAccess domainAccess, ContributionChangeType contributionChangeType) {
        if (contributionChangeType == null) return null;
        int code = contributionChangeType.getCode();

        return getConceptByConceptId(domainAccess, code, "en").getId();
    }

    private static I_KnowledgeCache.ConceptValue getConceptByConceptId(
            I_DomainAccess domainAccess, int code, String language) {
        return domainAccess
                .getKnowledgeManager()
                .getConceptByConceptId(
                        code, language, (c, l) -> loadConcept(domainAccess, CONCEPT.CONCEPTID, c, language));
    }

    private static I_KnowledgeCache.ConceptValue getConceptByUuid(I_DomainAccess domainAccess, UUID uuid) {
        return domainAccess
                .getKnowledgeManager()
                .getConceptById(uuid, u -> loadConcept(domainAccess, CONCEPT.ID, u, null));
    }

    private static I_KnowledgeCache.ConceptValue getConceptByDescription(
            I_DomainAccess domainAccess, String description, String language) {
        return domainAccess
                .getKnowledgeManager()
                .getConceptByDescription(
                        description, language, (d, l) -> loadConcept(domainAccess, CONCEPT.DESCRIPTION, d, language));
    }

    private static <T> I_KnowledgeCache.ConceptValue loadConcept(
            I_DomainAccess domainAccess, Field<T> field, T value, String language) {

        Condition condition = field.eq(value);
        if (language != null) {
            condition = condition.and(CONCEPT.LANGUAGE.equal(language));
        }
        return domainAccess
                .getContext()
                .fetchOptional(CONCEPT, condition)
                .map(r -> new I_KnowledgeCache.ConceptValue(
                        r.getId(), r.getConceptid(), r.getDescription(), r.getLanguage()))
                .orElseThrow();
    }
}
