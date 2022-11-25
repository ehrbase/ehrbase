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
package org.ehrbase.aql.sql.queryimpl.attribute.ehr;

import static org.ehrbase.jooq.pg.Tables.EHR_;

import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributePath;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributeResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.attribute.ehr.ehrstatus.StatusResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.system.SystemResolver;
import org.ehrbase.aql.sql.queryimpl.value_field.GenericJsonField;
import org.jooq.Field;

@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
public class EhrResolver extends AttributeResolver {

    public static final String EHR_STATUS = "ehr_status";
    public static final String SYSTEM_ID = "system_id";

    public EhrResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
    }

    public Field<?> sqlField(String path) {

        if (path.startsWith(EHR_STATUS)) {
            return new StatusResolver(fieldResolutionContext, joinSetup)
                    .sqlField(new AttributePath(EHR_STATUS).redux(path));
        } else if (path.startsWith(SYSTEM_ID))
            return new SystemResolver(fieldResolutionContext, joinSetup)
                    .sqlField(new AttributePath(SYSTEM_ID).redux(path));

        joinSetup.setJoinEhr(true);

        switch (path) {
            case "ehr_id":
                return new GenericJsonField(fieldResolutionContext, joinSetup)
                        .hierObjectId(JoinBinder.ehrRecordTable.field(EHR_.ID));
            case "ehr_id/value":
                return new EhrIdValue(fieldResolutionContext, joinSetup)
                        .forTableField(NULL_FIELD)
                        .sqlField();
            case "time_created":
                return new GenericJsonField(fieldResolutionContext, joinSetup)
                        .dvDateTime(
                                JoinBinder.ehrRecordTable.field(EHR_.DATE_CREATED),
                                JoinBinder.ehrRecordTable.field(EHR_.DATE_CREATED_TZID));
            case "time_created/value":
                return new GenericJsonField(fieldResolutionContext, joinSetup)
                        .forJsonPath("value")
                        .dvDateTime(
                                JoinBinder.ehrRecordTable.field(EHR_.DATE_CREATED),
                                JoinBinder.ehrRecordTable.field(EHR_.DATE_CREATED_TZID));
            default:
                return new FullEhrJson(fieldResolutionContext, joinSetup)
                        .forJsonPath(path)
                        .sqlField();
        }
    }

    public static boolean isEhrAttribute(String path) {
        if (path == null) return false;

        if (path.startsWith(EHR_STATUS) || path.startsWith(SYSTEM_ID)) return true;
        else return path.matches("ehr_id|ehr_id/value|time_created|time_created/value");
    }
}
