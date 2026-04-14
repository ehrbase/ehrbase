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
package org.ehrbase.repository;

import static org.ehrbase.jooq.pg.Tables.EHR_;
import static org.ehrbase.jooq.pg.Tables.EHR_STATUS_DATA;
import static org.ehrbase.jooq.pg.Tables.EHR_STATUS_VERSION;
import static org.ehrbase.jooq.pg.Tables.EHR_STATUS_VERSION_HISTORY;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.api.util.LocatableUtils;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.tables.Ehr;
import org.ehrbase.jooq.pg.tables.EhrStatusData;
import org.ehrbase.jooq.pg.tables.EhrStatusVersion;
import org.ehrbase.jooq.pg.tables.EhrStatusVersionHistory;
import org.ehrbase.jooq.pg.tables.records.EhrRecord;
import org.ehrbase.jooq.pg.tables.records.EhrStatusDataRecord;
import org.ehrbase.jooq.pg.tables.records.EhrStatusVersionHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.EhrStatusVersionRecord;
import org.ehrbase.openehr.dbformat.RmAttributeAlias;
import org.ehrbase.service.TimeProvider;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.TableField;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles DB Access to {@link  org.ehrbase.jooq.pg.tables.Ehr} and {@link  org.ehrbase.jooq.pg.tables.EhrStatusVersion} etc.
 */
