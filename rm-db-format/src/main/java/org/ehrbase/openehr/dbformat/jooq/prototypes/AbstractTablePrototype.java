/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.openehr.dbformat.jooq.prototypes;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

public abstract sealed class AbstractTablePrototype<P extends AbstractTablePrototype<P, R>, R extends Record>
        extends TableImpl<R>
        permits ObjectDataHistoryTablePrototype,
                ObjectDataTablePrototype,
                ObjectVersionHistoryTablePrototype,
                ObjectVersionTablePrototype {

    private static final long serialVersionUID = 1L;

    private final Map<FieldPrototype, TableField<R, ?>> fieldMap = new EnumMap<>(FieldPrototype.class);

    @Override
    public abstract Class<R> getRecordType();

    protected AbstractTablePrototype(Name alias, Table<R> aliased) {
        super(alias, null, aliased, null, DSL.comment(""), TableOptions.table());
    }

    protected abstract P instance(Name alias, P aliased);

    protected <T> TableField<R, T> createField(FieldPrototype fieldProto) {
        TableField<R, T> field = (TableField<R, T>) createField(fieldProto.fieldName(), fieldProto.type(), this, "");
        fieldMap.put(fieldProto, field);
        return field;
    }

    protected <T> TableField<R, T> getField(FieldPrototype fieldProto) {
        return Optional.of(fieldProto)
                .map(fieldMap::get)
                .map(f -> (TableField<R, T>) f)
                .orElseThrow(() -> new IllegalArgumentException("Unknown field: %s".formatted(fieldProto)));
    }

    @Override
    public Schema getSchema() {
        return null;
    }

    @Override
    public P as(String alias) {
        return instance(DSL.name(alias), (P) this);
    }

    @Override
    public P as(Name alias) {
        return instance(alias, (P) this);
    }

    @Override
    public P as(Table<?> alias) {
        return instance(alias.getQualifiedName(), (P) this);
    }

    /**
     * Rename this table
     */
    @Override
    public P rename(String name) {
        return instance(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public P rename(Name name) {
        return instance(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public P rename(Table<?> name) {
        return instance(name.getQualifiedName(), null);
    }
}
