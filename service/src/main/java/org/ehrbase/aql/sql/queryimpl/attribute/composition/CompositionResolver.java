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
package org.ehrbase.aql.sql.queryimpl.attribute.composition;

import static org.ehrbase.jooq.pg.Tables.ENTRY;

import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.aql.sql.queryimpl.QueryImplConstants;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributePath;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributeResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.attribute.concept.ConceptResolver;
import org.ehrbase.aql.sql.queryimpl.value_field.GenericJsonField;
import org.jooq.Field;

@SuppressWarnings("java:S1452")
public class CompositionResolver extends AttributeResolver {

    public static final String FEEDER_AUDIT = "feeder_audit";
    public static final String FEEDER_SYSTEM_IDS = "feeder_system_item_ids";

    public CompositionResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
        joinSetup.setJoinComposition(true);
    }

    public Field<?> sqlField(String path) {

        if (path == null || path.isEmpty())
            return new FullCompositionJson(fieldResolutionContext, joinSetup).sqlField();

        if (path.startsWith("category"))
            return new ConceptResolver(fieldResolutionContext, joinSetup)
                    .forTableField(ENTRY.CATEGORY)
                    .sqlField(new AttributePath("category").redux(path));

        if (path.startsWith(FEEDER_AUDIT)) {
            Field<?> retField;
            if (path.contains(FEEDER_SYSTEM_IDS) && !path.endsWith(FEEDER_SYSTEM_IDS)) {
                path = path.substring(path.indexOf(FEEDER_SYSTEM_IDS) + FEEDER_SYSTEM_IDS.length() + 1);
                // we insert a tag to indicate that the path operates on a json array
                fieldResolutionContext.setUsingSetReturningFunction(true); // to generate lateral join
                retField = new GenericJsonField(fieldResolutionContext, joinSetup)
                        .forJsonPath(
                                FEEDER_SYSTEM_IDS + "/" + QueryImplConstants.AQL_NODE_ITERATIVE_MARKER + "/" + path)
                        .feederAudit(JoinBinder.compositionRecordTable.field(FEEDER_AUDIT));
            } else
                retField = new GenericJsonField(fieldResolutionContext, joinSetup)
                        .forJsonPath(FEEDER_AUDIT, path)
                        .feederAudit(JoinBinder.compositionRecordTable.field(FEEDER_AUDIT));

            String regexpTerminalValues = ".*(id|issuer|assigner|type|formalism|system_id|name|namespace|value)$";
            if (path.matches(regexpTerminalValues)) fieldResolutionContext.setJsonDatablock(false);

            return retField;
        }

        switch (path) {
            case "uid":
                return new FullCompositionJson(fieldResolutionContext, joinSetup)
                        .forJsonPath(new String[] {"uid", ""})
                        .sqlField();
            case "uid/value":
                // this is an optimization
                // in WHERE clause, we can only select on UUID without versioning as it is not supported at this time
                if (fieldResolutionContext.getClause().equals(IQueryImpl.Clause.WHERE))
                    return new CompositionUidValue(fieldResolutionContext, joinSetup)
                            .forTableField(NULL_FIELD)
                            .sqlField();
                else
                    // in SELECT we do return the full versioned composition id (as a UID_BASED_ID)
                    return new FullCompositionJson(fieldResolutionContext, joinSetup)
                            .forJsonPath(new String[] {"uid", "value"})
                            .sqlField();
            case "name":
                // we force the path to use the single attribute 'value' from the name encoding
                return new FullCompositionJson(fieldResolutionContext, joinSetup)
                        .forJsonPath(new String[] {"name", ""})
                        .sqlField();
            case "name/value":
                // we force the path to use the single attribute 'value' from the name encoding
                return new GenericJsonField(fieldResolutionContext, joinSetup)
                        .forJsonPath(new String[] {"value", ""})
                        .dvCodedText(ENTRY.NAME);
            case "archetype_node_id":
                return new SimpleCompositionAttribute(fieldResolutionContext, joinSetup)
                        .forTableField(ENTRY.ARCHETYPE_ID)
                        .sqlField();
            case "template_id":
                return new SimpleCompositionAttribute(fieldResolutionContext, joinSetup)
                        .forTableField(ENTRY.TEMPLATE_ID)
                        .sqlField();
            case "archetype_details/template_id/value":
                return new SimpleCompositionAttribute(fieldResolutionContext, joinSetup)
                        .forTableField(ENTRY.TEMPLATE_ID)
                        .sqlField();
            default:
                break;
        }
        // else assume a partial json path

        return new FullCompositionJson(fieldResolutionContext, joinSetup)
                .forJsonPath(path)
                .sqlField();
    }
}