@Repository
public class EhrRepository
        extends AbstractVersionedObjectRepository<
                EhrStatusVersionRecord, EhrStatusDataRecord, EhrStatusVersionHistoryRecord, EhrStatus, Void> {

    public static final String[] IS_MODIFIABLE_JSON_PATH = RmAttributeAlias.rmToJsonPathParts("is_modifiable");
    public static final String[] SUBJECT_ID_JSON_PATH =
            RmAttributeAlias.rmToJsonPathParts("subject/external_ref/id/value");
    public static final String[] SUBJECT_NAMESPACE_JSON_PATH =
            RmAttributeAlias.rmToJsonPathParts("subject/external_ref/namespace");

    public EhrRepository(
            DSLContext context,
            ContributionRepository contributionRepository,
            SystemService systemService,
            TimeProvider timeProvider) {

        super(
                AuditDetailsTargetType.EHR_STATUS,
                EHR_STATUS_VERSION,
                EHR_STATUS_DATA,
                EHR_STATUS_VERSION_HISTORY,
                context,
                contributionRepository,
                systemService,
                timeProvider);
    }

    @Override
    protected List<TableField<EhrStatusVersionRecord, ?>> getVersionDataJoinFields() {
        return List.of(EHR_STATUS_VERSION.EHR_ID);
    }

    @Transactional
    public void commit(UUID ehrId, EhrStatus status, UUID contributionId, UUID auditId) {

        EhrRecord ehrRecord = context.newRecord(Ehr.EHR_);

        ehrRecord.setId(ehrId);
        ehrRecord.setCreationDate(timeProvider.getNow());
        ehrRecord.store();

        commitHead(
                ehrId,
                status,
                contributionId,
                auditId,
                ContributionChangeType.creation,
                r -> {},
                (n, r) -> r.setEhrId(ehrId));
    }

    @Override
    public boolean hasEhr(UUID ehrId) {
        return super.hasEhr(ehrId);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean hasEhrNewTransaction(UUID ehrId) {
        return hasEhr(ehrId);
    }

    public Boolean fetchIsModifiable(UUID ehrId) {
        Table<EhrStatusDataRecord> dataHead = tables.dataHead();
        return context.select(jsonDataField(dataHead, IS_MODIFIABLE_JSON_PATH).cast(Boolean.class))
                .from(dataHead)
                .where(singleEhrStatusCondition(ehrId).apply(dataHead))
                .and(dataRootCondition(dataHead))
                .fetchOptional()
                .map(Record1::value1)
                .orElse(null);
    }

    public Optional<UUID> findBySubject(String subjectId, String nameSpace) {
        EhrStatusData dataHead = EHR_STATUS_DATA.as("d");
        return context.select(dataHead.EHR_ID)
                .from(dataHead)
                .where(subjectCondition(subjectId, nameSpace, dataHead))
                .and(dataRootCondition(dataHead))
                .fetchOptional()
                .map(Record1::value1);
    }

    Condition subjectCondition(String subjectId, String nameSpace, Table<EhrStatusDataRecord> dataTable) {
        return dataRootCondition(dataTable)
                .and(jsonDataField(dataTable, SUBJECT_ID_JSON_PATH).eq(subjectId))
                .and(jsonDataField(dataTable, SUBJECT_NAMESPACE_JSON_PATH).eq(nameSpace));
    }

    public Optional<ObjectVersionId> findVersionByTime(UUID ehrId, OffsetDateTime time) {
        return findVersionByTime(singleEhrStatusCondition(ehrId), singleEhrStatusCondition(ehrId), time);
    }

    public Optional<ObjectVersionId> findLatestVersion(UUID ehrId) {
        EhrStatusVersion versionHead = EHR_STATUS_VERSION.as("v");
        return context.select(versionHead.VO_ID, versionHead.SYS_VERSION)
                .from(versionHead)
                .where(singleEhrStatusCondition(ehrId).apply(versionHead))
                .fetchOptional()
                .map(r -> buildObjectVersionId(r.value1(), r.value2(), systemService));
    }

    public Optional<EhrStatus> findHead(UUID ehrId) {
        return findHead(singleEhrStatusCondition(ehrId));
    }

    @Override
    protected boolean isDeleted(
            Function<Table<?>, Condition> condition, Function<Table<?>, Condition> historyCondition, Integer version) {
        return false;
    }

    public Optional<OriginalVersion<EhrStatus>> getOriginalVersionStatus(
            UUID ehrId, UUID versionedObjectUid, int version) {

        return getOriginalVersion(singleEhrStatusCondition(ehrId), singleEhrStatusCondition(ehrId), version)
                .filter(e -> LocatableUtils.getUuid(e.getUid()).equals(versionedObjectUid));
    }

    public RevisionHistory getRevisionHistory(UUID ehrId) {
        return getRevisionHistory(singleEhrStatusCondition(ehrId), singleEhrStatusCondition(ehrId));
    }

    public OffsetDateTime findEhrCreationTime(UUID ehrId) {

        return context.select(EHR_.CREATION_DATE)
                .from(EHR_)
                .where(EHR_.ID.eq(ehrId))
                .fetchOne(Record1::value1);
    }

    public void adminDelete(UUID ehrId) {

        EhrStatusVersion versionHead = EHR_STATUS_VERSION.as("v");
        context.deleteFrom(versionHead).where(versionHead.EHR_ID.eq(ehrId)).execute();
        EhrStatusVersionHistory history = EHR_STATUS_VERSION_HISTORY.as("h");
        context.deleteFrom(history).where(history.EHR_ID.eq(ehrId)).execute();
        context.deleteFrom(EHR_).where(EHR_.ID.eq(ehrId)).execute();
    }

    @Transactional
    public void update(UUID ehrId, EhrStatus ehrStatus, UUID contributionId, UUID auditId) {

        update(
                ehrId,
                ehrStatus,
                singleEhrStatusCondition(ehrId),
                singleEhrStatusCondition(ehrId),
                contributionId,
                auditId,
                r -> {},
                (n, r) -> r.setEhrId(ehrId),
                "No EHR_STATUS in ehr: %s".formatted(ehrId));
    }

    public Optional<VersionedEhrStatus> getVersionedEhrStatus(UUID ehrId) {

        return findRootRecordByVersion(singleEhrStatusCondition(ehrId), singleEhrStatusCondition(ehrId), 1)
                .map(root -> recordToVersionedEhrStatus(ehrId, root));
    }

    private Function<Table<?>, Condition> singleEhrStatusCondition(UUID ehrId) {

        return table -> table.field(VERSION_PROTOTYPE.EHR_ID).eq(ehrId);
    }

    @Override
    public Class<EhrStatus> getLocatableClass() {
        return EhrStatus.class;
    }

    private static VersionedEhrStatus recordToVersionedEhrStatus(UUID ehrId, EhrStatusVersionHistoryRecord record) {
        VersionedEhrStatus versionedComposition = new VersionedEhrStatus();
        versionedComposition.setUid(new HierObjectId(record.getVoId().toString()));
        versionedComposition.setOwnerId(new ObjectRef<>(new HierObjectId(ehrId.toString()), "local", "ehr"));
        versionedComposition.setTimeCreated(new DvDateTime(record.getSysPeriodLower()));
        return versionedComposition;
    }

    public String getSubjectExternalRef(final UUID uuid) {
        EhrStatusData data = EHR_STATUS_DATA.as("data");
        return context.select(jsonDataField(data, SUBJECT_ID_JSON_PATH))
                .from(data)
                .where(data.EHR_ID.eq(uuid), dataRootCondition(data))
                .fetchOne(Record1::value1);
    }
}
