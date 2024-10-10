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
package org.ehrbase.openehr.aqlengine.asl.model;

import static org.ehrbase.jooq.pg.Tables.COMP_VERSION;
import static org.ehrbase.jooq.pg.Tables.EHR_FOLDER_VERSION;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectDataTablePrototype;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectVersionTablePrototype;
import org.jooq.Field;

public enum AslStructureColumn {
    VO_ID(ObjectDataTablePrototype.INSTANCE.VO_ID, UUID.class, null),
    NUM(ObjectDataTablePrototype.INSTANCE.NUM, Integer.class, false),
    NUM_CAP(ObjectDataTablePrototype.INSTANCE.NUM_CAP, Integer.class, false),
    PARENT_NUM(ObjectDataTablePrototype.INSTANCE.PARENT_NUM, Integer.class, false),
    EHR_ID(ObjectVersionTablePrototype.INSTANCE.EHR_ID, UUID.class, true),
    ENTITY_IDX(ObjectDataTablePrototype.INSTANCE.ENTITY_IDX, String.class, false),
    ENTITY_IDX_LEN(ObjectDataTablePrototype.INSTANCE.ENTITY_IDX_LEN, Integer.class, false),
    ENTITY_CONCEPT(ObjectDataTablePrototype.INSTANCE.ENTITY_CONCEPT, String.class, false),
    ENTITY_NAME(ObjectDataTablePrototype.INSTANCE.ENTITY_NAME, String.class, AslExtractedColumn.NAME_VALUE, false),
    RM_ENTITY(ObjectDataTablePrototype.INSTANCE.RM_ENTITY, String.class, false),
    TEMPLATE_ID(COMP_VERSION.TEMPLATE_ID, UUID.class, AslExtractedColumn.TEMPLATE_ID, true),
    SYS_VERSION(ObjectVersionTablePrototype.INSTANCE.SYS_VERSION, Integer.class, true),

    // Columns for FOLDER querying
    EHR_FOLDER_IDX(EHR_FOLDER_VERSION.EHR_FOLDERS_IDX, Integer.class, true),

    // Columns for VERSION querying
    AUDIT_ID(ObjectVersionTablePrototype.INSTANCE.AUDIT_ID, UUID.class, true),
    CONTRIBUTION_ID(ObjectVersionTablePrototype.INSTANCE.CONTRIBUTION_ID, UUID.class, null, true),
    SYS_PERIOD_LOWER(ObjectVersionTablePrototype.INSTANCE.SYS_PERIOD_LOWER, OffsetDateTime.class, null, true);

    private final String fieldName;
    private final Class<?> clazz;
    private final AslExtractedColumn extractedColumn;
    private final Boolean fromVersionTable;

    AslStructureColumn(Field<?> field, Class<?> clazz, Boolean inVersionTable) {
        this(field, clazz, null, inVersionTable);
    }

    AslStructureColumn(Field<?> field, Class<?> clazz, AslExtractedColumn extractedColumn, Boolean inVersionTable) {
        this.fieldName = field.getName();
        this.clazz = clazz;
        this.extractedColumn = extractedColumn;
        this.fromVersionTable = inVersionTable;
    }

    public AslField field() {
        return new AslColumnField(clazz, fieldName, null, fromVersionTable, extractedColumn);
    }

    public String getFieldName() {
        return fieldName;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public boolean isFromVersionTable() {
        return !Boolean.FALSE.equals(fromVersionTable);
    }

    public boolean isFromDataTable() {
        return !Boolean.TRUE.equals(fromVersionTable);
    }
}
