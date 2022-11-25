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
package org.ehrbase.aql.sql.queryimpl.attribute;

import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.jsonpathItemAsText;
import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.jsonpathParameters;
import static org.ehrbase.jooq.pg.Routines.jsDvDateTime;
import static org.jooq.impl.DSL.field;

import org.ehrbase.aql.sql.queryimpl.attribute.eventcontext.SimpleEventContextAttribute;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.TableField;

@SuppressWarnings({"java:S3740", "java:S1452"})
public class TemporalWithTimeZone extends SimpleEventContextAttribute {

    private Field timeZoneField;

    public TemporalWithTimeZone(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        //                "ehr.js_dv_date_time("+tableField+"::timestamptz,
        // COALESCE("+timeZoneField+"::text,'UTC'))::json #>>'{value}'")
        return as(field(jsonpathItemAsText(
                jsDvDateTime(tableField, timeZoneField).cast(JSONB.class), jsonpathParameters("value"))));
    }

    public TemporalWithTimeZone useTimeZone(TableField tableField) {
        this.timeZoneField = tableField;
        return this;
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        this.tableField = tableField;
        if (timeZoneField == null) {
            String tzFieldName = tableField.getName().toUpperCase() + "_TZID"; // conventionally
            timeZoneField = field(tableField.getTable().getName() + "." + tzFieldName);
        }
        return this;
    }
}
