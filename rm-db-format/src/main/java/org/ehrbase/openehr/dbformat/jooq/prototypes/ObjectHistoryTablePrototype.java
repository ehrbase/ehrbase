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

public final class ObjectHistoryTablePrototype
        extends AbstractTablePrototype<ObjectHistoryTablePrototype, ObjectHistoryRecordPrototype> {

    private static final long serialVersionUID = 1L;

    public static final ObjectHistoryTablePrototype INSTANCE = new ObjectHistoryTablePrototype();

    {
        // Create fields in correct order
        ObjectHistoryRecordPrototype.COLUMNS.keySet().forEach(this::createField);
    }

    public final TableField<ObjectHistoryRecordPrototype, UUID> VO_ID = getField(FieldPrototype.VO_ID);
    public final TableField<ObjectHistoryRecordPrototype, UUID> EHR_ID = getField(FieldPrototype.EHR_ID);
    public final TableField<ObjectHistoryRecordPrototype, UUID> CONTRIBUTION_ID =
            getField(FieldPrototype.CONTRIBUTION_ID);
    public final TableField<ObjectHistoryRecordPrototype, UUID> AUDIT_ID = getField(FieldPrototype.AUDIT_ID);
    public final TableField<ObjectHistoryRecordPrototype, Integer> SYS_VERSION = getField(FieldPrototype.SYS_VERSION);
    public final TableField<ObjectHistoryRecordPrototype, OffsetDateTime> SYS_PERIOD_LOWER =
            getField(FieldPrototype.SYS_PERIOD_LOWER);
    public final TableField<ObjectHistoryRecordPrototype, OffsetDateTime> SYS_PERIOD_UPPER =
            getField(FieldPrototype.SYS_PERIOD_UPPER);
    public final TableField<ObjectHistoryRecordPrototype, Boolean> SYS_DELETED = getField(FieldPrototype.SYS_DELETED);
    public final TableField<ObjectHistoryRecordPrototype, String> OV_DATA = getField(FieldPrototype.OV_DATA);
    public final TableField<ObjectHistoryRecordPrototype, Integer> OV_REF = getField(FieldPrototype.OV_REF);

    private ObjectHistoryTablePrototype() {
        this(DSL.name("object_history_prototype"), null);
    }

    private ObjectHistoryTablePrototype(Name alias, Table<ObjectHistoryRecordPrototype> aliased) {
        super(alias, aliased);
    }

    @Override
    protected ObjectHistoryTablePrototype instance(Name alias, ObjectHistoryTablePrototype aliased) {
        return new ObjectHistoryTablePrototype(alias, aliased);
    }

    @Override
    public Class<ObjectHistoryRecordPrototype> getRecordType() {
        return ObjectHistoryRecordPrototype.class;
    }
}
