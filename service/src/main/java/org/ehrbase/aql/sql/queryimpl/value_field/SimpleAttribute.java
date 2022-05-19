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
package org.ehrbase.aql.sql.queryimpl.value_field;

import java.util.Optional;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.IRMObjectAttribute;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.attribute.composition.CompositionAttribute;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.DSL;

@SuppressWarnings({"java:S3740", "java:S1452"})
public class SimpleAttribute extends CompositionAttribute {

    protected Field tableField;
    Optional<String> type = Optional.empty();

    public SimpleAttribute(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        Field actualField;

        if (type.isPresent()) actualField = DSL.field(tableField + "::" + type.get());
        else actualField = DSL.field(tableField);

        return as(actualField);
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        return forTableField(tableField);
    }

    public SimpleAttribute forTableField(String pgtype, Field tableField) {
        if (pgtype != null) type = Optional.of(pgtype);

        this.tableField = tableField;
        return this;
    }
}
