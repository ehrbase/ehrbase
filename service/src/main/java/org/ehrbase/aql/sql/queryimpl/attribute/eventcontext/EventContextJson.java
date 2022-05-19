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

import static org.ehrbase.jooq.pg.tables.EventContext.EVENT_CONTEXT;

import java.util.Optional;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.IRMObjectAttribute;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.value_field.GenericJsonField;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.DSL;

public class EventContextJson extends EventContextAttribute {

    protected Optional<String> jsonPath = Optional.empty();

    public EventContextJson(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        // query the json representation of EVENT_CONTEXT and cast the result as TEXT
        Field jsonEventContext;

        if (jsonPath.isPresent())
            jsonEventContext = new GenericJsonField(fieldContext, joinSetup)
                    .forJsonPath(jsonPath.get())
                    .eventContext(EVENT_CONTEXT.ID);
        else jsonEventContext = new GenericJsonField(fieldContext, joinSetup).eventContext(EVENT_CONTEXT.ID);

        if (fieldContext.isWithAlias()) return aliased(DSL.field(jsonEventContext));
        else return defaultAliased(jsonEventContext);
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        return this;
    }

    public EventContextJson forJsonPath(String jsonPath) {
        if (jsonPath == null || jsonPath.isEmpty()) {
            this.jsonPath = Optional.empty();
            return this;
        }
        this.jsonPath = Optional.of(jsonPath);
        return this;
    }
}
