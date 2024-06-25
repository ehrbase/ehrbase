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

public final class ObjectDataTablePrototype
        extends AbstractTablePrototype<ObjectDataTablePrototype, ObjectDataRecordPrototype> {

    private static final long serialVersionUID = 1L;

    public static final ObjectDataTablePrototype INSTANCE = new ObjectDataTablePrototype();

    {
        // Create fields in correct order
        ObjectDataRecordPrototype.COLUMNS.keySet().forEach(this::createField);
    }

    public final TableField<ObjectDataRecordPrototype, UUID> VO_ID = getField(FieldPrototype.VO_ID);

    public final TableField<ObjectDataRecordPrototype, Integer> NUM = getField(FieldPrototype.NUM);
    public final TableField<ObjectDataRecordPrototype, Integer> PARENT_NUM = getField(FieldPrototype.PARENT_NUM);

    public final TableField<ObjectDataRecordPrototype, Integer> CITEM_NUM = getField(FieldPrototype.CITEM_NUM);

    public final TableField<ObjectDataRecordPrototype, String> RM_ENTITY = getField(FieldPrototype.RM_ENTITY);

    public final TableField<ObjectDataRecordPrototype, String> ENTITY_CONCEPT = getField(FieldPrototype.ENTITY_CONCEPT);

    public final TableField<ObjectDataRecordPrototype, String> ENTITY_NAME = getField(FieldPrototype.ENTITY_NAME);

    public final TableField<ObjectDataRecordPrototype, String> ENTITY_ATTRIBUTE =
            getField(FieldPrototype.ENTITY_ATTRIBUTE);

    public final TableField<ObjectDataRecordPrototype, String> ENTITY_PATH = getField(FieldPrototype.ENTITY_PATH);

    public final TableField<ObjectDataRecordPrototype, String> ENTITY_PATH_CAP =
            getField(FieldPrototype.ENTITY_PATH_CAP);

    public final TableField<ObjectDataRecordPrototype, String> ENTITY_IDX = getField(FieldPrototype.ENTITY_IDX);

    public final TableField<ObjectDataRecordPrototype, String> ENTITY_IDX_CAP = getField(FieldPrototype.ENTITY_IDX_CAP);

    public final TableField<ObjectDataRecordPrototype, Integer> ENTITY_IDX_LEN =
            getField(FieldPrototype.ENTITY_IDX_LEN);

    public final TableField<ObjectDataRecordPrototype, JSONB> DATA = getField(FieldPrototype.DATA);

    private ObjectDataTablePrototype() {
        this(DSL.name("object_data_prototype"), null);
    }

    private ObjectDataTablePrototype(Name alias, Table<ObjectDataRecordPrototype> aliased) {
        super(alias, aliased);
    }

    @Override
    protected ObjectDataTablePrototype instance(Name alias, ObjectDataTablePrototype aliased) {
        return new ObjectDataTablePrototype(alias, aliased);
    }

    @Override
    public Class<ObjectDataRecordPrototype> getRecordType() {
        return ObjectDataRecordPrototype.class;
    }
}
