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

import static org.ehrbase.jooq.pg.Tables.EHR_FOLDER_VERSION;
import static org.ehrbase.jooq.pg.Tables.EHR_FOLDER_VERSION_HISTORY;

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.EhrFolderData;
import org.ehrbase.jooq.pg.tables.EhrFolderDataHistory;
import org.ehrbase.jooq.pg.tables.EhrFolderVersion;
import org.ehrbase.jooq.pg.tables.EhrFolderVersionHistory;
import org.ehrbase.jooq.pg.tables.records.EhrFolderDataHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.EhrFolderDataRecord;
import org.ehrbase.jooq.pg.tables.records.EhrFolderVersionHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.EhrFolderVersionRecord;
import org.ehrbase.service.TimeProvider;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.Table;
import org.jooq.TableField;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles DB-Access to {@link org.ehrbase.jooq.pg.tables.EhrFolderVersion} etc.
 */
@Repository
public class EhrFolderRepository
        extends AbstractVersionedObjectRepository<
                EhrFolderVersionRecord,
                EhrFolderDataRecord,
                EhrFolderVersionHistoryRecord,
                EhrFolderDataHistoryRecord,
                Folder> {

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
                EhrFolderDataHistory.EHR_FOLDER_DATA_HISTORY,
                context,
                contributionRepository,
                systemService,
                timeProvider);
    }

    @Override
    protected List<TableField<EhrFolderVersionRecord, ?>> getVersionDataJoinFields() {
        return List.of(EHR_FOLDER_VERSION.EHR_ID, EHR_FOLDER_VERSION.EHR_FOLDERS_IDX);
    }

    /**
     * Create a new Folder in the DB
     *
     * @param ehrId
     * @param folder
     * @param contributionId   If <code>null</code> default contribution will be created {@link ContributionRepository#createDefault(UUID, ContributionDataType, ContributionChangeType)}
     * @param auditId          If <code>null</code> default audit will be created {@link ContributionRepository#createDefaultAudit(ContributionChangeType, AuditDetailsTargetType)}
     */
    @Transactional
    public void commit(
            UUID ehrId, Folder folder, @Nullable UUID contributionId, @Nullable UUID auditId, int ehrFoldersIdx) {
        commitHead(
                ehrId,
                folder,
                contributionId,
                auditId,
                ContributionChangeType.creation,
                r -> r.setEhrFoldersIdx(ehrFoldersIdx),
                r -> {
                    r.setEhrId(ehrId);
                    r.setEhrFoldersIdx(ehrFoldersIdx);
                });
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
    public void update(
            UUID ehrId, Folder folder, @Nullable UUID contributionId, @Nullable UUID auditId, int ehrFoldersIdx) {

        update(
                ehrId,
                folder,
                singleFolderCondition(ehrId, ehrFoldersIdx, tables.versionHead()),
                singleFolderCondition(ehrId, ehrFoldersIdx, tables.versionHistory()),
                contributionId,
                auditId,
                r -> r.setEhrFoldersIdx(ehrFoldersIdx),
                r -> {
                    r.setEhrId(ehrId);
                    r.setEhrFoldersIdx(ehrFoldersIdx);
                },
                "No FOLDER in ehr: %s".formatted(ehrId));
    }

    public Optional<Folder> findHead(UUID ehrId, int ehrFoldersIdx) {
        return findHead(singleFolderCondition(ehrId, ehrFoldersIdx, tables.versionHead()));
    }

    /**
     * Delete a  Folder in the DB
     *
     * @param ehrId
     * @param rootFolderId
     * @param version        Version to be deleted. Must match latest.
     * @param ehrFoldersIdx
     * @param contributionId If <code>null</code> default contribution will be created {@link ContributionRepository#createDefault(UUID, ContributionDataType, ContributionChangeType)}
     * @param auditId        If <code>null</code> default audit will be created {@link ContributionRepository#createDefaultAudit(ContributionChangeType, AuditDetailsTargetType)}
     */
    @Transactional
    public void delete(
            UUID ehrId, UUID rootFolderId, int version, int ehrFoldersIdx, UUID contributionId, UUID auditId) {
        delete(
                ehrId,
                singleFolderCondition(ehrId, ehrFoldersIdx, tables.versionHead())
                        .and(field(VERSION_PROTOTYPE.VO_ID).eq(rootFolderId)),
                version,
                contributionId,
                auditId,
                "No folder with %s ".formatted(rootFolderId));
    }

    public Optional<Folder> findByVersion(UUID ehrId, int folderIdx, int version) {

        return findByVersion(
                singleFolderCondition(ehrId, folderIdx, tables.versionHead()),
                singleFolderCondition(ehrId, folderIdx, tables.versionHistory()),
                version);
    }

    @Override
    protected Class<Folder> getLocatableClass() {
        return Folder.class;
    }

    private Condition singleFolderCondition(UUID ehrId, int folderIdx, Table<?> table) {
        return table.field(VERSION_PROTOTYPE.EHR_ID)
                .eq(ehrId)
                .and(table.field(EHR_FOLDER_VERSION.EHR_FOLDERS_IDX).eq(folderIdx));
    }

    public Optional<ObjectVersionId> findVersionByTime(UUID ehrId, int folderIdx, OffsetDateTime time) {
        return findVersionByTime(
                singleFolderCondition(ehrId, folderIdx, tables.versionHead()),
                singleFolderCondition(ehrId, folderIdx, tables.versionHistory()),
                time);
    }

    public boolean hasFolder(UUID ehrId, int ehrFolderIdx) {

        var headQuery = context.selectOne()
                .from(tables.versionHead())
                .where(singleFolderCondition(ehrId, ehrFolderIdx, tables.versionHead()));

        var historyQuery = context.selectOne()
                .from(tables.versionHistory())
                .where(singleFolderCondition(ehrId, ehrFolderIdx, tables.versionHistory()));

        return context.fetchExists(headQuery.unionAll(historyQuery));
    }

    public boolean folderUidExist(UUID folderId) {

        var headQuery = folderUidExistCondition(tables.versionHead(), folderId);
        var historyQuery = folderUidExistCondition(tables.versionHistory(), folderId);
        return context.fetchExists(headQuery.unionAll(historyQuery));
    }

    private SelectConditionStep<Record1<Integer>> folderUidExistCondition(Table<?> table, UUID folderId) {
        return context.selectOne()
                .from(table)
                .where(table.field(VERSION_PROTOTYPE.VO_ID).eq(folderId));
    }

    @Transactional
    public void adminDelete(UUID ehrId, Integer ehrFoldersIdx) {
        DeleteConditionStep<EhrFolderVersionRecord> deleteQuery = context.deleteFrom(tables.versionHead())
                .where(field(VERSION_PROTOTYPE.EHR_ID).eq(ehrId));
        if (ehrFoldersIdx != null) {
            deleteQuery = deleteQuery.and(EHR_FOLDER_VERSION.EHR_FOLDERS_IDX.eq(ehrFoldersIdx));
        }
        deleteQuery.execute();

        DeleteConditionStep<EhrFolderVersionHistoryRecord> deleteHistoryQuery = context.deleteFrom(
                        tables.versionHistory())
                .where(field(VERSION_HISTORY_PROTOTYPE.EHR_ID).eq(ehrId));

        if (ehrFoldersIdx != null) {
            deleteHistoryQuery = deleteHistoryQuery.and(EHR_FOLDER_VERSION_HISTORY.EHR_FOLDERS_IDX.eq(ehrFoldersIdx));
        }

        deleteHistoryQuery.execute();
    }

    public List<ObjectVersionId> findForContribution(UUID ehrId, UUID contributionId) {

        return findVersionIdsByContribution(ehrId, contributionId);
    }
}
