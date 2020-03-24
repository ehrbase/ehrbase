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

import org.ehrbase.jooq.pg.tables.records.ConceptRecord;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.support.identification.TerminologyId;

import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.CONCEPT;

/**
 * access layer to Concepts
 * ETHERCIS Project ehrservice
 * Created by Christian Chevalley on 4/27/2015.
 */
public interface I_ConceptAccess {

    enum ContributionChangeType {
        CREATION(249), AMENDMENT(250), MODIFICATION(251), SYNTHESIS(252), UNKNOWN(253), DELETED(523);
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
        return domainAccess.getContext().fetchAny(CONCEPT, CONCEPT.CONCEPTID.eq(conceptId).and(CONCEPT.LANGUAGE.equal(language))).getId();
    }

    static DvCodedText fetchConceptText(I_DomainAccess domainAccess, UUID uuid) {
        ConceptRecord conceptRecord = domainAccess.getContext().fetchAny(CONCEPT, CONCEPT.ID.eq(uuid));
        return new DvCodedText(conceptRecord.getDescription(), new CodePhrase(new TerminologyId("openehr"), "" + conceptRecord.getConceptid()));
    }

    static String fetchConceptLiteral(I_DomainAccess domainAccess, Integer conceptId, String language) {
        return domainAccess.getContext().fetchAny(CONCEPT, CONCEPT.CONCEPTID.eq(conceptId).and(CONCEPT.LANGUAGE.equal(language))).getDescription();
    }

    static String fetchConceptLiteral(I_DomainAccess domainAccess, UUID uuid) {
        return domainAccess.getContext().fetchAny(CONCEPT, CONCEPT.ID.eq(uuid)).getDescription();
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
        int code = contributionChangeType.getCode();
        return domainAccess.getContext().fetchAny(CONCEPT, CONCEPT.CONCEPTID.eq(code).and(CONCEPT.LANGUAGE.equal("en"))).getId();
    }

    static UUID fetchContributionChangeType(I_DomainAccess domainAccess, ContributionChangeType contributionChangeType) {
        if (contributionChangeType == null)
            return null;
        int code = contributionChangeType.getCode();
        return domainAccess.getContext().fetchAny(CONCEPT, CONCEPT.CONCEPTID.eq(code).and(CONCEPT.LANGUAGE.equal("en"))).getId();
    }

    /**
     * convenience statics to get VERSION.lifecycle_state
     * DRAFT (code: 244)
     *
     * @param domainAccess SQL context
     * @return the record {@link UUID}
     */
    static UUID getVlcsDraft(I_DomainAccess domainAccess) {
        return domainAccess.getContext().fetchAny(CONCEPT, CONCEPT.CONCEPTID.eq(244).and(CONCEPT.LANGUAGE.equal("en"))).getId();
    }

    /**
     * convenience statics to get VERSION.lifecycle_state
     * ACTIVE (code: 245)
     *
     * @param domainAccess SQL context
     * @return the record {@link UUID}
     */
    static UUID getVlcsActive(I_DomainAccess domainAccess) {
        return domainAccess.getContext().fetchAny(CONCEPT, CONCEPT.CONCEPTID.eq(245).and(CONCEPT.LANGUAGE.equal("en"))).getId();
    }

    /**
     * convenience statics to get VERSION.lifecycle_state
     * INACTIVE (code: 246)
     *
     * @param domainAccess SQL context
     * @return the record {@link UUID}
     */
    static UUID getVlcsInactive(I_DomainAccess domainAccess) {
        return domainAccess.getContext().fetchAny(CONCEPT, CONCEPT.CONCEPTID.eq(246).and(CONCEPT.LANGUAGE.equal("en"))).getId();
    }

    /**
     * convenience statics to get VERSION.lifecycle_state
     * AWAITING APPROVAL (code: 247)
     *
     * @param domainAccess SQL context
     * @return the record {@link UUID}
     */
    static UUID getVlcsAwaitingApproval(I_DomainAccess domainAccess) {
        return domainAccess.getContext().fetchAny(CONCEPT, CONCEPT.CONCEPTID.eq(247).and(CONCEPT.LANGUAGE.equal("en"))).getId();
    }
}
