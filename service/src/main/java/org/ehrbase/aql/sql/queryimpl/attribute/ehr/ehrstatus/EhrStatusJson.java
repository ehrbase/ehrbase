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
package org.ehrbase.aql.sql.queryimpl.attribute.ehr.ehrstatus;

import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.jsonpathItemAsText;
import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.jsonpathParameters;
import static org.ehrbase.jooq.pg.Routines.jsEhrStatus2;
import static org.ehrbase.jooq.pg.Tables.STATUS;

import java.util.Optional;
import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.IRMObjectAttribute;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.value_field.GenericJsonField;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.TableField;
import org.jooq.impl.DSL;

@SuppressWarnings({"java:S3776", "java:S3740"})
public class EhrStatusJson extends EhrStatusAttribute {

    protected Optional<String> jsonPath = Optional.empty();

    public EhrStatusJson(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        fieldContext.setJsonDatablock(true);
        fieldContext.setRmType("EHR_STATUS");

        // query the json representation of EVENT_CONTEXT and cast the result as TEXT
        Field jsonEhrStatusField;
        if (jsonPath.isPresent()) {
            if (jsonPath.get().startsWith("name"))
                jsonEhrStatusField = new GenericJsonField(fieldContext, joinSetup)
                        .forJsonPath(jsonPath.get().split("/"))
                        .ehrStatus(JoinBinder.statusRecordTable.field(STATUS.EHR_ID));
            else if (jsonPath.get().contains("uid")) {
                // this is required since jOOQ doesn't allow (easily) to convert a Field to a TableField
                jsonEhrStatusField = DSL.field(jsonpathItemAsText(
                        fieldContext.getContext().configuration(),
                        jsEhrStatus2(
                                        JoinBinder.statusRecordTable.field(STATUS.EHR_ID),
                                        DSL.val(fieldContext.getServerNodeId()))
                                .cast(JSONB.class),
                        jsonpathParameters(jsonPath.get().replace("/", ","))));
            } else
                jsonEhrStatusField = new GenericJsonField(fieldContext, joinSetup)
                        .forJsonPath(jsonPath.get())
                        .ehrStatus(JoinBinder.statusRecordTable.field(STATUS.EHR_ID));
        } else
            jsonEhrStatusField = DSL.field(jsEhrStatus2(
                            JoinBinder.statusRecordTable.field(STATUS.EHR_ID), DSL.val(fieldContext.getServerNodeId()))
                    .cast(String.class));

        return as(DSL.field(jsonEhrStatusField));
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        return this;
    }

    public EhrStatusJson forJsonPath(String jsonPath) {
        if (jsonPath == null || jsonPath.isEmpty()) {
            this.jsonPath = Optional.empty();
            return this;
        }
        this.jsonPath = Optional.of(jsonPath);
        return this;
    }
}
