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

import org.ehrbase.aql.sql.queryImpl.attribute.AttributePath;
import org.ehrbase.aql.sql.queryImpl.attribute.AttributeResolver;
import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryImpl.attribute.setting.SettingResolver;
import org.jooq.Field;

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
            return new SettingResolver(fieldResolutionContext, joinSetup).sqlField(new AttributePath("category").redux(path));


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
