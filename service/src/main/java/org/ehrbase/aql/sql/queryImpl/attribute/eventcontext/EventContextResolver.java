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
package org.ehrbase.aql.sql.queryImpl.attribute.eventcontext;


import org.ehrbase.aql.sql.queryImpl.attribute.*;
import org.ehrbase.aql.sql.queryImpl.attribute.eventcontext.facility.FacilityResolver;
import org.ehrbase.aql.sql.queryImpl.attribute.setting.SettingResolver;
import org.ehrbase.aql.sql.queryImpl.value_field.GenericJsonField;
import org.jooq.Field;

import static org.ehrbase.jooq.pg.Tables.EVENT_CONTEXT;

public class EventContextResolver extends AttributeResolver
{
    public EventContextResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
    }

    public Field<?> sqlField(String path){

        if (path.startsWith("context/other_context")) {
            return new ContextOtherContext(fieldResolutionContext, joinSetup).forTableField(NULL_FIELD).sqlField();
        }


        if (path.equals("context/health_care_facility")){
            return new EventContextJson(fieldResolutionContext, joinSetup).forJsonPath("health_care_facility").sqlField();
        }

        if (path.equals("context/health_care_facility/external_ref")){
            return new EventContextJson(fieldResolutionContext, joinSetup).forJsonPath("health_care_facility/external_ref").sqlField();
        }

        if (path.equals("context/health_care_facility/external_ref/id")){
            return new EventContextJson(fieldResolutionContext, joinSetup).forJsonPath("health_care_facility/external_ref/id").sqlField();
        }

        if (path.startsWith("context/health_care_facility")) {
            return new FacilityResolver(fieldResolutionContext, joinSetup).sqlField(new AttributePath("context/health_care_facility").redux(path));
        }

        if (path.startsWith("context/facility")) {
            return new FacilityResolver(fieldResolutionContext, joinSetup).sqlField(new AttributePath("context/health_care_facility").redux(path));
        }

        if (path.startsWith("context/setting")) {
            return new SettingResolver(fieldResolutionContext, joinSetup).sqlField(new AttributePath("context/setting").redux(path));
        }

        switch (path){
            case "context":
                return new EventContextJson(fieldResolutionContext, joinSetup).forTableField(NULL_FIELD).sqlField();
            case "context/start_time":
                return new EventContextJson(fieldResolutionContext, joinSetup).forJsonPath("start_time").forTableField(EVENT_CONTEXT.START_TIME).sqlField();
            case "context/start_time/value":
                return new TemporalWithTimeZone(fieldResolutionContext, joinSetup).forTableField(EVENT_CONTEXT.START_TIME).sqlField();
            case "context/end_time":
                return new EventContextJson(fieldResolutionContext, joinSetup).forJsonPath("end_time").forTableField(EVENT_CONTEXT.END_TIME).sqlField();
            case "context/end_time/value":
                return new TemporalWithTimeZone(fieldResolutionContext, joinSetup).forTableField(EVENT_CONTEXT.END_TIME).sqlField();
            case "context/location":
                return new SimpleEventContextAttribute(fieldResolutionContext, joinSetup).forTableField(EVENT_CONTEXT.LOCATION).sqlField();
            case "context/setting":
                return new SimpleEventContextAttribute(fieldResolutionContext, joinSetup).forTableField(EVENT_CONTEXT.LOCATION).sqlField();


        }
        throw new IllegalArgumentException("Unresolved context path:"+path);
    }
}
