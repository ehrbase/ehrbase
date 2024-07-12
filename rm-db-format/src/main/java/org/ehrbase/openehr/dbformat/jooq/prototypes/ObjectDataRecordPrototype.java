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
import java.util.UUID;
import org.jooq.JSONB;
import org.jooq.Record3;

public class ObjectDataRecordPrototype extends AbstractRecordPrototype<ObjectDataRecordPrototype> {

    private static final long serialVersionUID = 1L;
    static final EnumMap<FieldPrototype, Integer> COLUMNS = determineColumns(false, true);

    public void setVoId(UUID value) {
        set(FieldPrototype.VO_ID, value);
    }

    public UUID getVoId() {
        return (UUID) get(FieldPrototype.VO_ID);
    }

    public void setNum(Integer value) {
        set(FieldPrototype.NUM, value);
    }

    public Integer getNum() {
        return (Integer) get(FieldPrototype.NUM);
    }

    public void setCitemNum(Integer value) {
        set(FieldPrototype.CITEM_NUM, value);
    }

    public Integer getCitemNum() {
        return (Integer) get(FieldPrototype.CITEM_NUM);
    }

    public void setParentNum(Integer value) {
        set(FieldPrototype.PARENT_NUM, value);
    }

    public Integer getParentNum() {
        return (Integer) get(FieldPrototype.PARENT_NUM);
    }

    public void setNumCap(Integer value) {
        set(FieldPrototype.NUM_CAP, value);
    }

    public Integer getNumCap() {
        return (Integer) get(FieldPrototype.NUM_CAP);
    }

    public void setRmEntity(String value) {
        set(FieldPrototype.RM_ENTITY, value);
    }

    public String getRmEntity() {
        return (String) get(FieldPrototype.RM_ENTITY);
    }

    public void setEntityConcept(String value) {
        set(FieldPrototype.ENTITY_CONCEPT, value);
    }

    public String getEntityConcept() {
        return (String) get(FieldPrototype.ENTITY_CONCEPT);
    }

    public void setEntityName(String value) {
        set(FieldPrototype.ENTITY_NAME, value);
    }

    public String getEntityName() {
        return (String) get(FieldPrototype.ENTITY_NAME);
    }

    public void setEntityAttribute(String value) {
        set(FieldPrototype.ENTITY_ATTRIBUTE, value);
    }

    public String getEntityAttribute() {
        return (String) get(FieldPrototype.ENTITY_ATTRIBUTE);
    }

    public void setEntityPath(String value) {
        set(FieldPrototype.ENTITY_PATH, value);
    }

    public String getEntityPath() {
        return (String) get(FieldPrototype.ENTITY_PATH);
    }

    public void setEntityPathCap(String value) {
        set(FieldPrototype.ENTITY_PATH_CAP, value);
    }

    public String getEntityPathCap() {
        return (String) get(FieldPrototype.ENTITY_PATH_CAP);
    }

    public void setEntityIdx(String value) {
        set(FieldPrototype.ENTITY_IDX, value);
    }

    public String getEntityIdx() {
        return (String) get(FieldPrototype.ENTITY_IDX);
    }

    public void setEntityIdxCap(String value) {
        set(FieldPrototype.ENTITY_IDX_CAP, value);
    }

    public String getEntityIdxCap() {
        return (String) get(FieldPrototype.ENTITY_IDX_CAP);
    }

    public void setEntityIdxLen(Integer value) {
        set(FieldPrototype.ENTITY_IDX_LEN, value);
    }

    public Integer getEntityIdxLen() {
        return (Integer) get(FieldPrototype.ENTITY_IDX_LEN);
    }

    public void setData(JSONB value) {
        set(FieldPrototype.DATA, value);
    }

    public JSONB getData() {
        return (JSONB) get(FieldPrototype.DATA);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record3<UUID, Short, Integer> key() {
        return (Record3) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ObjectDataRecordPrototype() {
        super(ObjectDataTablePrototype.INSTANCE);
    }

    public ObjectDataRecordPrototype(
            UUID voId,
            Integer num,
            Integer citemNum,
            String rmEntity,
            String entityConcept,
            String entityName,
            String entityAttribute,
            String entityPath,
            String entityPathCap,
            String entityIdx,
            String entityIdxCap,
            Integer entityIdxLen,
            JSONB data) {
        super(
                ObjectDataTablePrototype.INSTANCE,
                voId,
                num,
                citemNum,
                rmEntity,
                entityConcept,
                entityName,
                entityAttribute,
                entityPath,
                entityPathCap,
                entityIdx,
                entityIdxCap,
                entityIdxLen,
                data);
    }

    @Override
    protected int columnIndex(FieldPrototype f) {
        return COLUMNS.get(f);
    }
}
