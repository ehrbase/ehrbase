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
import static org.ehrbase.jooq.pg.Tables.EHR_STATUS_DATA_HISTORY;
import static org.ehrbase.jooq.pg.Tables.EHR_STATUS_VERSION;
import static org.ehrbase.jooq.pg.Tables.EHR_STATUS_VERSION_HISTORY;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.ehr.VersionedEhrStatus;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.tables.Ehr;
import org.ehrbase.jooq.pg.tables.records.EhrRecord;
import org.ehrbase.jooq.pg.tables.records.EhrStatusDataHistoryRecord;
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
                EhrStatusVersionRecord,
                EhrStatusDataRecord,
                EhrStatusVersionHistoryRecord,
                EhrStatusDataHistoryRecord,
                EhrStatus> {

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
                EHR_STATUS_DATA_HISTORY,
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
    public void commit(UUID ehrId, EhrStatus status, @Nullable UUID contributionId, @Nullable UUID auditId) {

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
        return fromJoinedVersionData(
                        context.select(jsonDataField(tables.dataHead(), IS_MODIFIABLE_JSON_PATH)
                                .cast(Boolean.class)),
                        true)
                .where(singleEhrStatusCondition(ehrId, tables.versionHead()))
                .and(dataRootCondition(tables.dataHead()))
                .fetchOptional()
                .map(Record1::value1)
                .orElse(null);
    }

    public Optional<UUID> findBySubject(String subjectId, String nameSpace) {

        return fromJoinedVersionData(context.select(field(VERSION_PROTOTYPE.EHR_ID)), true)
                .where(subjectCondition(subjectId, nameSpace, tables.dataHead()))
                .and(dataRootCondition(tables.dataHead()))
                .fetchOptional()
                .map(Record1::value1);
    }

    Condition subjectCondition(String subjectId, String nameSpace, Table<EhrStatusDataRecord> dataTable) {
        return dataRootCondition(dataTable)
                .and(jsonDataField(dataTable, SUBJECT_ID_JSON_PATH).eq(subjectId))
                .and(jsonDataField(dataTable, SUBJECT_NAMESPACE_JSON_PATH).eq(nameSpace));
    }

    public Optional<ObjectVersionId> findVersionByTime(UUID ehrId, OffsetDateTime time) {
        return findVersionByTime(
                singleEhrStatusCondition(ehrId, tables.versionHead()),
                singleEhrStatusCondition(ehrId, tables.versionHistory()),
                time);
    }

    public Optional<ObjectVersionId> findLatestVersion(UUID ehrId) {
        return context.select(field(VERSION_PROTOTYPE.VO_ID), field(VERSION_PROTOTYPE.SYS_VERSION))
                .from(tables.versionHead())
                .where(singleEhrStatusCondition(ehrId, tables.versionHead()))
                .fetchOptional()
                .map(r -> buildObjectVersionId(r.value1(), r.value2(), systemService));
    }

    public Optional<EhrStatus> findHead(UUID ehrId) {
        return findHead(singleEhrStatusCondition(ehrId, tables.dataHead()));
    }

    @Override
    protected boolean isDeleted(Condition condition, Condition historyCondition, Integer version) {
        return false;
    }

    public Optional<OriginalVersion<EhrStatus>> getOriginalVersionStatus(
            UUID ehrId, UUID versionedObjectUid, int version) {

        return getOriginalVersion(
                        singleEhrStatusCondition(ehrId, tables.versionHead()),
                        singleEhrStatusCondition(ehrId, tables.versionHistory()),
                        version)
                .filter(e -> UUID.fromString(e.getUid().getRoot().getValue()).equals(versionedObjectUid));
    }

    public OffsetDateTime findEhrCreationTime(UUID ehrId) {

        return context.select(EHR_.CREATION_DATE)
                .from(EHR_)
                .where(EHR_.ID.eq(ehrId))
                .fetchOne()
                .value1();
    }

    public void adminDelete(UUID ehrId) {

        context.deleteFrom(tables.versionHead())
                .where(field(VERSION_PROTOTYPE.EHR_ID).eq(ehrId))
                .execute();
        context.deleteFrom(tables.versionHistory())
                .where(field(VERSION_HISTORY_PROTOTYPE.EHR_ID).eq(ehrId))
                .execute();
        context.deleteFrom(EHR_).where(EHR_.ID.eq(ehrId)).execute();
    }

    @Transactional
    public void update(UUID ehrId, EhrStatus ehrStatus, @Nullable UUID contributionId, @Nullable UUID auditId) {

        update(
                ehrId,
                ehrStatus,
                singleEhrStatusCondition(ehrId, tables.versionHead()),
                singleEhrStatusCondition(ehrId, tables.versionHistory()),
                contributionId,
                auditId,
                r -> {},
                (n, r) -> r.setEhrId(ehrId),
                "No EHR_STATUS in ehr: %s".formatted(ehrId));
    }

    public Optional<VersionedEhrStatus> getVersionedEhrStatus(UUID ehrId) {

        return findRootRecordByVersion(
                        singleEhrStatusCondition(ehrId, tables.versionHead()),
                        singleEhrStatusCondition(ehrId, tables.versionHistory()),
                        1)
                .map(root -> recordToVersionedEhrStatus(ehrId, root));
    }

    private Condition singleEhrStatusCondition(UUID ehrId, Table<?> table) {

        return table.field(VERSION_PROTOTYPE.EHR_ID).eq(ehrId);
    }

    @Override
    protected Class<EhrStatus> getLocatableClass() {
        return EhrStatus.class;
    }

    private static VersionedEhrStatus recordToVersionedEhrStatus(UUID ehrId, EhrStatusVersionHistoryRecord record) {
        VersionedEhrStatus versionedComposition = new VersionedEhrStatus();
        versionedComposition.setUid(new HierObjectId(record.getVoId().toString()));
        versionedComposition.setOwnerId(new ObjectRef<>(new HierObjectId(ehrId.toString()), "local", "ehr"));
        versionedComposition.setTimeCreated(new DvDateTime(record.getSysPeriodLower()));
        return versionedComposition;
    }
}
