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
import java.util.function.Function;
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

    private static final ExtractedColumnResultPostprocessor OV_DATETIME_DV_PP =
            create(columnValue -> new DvDateTime((TemporalAccessor) columnValue));
    private static final ExtractedColumnResultPostprocessor OV_DATETIME_VALUE_PP =
            create(columnValue -> OpenEHRDateTimeSerializationUtils.formatDateTime((TemporalAccessor) columnValue));
    private static final ExtractedColumnResultPostprocessor DV_TEXT_PP =
            create(columnValue -> new DvText((String) columnValue));
    private static final ExtractedColumnResultPostprocessor CHANGE_TYPE_PP = new ExtractedColumnResultPostprocessor(
            columnValue -> contributionChangeTypeAsDvCodedText((ContributionChangeType) columnValue));
    private static final ExtractedColumnResultPostprocessor CHANGE_TYPE_VALUE_PP = create(
            columnValue -> ((ContributionChangeType) columnValue).getLiteral().toLowerCase());
    private static final ExtractedColumnResultPostprocessor CHANGE_TYPE_CS_PP = new ExtractedColumnResultPostprocessor(
            columnValue -> ChangeTypeUtils.getCodeByJooqChangeType((ContributionChangeType) columnValue));
    private static final ExtractedColumnResultPostprocessor ROOT_CONCEPT_PP = new ExtractedColumnResultPostprocessor(
            columnValue -> AslRmTypeAndConcept.ARCHETYPE_PREFIX + RmConstants.COMPOSITION + columnValue);
    private static final ExtractedColumnResultPostprocessor ARCHETYPE_NODE_ID_PP =
            create(columnValue -> restoreArchetypeNodeId((Record) columnValue));
    private static final ExtractedColumnResultPostprocessor SYSTEM_ID_PP =
            create(columnValue -> new HierObjectId((String) columnValue));
    private static final ExtractedColumnResultPostprocessor NOOP_PP = create(columnValue -> columnValue);

    private final Function<Object, Object> processorOp;

    private ExtractedColumnResultPostprocessor(Function<Object, Object> processorOp) {
        this.processorOp = processorOp;
    }

    private static ExtractedColumnResultPostprocessor create(Function<Object, Object> processorOp) {
        return new ExtractedColumnResultPostprocessor(processorOp);
    }

    public static ExtractedColumnResultPostprocessor get(
            AslExtractedColumn extractedColumn, KnowledgeCacheService knowledgeCache, String nodeName) {
        return switch (extractedColumn) {
            case OV_TIME_COMMITTED_DV, EHR_TIME_CREATED_DV -> OV_DATETIME_DV_PP;
            case OV_TIME_COMMITTED, EHR_TIME_CREATED -> OV_DATETIME_VALUE_PP;
            case AD_DESCRIPTION_DV -> DV_TEXT_PP;
            case AD_CHANGE_TYPE_DV -> CHANGE_TYPE_PP;
            case AD_CHANGE_TYPE_VALUE, AD_CHANGE_TYPE_PREFERRED_TERM -> CHANGE_TYPE_VALUE_PP;
            case AD_CHANGE_TYPE_CODE_STRING -> CHANGE_TYPE_CS_PP;
            // the root is always archetyped
            case ROOT_CONCEPT -> ROOT_CONCEPT_PP;
            case ARCHETYPE_NODE_ID -> ARCHETYPE_NODE_ID_PP;
            case EHR_SYSTEM_ID_DV -> SYSTEM_ID_PP;
            case NAME_VALUE,
                    EHR_ID,
                    OV_CONTRIBUTION_ID,
                    AD_SYSTEM_ID,
                    AD_DESCRIPTION_VALUE,
                    AD_CHANGE_TYPE_TERMINOLOGY_ID_VALUE,
                    EHR_SYSTEM_ID -> NOOP_PP;
            case TEMPLATE_ID ->
                create(columnValue ->
                        knowledgeCache.findTemplateIdByUuid((UUID) columnValue).orElse(null));
            case VO_ID -> create(columnValue -> restoreVoId((Record) columnValue, nodeName));
        };
    }

    @Override
    public Object postProcessColumn(Object columnValue) {
        if (columnValue == null) {
            return null;
        }
        return processorOp.apply(columnValue);
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

    private static DvCodedText contributionChangeTypeAsDvCodedText(ContributionChangeType changeType) {
        return new DvCodedText(
                changeType.getLiteral().toLowerCase(),
                new CodePhrase(
                        new TerminologyId("openehr"),
                        ChangeTypeUtils.getCodeByJooqChangeType(changeType),
                        changeType.getLiteral().toLowerCase()));
    }
}
