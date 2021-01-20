/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 * This file is part of project EHRbase
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
package org.ehrbase.aql.sql.queryimpl.attribute.composition;

import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributePath;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributeResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.attribute.concept.ConceptResolver;
import org.ehrbase.aql.sql.queryimpl.value_field.GenericJsonField;
import org.jooq.Field;

import static org.ehrbase.jooq.pg.Tables.COMPOSITION;
import static org.ehrbase.jooq.pg.Tables.ENTRY;

@SuppressWarnings("java:S1452")
public class CompositionResolver extends AttributeResolver
{

    public static final String FEEDER_AUDIT = "feeder_audit";

    public CompositionResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
        joinSetup.setJoinComposition(true);
    }

    public Field<?> sqlField(String path){

        if (path == null || path.isEmpty())
            return new FullCompositionJson(fieldResolutionContext, joinSetup).sqlField();

        if (path.startsWith("category"))
            return new ConceptResolver(fieldResolutionContext, joinSetup).forTableField(ENTRY.CATEGORY).sqlField(new AttributePath("category").redux(path));

        if (path.startsWith(FEEDER_AUDIT)) {

            Field<?> retField = new GenericJsonField(fieldResolutionContext, joinSetup)
                    .forJsonPath(FEEDER_AUDIT, path)
                    .feederAudit(JoinBinder.compositionRecordTable.field(FEEDER_AUDIT));

            String regexpTerminalValues = ".*(id|issuer|assigner|type|original_content|system_id|name|namespace|value)$";
            if (path.matches(regexpTerminalValues))
                fieldResolutionContext.setJsonDatablock(false);

            return retField;
        }


        switch (path){
            case "uid/value":
                return new CompositionUidValue(fieldResolutionContext, joinSetup).forTableField(NULL_FIELD).sqlField();
            case "name/value":
                return new GenericJsonField(fieldResolutionContext, joinSetup).forJsonPath("value").dvCodedText(ENTRY.NAME);
            case "archetype_node_id":
                return new SimpleCompositionAttribute(fieldResolutionContext, joinSetup).forTableField(ENTRY.ARCHETYPE_ID).sqlField();
            case "template_id":
                return new SimpleCompositionAttribute(fieldResolutionContext, joinSetup).forTableField(ENTRY.TEMPLATE_ID).sqlField();
            case "language/value":
                return new SimpleCompositionAttribute(fieldResolutionContext, joinSetup).forTableField(COMPOSITION.LANGUAGE).sqlField();
            case "territory/value":
                return new SimpleCompositionAttribute(fieldResolutionContext, joinSetup).forTableField(COMPOSITION.TERRITORY).sqlField();
            case "archetype_details/template_id/value":
                return new SimpleCompositionAttribute(fieldResolutionContext, joinSetup).forTableField(ENTRY.TEMPLATE_ID).sqlField();
            default:
                break;
        }
        //else assume a partial json path

        return new FullCompositionJson(fieldResolutionContext, joinSetup).forJsonPath(path).sqlField();
    }
}
