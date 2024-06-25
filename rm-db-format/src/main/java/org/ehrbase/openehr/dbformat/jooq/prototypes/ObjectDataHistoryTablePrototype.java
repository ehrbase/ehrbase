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

import java.util.UUID;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;

public final class ObjectDataHistoryTablePrototype
        extends AbstractTablePrototype<ObjectDataHistoryTablePrototype, ObjectDataHistoryRecordPrototype> {

    private static final long serialVersionUID = 1L;

    public static final ObjectDataHistoryTablePrototype INSTANCE = new ObjectDataHistoryTablePrototype();

    {
        // Create fields in correct order
        ObjectDataHistoryRecordPrototype.COLUMNS.keySet().forEach(this::createField);
    }

    public final TableField<ObjectDataHistoryRecordPrototype, UUID> VO_ID = getField(FieldPrototype.VO_ID);

    public final TableField<ObjectDataHistoryRecordPrototype, Integer> NUM = getField(FieldPrototype.NUM);

    public final TableField<ObjectDataHistoryRecordPrototype, Integer> CITEM_NUM = getField(FieldPrototype.CITEM_NUM);
    public final TableField<ObjectDataHistoryRecordPrototype, Integer> PARENT_NUM = getField(FieldPrototype.PARENT_NUM);

    public final TableField<ObjectDataHistoryRecordPrototype, String> RM_ENTITY = getField(FieldPrototype.RM_ENTITY);

    public final TableField<ObjectDataHistoryRecordPrototype, String> ENTITY_CONCEPT =
            getField(FieldPrototype.ENTITY_CONCEPT);

    public final TableField<ObjectDataHistoryRecordPrototype, String> ENTITY_NAME =
            getField(FieldPrototype.ENTITY_NAME);

    public final TableField<ObjectDataHistoryRecordPrototype, String> ENTITY_ATTRIBUTE =
            getField(FieldPrototype.ENTITY_ATTRIBUTE);

    public final TableField<ObjectDataHistoryRecordPrototype, String> ENTITY_PATH =
            getField(FieldPrototype.ENTITY_PATH);

    public final TableField<ObjectDataHistoryRecordPrototype, String> ENTITY_PATH_CAP =
            getField(FieldPrototype.ENTITY_PATH_CAP);

    public final TableField<ObjectDataHistoryRecordPrototype, String> ENTITY_IDX = getField(FieldPrototype.ENTITY_IDX);

    public final TableField<ObjectDataHistoryRecordPrototype, String> ENTITY_IDX_CAP =
            getField(FieldPrototype.ENTITY_IDX_CAP);

    public final TableField<ObjectDataHistoryRecordPrototype, Integer> ENTITY_IDX_LEN =
            getField(FieldPrototype.ENTITY_IDX_LEN);

    public final TableField<ObjectDataHistoryRecordPrototype, JSONB> DATA = getField(FieldPrototype.DATA);

    public final TableField<ObjectDataHistoryRecordPrototype, Integer> SYS_VERSION =
            getField(FieldPrototype.SYS_VERSION);

    private ObjectDataHistoryTablePrototype() {
        this(DSL.name("object_data_history_prototype"), null);
    }

    private ObjectDataHistoryTablePrototype(Name alias, Table<ObjectDataHistoryRecordPrototype> aliased) {
        super(alias, aliased);
    }

    @Override
    protected ObjectDataHistoryTablePrototype instance(Name alias, ObjectDataHistoryTablePrototype aliased) {
        return new ObjectDataHistoryTablePrototype(alias, aliased);
    }

    @Override
    public Class<ObjectDataHistoryRecordPrototype> getRecordType() {
        return ObjectDataHistoryRecordPrototype.class;
    }
}
