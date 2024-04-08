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
import java.util.EnumMap;
import java.util.UUID;
import org.jooq.Record3;

public class ObjectVersionHistoryRecordPrototype extends AbstractRecordPrototype<ObjectVersionHistoryRecordPrototype> {

    private static final long serialVersionUID = 1L;
    static final EnumMap<FieldPrototype, Integer> COLUMNS = determineColumns(true, false);

    public void setVoId(UUID value) {
        set(FieldPrototype.VO_ID, value);
    }

    public UUID getVoId() {
        return (UUID) get(FieldPrototype.VO_ID);
    }

    public void setEhrId(UUID value) {
        set(FieldPrototype.EHR_ID, value);
    }

    public UUID getEhrId() {
        return (UUID) get(FieldPrototype.EHR_ID);
    }

    public void setContributionId(UUID value) {
        set(FieldPrototype.CONTRIBUTION_ID, value);
    }

    public UUID getContributionId() {
        return (UUID) get(FieldPrototype.CONTRIBUTION_ID);
    }

    public void setAuditId(UUID value) {
        set(FieldPrototype.AUDIT_ID, value);
    }

    public UUID getAuditId() {
        return (UUID) get(FieldPrototype.AUDIT_ID);
    }

    public void setSysVersion(Integer value) {
        set(FieldPrototype.SYS_VERSION, value);
    }

    public Integer getSysVersion() {
        return (Integer) get(FieldPrototype.SYS_VERSION);
    }

    public void setSysPeriodLower(OffsetDateTime value) {
        set(FieldPrototype.SYS_PERIOD_LOWER, value);
    }

    public OffsetDateTime getSysPeriodLower() {
        return (OffsetDateTime) get(FieldPrototype.SYS_PERIOD_LOWER);
    }

    public void setSysPeriodUpper(OffsetDateTime value) {
        set(FieldPrototype.SYS_PERIOD_UPPER, value);
    }

    public OffsetDateTime getSysPeriodUpper() {
        return (OffsetDateTime) get(FieldPrototype.SYS_PERIOD_UPPER);
    }

    public void setSysDeleted(Boolean value) {
        set(FieldPrototype.SYS_DELETED, value);
    }

    public Boolean getSysDeleted() {
        return (Boolean) get(FieldPrototype.SYS_DELETED);
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

    public ObjectVersionHistoryRecordPrototype() {
        super(ObjectVersionHistoryTablePrototype.INSTANCE);
    }

    public ObjectVersionHistoryRecordPrototype(
            UUID voId,
            UUID ehrId,
            UUID contributionId,
            UUID auditId,
            Integer sysVersion,
            OffsetDateTime sysPeriodLower,
            OffsetDateTime sysPeriodUpper,
            Boolean sysDeleted) {
        super(
                ObjectVersionHistoryTablePrototype.INSTANCE,
                voId,
                ehrId,
                contributionId,
                auditId,
                sysVersion,
                sysPeriodLower,
                sysPeriodUpper,
                sysDeleted);
    }

    @Override
    protected int columnIndex(FieldPrototype f) {
        return COLUMNS.get(f);
    }
}
