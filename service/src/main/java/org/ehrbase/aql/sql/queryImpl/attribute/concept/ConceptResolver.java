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
package org.ehrbase.aql.sql.queryImpl.attribute.concept;

import org.ehrbase.aql.sql.queryImpl.attribute.AttributeResolver;
import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.I_RMObjectAttribute;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryImpl.attribute.eventcontext.EventContextJson;
import org.ehrbase.aql.sql.queryImpl.attribute.setting.SettingAttribute;
import org.jooq.Field;
import org.jooq.TableField;

import static org.ehrbase.jooq.pg.tables.EventContext.EVENT_CONTEXT;

public class ConceptResolver extends AttributeResolver
{

    TableField tableField;

    public ConceptResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
    }

    public Field<?> sqlField(String path){

        if (path.isEmpty())
            return new EventContextJson(fieldResolutionContext, joinSetup).forJsonPath("setting").sqlField();

        switch (path){
            case "value":
                return new SettingAttribute(fieldResolutionContext, joinSetup).forJsonPath(path).forTableField(tableField).sqlField();
            case "defining_code":
                return new EventContextJson(fieldResolutionContext, joinSetup).forJsonPath("setting/"+path).forTableField(tableField).sqlField();
            case "defining_code/terminology_id":
            case "defining_code/terminology_id/value":
                return new SettingAttribute(fieldResolutionContext, joinSetup).forJsonPath(path).forTableField(tableField).sqlField();
            case "defining_code/code_string":
                return new SettingAttribute(fieldResolutionContext, joinSetup).forJsonPath(path).forTableField(tableField).sqlField();


        }
        throw new IllegalArgumentException("Unresolved context/facility attribute path:"+path);
    }

    public ConceptResolver forTableField(TableField tableField) {
        this.tableField = tableField;
        return this;
    }
}
