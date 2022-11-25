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
package org.ehrbase.aql.sql.queryimpl.attribute.eventcontext;

import static org.ehrbase.jooq.pg.Tables.EVENT_CONTEXT;

import org.ehrbase.aql.sql.queryimpl.attribute.*;
import org.ehrbase.aql.sql.queryimpl.attribute.eventcontext.facility.FacilityResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.eventcontext.participations.ParticipationsJson;
import org.ehrbase.aql.sql.queryimpl.attribute.setting.SettingResolver;
import org.jooq.Field;

@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
public class EventContextResolver extends AttributeResolver {
    public static final String CONTEXT = "context";
    public static final String START_TIME = "start_time";
    public static final String END_TIME = "end_time";
    public static final String HEALTH_CARE_FACILITY = "health_care_facility";
    public static final String EXTERNAL_REF = "external_ref";

    public static final String OTHER_CONTEXT = "/other_context";
    public static final String CONTEXT_OTHER_CONTEXT = CONTEXT + OTHER_CONTEXT;
    public static final String CONTEXT_PARTICIPATIONS = CONTEXT + "/participations";

    public static final String CONTEXT_HEALTH_CARE_FACILITY = CONTEXT + "/" + HEALTH_CARE_FACILITY;
    public static final String HEALTH_CARE_FACILITY_EXTERNAL_REF = HEALTH_CARE_FACILITY + "/" + EXTERNAL_REF;
    public static final String CONTEXT_HEALTH_CARE_FACILITY_EXTERNAL_REF =
            CONTEXT + "/" + HEALTH_CARE_FACILITY_EXTERNAL_REF;
    public static final String CONTEXT_HEALTH_CARE_FACILITY_EXTERNAL_REF_ID =
            CONTEXT_HEALTH_CARE_FACILITY_EXTERNAL_REF + "/id";
    public static final String HEALTH_CARE_FACILITY_EXTERNAL_REF_ID = HEALTH_CARE_FACILITY_EXTERNAL_REF + "/id";

    public static final String CONTEXT_FACILITY = CONTEXT + "/facility";
    public static final String CONTEXT_SETTING = CONTEXT + "/setting";

    public static final String CONTEXT_START_TIME = CONTEXT + "/" + START_TIME;
    public static final String CONTEXT_START_TIME_VALUE = CONTEXT_START_TIME + "/value";
    public static final String CONTEXT_END_TIME = CONTEXT + "/" + END_TIME;
    public static final String CONTEXT_END_TIME_VALUE = CONTEXT_END_TIME + "/value";

    public static final String CONTEXT_LOCATION = CONTEXT + "/location";

    public EventContextResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
    }

    public Field<?> sqlField(String path) {

        if (path.startsWith(CONTEXT_OTHER_CONTEXT)) {
            return new ContextOtherContext(fieldResolutionContext, joinSetup)
                    .forTableField(NULL_FIELD)
                    .sqlField();
        }

        if (path.startsWith(CONTEXT_PARTICIPATIONS)) {
            return new ParticipationsJson(fieldResolutionContext, joinSetup)
                    .forJsonPath(new AttributePath(CONTEXT_PARTICIPATIONS).redux(path))
                    .sqlField();
        }

        if (path.equals(CONTEXT_HEALTH_CARE_FACILITY_EXTERNAL_REF)) {
            return new EventContextJson(fieldResolutionContext, joinSetup)
                    .forJsonPath(HEALTH_CARE_FACILITY_EXTERNAL_REF)
                    .sqlField();
        }

        if (path.equals(CONTEXT_HEALTH_CARE_FACILITY_EXTERNAL_REF_ID)) {
            return new EventContextJson(fieldResolutionContext, joinSetup)
                    .forJsonPath(HEALTH_CARE_FACILITY_EXTERNAL_REF_ID)
                    .sqlField();
        }

        if (path.startsWith(CONTEXT_HEALTH_CARE_FACILITY)) {
            return new FacilityResolver(fieldResolutionContext, joinSetup)
                    .sqlField(new AttributePath(CONTEXT_HEALTH_CARE_FACILITY).redux(path));
        }

        if (path.startsWith(CONTEXT_FACILITY)) {
            return new FacilityResolver(fieldResolutionContext, joinSetup)
                    .sqlField(new AttributePath(CONTEXT_HEALTH_CARE_FACILITY).redux(path));
        }

        if (path.startsWith(CONTEXT_SETTING)) {
            return new SettingResolver(fieldResolutionContext, joinSetup)
                    .sqlField(new AttributePath(CONTEXT_SETTING).redux(path));
        }

        switch (path) {
            case CONTEXT:
                return new EventContextJson(fieldResolutionContext, joinSetup)
                        .forTableField(NULL_FIELD)
                        .sqlField();
            case CONTEXT_START_TIME:
                return new EventContextJson(fieldResolutionContext, joinSetup)
                        .forJsonPath(START_TIME)
                        .forTableField(EVENT_CONTEXT.START_TIME)
                        .sqlField();
            case CONTEXT_START_TIME_VALUE:
                return new TemporalWithTimeZone(fieldResolutionContext, joinSetup)
                        .forTableField(EVENT_CONTEXT.START_TIME)
                        .sqlField();
            case CONTEXT_END_TIME:
                return new EventContextJson(fieldResolutionContext, joinSetup)
                        .forJsonPath(END_TIME)
                        .forTableField(EVENT_CONTEXT.END_TIME)
                        .sqlField();
            case CONTEXT_END_TIME_VALUE:
                return new TemporalWithTimeZone(fieldResolutionContext, joinSetup)
                        .forTableField(EVENT_CONTEXT.END_TIME)
                        .sqlField();
            case CONTEXT_LOCATION:
                return new SimpleEventContextAttribute(fieldResolutionContext, joinSetup)
                        .forTableField(EVENT_CONTEXT.LOCATION)
                        .sqlField();
            default:
                throw new IllegalStateException("Unhandled path:" + path);
        }
    }
}
