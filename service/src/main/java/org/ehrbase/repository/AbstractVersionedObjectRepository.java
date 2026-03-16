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

import static org.ehrbase.jooq.pg.Tables.AUDIT_DETAILS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.generic.RevisionHistoryItem;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.PrimitiveIterator.OfInt;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.ResourceGoneException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.api.util.LocatableUtils;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.Ehr;
import org.ehrbase.jooq.pg.util.AdditionalSQLFunctions;
import org.ehrbase.openehr.dbformat.DbToRmFormat;
import org.ehrbase.openehr.dbformat.StructureNode;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectDataTablePrototype;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectHistoryTablePrototype;
import org.ehrbase.openehr.dbformat.jooq.prototypes.ObjectVersionTablePrototype;
import org.ehrbase.openehr.dbformat.json.RmDbJson;
import org.ehrbase.service.TimeProvider;
import org.jooq.CaseConditionStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.JoinType;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectHavingStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectLimitPercentStep;
import org.jooq.SelectOrderByStep;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public abstract class AbstractVersionedObjectRepository<
        VR extends UpdatableRecord, DR extends UpdatableRecord, HR extends UpdatableRecord, O extends Locatable> {

    protected record Tables<V extends Record, D extends Record, H extends Record>(
            Table<V> versionHead, Table<D> dataHead, Table<H> history) {}

    protected record VersionDataJoin(Table<?> versionTable, Table<?> dataTable, Table<?> joined) {}

    public record AdditionalDataQuerySelectFields(Field<?>[] selectFields, Field<?>[] groupByFields) {}

    public static final String NOT_MATCH_UID = "If-Match version_uid does not match uid";
    public static final String NOT_MATCH_SYSTEM_ID = "If-Match version_uid does not match system id";
    public static final String NOT_MATCH_LATEST_VERSION = "If-Match version_uid does not match latest version";
    public static final String GONE_MARKER = "GONE";

    public static final ObjectVersionTablePrototype VERSION_PROTOTYPE = ObjectVersionTablePrototype.INSTANCE;
    public static final ObjectHistoryTablePrototype HISTORY_PROTOTYPE = ObjectHistoryTablePrototype.INSTANCE;
    public static final ObjectDataTablePrototype DATA_PROTOTYPE = ObjectDataTablePrototype.INSTANCE;

    private final AuditDetailsTargetType targetType;

    protected final Tables<VR, DR, HR> tables;
    protected final DSLContext context;
    protected final ContributionRepository contributionRepository;
    protected final SystemService systemService;
    protected final TimeProvider timeProvider;

    protected AbstractVersionedObjectRepository(
            AuditDetailsTargetType targetType,
            Table<VR> versionHead,
            Table<DR> dataHead,
            Table<HR> versionHistory,
            DSLContext context,
            ContributionRepository contributionRepository,
            SystemService systemService,
            TimeProvider timeProvider) {
        this.targetType = targetType;
        this.tables = new Tables<>(versionHead.as("version"), dataHead.as("data"), versionHistory.as("history"));
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

    protected Optional<O> findHead(Function<Table<?>, Condition> condition) {
        SelectQuery<Record> locatableDataQuery = buildLocatableDataQuery(condition, true);
        return toRootLocatable(locatableDataQuery.fetchOne(), getLocatableClass());
    }

    public Optional<O> findByVersion(
            Function<Table<?>, Condition> condition, Function<Table<?>, Condition> historyCondition, int version) {
        SelectQuery<Record /*<UUID, Integer, JSONB, …>*/> headQuery = buildLocatableDataQuery(condition, true);
        headQuery.addConditions(
                tables.versionHead().field(VERSION_PROTOTYPE.SYS_VERSION).eq(version));

        SelectQuery<Record /*<UUID, Integer, JSONB, …>*/> historyQuery =
                buildLocatableDataQuery(historyCondition, false);
        historyQuery.addConditions(
                tables.history().field(HISTORY_PROTOTYPE.SYS_VERSION).eq(version));

        Record /*<UUID, Integer, JSONB, …>*/ dataRecord =
                headQuery.unionAll(historyQuery).fetchOne();
        if (dataRecord == null && !isDeleted(condition, historyCondition, version)) {
            String typeName = targetType.name();
            throw new ObjectNotFoundException(typeName, "No %s with given ID found".formatted(typeName));
        }

        return toRootLocatable(dataRecord, getLocatableClass());
    }

    protected Optional<HR> findRootRecordByVersion(
            Function<Table<?>, Condition> condition, Function<Table<?>, Condition> historyCondition, int version) {

        Table<VR> head = tables.versionHead();
        Table<HR> history = tables.history();

        Field[] historyFields = Stream.concat(
                        Arrays.stream(head.fields()),
                        Stream.of(HISTORY_PROTOTYPE.SYS_PERIOD_UPPER, HISTORY_PROTOTYPE.SYS_DELETED))
                .map(history::field)
                .map(f -> f == null ? DSL.inline((String) null) : f)
                .toArray(Field[]::new);

        return versionHeadQueryExtended(head, context)
                .where(condition.apply(head))
                .and(head.field(VERSION_PROTOTYPE.SYS_VERSION).eq(version))
                .unionAll(context.select(historyFields)
                        .from(history)
                        .where(historyCondition.apply(history))
                        .and(history.field(HISTORY_PROTOTYPE.SYS_VERSION).eq(version)))
                .fetchOptional()
                .map(r -> r.into(history));
    }

    public SelectOrderByStep<Record3<String, UUID, Integer>> buildVersionIdsByContributionQuery(
            String rmKey, UUID ehrId, UUID contributionId) {
        Table<VR> versionHead = tables.versionHead();
        Table<HR> history = tables.history();
        return context.select(
                        DSL.inline(rmKey),
                        versionHead.field(VERSION_PROTOTYPE.VO_ID),
                        versionHead.field(VERSION_PROTOTYPE.SYS_VERSION))
                .from(versionHead)
                .where(contributionCondition(ehrId, contributionId, versionHead))
                .unionAll(context.select(
                                DSL.inline(rmKey),
                                history.field(HISTORY_PROTOTYPE.VO_ID),
                                history.field(HISTORY_PROTOTYPE.SYS_VERSION))
                        .from(history)
                        .where(contributionCondition(ehrId, contributionId, history)));
    }

    protected Condition contributionCondition(UUID ehrId, UUID contributionId, Table<?> table) {
        return table.field(VERSION_PROTOTYPE.CONTRIBUTION_ID)
                .eq(contributionId)
                .and(table.field(VERSION_PROTOTYPE.EHR_ID).eq(ehrId));
    }

    protected boolean isDeleted(
            Function<Table<?>, Condition> condition, Function<Table<?>, Condition> historyCondition, Integer version) {
        return findRootRecordByVersion(condition, historyCondition, version)
                .filter(r -> r.get(HISTORY_PROTOTYPE.SYS_DELETED))
                .isPresent();
    }

    protected Optional<HR> findLatestHistoryRoot(Function<Table<?>, Condition> condition) {
        Table<HR> history = tables.history();
        return context.selectFrom(history)
                .where(condition.apply(history))
                .orderBy(history.field(HISTORY_PROTOTYPE.SYS_VERSION).desc())
                .limit(1)
                .fetchOptional();
    }

    protected void delete(
            UUID ehrId,
            Function<Table<?>, Condition> condition,
            int version,
            UUID contributionId,
            UUID auditId,
            String notfoundMessage) {

        Result<HR> versionHeads = findVersionHeadRecords(condition);

        if (versionHeads.isEmpty()) {
            // not found
            throw new ObjectNotFoundException(getLocatableClass().getSimpleName(), notfoundMessage);
        }
        if (versionHeads.size() > 1) {
            throw new IllegalArgumentException("The implementation is limited to deleting one entry");
        }

        // The record is recycled fot the delete entry in the history
        HR versionHead = versionHeads.getFirst();
        HR firstRecord = versionHead.into(tables.history());

        if (firstRecord.get(HISTORY_PROTOTYPE.SYS_VERSION) != version) {
            // concurrent modification
            throw new StateConflictException(NOT_MATCH_LATEST_VERSION);
        }

        OffsetDateTime now = createCurrentTime(firstRecord.get(HISTORY_PROTOTYPE.SYS_PERIOD_LOWER));
        copyHeadToHistory(versionHead, now);

        deleteHead(condition, version, StateConflictException::new);

        UUID finalContributionId = Optional.ofNullable(contributionId)
                .orElseGet(() -> contributionRepository.createDefault(
                        ehrId, ContributionDataType.folder, ContributionChangeType.deleted));

        UUID finalAuditId = Optional.ofNullable(auditId)
                .orElseGet(() -> contributionRepository.createDefaultAudit(ContributionChangeType.deleted, targetType));

        firstRecord.set(HISTORY_PROTOTYPE.SYS_DELETED, true);

        firstRecord.set(HISTORY_PROTOTYPE.SYS_VERSION, version + 1);
        firstRecord.set(HISTORY_PROTOTYPE.AUDIT_ID, finalAuditId);
        firstRecord.set(HISTORY_PROTOTYPE.CONTRIBUTION_ID, finalContributionId);
        firstRecord.set(HISTORY_PROTOTYPE.SYS_PERIOD_LOWER, now);

        firstRecord.changed(true);

        firstRecord.insert();
    }

    protected RevisionHistory getRevisionHistory(
            Function<Table<?>, Condition> condition, Function<Table<?>, Condition> historyCondition) {

        String systemId = systemService.getSystemId();
        Table<VR> vt = tables.versionHead();
        SelectConditionStep<Record> versionSq = context.select(
                        vt.fields(VERSION_PROTOTYPE.VO_ID, VERSION_PROTOTYPE.SYS_VERSION, VERSION_PROTOTYPE.AUDIT_ID))
                .from(vt)
                .where(condition.apply(vt));
        Table<HR> ht = tables.history();
        SelectConditionStep<Record> versionHistorySq = context.select(
                        ht.fields(HISTORY_PROTOTYPE.VO_ID, HISTORY_PROTOTYPE.SYS_VERSION, VERSION_PROTOTYPE.AUDIT_ID))
                .from(ht)
                .where(historyCondition.apply(ht));
        SelectOrderByStep<Record> union = context.selectFrom(versionSq).unionAll(versionHistorySq);
        List<RevisionHistoryItem> revisionHistoryItems = context.select(
                        union.field(VERSION_PROTOTYPE.VO_ID), union.field(VERSION_PROTOTYPE.SYS_VERSION), AUDIT_DETAILS)
                .from(union)
                .join(AUDIT_DETAILS, JoinType.LEFT_OUTER_JOIN)
                .on(union.field(VERSION_PROTOTYPE.AUDIT_ID).eq(AUDIT_DETAILS.ID))
                .orderBy(union.field(VERSION_PROTOTYPE.SYS_VERSION))
                .fetch(r -> {
                    ObjectVersionId vid = new ObjectVersionId(
                            r.value1().toString(), systemId, r.value2().toString());
                    // Note: is List but only has more than one item when there are contributions regarding this
                    // object of change type attestation (currently not supported)
                    List<AuditDetails> auditList = new ArrayList<>();
                    AuditDetails auditDetails = contributionRepository.mapToAuditDetails(r.value3());
                    auditList.add(auditDetails);
                    return new RevisionHistoryItem(vid, auditList);
                });

        return new RevisionHistory(revisionHistoryItems);
    }

    protected Optional<OriginalVersion<O>> getOriginalVersion(
            Function<Table<?>, Condition> condition, Function<Table<?>, Condition> historyCondition, int version) {

        Optional<HR> root = findRootRecordByVersion(condition, historyCondition, version);

        if (root.isEmpty()) {
            return Optional.empty();
        }
        HR versionRecord = root.get();

        // create data for output, i.e. fields of the OriginalVersion<Composition>
        ObjectVersionId versionId =
                buildObjectVersionId(versionRecord.get(HISTORY_PROTOTYPE.VO_ID), version, systemService);
        DvCodedText lifecycleState = new DvCodedText(
                "complete", new CodePhrase("532")); // TODO: once lifecycle state is supported, get it here dynamically
        AuditDetails commitAudit =
                contributionRepository.findAuditDetails(versionRecord.get(HISTORY_PROTOTYPE.AUDIT_ID));
        ObjectRef<HierObjectId> objectRef = new ObjectRef<>(
                new HierObjectId(
                        versionRecord.get(HISTORY_PROTOTYPE.CONTRIBUTION_ID).toString()),
                "openehr",
                "CONTRIBUTION");

        ObjectVersionId precedingVersionId = null;
        // check if there is a preceding version and set it, if available
        if (version > 1) {
            // in the current scope version is an int and therefore: preceding = current - 1
            precedingVersionId =
                    buildObjectVersionId(versionRecord.get(HISTORY_PROTOTYPE.VO_ID), version - 1, systemService);
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

    public abstract Class<O> getLocatableClass();

    public static UUID extractUid(UIDBasedId uid) {
        return LocatableUtils.getUuid(uid);
    }

    public static String extractSystemId(UIDBasedId uid) {
        return ((ObjectVersionId) uid).getCreatingSystemId().getValue();
    }

    protected void commitHead(
            UUID ehrId,
            O versionDataObject,
            UUID contributionId,
            UUID auditId,
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
        VR versionRecord = versionData.versionRecord().into(tables.versionHead());
        addVersionFieldsFunction.accept(versionRecord);
        versionRecord.store();

        // Data
        RepositoryHelper.executeBulkInsert(
                context,
                versionData.dataRecords().get().map(r -> {
                    var v = r.getValue().into(tables.dataHead());
                    addDataFieldsFunction.accept(r.getKey(), v);
                    return v;
                }),
                tables.dataHead());
    }

    protected final VersionDataDbRecord toRecords(
            UUID ehrId, Locatable versionDataObject, UUID contributionId, UUID auditId) {
        return VersionDataDbRecord.toRecords(
                ehrId, versionDataObject, contributionId, auditId, timeProvider.getNow(), context);
    }

    public void update(
            UUID ehrId,
            O versionedObject,
            Function<Table<?>, Condition> condition,
            Function<Table<?>, Condition> historyCondition,
            UUID contributionId,
            UUID auditId,
            Consumer<VR> addVersionFieldsFunction,
            BiConsumer<StructureNode, DR> addDataFieldsFunction,
            String notFoundErrorMessage) {

        UIDBasedId nextUid = versionedObject.getUid();

        Result<HR> versionHeads = findVersionHeadRecords(condition);
        if (versionHeads.size() > 1) {
            throw new IllegalArgumentException("%d versions were returned".formatted(versionHeads.size()));
        }

        int headVersion;
        UUID headVoId;
        OffsetDateTime now;
        HR delRecord;

        if (versionHeads.isEmpty()) {

            Optional<HR> latestHistoryRoot = findLatestHistoryRoot(historyCondition);
            if (latestHistoryRoot.isEmpty()) {

                // sanity check for existing ehr uid - this provides a more precise error
                if (!hasEhr(ehrId)) {
                    throw new ObjectNotFoundException("EHR", "EHR %s does not exist".formatted(ehrId));
                }

                // not found
                throw new ObjectNotFoundException(getLocatableClass().getSimpleName(), notFoundErrorMessage);
            }

            delRecord = latestHistoryRoot
                    .filter(r -> r.get(HISTORY_PROTOTYPE.SYS_DELETED))
                    .orElseThrow(() -> new PreconditionFailedException(NOT_MATCH_LATEST_VERSION));

            headVersion = delRecord.get(HISTORY_PROTOTYPE.SYS_VERSION);
            headVoId = delRecord.get(HISTORY_PROTOTYPE.VO_ID);
            now = createCurrentTime(delRecord.get(HISTORY_PROTOTYPE.SYS_PERIOD_LOWER));

        } else {
            delRecord = null;
            HR root = versionHeads.getFirst();
            headVersion = root.get(HISTORY_PROTOTYPE.SYS_VERSION);
            headVoId = root.get(HISTORY_PROTOTYPE.VO_ID);
            now = createCurrentTime(root.get(HISTORY_PROTOTYPE.SYS_PERIOD_LOWER));
        }

        // sanity check: valid next uid in system with version
        checkIsNextHeadVoId(headVoId, headVersion, nextUid);

        if (delRecord != null) {
            // update delete record period
            delRecord.set(HISTORY_PROTOTYPE.SYS_PERIOD_UPPER, now);
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
    protected AdditionalDataQuerySelectFields getAdditionalDataQuerySelectFields(
            Table<?> versionTable, Table<?> dataTable, boolean head) {
        return null;
    }

    /**
     *
     * @param condition
     * @param head
     * @return SelectQuery<Record<UUID, Integer, String, ...>
     */
    protected SelectQuery<Record> buildLocatableDataQuery(Function<Table<?>, Condition> condition, boolean head) {

        VersionDataJoin versionDataJoin = fromJoinedVersionData(head);
        Table<?> versionTable = versionDataJoin.versionTable();
        Table<?> dataTable = versionDataJoin.dataTable();

        Field<UUID> voIdField = versionTable.field(VERSION_PROTOTYPE.VO_ID);
        Field<Integer> sysVersionField = versionTable.field(VERSION_PROTOTYPE.SYS_VERSION);
        Field<String> stringAggregationField;
        if (head) {
            stringAggregationField = stringAggregation((Table<DR>) dataTable);
        } else {
            stringAggregationField = historyDataField(dataTable);
        }

        List<Field<?>> selectFields;
        List<Field<?>> groupByFields = Collections.emptyList();
        AdditionalDataQuerySelectFields additionalFields =
                getAdditionalDataQuerySelectFields(versionTable, dataTable, head);
        if (additionalFields == null) {
            selectFields = List.of(voIdField, sysVersionField, stringAggregationField);
            if (head) {
                groupByFields = List.of(voIdField, sysVersionField);
            }
        } else {
            Field<?>[] additionalSelectFields = additionalFields.selectFields();
            selectFields = new ArrayList<>(3 + additionalSelectFields.length);
            selectFields.add(voIdField);
            selectFields.add(sysVersionField);
            selectFields.add(stringAggregationField);
            Collections.addAll(selectFields, additionalSelectFields);
            if (head) {
                Field<?>[] additionalGroupByFields = additionalFields.groupByFields();
                groupByFields = new ArrayList<>(2 + additionalGroupByFields.length);
                groupByFields.add(voIdField);
                groupByFields.add(sysVersionField);
                Collections.addAll(groupByFields, additionalGroupByFields);
            }
        }

        SelectConditionStep<Record> query = context.select(selectFields)
                .from(versionDataJoin.joined())
                .where(condition.apply(versionDataJoin.joined()));
        return (groupByFields.isEmpty() ? query : query.groupBy(groupByFields)).getQuery();
    }

    protected Field<String> historyDataField(final Table<?> dataTable) {
        return historyDataGoneCase(dataTable).else_(dataTable.field(HISTORY_PROTOTYPE.OV_DATA));
    }

    public static CaseConditionStep<String> historyDataGoneCase(final Table<?> dataTable) {
        return DSL.case_()
                .when(
                        dataTable
                                .field(HISTORY_PROTOTYPE.OV_REF)
                                .isNull()
                                .and(dataTable.field(HISTORY_PROTOTYPE.OV_DATA).isNull())
                                .and(DSL.not(dataTable.field(HISTORY_PROTOTYPE.SYS_DELETED))),
                        DSL.inline(GONE_MARKER));
    }

    protected VersionDataJoin fromJoinedVersionData(boolean head) {
        if (head) {
            Table<?> versionTable = tables.versionHead();
            Table<?> dataTable = tables.dataHead();
            Condition joinCondition =
                    versionDataJoinCondition(f -> versionTable.field(f).eq(dataTable.field(f)));
            return new VersionDataJoin(
                    versionTable, dataTable, versionTable.join(dataTable).on(joinCondition));
        } else {
            Table<?> historyTable = tables.history();
            return new VersionDataJoin(historyTable, historyTable, historyTable);
        }
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
            Function<Table<?>, Condition> condition,
            Function<Table<?>, Condition> historyCondition,
            OffsetDateTime time) {

        Table<VR> versionHead = tables.versionHead();
        SelectLimitPercentStep<Record2<Integer, UUID>> headQuery = context.select(
                        versionHead.field(VERSION_PROTOTYPE.SYS_VERSION), versionHead.field(VERSION_PROTOTYPE.VO_ID))
                .from(versionHead)
                .where(versionHead.field(VERSION_PROTOTYPE.SYS_PERIOD_LOWER).lessOrEqual(time))
                .and(condition.apply(versionHead))
                .limit(1);

        Table<HR> history = tables.history();
        SelectLimitPercentStep<Record2<Integer, UUID>> historyQuery = context.select(
                        history.field(HISTORY_PROTOTYPE.SYS_VERSION), history.field(HISTORY_PROTOTYPE.VO_ID))
                .from(history)
                .where(
                        history.field(HISTORY_PROTOTYPE.SYS_PERIOD_LOWER).lessOrEqual(time),
                        history.field(HISTORY_PROTOTYPE.SYS_PERIOD_UPPER)
                                .greaterThan(time)
                                .or(history.field(HISTORY_PROTOTYPE.SYS_PERIOD_UPPER)
                                        .isNull()))
                .and(historyCondition.apply(history))
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
    protected <L extends Locatable> Optional<L> toRootLocatable(
            Record /*<UUID, Integer, String, …>*/ jsonbRecord, Class<L> locatableClass) {
        if (jsonbRecord == null || jsonbRecord.get(2) == null) {
            return Optional.empty();
        }
        UUID id = jsonbRecord.get(0, UUID.class);
        Integer version = jsonbRecord.get(1, Integer.class);
        if (GONE_MARKER.equals(jsonbRecord.get(2))) {
            throw new ResourceGoneException(
                    "Data for %s (id: %s, version: %s) gone".formatted(targetType, id, version));
        }
        String dbFormat = jsonbRecord.get(2, String.class);
        ObjectNode reconstructed = reconstruct(dbFormat, (p, idx) -> parseJsonData(p, jsonbRecord, idx));
        DbToRmFormat.revertDbInPlace(reconstructed, false, true, true);
        final Locatable rmObject;
        try {
            rmObject = RmDbJson.MARSHAL_OM.treeToValue(reconstructed, locatableClass);
        } catch (JsonProcessingException e) {
            throw new InternalServerException(e);
        }
        rmObject.setUid(buildObjectVersionId(id, version, systemService));
        return Optional.of((L) rmObject);
    }

    public static ObjectNode reconstruct(
            final String dbFormat,
            BiFunction<Pair<CharSequence, CharSequence>, Integer, Pair<CharSequence, ObjectNode>> parseFunc) {
        OfInt idx = IntStream.iterate(0, i -> i + 1).iterator();
        Pair<CharSequence, ObjectNode>[] parsed = Arrays.stream(DbToRmFormat.parseDbObjectAggregateString(dbFormat))
                .sequential()
                .map(p -> parseFunc.apply(p, idx.next()))
                .toArray(Pair[]::new);
        return DbToRmFormat.reconstructRmObjectTree(parsed, Pair::getLeft, Pair::getRight);
    }

    protected Pair<CharSequence, ObjectNode> parseJsonData(Pair<CharSequence, CharSequence> p, Record rec, int idx) {
        return parseRow(p);
    }

    public static Pair<CharSequence, ObjectNode> parseRow(final Pair<CharSequence, CharSequence> p) {
        try {
            return Pair.of(
                    p.getLeft(), (ObjectNode) RmDbJson.MARSHAL_OM.readTree(new CharSequenceReader(p.getRight())));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected Pair<Stream<Field<?>>, Stream<Field<?>>> additionalCopyToHistoryFields(
            final Table<VR> versionHead, final Table<DR> dataHead, OffsetDateTime now) {
        return Pair.of(
                Stream.of(DSL.inline(now), DSL.inline(false), DSL.castNull(Integer.class), stringAggregation(dataHead)),
                Stream.of(
                        HISTORY_PROTOTYPE.SYS_PERIOD_UPPER,
                        HISTORY_PROTOTYPE.SYS_DELETED,
                        HISTORY_PROTOTYPE.OV_REF,
                        HISTORY_PROTOTYPE.OV_DATA));
    }

    protected void copyHeadToHistory(HR historyRecord, OffsetDateTime now) {

        VersionDataJoin versionDataJoin = fromJoinedVersionData(true);
        Table<DR> dataHead = (Table<DR>) versionDataJoin.dataTable();
        Table<VR> versionHead = (Table<VR>) versionDataJoin.versionTable();
        Pair<Stream<Field<?>>, Stream<Field<?>>> additionalFields =
                additionalCopyToHistoryFields(versionHead, dataHead, now);
        Field<?>[] fields = Streams.concat(
                        // version fields which are also present in version_history
                        Arrays.stream(tables.history().fields())
                                .map(versionHead::field)
                                .filter(Objects::nonNull),
                        additionalFields.getLeft())
                .toArray(Field<?>[]::new);

        Condition pkeyConstraint = DSL.and(getVersionDataJoinFields().stream()
                .map(f -> {
                    Field val = DSL.val(historyRecord.get(f));
                    return tables.dataHead().field(f).eq(val);
                })
                .toArray(Condition[]::new));
        SelectHavingStep<Record> dataSelect = context.select(fields)
                .from(versionDataJoin.joined())
                .where(pkeyConstraint)
                .groupBy(versionHead.fields(versionHead.getPrimaryKey().getFieldsArray()));

        Field<?>[] historyFields = Streams.concat(Arrays.stream(versionHead.fields()), additionalFields.getRight())
                .map(tables.history()::field)
                .filter(Objects::nonNull)
                .toArray(Field<?>[]::new);

        context.insertInto(tables.history())
                .columns(historyFields)
                .select(dataSelect)
                .execute();
    }

    public Field<String> stringAggregation(final Table<DR> dataHead) {
        return AdditionalSQLFunctions.string_agg(
                getDataAggregationBase(dataHead),
                // \n control char cannot be present in jsonb -> use it as separator
                DSL.field("E'\\n'", String.class),
                dataHead.field(DATA_PROTOTYPE.NUM).asc());
    }

    protected Field<String> getDataAggregationBase(final Table<DR> dataHead) {
        Field<JSONB> dataField = dataHead.field(DATA_PROTOTYPE.DATA);
        return dataHead.field(DATA_PROTOTYPE.ENTITY_IDX)
                .concat(DSL.case_(dataHead.field(DATA_PROTOTYPE.NUM))
                        .when(DSL.inline(0), DSL.field("{0} - '" + DbToRmFormat.UID_ALIAS + "'", dataField))
                        .else_(dataField)
                        .cast(SQLDataType.CLOB));
    }

    protected void deleteHead(
            Function<Table<?>, Condition> versionCondition,
            int oldVersion,
            Function<String, RuntimeException> exceptionProvider) {

        // delete head
        Table<VR> table = tables.versionHead();
        int deleteCount = context.deleteFrom(table)
                .where(versionCondition
                        .apply(table)
                        .and(table.field(VERSION_PROTOTYPE.SYS_VERSION).eq(oldVersion)))
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
    protected SelectJoinStep<Record> versionHeadQueryExtended(Table<?> versionHead, DSLContext context) {
        return context.select(versionHead.fields())
                .select(
                        DSL.inline((Object) null).as(HISTORY_PROTOTYPE.SYS_PERIOD_UPPER.getName()),
                        DSL.inline(false).as(HISTORY_PROTOTYPE.SYS_DELETED.getName()))
                .from(versionHead);
    }

    protected Result<HR> findVersionHeadRecords(Function<Table<?>, Condition> condition) {
        Table<VR> versionHead = tables.versionHead();
        return versionHeadQueryExtended(versionHead, context)
                .where(condition.apply(versionHead))
                .fetchInto(tables.history());
    }

    protected Field<String> jsonDataField(Table<?> table, String... path) {
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

        // uuid mismatch
        if (!Objects.equals(headVoid, extractUid(uid))) {
            throw new PreconditionFailedException(NOT_MATCH_UID);
        }
        // system id mismatch
        if (!Objects.equals(systemService.getSystemId(), extractSystemId(uid))) {
            throw new PreconditionFailedException(NOT_MATCH_SYSTEM_ID);
        }
        // versions not consecutive
        if ((headVersion + 1) != LocatableUtils.getUidVersion(uid)) {
            throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
        }
    }
}
