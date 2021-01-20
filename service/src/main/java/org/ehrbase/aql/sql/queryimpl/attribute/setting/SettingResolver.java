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
package org.ehrbase.aql.sql.queryimpl.attribute.setting;

import org.ehrbase.aql.sql.queryimpl.QueryImplConstants;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributeResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.attribute.eventcontext.EventContextJson;
import org.jooq.Field;

import static org.ehrbase.jooq.pg.tables.EventContext.EVENT_CONTEXT;
@SuppressWarnings({"java:S3740","java:S1452"})
public class SettingResolver extends AttributeResolver
{

    public static final String MAPPINGS = "mappings";

    public SettingResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
    }

    public Field<?> sqlField(String path){

        if (path.isEmpty())
            return new EventContextJson(fieldResolutionContext, joinSetup).forJsonPath("setting").sqlField();


        if (!path.equals(MAPPINGS) && path.startsWith(MAPPINGS)) {
            path = path.substring(path.indexOf(MAPPINGS)+ MAPPINGS.length()+1);
            //we insert a tag to indicate that the path operates on a json array
            return new EventContextJson(fieldResolutionContext, joinSetup).forJsonPath("setting/mappings/"+ QueryImplConstants.AQL_NODE_ITERATIVE_MARKER+"/" + path).forTableField(EVENT_CONTEXT.SETTING).sqlField();
        }

        switch (path){
            case "value":
                return new SettingAttribute(fieldResolutionContext, joinSetup).forJsonPath(path).forTableField(EVENT_CONTEXT.SETTING).sqlField();
            case "defining_code":
            case "formatting":
            case "language":
            case "encoding":
                return new EventContextJson(fieldResolutionContext, joinSetup).forJsonPath("setting/"+path).forTableField(EVENT_CONTEXT.SETTING).sqlField();
            case MAPPINGS:
                fieldResolutionContext.setJsonDatablock(true);
                return new EventContextJson(fieldResolutionContext, joinSetup).forJsonPath("setting/"+path).forTableField(EVENT_CONTEXT.SETTING).sqlField();
            case "defining_code/terminology_id":
            case "defining_code/terminology_id/value":
                return new SettingAttribute(fieldResolutionContext, joinSetup).forJsonPath(path).forTableField(EVENT_CONTEXT.SETTING).sqlField();
            case "defining_code/code_string":
                return new SettingAttribute(fieldResolutionContext, joinSetup).forJsonPath(path).forTableField(EVENT_CONTEXT.SETTING).sqlField();
            default:
                throw new IllegalArgumentException("Unresolved context/facility attribute path:"+path);
        }
    }
}
