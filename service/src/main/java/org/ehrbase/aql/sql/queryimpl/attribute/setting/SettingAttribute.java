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
package org.ehrbase.aql.sql.queryimpl.attribute.setting;

import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.*;
import static org.ehrbase.jooq.pg.tables.EventContext.EVENT_CONTEXT;

import java.util.Optional;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.GenericJsonPath;
import org.ehrbase.aql.sql.queryimpl.attribute.IRMObjectAttribute;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.attribute.eventcontext.EventContextAttribute;
import org.ehrbase.aql.sql.queryimpl.value_field.GenericJsonField;
import org.ehrbase.jooq.pg.Routines;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.TableField;
import org.jooq.impl.DSL;

@SuppressWarnings({"java:S3740", "java:S1452"})
public class SettingAttribute extends EventContextAttribute {

    protected Field tableField;
    protected Optional<String> jsonPath = Optional.empty();
    private boolean isJsonDataBlock = false;

    public SettingAttribute(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        if (jsonPath.isPresent()) {
            if (isJsonDataBlock)
                return new GenericJsonField(fieldContext, joinSetup)
                        .forJsonPath(jsonPath.get())
                        .eventContext(EVENT_CONTEXT.ID);

            Field jsonContextField = DSL.field(jsonpathItem(
                            fieldContext.getContext().configuration(),
                            Routines.jsDvCodedText2(tableField).cast(JSONB.class),
                            jsonpathParameters(new GenericJsonPath(jsonPath.get()).jqueryPath())))
                    .cast(String.class);

            return as(DSL.field(jsonContextField));
        }
        return null;
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        this.tableField = tableField;
        return this;
    }

    public IRMObjectAttribute forJsonPath(String jsonPath) {
        this.jsonPath = Optional.of(jsonPath);
        return this;
    }

    public SettingAttribute setJsonDataBlock(boolean jsonDataBlock) {
        this.isJsonDataBlock = jsonDataBlock;
        return this;
    }
}
