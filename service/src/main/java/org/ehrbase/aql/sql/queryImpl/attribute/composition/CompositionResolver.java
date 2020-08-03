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
package org.ehrbase.aql.sql.queryImpl.attribute.composition;

import org.ehrbase.aql.sql.binding.I_JoinBinder;
import org.ehrbase.aql.sql.queryImpl.attribute.*;
import org.ehrbase.aql.sql.queryImpl.attribute.concept.ConceptResolver;
import org.ehrbase.aql.sql.queryImpl.attribute.setting.SettingResolver;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.impl.DSL;

import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.COMPOSITION;
import static org.ehrbase.jooq.pg.Tables.ENTRY;

public class CompositionResolver extends AttributeResolver
{

    public CompositionResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
    }

    public Field<?> sqlField(String path){

        if (path == null || path.isEmpty())
            return new FullCompositionJson(fieldResolutionContext, joinSetup).sqlField();

        if (path.startsWith("category"))
            return new ConceptResolver(fieldResolutionContext, joinSetup).forTableField(ENTRY.CATEGORY).sqlField(new AttributePath("category").redux(path));

        if (path.startsWith("feeder_audit")) {
            joinSetup.setJoinComposition(true);
            fieldResolutionContext.getVariableDefinition();
            return DSL.field(I_JoinBinder.compositionRecordTable.field("feeder_audit", JSONB.class)
                    + "::json #>>"
                    + new GenericJsonPath(new AttributePath("feeder_audit").redux(path)).jqueryPath())
                    .as(DSL.field(path)); //if missing, cannot assign the result to the respective column!
        }


        switch (path){
            case "uid/value":
                return new CompositionUidValue(fieldResolutionContext, joinSetup).forTableField(NULL_FIELD).sqlField();
            case "name/value":
                return new CompositionName(fieldResolutionContext, joinSetup).forTableField(NULL_FIELD).sqlField();
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
        }
        //else assume a partial json path

        return new FullCompositionJson(fieldResolutionContext, joinSetup).forJsonPath(path).sqlField();
    }
}
