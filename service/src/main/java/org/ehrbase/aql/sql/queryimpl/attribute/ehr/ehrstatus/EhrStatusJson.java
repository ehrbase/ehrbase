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
package org.ehrbase.aql.sql.queryimpl.attribute.ehr.ehrstatus;

import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.IRMObjectAttribute;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.value_field.GenericJsonField;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import java.util.Optional;

import static org.ehrbase.jooq.pg.Routines.jsEhrStatus;
import static org.ehrbase.jooq.pg.Tables.STATUS;
@SuppressWarnings({"java:S3776","java:S3740"})
public class EhrStatusJson extends EhrStatusAttribute {

    protected Optional<String> jsonPath = Optional.empty();

    public EhrStatusJson(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        fieldContext.setJsonDatablock(true);
        fieldContext.setRmType("EHR_STATUS");
        //query the json representation of EVENT_CONTEXT and cast the result as TEXT
        Field jsonEhrStatusField;
        if (jsonPath.isPresent())
            jsonEhrStatusField =  new GenericJsonField(fieldContext, joinSetup).forJsonPath(jsonPath.get()).ehrStatus(JoinBinder.statusRecordTable.field(STATUS.EHR_ID));
        else
            jsonEhrStatusField = DSL.field(jsEhrStatus(JoinBinder.statusRecordTable.field(STATUS.EHR_ID)).cast(String.class));


        return as(DSL.field(jsonEhrStatusField));
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        return this;
    }

    public EhrStatusJson forJsonPath(String jsonPath){
        if (jsonPath == null || jsonPath.isEmpty()) {
            this.jsonPath = Optional.empty();
            return this;
        }
        this.jsonPath = Optional.of(jsonPath);
        return this;
    }
}
