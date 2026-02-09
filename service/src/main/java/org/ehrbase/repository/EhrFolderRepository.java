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
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
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
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
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

    /*
    * entity_idx || jsonb_set(
                    CASE WHEN num=0 THEN data-'U' ELSE data END,
                    '{IA}',
                    to_jsonb(item_uuids)
                )::text
    * */
    @Override
    protected Field<String> getDataAggregationBase(final Table<EhrFolderDataRecord> dataHead) {
        Field<JSONB> dataField = dataHead.field(DATA_PROTOTYPE.DATA);
        return dataHead.field(DATA_PROTOTYPE.ENTITY_IDX)
                .concat(AdditionalSQLFunctions.jsonb_set(
                                DSL.case_(dataHead.field(DATA_PROTOTYPE.NUM))
                                        .when(
                                                DSL.inline(0),
                                                DSL.field(
                                                        "{0} - '" + DbToRmFormat.UID_ALIAS + "'",
                                                        SQLDataType.JSONB,
                                                        dataField))
                                        .else_(dataField),
                                AdditionalSQLFunctions.to_jsonb(dataHead.field(EHR_FOLDER_DATA.ITEM_UUIDS)),
                                DbToRmFormat.FOLDER_ITEMS_UUID_ARRAY_ALIAS)
                        .cast(SQLDataType.CLOB));
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
                singleFolderInEhrCondition(tables.versionHead(), ehrId, ehrFoldersIdx),
                singleFolderInEhrCondition(tables.history(), ehrId, ehrFoldersIdx),
                contributionId,
                auditId,
                r -> r.setEhrFoldersIdx(ehrFoldersIdx),
                (n, r) -> addExtraFolderData(ehrId, ehrFoldersIdx, n, r),
                "No FOLDER in ehr: %s".formatted(ehrId));
    }

    public Optional<Folder> findHead(UUID ehrId, int ehrFoldersIdx) {
        return findHead(singleFolderInEhrCondition(tables.versionHead(), ehrId, ehrFoldersIdx));
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
        delete(
                ehrId,
                singleFolderInEhrCondition(tables.versionHead(), ehrId, ehrFoldersIdx)
                        .and(field(VERSION_PROTOTYPE.VO_ID).eq(rootFolderId)),
                version,
                contributionId,
                auditId,
                "No folder with %s ".formatted(rootFolderId));
    }

    public Optional<Folder> findByVersion(UUID ehrId, int folderIdx, int version) {

        return findByVersion(
                singleFolderInEhrCondition(tables.versionHead(), ehrId, folderIdx),
                singleFolderInEhrCondition(tables.history(), ehrId, folderIdx),
                version);
    }

    @Override
    protected Class<Folder> getLocatableClass() {
        return Folder.class;
    }

    public Optional<ObjectVersionId> findVersionByTime(UUID ehrId, int folderIdx, OffsetDateTime time) {
        return findVersionByTime(
                singleFolderInEhrCondition(tables.versionHead(), ehrId, folderIdx),
                singleFolderInEhrCondition(tables.history(), ehrId, folderIdx),
                time);
    }

    public boolean hasFolderAtIndex(UUID ehrId, int ehrFolderIdx) {

        var headQuery = context.selectOne()
                .from(tables.versionHead())
                .where(singleFolderInEhrCondition(tables.versionHead(), ehrId, ehrFolderIdx));

        var historyQuery = context.selectOne()
                .from(tables.history())
                .where(singleFolderInEhrCondition(tables.history(), ehrId, ehrFolderIdx));

        return context.fetchExists(headQuery.unionAll(historyQuery));
    }

    public boolean hasFolderInEhrForVoId(UUID ehrId, UUID voId, int ehrFolderIdx) {

        final Table<EhrFolderVersionRecord> versionTable = tables.versionHead();
        final Table<EhrFolderVersionHistoryRecord> historyTable = tables.history();

        var headQuery = context.selectOne()
                .from(versionTable)
                .where(folderInEhrWithVoIdCondition(versionTable, ehrId, voId, ehrFolderIdx));

        var historyQuery = context.selectOne()
                .from(historyTable)
                .where(folderInEhrWithVoIdCondition(historyTable, ehrId, voId, ehrFolderIdx)
                        .and(historyTable.field(EHR_FOLDER_VERSION.SYS_VERSION).eq(1)));

        return context.fetchExists(headQuery.unionAll(historyQuery));
    }

    @Transactional
    public void adminDelete(UUID ehrId, Integer ehrFoldersIdx) {
        DeleteConditionStep<EhrFolderVersionRecord> deleteQuery = context.deleteFrom(tables.versionHead())
                .where(field(VERSION_PROTOTYPE.EHR_ID).eq(ehrId));
        if (ehrFoldersIdx != null) {
            deleteQuery = deleteQuery.and(EHR_FOLDER_VERSION.EHR_FOLDERS_IDX.eq(ehrFoldersIdx));
        }
        deleteQuery.execute();

        DeleteConditionStep<EhrFolderVersionHistoryRecord> deleteHistoryQuery = context.deleteFrom(tables.history())
                .where(field(HISTORY_PROTOTYPE.EHR_ID).eq(ehrId));

        if (ehrFoldersIdx != null) {
            deleteHistoryQuery = deleteHistoryQuery.and(EHR_FOLDER_VERSION_HISTORY.EHR_FOLDERS_IDX.eq(ehrFoldersIdx));
        }

        deleteHistoryQuery.execute();
    }

    private Condition singleFolderInEhrCondition(Table<?> table, UUID ehrId, int folderIdx) {
        return table.field(VERSION_PROTOTYPE.EHR_ID)
                .eq(ehrId)
                .and(table.field(EHR_FOLDER_VERSION.EHR_FOLDERS_IDX).eq(folderIdx));
    }

    private Condition folderInEhrWithVoIdCondition(Table<?> table, UUID ehrId, UUID voId, int folderIdx) {
        return Objects.requireNonNull(table.field(VERSION_PROTOTYPE.EHR_ID))
                .eq(ehrId)
                .and(table.field(EHR_FOLDER_VERSION.VO_ID).eq(voId))
                .and(table.field(EHR_FOLDER_VERSION.EHR_FOLDERS_IDX).eq(folderIdx));
    }
}
