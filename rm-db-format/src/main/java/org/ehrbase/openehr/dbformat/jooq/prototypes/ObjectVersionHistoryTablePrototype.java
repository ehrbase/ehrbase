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

import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.Name;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;

public final class ObjectVersionHistoryTablePrototype
        extends AbstractTablePrototype<ObjectVersionHistoryTablePrototype, ObjectVersionHistoryRecordPrototype> {

    private static final long serialVersionUID = 1L;

    public static final ObjectVersionHistoryTablePrototype INSTANCE = new ObjectVersionHistoryTablePrototype();

    {
        // Create fields in correct order
        ObjectVersionHistoryRecordPrototype.COLUMNS.keySet().forEach(this::createField);
    }

    public final TableField<ObjectVersionHistoryRecordPrototype, UUID> VO_ID = getField(FieldPrototype.VO_ID);

    public final TableField<ObjectVersionHistoryRecordPrototype, UUID> EHR_ID = getField(FieldPrototype.EHR_ID);

    public final TableField<ObjectVersionHistoryRecordPrototype, UUID> CONTRIBUTION_ID =
            getField(FieldPrototype.CONTRIBUTION_ID);

    public final TableField<ObjectVersionHistoryRecordPrototype, UUID> AUDIT_ID = getField(FieldPrototype.AUDIT_ID);

    public final TableField<ObjectVersionHistoryRecordPrototype, Integer> SYS_VERSION =
            getField(FieldPrototype.SYS_VERSION);

    public final TableField<ObjectVersionHistoryRecordPrototype, OffsetDateTime> SYS_PERIOD_LOWER =
            getField(FieldPrototype.SYS_PERIOD_LOWER);

    public final TableField<ObjectVersionHistoryRecordPrototype, OffsetDateTime> SYS_PERIOD_UPPER =
            getField(FieldPrototype.SYS_PERIOD_UPPER);

    public final TableField<ObjectVersionHistoryRecordPrototype, Boolean> SYS_DELETED =
            getField(FieldPrototype.SYS_DELETED);

    private ObjectVersionHistoryTablePrototype() {
        this(DSL.name("object_version_history_prototype"), null);
    }

    private ObjectVersionHistoryTablePrototype(Name alias, Table<ObjectVersionHistoryRecordPrototype> aliased) {
        super(alias, aliased);
    }

    @Override
    protected ObjectVersionHistoryTablePrototype instance(Name alias, ObjectVersionHistoryTablePrototype aliased) {
        return new ObjectVersionHistoryTablePrototype(alias, aliased);
    }

    @Override
    public Class<ObjectVersionHistoryRecordPrototype> getRecordType() {
        return ObjectVersionHistoryRecordPrototype.class;
    }
}
