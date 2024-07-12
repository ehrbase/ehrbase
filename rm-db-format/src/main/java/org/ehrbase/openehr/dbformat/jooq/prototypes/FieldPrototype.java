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

import org.jooq.DataType;
import org.jooq.Name;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

/**
 * All fields
 */
public enum FieldPrototype {
    // all
    VO_ID(SQLDataType.UUID.nullable(false), true, true, true, true),
    // version
    EHR_ID(SQLDataType.UUID.nullable(false), true, true, false, false),
    CONTRIBUTION_ID(SQLDataType.UUID.nullable(false), true, true, false, false),
    AUDIT_ID(SQLDataType.UUID.nullable(false), true, true, false, false),
    SYS_PERIOD_LOWER(SQLDataType.TIMESTAMPWITHTIMEZONE(6).nullable(false), true, true, false, false),
    // version and data history
    SYS_VERSION(SQLDataType.INTEGER.nullable(false), true, true, false, true),
    // version history
    SYS_PERIOD_UPPER(SQLDataType.TIMESTAMPWITHTIMEZONE(6), false, true, false, false),
    SYS_DELETED(SQLDataType.BOOLEAN.nullable(false), false, true, false, false),

    // DATA
    NUM(SQLDataType.INTEGER.nullable(false), false, false, true, true),
    NUM_CAP(SQLDataType.INTEGER.nullable(false), false, false, true, true),
    PARENT_NUM(SQLDataType.INTEGER.nullable(false), false, false, true, true),
    CITEM_NUM(SQLDataType.INTEGER, false, false, true, true),
    RM_ENTITY(SQLDataType.CLOB.nullable(false), false, false, true, true),
    ENTITY_CONCEPT(SQLDataType.CLOB, false, false, true, true),
    ENTITY_NAME(SQLDataType.CLOB, false, false, true, true),
    ENTITY_ATTRIBUTE(SQLDataType.CLOB, false, false, true, true),
    ENTITY_PATH(SQLDataType.CLOB.nullable(false), false, false, true, true),
    ENTITY_PATH_CAP(SQLDataType.CLOB.nullable(false), false, false, true, true),
    ENTITY_IDX(SQLDataType.CLOB.nullable(false), false, false, true, true),
    ENTITY_IDX_CAP(SQLDataType.CLOB.nullable(false), false, false, true, true),
    ENTITY_IDX_LEN(SQLDataType.INTEGER.nullable(false), false, false, true, true),
    DATA(SQLDataType.JSONB.nullable(false), false, false, true, true);

    private final DataType<?> type;
    private final Name fieldName;
    private final boolean[][] availableIn;

    FieldPrototype(
            DataType<?> type, boolean versionHead, boolean versionHistory, boolean dataHead, boolean dataHistory) {
        this.availableIn = new boolean[][] {{versionHead, versionHistory}, {dataHead, dataHistory}};
        this.fieldName = DSL.name(name().toLowerCase());
        this.type = type;
    }

    boolean isAvailable(boolean version, boolean head) {
        return availableIn[version ? 0 : 1][head ? 0 : 1];
    }

    public Name fieldName() {
        return fieldName;
    }

    public DataType<?> type() {
        return type;
    }
}
