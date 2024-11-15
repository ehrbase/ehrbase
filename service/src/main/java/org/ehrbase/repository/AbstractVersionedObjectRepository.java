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

import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.Ehr;
import org.ehrbase.jooq.pg.util.AdditionalSQLFunctions;
import org.ehrbase.openehr.dbformat.DbToRmFormat;
import org.ehrbase.openehr.dbformat.StructureNode;
import org.ehrbase.openehr.dbformat.jooq.prototypes.AbstractRecordPrototype;
import org.ehrbase.openehr.dbformat.jooq.prototypes.AbstractTablePrototype;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectDataHistoryTablePrototype;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectDataTablePrototype;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectVersionHistoryTablePrototype;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectVersionTablePrototype;
import org.ehrbase.service.TimeProvider;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertQuery;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectFromStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitPercentStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;

public abstract class AbstractVersionedObjectRepository<
        VR extends UpdatableRecord,
        DR extends UpdatableRecord,
        VH extends UpdatableRecord,
        DH extends UpdatableRecord,
        O extends Locatable> {

    public static final String NOT_MATCH_UID = "If-Match version_uid does not match uid";
    public static final String NOT_MATCH_SYSTEM_ID = "If-Match version_uid does not match system id";
    public static final String NOT_MATCH_LATEST_VERSION = "If-Match version_uid does not match latest version";

    protected static final ObjectVersionTablePrototype VERSION_PROTOTYPE = ObjectVersionTablePrototype.INSTANCE;
    protected static final ObjectVersionHistoryTablePrototype VERSION_HISTORY_PROTOTYPE =
            ObjectVersionHistoryTablePrototype.INSTANCE;
    protected static final ObjectDataTablePrototype DATA_PROTOTYPE = ObjectDataTablePrototype.INSTANCE;
    protected static final ObjectDataHistoryTablePrototype DATA_HISTORY_PROTOTYPE =
            ObjectDataHistoryTablePrototype.INSTANCE;
    private final AuditDetailsTargetType targetType;

    protected final class Tables {
        private final Table<VR> versionHead;
        private final Table<DR> dataHead;
        private final Table<VH> versionHistory;
        private final Table<DH> dataHistory;

        private Tables(Table<VR> versionHead, Table<DR> dataHead, Table<VH> versionHistory, Table<DH> dataHistory) {
            this.versionHead = versionHead;
            this.dataHead = dataHead;
            this.versionHistory = versionHistory;
            this.dataHistory = dataHistory;
        }

        public Table<VR> versionHead() {
            return versionHead;
        }

        public Table<DR> dataHead() {
            return dataHead;
        }

        public Table<VH> versionHistory() {
            return versionHistory;
        }

        public Table<DH> dataHistory() {
            return dataHistory;
        }

        public Table<?> get(boolean version, boolean head) {
            if (version) {
                if (head) {
                    return versionHead;
                } else {
                    return versionHistory;
                }
            } else if (head) {
                return dataHead;
            } else {
                return dataHistory;
            }
        }
    }

    protected final Tables tables;

    protected final DSLContext context;
    protected final ContributionRepository contributionRepository;

    protected final SystemService systemService;
    protected final TimeProvider timeProvider;

    protected AbstractVersionedObjectRepository(
            AuditDetailsTargetType targetType,
            Table<VR> versionHead,
            Table<DR> dataHead,
            Table<VH> versionHistory,
            Table<DH> dataHistory,
            DSLContext context,
            ContributionRepository contributionRepository,
            SystemService systemService,
            TimeProvider timeProvider) {
        this.targetType = targetType;
        this.tables = new Tables(versionHead, dataHead, versionHistory, dataHistory);
        this.context = context;
        this.contributionRepository = contributionRepository;
        this.systemService = systemService;
        this.timeProvider = timeProvider;
    }

    public static ObjectVersionId buildObjectVersionId(
            UUID versionedObjectId, int sysVersion, SystemService systemService) {
        return new ObjectVersionId(
                versionedObjectId.toString(), systemService.getSystemId(), Integer.toString(sysVersion));
    }

    protected Optional<O> findHead(Condition condition) {
        SelectQuery<Record> locatableDataQuery = buildLocatableDataQuery(condition, true);
        return toLocatable(locatableDataQuery.fetchOne(), getLocatableClass());
    }

    public Optional<O> findByVersion(Condition condition, Condition historyCondition, int version) {
        SelectQuery<Record /*<UUID, Integer, JSONB, …>*/> headQuery = buildLocatableDataQuery(condition, true);
        headQuery.addConditions(field(VERSION_PROTOTYPE.SYS_VERSION).eq(version));

        SelectQuery<Record /*<UUID, Integer, JSONB, …>*/> historyQuery =
                buildLocatableDataQuery(historyCondition, false);
        historyQuery.addConditions(field(VERSION_HISTORY_PROTOTYPE.SYS_VERSION).eq(version));

        Record /*<UUID, Integer, JSONB, …>*/ dataRecord =
                headQuery.unionAll(historyQuery).fetchOne();
        if (dataRecord == null && !isDeleted(condition, historyCondition, version)) {
            String typeName = targetType.name();
            throw new ObjectNotFoundException(typeName, "No %s with given ID found".formatted(typeName));
        }

        return toLocatable(dataRecord, getLocatableClass());
    }

    protected <T> Field<T> field(TableField<? extends AbstractRecordPrototype<?>, T> field) {
        if (field.getTable() instanceof AbstractTablePrototype t) {
            var targetTable =
                    switch (t) {
                        case ObjectVersionTablePrototype __ -> tables.versionHead;
                        case ObjectVersionHistoryTablePrototype __ -> tables.versionHistory;
                        case ObjectDataTablePrototype __ -> tables.dataHead;
                        case ObjectDataHistoryTablePrototype __ -> tables.dataHistory;
                    };
            return targetTable.field(field);
        } else {
            throw new IllegalArgumentException("Type of table not supported: %s".formatted(field.getTable()));
        }
    }

    protected Optional<VH> findRootRecordByVersion(Condition condition, Condition historyCondition, int version) {

        var head = tables.versionHead;
        var history = tables.versionHistory;

        Field[] historyFields = Stream.concat(
                        Arrays.stream(head.fields()),
                        Stream.of(VERSION_HISTORY_PROTOTYPE.SYS_PERIOD_UPPER, VERSION_HISTORY_PROTOTYPE.SYS_DELETED))
                .map(history::field)
                .toArray(Field[]::new);

        return versionHeadQueryExtended(context)
                .where(condition)
                .and(field(VERSION_PROTOTYPE.SYS_VERSION).eq(version))
                .unionAll(context.select(historyFields)
                        .from(history)
                        .where(historyCondition)
                        .and(history.field(VERSION_HISTORY_PROTOTYPE.SYS_VERSION)
                                .eq(version)))
                .fetchOptional()
                .map(r -> r.into(history));
    }

    public List<ObjectVersionId> findVersionIdsByContribution(UUID ehrId, UUID contributionId) {
        return context
                .select(field(VERSION_PROTOTYPE.VO_ID), field(VERSION_PROTOTYPE.SYS_VERSION))
                .from(tables.versionHead)
                .where(contributionCondition(ehrId, contributionId, tables.versionHead))
                .unionAll(context.select(
                                tables.versionHistory.field(VERSION_HISTORY_PROTOTYPE.VO_ID),
                                tables.versionHistory.field(VERSION_HISTORY_PROTOTYPE.SYS_VERSION))
                        .from(tables.versionHistory)
                        .where(contributionCondition(ehrId, contributionId, tables.versionHistory)))
                .orderBy(tables.versionHead.field(VERSION_PROTOTYPE.SYS_VERSION).asc())
                .stream()
                .map(r -> buildObjectVersionId(r.value1(), r.value2(), systemService))
                .toList();
    }

    protected Condition contributionCondition(UUID ehrId, UUID contributionId, Table<?> table) {
        return table.field(VERSION_PROTOTYPE.CONTRIBUTION_ID)
                .eq(contributionId)
                .and(table.field(VERSION_PROTOTYPE.EHR_ID).eq(ehrId));
    }

    protected boolean isDeleted(Condition condition, Condition historyCondition, Integer version) {
        return findRootRecordByVersion(condition, historyCondition, version)
                .filter(r -> r.get(field(VERSION_HISTORY_PROTOTYPE.SYS_DELETED)))
                .isPresent();
    }

    protected Optional<VH> findLatestHistoryRoot(Condition condition) {
        return context.selectFrom(tables.versionHistory)
                .where(condition)
                .orderBy(field(VERSION_HISTORY_PROTOTYPE.SYS_VERSION).desc())
                .limit(1)
                .fetchOptional();
    }

    protected void delete(
            UUID ehrId,
            Condition condition,
            int version,
            @Nullable UUID contributionId,
            @Nullable UUID auditId,
            String notfoundMessage) {

        Result<VH> versionHeads = findVersionHeadRecords(condition);

        if (versionHeads.isEmpty()) {
            // not found
            throw new ObjectNotFoundException(getLocatableClass().getSimpleName(), notfoundMessage);
        }
        if (versionHeads.size() > 1) {
            throw new IllegalArgumentException("The implementation is limited to deleting one entry");
        }

        // The record is recycled fot the delete entry in the history
        VH versionHead = versionHeads.getFirst();
        VH firstRecord = versionHead.into(tables.versionHistory);

        if (firstRecord.get(VERSION_HISTORY_PROTOTYPE.SYS_VERSION) != version) {
            // concurrent modification
            throw new StateConflictException(NOT_MATCH_LATEST_VERSION);
        }

        copyHeadToHistory(versionHead, createCurrentTime(firstRecord.get(VERSION_HISTORY_PROTOTYPE.SYS_PERIOD_LOWER)));

        deleteHead(condition, version, StateConflictException::new);

        UUID finalContributionId = Optional.ofNullable(contributionId)
                .orElseGet(() -> contributionRepository.createDefault(
                        ehrId, ContributionDataType.folder, ContributionChangeType.deleted));

        UUID finalAuditId = Optional.ofNullable(auditId)
                .orElseGet(() -> contributionRepository.createDefaultAudit(ContributionChangeType.deleted, targetType));

        firstRecord.set(VERSION_HISTORY_PROTOTYPE.SYS_DELETED, true);

        firstRecord.set(VERSION_HISTORY_PROTOTYPE.SYS_VERSION, version + 1);
        firstRecord.set(VERSION_HISTORY_PROTOTYPE.AUDIT_ID, finalAuditId);
        firstRecord.set(VERSION_HISTORY_PROTOTYPE.CONTRIBUTION_ID, finalContributionId);

        firstRecord.changed(true);

        firstRecord.insert();
    }

    protected Optional<OriginalVersion<O>> getOriginalVersion(
            Condition condition, Condition historyCondition, int version) {

        Optional<VH> root = findRootRecordByVersion(condition, historyCondition, version);

        if (root.isEmpty()) {
            return Optional.empty();
        }
        VH versionRecord = root.get();

        // create data for output, i.e. fields of the OriginalVersion<Composition>
        ObjectVersionId versionId =
                buildObjectVersionId(versionRecord.get(VERSION_HISTORY_PROTOTYPE.VO_ID), version, systemService);
        DvCodedText lifecycleState = new DvCodedText(
                "complete", new CodePhrase("532")); // TODO: once lifecycle state is supported, get it here dynamically
        AuditDetails commitAudit =
                contributionRepository.findAuditDetails(versionRecord.get(VERSION_HISTORY_PROTOTYPE.AUDIT_ID));
        ObjectRef<HierObjectId> objectRef = new ObjectRef<>(
                new HierObjectId(versionRecord
                        .get(VERSION_HISTORY_PROTOTYPE.CONTRIBUTION_ID)
                        .toString()),
                "openehr",
                "contribution");

        ObjectVersionId precedingVersionId = null;
        // check if there is a preceding version and set it, if available
        if (version > 1) {
            // in the current scope version is an int and therefore: preceding = current - 1
            precedingVersionId = buildObjectVersionId(
                    versionRecord.get(VERSION_HISTORY_PROTOTYPE.VO_ID), version - 1, systemService);
        }

        Optional<O> composition = findByVersion(condition, historyCondition, version);
        OriginalVersion<O> originalVersion = new OriginalVersion<>(
                versionId,
                precedingVersionId,
                composition.orElse(null),
                lifecycleState,
                commitAudit,
                objectRef,
                null,
                null,
                null);

        return Optional.of(originalVersion);
    }

    protected boolean hasEhr(UUID ehrId) {
        return context.fetchExists(Ehr.EHR_, Ehr.EHR_.ID.eq(ehrId));
    }

    protected abstract Class<O> getLocatableClass();

    public static int extractVersion(UIDBasedId uid) {
        return Integer.parseInt(((ObjectVersionId) uid).getVersionTreeId().getValue());
    }

    public static UUID extractUid(UIDBasedId uid) {

        return UUID.fromString(uid.getRoot().getValue());
    }

    public static String extractSystemId(UIDBasedId uid) {
        return ((ObjectVersionId) uid).getCreatingSystemId().getValue();
    }

    protected void commitHead(
            UUID ehrId,
            Locatable versionDataObject,
            @Nullable UUID contributionId,
            @Nullable UUID auditId,
            ContributionChangeType changeType,
            Consumer<VR> addVersionFieldsFunction,
            BiConsumer<StructureNode, DR> addDataFieldsFunction) {

        UUID finalContributionId = Optional.ofNullable(contributionId)
                .orElseGet(() ->
                        contributionRepository.createDefault(ehrId, ContributionDataType.composition, changeType));

        UUID finalAuditId = Optional.ofNullable(auditId)
                .orElseGet(() -> contributionRepository.createDefaultAudit(changeType, targetType));

        VersionDataDbRecord versionData = toRecords(ehrId, versionDataObject, finalContributionId, finalAuditId);

        // Version
        VR versionRecord = versionData.versionRecord().into(tables.versionHead);
        addVersionFieldsFunction.accept(versionRecord);
        versionRecord.store();

        // Data
        RepositoryHelper.executeBulkInsert(
                context,
                versionData.dataRecords().get().map(r -> {
                    var v = r.getValue().into(tables.dataHead);
                    addDataFieldsFunction.accept(r.getKey(), v);
                    return v;
                }),
                tables.dataHead);
    }

    protected final VersionDataDbRecord toRecords(
            UUID ehrId, Locatable versionDataObject, UUID contributionId, UUID auditId) {
        return VersionDataDbRecord.toRecords(
                ehrId, versionDataObject, contributionId, auditId, timeProvider.getNow(), context);
    }

    public void update(
            UUID ehrId,
            O versionedObject,
            Condition condition,
            Condition historyCondition,
            @Nullable UUID contributionId,
            @Nullable UUID auditId,
            Consumer<VR> addVersionFieldsFunction,
            BiConsumer<StructureNode, DR> addDataFieldsFunction,
            String notFoundErrorMessage) {

        UIDBasedId nextUid = versionedObject.getUid();

        Result<VH> versionHeads = findVersionHeadRecords(condition);
        if (versionHeads.size() > 1) {
            throw new IllegalArgumentException("%d versions were returned".formatted(versionHeads.size()));
        }

        int headVersion;
        UUID headVoId;
        OffsetDateTime now;
        VH delRecord;

        if (versionHeads.isEmpty()) {

            Optional<VH> latestHistoryRoot = findLatestHistoryRoot(historyCondition);
            if (latestHistoryRoot.isEmpty()) {

                // sanity check for existing ehr uid - this provides a more precise error
                if (!hasEhr(ehrId)) {
                    throw new ObjectNotFoundException("EHR", "EHR %s does not exist".formatted(ehrId));
                }

                // not found
                throw new ObjectNotFoundException(getLocatableClass().getSimpleName(), notFoundErrorMessage);
            }

            delRecord = latestHistoryRoot
                    .filter(r -> r.get(VERSION_HISTORY_PROTOTYPE.SYS_DELETED))
                    .orElseThrow(() -> new PreconditionFailedException(NOT_MATCH_LATEST_VERSION));

            headVersion = delRecord.get(VERSION_HISTORY_PROTOTYPE.SYS_VERSION);
            headVoId = delRecord.get(VERSION_HISTORY_PROTOTYPE.VO_ID);
            now = createCurrentTime(delRecord.get(VERSION_HISTORY_PROTOTYPE.SYS_PERIOD_LOWER));

        } else {
            delRecord = null;
            VH root = versionHeads.getFirst();
            headVersion = root.get(VERSION_HISTORY_PROTOTYPE.SYS_VERSION);
            headVoId = root.get(VERSION_HISTORY_PROTOTYPE.VO_ID);
            now = createCurrentTime(root.get(VERSION_HISTORY_PROTOTYPE.SYS_PERIOD_LOWER));
        }

        // sanity check: valid next uid in system with version
        checkIsNextHeadVoId(headVoId, headVersion, nextUid);

        if (delRecord != null) {
            // update delete record period
            delRecord.set(VERSION_HISTORY_PROTOTYPE.SYS_PERIOD_UPPER, now);
            int updateCount = context.executeUpdate(delRecord);
            if (updateCount != 1) {
                // concurrent modification
                throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
            }

        } else {
            copyHeadToHistory(versionHeads.getFirst(), now);
            deleteHead(condition, headVersion, PreconditionFailedException::new);
        }

        // commit new version
        commitHead(
                ehrId,
                versionedObject,
                contributionId,
                auditId,
                ContributionChangeType.modification,
                addVersionFieldsFunction,
                addDataFieldsFunction);
    }

    /**
     * When reading some types of versioned objects additional data may be needed
     *
     * @param versionTable
     * @param dataTable
     * @param head
     * @return
     */
    protected Field<?>[] getAdditionalSelectFields(Table<?> versionTable, Table<?> dataTable, boolean head) {
        return null;
    }

    /**
     *
     * @param condition
     * @param head
     * @return SelectQuery<Record<UUID, Integer, JSONB, ...>
     */
    protected SelectQuery<Record> buildLocatableDataQuery(Condition condition, boolean head) {
        Table<?> versionTable = tables.get(true, head);
        Table<?> dataTable = tables.get(false, head);

        Field<UUID> voIdField = versionTable.field(VERSION_PROTOTYPE.VO_ID);
        Field<Integer> sysVersionField = versionTable.field(VERSION_PROTOTYPE.SYS_VERSION);
        Field<JSONB> jsonbField = jsonbDataAggregation(dataTable);

        List<Field<?>> selectFields;
        List<Field<?>> groupByFields;
        Field<?>[] additionalFields = getAdditionalSelectFields(versionTable, dataTable, head);
        if (additionalFields == null) {
            selectFields = List.of(voIdField, sysVersionField, jsonbField);
            groupByFields = List.of(voIdField, sysVersionField);

        } else {
            selectFields = new ArrayList<>(3 + additionalFields.length);
            groupByFields = new ArrayList<>(2 + additionalFields.length);
            selectFields.add(voIdField);
            selectFields.add(sysVersionField);
            selectFields.add(jsonbField);
            Collections.addAll(selectFields, additionalFields);
            groupByFields.add(voIdField);
            groupByFields.add(sysVersionField);
            Collections.addAll(groupByFields, additionalFields);
        }

        return fromJoinedVersionData(context.select(selectFields), head)
                .where(condition)
                .groupBy(groupByFields)
                .getQuery();
    }

    protected Field<JSONB> jsonbDataAggregation(Table<?> dataTable) {
        return DSL.jsonbObjectAgg(dataTable.field(DATA_PROTOTYPE.ENTITY_IDX), dataTable.field(DATA_PROTOTYPE.DATA))
                .as(DSL.name("data"));
    }

    protected <S extends SelectFromStep<R>, R extends Record> SelectOnConditionStep<R> fromJoinedVersionData(
            SelectFromStep<R> select, boolean head) {
        Table<?> versionTable = tables.get(true, head);
        Table<?> dataTable = tables.get(false, head);

        Condition joinCondition =
                versionDataJoinCondition(f -> versionTable.field(f).eq(dataTable.field(f)));

        if (!head) {
            joinCondition = joinCondition.and(versionTable
                    .field(VERSION_PROTOTYPE.SYS_VERSION)
                    .eq(dataTable.field(DATA_HISTORY_PROTOTYPE.SYS_VERSION)));
        }
        return select.from(versionTable).join(dataTable).on(joinCondition);
    }

    protected abstract List<TableField<VR, ?>> getVersionDataJoinFields();

    private Condition versionDataJoinCondition(Function<Field, Condition> fieldConditionCreator) {
        var versionDataJoinFields = getVersionDataJoinFields();
        Condition joinCondition;
        if (versionDataJoinFields.size() == 1) {
            joinCondition = fieldConditionCreator.apply(versionDataJoinFields.getFirst());
        } else {
            joinCondition = DSL.and(
                    versionDataJoinFields.stream().map(fieldConditionCreator).toList());
        }
        return joinCondition;
    }

    protected Condition dataRootCondition(Table<?> dataTable) {
        return dataTable.field(DATA_PROTOTYPE.NUM).eq(0);
    }

    protected Optional<ObjectVersionId> findVersionByTime(
            Condition condition, Condition historyCondition, OffsetDateTime time) {

        SelectLimitPercentStep<Record2<Integer, UUID>> headQuery = context.select(
                        field(VERSION_PROTOTYPE.SYS_VERSION), field(VERSION_PROTOTYPE.VO_ID))
                .from(tables.versionHead)
                .where(field(VERSION_PROTOTYPE.SYS_PERIOD_LOWER).lessOrEqual(time))
                .and(condition)
                .limit(1);

        SelectLimitPercentStep<Record2<Integer, UUID>> historyQuery = context.select(
                        field(VERSION_HISTORY_PROTOTYPE.SYS_VERSION), field(VERSION_HISTORY_PROTOTYPE.VO_ID))
                .from(tables.versionHistory)
                .where(
                        field(VERSION_HISTORY_PROTOTYPE.SYS_PERIOD_LOWER).lessOrEqual(time),
                        field(VERSION_HISTORY_PROTOTYPE.SYS_PERIOD_UPPER)
                                .greaterThan(time)
                                .or(field(VERSION_HISTORY_PROTOTYPE.SYS_PERIOD_UPPER)
                                        .isNull()))
                .and((historyCondition))
                .limit(1);

        return headQuery
                .unionAll(historyQuery)
                .limit(1)
                .fetchOptional()
                .map(r -> buildObjectVersionId(r.value2(), r.value1(), systemService));
    }

    /**
     *
     * @param jsonbRecord {vo_id, sys_version, jsonData}
     * @param locatableClass
     * @return
     * @param <L>
     */
    protected <L extends Locatable> Optional<L> toLocatable(
            Record /*<UUID, Integer, JSONB, …>*/ jsonbRecord, Class<L> locatableClass) {
        if (jsonbRecord == null) {
            return Optional.empty();
        }
        final L rmObject = DbToRmFormat.reconstructRmObject(
                locatableClass, jsonbRecord.get(2, JSONB.class).data());
        rmObject.setUid(
                buildObjectVersionId(jsonbRecord.get(0, UUID.class), jsonbRecord.get(1, Integer.class), systemService));
        return Optional.of(rmObject);
    }

    protected void copyHeadToHistory(VH versionRecord, OffsetDateTime now) {

        // copy version to history
        versionRecord.set(VERSION_HISTORY_PROTOTYPE.SYS_PERIOD_UPPER, now);
        versionRecord.set(VERSION_HISTORY_PROTOTYPE.SYS_DELETED, false);
        versionRecord.changed(true);
        versionRecord.insert();

        Field<?>[] headFields = Stream.concat(
                        Arrays.stream(tables.dataHead.fields()), Stream.of(field(VERSION_PROTOTYPE.SYS_VERSION)))
                .toArray(Field<?>[]::new);
        Field<?>[] historyFields =
                Arrays.stream(headFields).map(tables.dataHistory::field).toArray(Field<?>[]::new);

        InsertQuery<DH> dataInsert = context.insertQuery(tables.dataHistory);

        // copy data directly: select by join fields, add sys_version
        Condition joinCondition =
                versionDataJoinCondition(f -> tables.dataHead.field(f).eq(DSL.val(versionRecord.get(f))));

        SelectConditionStep<Record> dataSelect =
                fromJoinedVersionData(context.select(headFields), true).where(joinCondition);

        dataInsert.setSelect(historyFields, dataSelect);
        dataInsert.execute();
    }

    protected void deleteHead(
            Condition versionCondition, int oldVersion, Function<String, RuntimeException> exceptionProvider) {

        // delete head
        int deleteCount = context.deleteFrom(tables.versionHead)
                .where(versionCondition.and(field(VERSION_PROTOTYPE.SYS_VERSION).eq(oldVersion)))
                .execute();

        if (deleteCount == 0) {
            // concurrent modification
            throw exceptionProvider.apply(NOT_MATCH_LATEST_VERSION);
        }
    }

    /**
     * version head + empty history fields:
     * SYS_PERIOD_UPPER,
     * SYS_DELETED
     *
     * @param context
     * @return
     */
    protected SelectJoinStep<Record> versionHeadQueryExtended(DSLContext context) {
        return context.select(tables.versionHead.fields())
                .select(
                        DSL.inline((Object) null).as(VERSION_HISTORY_PROTOTYPE.SYS_PERIOD_UPPER.getName()),
                        DSL.inline(false).as(VERSION_HISTORY_PROTOTYPE.SYS_DELETED.getName()))
                .from(tables.versionHead);
    }

    protected Result<VH> findVersionHeadRecords(Condition condition) {
        return versionHeadQueryExtended(context).where(condition).fetchInto(tables.versionHistory);
    }

    protected Field<String> jsonDataField(Table<DR> table, String... path) {
        return AdditionalSQLFunctions.jsonbAttributePathText(table.field(DATA_PROTOTYPE.DATA), path);
    }

    /**
     * Determines the current time.
     *
     * @param lowerBound For proper version intervals the value is guaranteed to be at least 1 microsecond after lowerBound
     * @return
     */
    protected OffsetDateTime createCurrentTime(OffsetDateTime lowerBound) {
        OffsetDateTime now = timeProvider.getNow();
        // sysPeriodUpper must be after sysPeriodLower for proper intervals
        if (now.isAfter(lowerBound)) {
            return now;
        }
        // Add one microsecond, so the interval is valid.
        // Resolution of postgres timestamps is 1 microsecond
        // https://www.postgresql.org/docs/14/datatype-datetime.html#DATATYPE-DATETIME-TABLE
        return lowerBound.plusNanos(1_000);
    }

    protected void checkIsNextHeadVoId(UUID headVoid, int headVersion, UIDBasedId uid) {

        // uuid missmatch
        if (!Objects.equals(headVoid, extractUid(uid))) {
            throw new PreconditionFailedException(NOT_MATCH_UID);
        }
        // system id mismatch
        if (!Objects.equals(systemService.getSystemId(), extractSystemId(uid))) {
            throw new PreconditionFailedException(NOT_MATCH_SYSTEM_ID);
        }
        // versions not consecutive
        if ((headVersion + 1) != extractVersion(uid)) {
            throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
        }
    }
}
