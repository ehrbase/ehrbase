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
package org.ehrbase.openehr.aqlengine.sql.postprocessor;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.time.temporal.TemporalAccessor;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.openehr.aqlengine.ChangeTypeUtils;
import org.ehrbase.openehr.aqlengine.asl.model.AslExtractedColumn;
import org.ehrbase.openehr.aqlengine.asl.model.AslRmTypeAndConcept;
import org.ehrbase.openehr.dbformat.RmTypeAlias;
import org.ehrbase.openehr.sdk.util.OpenEHRDateTimeSerializationUtils;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.jooq.Record;

/**
 * Handles a result column based on the given extracted column (includes complex extracted columns).
 */
public class ExtractedColumnResultPostprocessor implements AqlSqlResultPostprocessor {

    private final AslExtractedColumn extractedColumn;
    private final KnowledgeCacheService knowledgeCache;
    private final String nodeName;

    public ExtractedColumnResultPostprocessor(
            AslExtractedColumn extractedColumn, KnowledgeCacheService knowledgeCache, String nodeName) {
        this.extractedColumn = extractedColumn;
        this.knowledgeCache = knowledgeCache;
        this.nodeName = nodeName;
    }

    @Override
    public Object postProcessColumn(Object columnValue) {
        if (columnValue == null) {
            return null;
        }

        return switch (extractedColumn) {
            case TEMPLATE_ID -> knowledgeCache
                    .findTemplateIdByUuid((UUID) columnValue)
                    .orElse(null);
            case OV_TIME_COMMITTED_DV, EHR_TIME_CREATED_DV -> new DvDateTime((TemporalAccessor) columnValue);
            case OV_TIME_COMMITTED, EHR_TIME_CREATED -> OpenEHRDateTimeSerializationUtils.formatDateTime(
                    (TemporalAccessor) columnValue);
            case AD_DESCRIPTION_DV -> new DvText((String) columnValue);
            case AD_CHANGE_TYPE_DV -> contributionChangeTypeAsDvCodedText((ContributionChangeType) columnValue);
            case AD_CHANGE_TYPE_VALUE, AD_CHANGE_TYPE_PREFERRED_TERM -> ((ContributionChangeType) columnValue)
                    .getLiteral()
                    .toLowerCase();
            case AD_CHANGE_TYPE_CODE_STRING -> ChangeTypeUtils.getCodeByJooqChangeType(
                    (ContributionChangeType) columnValue);
            case VO_ID -> restoreVoId((Record) columnValue, nodeName);
                // the root is always archetyped
            case ROOT_CONCEPT -> AslRmTypeAndConcept.ARCHETYPE_PREFIX + RmConstants.COMPOSITION + columnValue;
            case ARCHETYPE_NODE_ID -> restoreArchetypeNodeId((Record) columnValue);
            case EHR_SYSTEM_ID_DV -> new HierObjectId((String) columnValue);
            case NAME_VALUE,
                    EHR_ID,
                    OV_CONTRIBUTION_ID,
                    AD_SYSTEM_ID,
                    AD_DESCRIPTION_VALUE,
                    AD_CHANGE_TYPE_TERMINOLOGY_ID_VALUE,
                    EHR_SYSTEM_ID -> columnValue;
        };
    }

    private static String restoreArchetypeNodeId(Record srcRow) {
        String entityConcept = (String) srcRow.get(0);
        if (!entityConcept.startsWith(".")) {
            // at or id code
            return entityConcept;
        }
        String rmType = RmTypeAlias.getRmType((String) srcRow.get(1));
        return AslRmTypeAndConcept.ARCHETYPE_PREFIX + rmType + entityConcept;
    }

    private static String restoreVoId(Record srcRow, String nodeName) {
        Object id = srcRow.get(0);
        if (id == null) {
            return null;
        }
        return id + "::" + nodeName + "::" + srcRow.get(1);
    }

    @Nonnull
    private static DvCodedText contributionChangeTypeAsDvCodedText(ContributionChangeType changeType) {
        return new DvCodedText(
                changeType.getLiteral().toLowerCase(),
                new CodePhrase(
                        new TerminologyId("openehr"),
                        ChangeTypeUtils.getCodeByJooqChangeType(changeType),
                        changeType.getLiteral().toLowerCase()));
    }
}
