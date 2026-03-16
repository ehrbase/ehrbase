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

import static org.ehrbase.jooq.pg.Tables.EHR_FOLDER_DATA;
import static org.ehrbase.jooq.pg.Tables.EHR_FOLDER_VERSION;
import static org.ehrbase.jooq.pg.Tables.EHR_FOLDER_VERSION_HISTORY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Streams;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.EhrFolderData;
import org.ehrbase.jooq.pg.tables.EhrFolderVersion;
import org.ehrbase.jooq.pg.tables.EhrFolderVersionHistory;
import org.ehrbase.jooq.pg.tables.records.EhrFolderDataRecord;
import org.ehrbase.jooq.pg.tables.records.EhrFolderVersionHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.EhrFolderVersionRecord;
import org.ehrbase.jooq.pg.util.AdditionalSQLFunctions;
import org.ehrbase.openehr.dbformat.DbToRmFormat;
import org.ehrbase.openehr.dbformat.StructureNode;
import org.ehrbase.openehr.dbformat.VersionedObjectDataStructure;
import org.ehrbase.service.TimeProvider;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.SelectSelectStep;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles DB-Access to {@link org.ehrbase.jooq.pg.tables.EhrFolderVersion} etc.
 */
@Repository
public class EhrFolderRepository
        extends AbstractVersionedObjectRepository<
                EhrFolderVersionRecord, EhrFolderDataRecord, EhrFolderVersionHistoryRecord, Folder> {

    public EhrFolderRepository(
            DSLContext context,
            ContributionRepository contributionRepository,
            SystemService systemService,
            TimeProvider timeProvider) {
        super(
                AuditDetailsTargetType.EHR_FOLDER,
                EhrFolderVersion.EHR_FOLDER_VERSION,
                EhrFolderData.EHR_FOLDER_DATA,
                EhrFolderVersionHistory.EHR_FOLDER_VERSION_HISTORY,
                context,
                contributionRepository,
                systemService,
                timeProvider);
    }

    @Override
    protected List<TableField<EhrFolderVersionRecord, ?>> getVersionDataJoinFields() {
        return List.of(EHR_FOLDER_VERSION.EHR_ID, EHR_FOLDER_VERSION.EHR_FOLDERS_IDX);
    }

    @Override
    protected Pair<Field<?>[], Field<?>[]> getAdditionalDataQuerySelectFields(
            final Table<?> versionTable, final Table<?> dataTable, final boolean head) {
        if (head) {
            return Pair.of(new Field[] {itemUuidFieldAggregation(versionTable)}, new Field[] {
                versionTable.field(EHR_FOLDER_VERSION.EHR_ID), versionTable.field(EHR_FOLDER_VERSION.EHR_FOLDERS_IDX)
            });
        } else {
            return Pair.of(new Field[] {versionTable.field(EHR_FOLDER_VERSION_HISTORY.OV_ITEM_UUIDS)}, new Field[] {});
        }
    }

    @Override
    protected Pair<Stream<Field<?>>, Stream<Field<?>>> additionalCopyToHistoryFields(
            final Table<EhrFolderVersionRecord> versionHead,
            final Table<EhrFolderDataRecord> dataHead,
            final OffsetDateTime now) {
        Pair<Stream<Field<?>>, Stream<Field<?>>> base = super.additionalCopyToHistoryFields(versionHead, dataHead, now);
        Field<?> uuidArrayField = itemUuidFieldAggregation(versionHead);
        return Pair.of(
                Streams.concat(base.getLeft(), Stream.of(uuidArrayField)),
                Streams.concat(base.getRight(), Stream.of(EHR_FOLDER_VERSION_HISTORY.OV_ITEM_UUIDS)));
    }

    /**
     * trim_array(
     * (SELECT array_agg(uid.v ORDER BY num ASC)
     * 				FROM
     * 				ehr_folder_data h2
     * 				join lateral (
     * 				select * from
     * 				unnest(h2.item_uuids)
     * 				UNION ALL
     * 				SELECT NULL) as uid(v) on true
     * 				where (h.ehr_id, h.ehr_folders_idx)=(h2.ehr_id, h2.ehr_folders_idx)
     * 				)
     * 	, 1)
     *
     * @param versionHead
     * @return
     */
    private @NonNull Field<?> itemUuidFieldAggregation(final Table<?> versionHead) {
        Table<EhrFolderDataRecord> sqTable = tables.dataHead().as("h2");

        SelectSelectStep<Record1<UUID>> separator = DSL.select(DSL.castNull(UUID.class));
        Table<?> itemUuids = DSL.unnest(sqTable.field(EHR_FOLDER_DATA.ITEM_UUIDS));
        Field<UUID> unnestedUuid = itemUuids.field(0, UUID.class).as("v");
        Table<Record1<UUID>> unnestedWithSeparator = DSL.lateral(
                        context.select(unnestedUuid).from(itemUuids).unionAll(separator))
                .as("uid");

        SelectConditionStep<Record1<UUID[]>> aggregated = context.select(DSL.arrayAgg(unnestedUuid)
                        .orderBy(sqTable.field(DATA_PROTOTYPE.NUM).asc()))
                .from(sqTable)
                .join(unnestedWithSeparator)
                .on(DSL.trueCondition())
                .where(sqTable.field(EHR_FOLDER_DATA.EHR_ID)
                        .eq(versionHead.field(VERSION_PROTOTYPE.EHR_ID))
                        .and(sqTable.field(EHR_FOLDER_DATA.EHR_FOLDERS_IDX)
                                .eq(versionHead.field(EHR_FOLDER_VERSION.EHR_FOLDERS_IDX))));

        return AdditionalSQLFunctions.trim_array(aggregated.asField(), DSL.inline(1));
    }

    @Override
    protected Pair<CharSequence, ObjectNode> parseJsonData(
            final Pair<CharSequence, CharSequence> p, final Record rec, final int idx) {
        Pair<CharSequence, ObjectNode> parsed = super.parseJsonData(p, rec, idx);
        return insertFolderItemsArray(rec.get(3, UUID[].class), idx, parsed);
    }

    public static @NonNull Pair<CharSequence, ObjectNode> insertFolderItemsArray(
            UUID[] itemIds, final int idx, final Pair<CharSequence, ObjectNode> parsed) {
        ObjectNode node = parsed.getRight();
        int row = 0;
        ArrayNode itemUuidsNode = node.arrayNode();
        for (final UUID uuid : itemIds) {
            if (uuid == null) {
                row++;
                if (row > idx) {
                    break;
                }
            } else if (row == idx) {
                itemUuidsNode.add(uuid.toString());
            }
        }
        if (!itemUuidsNode.isEmpty()) {
            node.set(DbToRmFormat.FOLDER_ITEMS_UUID_ARRAY_ALIAS, itemUuidsNode);
        }
        return parsed;
    }

    /**
     * Create a new Folder in the DB
     *
     * @param ehrId            Affected <code>EHR</code>
     * @param folder           The {@link Folder} to commit
     * @param contributionId   If <code>null</code> default contribution will be created {@link ContributionRepository#createDefault(UUID, ContributionDataType, ContributionChangeType)}
     * @param auditId          If <code>null</code> default audit will be created {@link ContributionRepository#createDefaultAudit(ContributionChangeType, AuditDetailsTargetType)}
     */
    @Transactional
    public void commit(UUID ehrId, Folder folder, UUID contributionId, UUID auditId, int ehrFoldersIdx) {
        commitHead(
                ehrId,
                folder,
                contributionId,
                auditId,
                ContributionChangeType.creation,
                r -> r.setEhrFoldersIdx(ehrFoldersIdx),
                (n, r) -> addExtraFolderData(ehrId, ehrFoldersIdx, n, r));
    }

    public static void addExtraFolderData(UUID ehrId, int ehrFoldersIdx, StructureNode n, EhrFolderDataRecord r) {
        // TODO could be moved to earlier stage.
        //  r.data - items needs to be performed and setting data twice should be omitted
        JsonNode itemsNode = n.getJsonNode().remove("items");
        r.setItemUuids(getItemUuids(itemsNode));

        if (itemsNode != null) {
            // re-serialize the json because items was removed
            r.setData(JSONB.valueOf(
                    VersionedObjectDataStructure.applyRmAliases(n.getJsonNode()).toString()));
        }
        r.setEhrId(ehrId);
        r.setEhrFoldersIdx(ehrFoldersIdx);
    }

    private static UUID[] getItemUuids(JsonNode itemsNode) {
        if (itemsNode == null) {
            return new UUID[0];
        }
        int size = itemsNode.size();
        if (size == 0) {
            return new UUID[0];
        }
        UUID[] result = new UUID[size];
        for (int i = 0; i < size; i++) {
            // id and value are not optional
            String uuidText = itemsNode.get(i).get("id").get("value").asText();
            try {
                result[i] = UUID.fromString(uuidText);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Only UUIDs are supported as FOLDER.items.id.value");
            }
        }
        return result;
    }

    /**
     * Update a Folder in the DB
     *
     * @param ehrId            Affected <code>EHR</code>
     * @param folder           Affected <code>Folder</code> with new head system version
     * @param contributionId   If <code>null</code> default contribution will be created {@link ContributionRepository#createDefault(UUID, ContributionDataType, ContributionChangeType)}
     * @param auditId          If <code>null</code> default audit will be created {@link ContributionRepository#createDefaultAudit(ContributionChangeType, AuditDetailsTargetType)}
     */
    @Transactional
    public void update(UUID ehrId, Folder folder, UUID contributionId, UUID auditId, int ehrFoldersIdx) {

        update(
                ehrId,
                folder,
                singleFolderInEhrCondition(ehrId, ehrFoldersIdx),
                singleFolderInEhrCondition(ehrId, ehrFoldersIdx),
                contributionId,
                auditId,
                r -> r.setEhrFoldersIdx(ehrFoldersIdx),
                (n, r) -> addExtraFolderData(ehrId, ehrFoldersIdx, n, r),
                "No FOLDER in ehr: %s".formatted(ehrId));
    }

    public Optional<Folder> findHead(UUID ehrId, int ehrFoldersIdx) {
        return findHead(singleFolderInEhrCondition(ehrId, ehrFoldersIdx));
    }

    /**
     * Delete a  Folder in the DB
     *
     * @param ehrId          Affected <code>EHR</code>
     * @param rootFolderId   <code>EHR</code> root {@link Folder}
     * @param version        Version to be deleted. Must match latest
     * @param ehrFoldersIdx  <code>EHR</code> folder index to delete
     * @param contributionId If <code>null</code> default contribution will be created {@link ContributionRepository#createDefault(UUID, ContributionDataType, ContributionChangeType)}
     * @param auditId        If <code>null</code> default audit will be created {@link ContributionRepository#createDefaultAudit(ContributionChangeType, AuditDetailsTargetType)}
     */
    @Transactional
    public void delete(
            UUID ehrId, UUID rootFolderId, int version, int ehrFoldersIdx, UUID contributionId, UUID auditId) {
        Table<EhrFolderVersionRecord> table = tables.versionHead();
        delete(
                ehrId,
                singleFolderInEhrCondition(ehrId, ehrFoldersIdx)
                        .andThen(c -> c.and(table.field(VERSION_PROTOTYPE.VO_ID).eq(rootFolderId))),
                version,
                contributionId,
                auditId,
                "No folder with %s ".formatted(rootFolderId));
    }

    public Optional<Folder> findByVersion(UUID ehrId, int folderIdx, int version) {

        return findByVersion(
                singleFolderInEhrCondition(ehrId, folderIdx), singleFolderInEhrCondition(ehrId, folderIdx), version);
    }

    @Override
    public Class<Folder> getLocatableClass() {
        return Folder.class;
    }

    public Optional<ObjectVersionId> findVersionByTime(UUID ehrId, int folderIdx, OffsetDateTime time) {
        return findVersionByTime(
                singleFolderInEhrCondition(ehrId, folderIdx), singleFolderInEhrCondition(ehrId, folderIdx), time);
    }

    public boolean hasFolderAtIndex(UUID ehrId, int ehrFolderIdx) {

        Table<EhrFolderVersionRecord> versionHead = tables.versionHead();
        var headQuery = context.selectOne()
                .from(versionHead)
                .where(singleFolderInEhrCondition(ehrId, ehrFolderIdx).apply(versionHead));

        Table<EhrFolderVersionHistoryRecord> history = tables.history();
        var historyQuery = context.selectOne()
                .from(history)
                .where(singleFolderInEhrCondition(ehrId, ehrFolderIdx).apply(history));

        return context.fetchExists(headQuery.unionAll(historyQuery));
    }

    public boolean hasFolderInEhrForVoId(UUID ehrId, UUID voId, int ehrFolderIdx) {

        final Table<EhrFolderVersionRecord> versionTable = tables.versionHead();
        final Table<EhrFolderVersionHistoryRecord> historyTable = tables.history();

        var headQuery = context.selectOne()
                .from(versionTable)
                .where(folderInEhrWithVoIdCondition(ehrId, voId, ehrFolderIdx).apply(versionTable));

        var historyQuery = context.selectOne()
                .from(historyTable)
                .where(folderInEhrWithVoIdCondition(ehrId, voId, ehrFolderIdx)
                        .apply(historyTable)
                        .and(historyTable.field(EHR_FOLDER_VERSION.SYS_VERSION).eq(1)));

        return context.fetchExists(headQuery.unionAll(historyQuery));
    }

    @Transactional
    public void adminDelete(UUID ehrId, Integer ehrFoldersIdx) {
        Table<EhrFolderVersionRecord> versionHead = tables.versionHead();
        DeleteConditionStep<EhrFolderVersionRecord> deleteQuery = context.deleteFrom(versionHead)
                .where(versionHead.field(VERSION_PROTOTYPE.EHR_ID).eq(ehrId));
        if (ehrFoldersIdx != null) {
            deleteQuery = deleteQuery.and(
                    versionHead.field(EHR_FOLDER_VERSION.EHR_FOLDERS_IDX).eq(ehrFoldersIdx));
        }
        deleteQuery.execute();

        Table<EhrFolderVersionHistoryRecord> history = tables.history();
        DeleteConditionStep<EhrFolderVersionHistoryRecord> deleteHistoryQuery = context.deleteFrom(history)
                .where(history.field(HISTORY_PROTOTYPE.EHR_ID).eq(ehrId));

        if (ehrFoldersIdx != null) {
            deleteHistoryQuery = deleteHistoryQuery.and(
                    history.field(EHR_FOLDER_VERSION_HISTORY.EHR_FOLDERS_IDX).eq(ehrFoldersIdx));
        }

        deleteHistoryQuery.execute();
    }

    private Function<Table<?>, Condition> singleFolderInEhrCondition(UUID ehrId, int folderIdx) {
        return table -> table.field(VERSION_PROTOTYPE.EHR_ID)
                .eq(ehrId)
                .and(table.field(EHR_FOLDER_VERSION.EHR_FOLDERS_IDX).eq(folderIdx));
    }

    private Function<Table<?>, Condition> folderInEhrWithVoIdCondition(UUID ehrId, UUID voId, int folderIdx) {
        return table -> Objects.requireNonNull(table.field(VERSION_PROTOTYPE.EHR_ID))
                .eq(ehrId)
                .and(table.field(EHR_FOLDER_VERSION.VO_ID).eq(voId))
                .and(table.field(EHR_FOLDER_VERSION.EHR_FOLDERS_IDX).eq(folderIdx));
    }
}
