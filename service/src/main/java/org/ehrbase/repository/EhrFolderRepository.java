/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.repository;

import static org.ehrbase.jooq.pg.tables.EhrFolder.EHR_FOLDER;
import static org.ehrbase.jooq.pg.tables.EhrFolderHistory.EHR_FOLDER_HISTORY;

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Triple;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.records.EhrFolderHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.EhrFolderRecord;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles DB-Access to {@link org.ehrbase.jooq.pg.tables.EhrFolder} and {@link org.ehrbase.jooq.pg.tables.EhrFolderHistory}
 *
 * @author Stefan Spiska
 */
@Repository
public class EhrFolderRepository {

    public static final String NOT_MATCH_LATEST_VERSION = "If-Match version_uid does not match latest version.";
    private final DSLContext context;

    private final TenantService tenantService;

    private final ContributionRepository contributionRepository;

    private final ServerConfig serverConfig;

    public EhrFolderRepository(
            DSLContext context,
            TenantService tenantService,
            ContributionRepository contributionRepository,
            ServerConfig serverConfig) {
        this.context = context;
        this.tenantService = tenantService;
        this.contributionRepository = contributionRepository;
        this.serverConfig = serverConfig;
    }

    /**
     * Create a new Folder in the DB
     *
     * @param folderRecordList
     * @param contributionId   If <code>null</code> default contribution will be created {@link ContributionRepository#createDefault(UUID, ContributionDataType, ContributionChangeType)}
     * @param auditId          If <code>null</code> default audit will be created {@link ContributionRepository#createDefaultAudit(ContributionChangeType)}
     */
    @Transactional
    public void commit(List<EhrFolderRecord> folderRecordList, @Nullable UUID contributionId, @Nullable UUID auditId) {
        storeHead(folderRecordList, OffsetDateTime.now(), contributionId, ContributionChangeType.creation, auditId);
    }

    private void storeHead(
            List<EhrFolderRecord> folderRecordList,
            OffsetDateTime sysPeriodLower,
            UUID contributionId,
            ContributionChangeType contributionChangeType,
            UUID auditId) {

        UUID finalContributionId = Optional.ofNullable(contributionId)
                .orElseGet(() -> contributionRepository.createDefault(
                        folderRecordList.get(0).getEhrId(), ContributionDataType.folder, contributionChangeType));

        UUID finalAuditId = Optional.ofNullable(auditId)
                .orElseGet(() -> contributionRepository.createDefaultAudit(ContributionChangeType.creation));

        Short sysTenant = tenantService.getCurrentSysTenant();

        folderRecordList.forEach(r -> {
            r.setSysPeriodLower(sysPeriodLower);
            r.setSysTenant(sysTenant);
            r.setContributionId(finalContributionId);
            r.setAuditId(finalAuditId);
        });

        RepositoryHelper.executeBulkInsert(context, folderRecordList, EHR_FOLDER);
    }

    /**
     * Update a Folder in the DB
     *
     * @param folderRecordList
     * @param contributionId   If <code>null</code> default contribution will be created {@link ContributionRepository#createDefault(UUID, ContributionDataType, ContributionChangeType)}
     * @param auditId          If <code>null</code> default audit will be created {@link ContributionRepository#createDefaultAudit(ContributionChangeType)}
     */
    @Transactional
    public void update(List<EhrFolderRecord> folderRecordList, UUID contributionId, UUID auditId) {

        EhrFolderRecord rootFolder = findRoot(folderRecordList);
        UUID ehrId = rootFolder.getEhrId();
        int ehrFoldersIdx = rootFolder.getEhrFoldersIdx();
        Result<EhrFolderRecord> oldHead = getFolderHead(ehrId, ehrFoldersIdx);

        boolean isDeleted;
        int oldVersion;
        OffsetDateTime now;
        EhrFolderHistoryRecord delRecord;
        UUID rootId;
        if (oldHead.isEmpty()) {
            Optional<EhrFolderHistoryRecord> history = getLatestHistoryRoot(ehrId, ehrFoldersIdx);

            isDeleted = history.map(x -> x.getSysDeleted())
                    .filter(deleted -> deleted)
                    .isPresent();
            if (!isDeleted) {
                throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
            }
            delRecord = history.get();
            oldVersion = delRecord.getSysVersion();
            now = createCurrentTime(delRecord.getSysPeriodLower());
            rootId = delRecord.getId();

        } else {
            isDeleted = false;
            delRecord = null;
            EhrFolderRecord root = findRoot(oldHead);
            oldVersion = root.getSysVersion();
            now = createCurrentTime(root.getSysPeriodLower());
            rootId = root.getId();
        }

        // versions not consecutive
        if (oldVersion + 1 != rootFolder.getSysVersion()) {
            throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
        }

        // root ids do not match
        if (!rootId.equals(rootFolder.getId())) {
            throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
        }

        if (isDeleted) {
            // update delete record period
            delRecord.setSysPeriodUpper(now);
            int updateCount = context.executeUpdate(delRecord);
            if (updateCount != 1) {
                // concurrent modification
                throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
            }

        } else {
            // move to history
            List<EhrFolderHistoryRecord> historyRecords =
                    oldHead.stream().map(r -> toHistory(r, now)).toList();

            RepositoryHelper.executeBulkInsert(context, historyRecords, EHR_FOLDER_HISTORY);

            int deleteCount = context.deleteFrom(EHR_FOLDER)
                    .where(EHR_FOLDER.EHR_ID.eq(ehrId))
                    .and(EHR_FOLDER.SYS_VERSION.eq(oldVersion))
                    .execute();

            if (deleteCount == 0) {
                // concurrent modification
                throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
            }
        }

        // store new head
        storeHead(folderRecordList, now, contributionId, ContributionChangeType.modification, auditId);
    }

    private static EhrFolderRecord findRoot(List<EhrFolderRecord> folderRecordList) {
        return folderRecordList.stream()
                .filter(r -> r.getPath().length == 1)
                .findAny()
                .orElseThrow();
    }

    private static EhrFolderHistoryRecord toHistory(EhrFolderRecord ehrFolderRecord, OffsetDateTime sysPeriodUpper) {
        EhrFolderHistoryRecord historyRecord = ehrFolderRecord.into(EHR_FOLDER_HISTORY);
        historyRecord.setSysPeriodUpper(sysPeriodUpper);
        historyRecord.setSysDeleted(false);

        return historyRecord;
    }

    /**
     * Determines the current time.
     *
     * @param lowerBound For proper version intervals the value is guaranteed to be at least 1 microsecond after lowerBound
     * @return
     */
    private static OffsetDateTime createCurrentTime(OffsetDateTime lowerBound) {
        OffsetDateTime now = OffsetDateTime.now();
        // sysPeriodUpper must be after sysPeriodLower for proper intervals
        if (now.isAfter(lowerBound)) {
            return now;
        }
        // Add one microsecond, so the interval is valid.
        // Resolution of postgres timestamps is 1 microsecond
        // https://www.postgresql.org/docs/14/datatype-datetime.html#DATATYPE-DATETIME-TABLE
        return lowerBound.plusNanos(1_000);
    }

    public List<EhrFolderRecord> fromHistory(List<EhrFolderHistoryRecord> historyRecords) {
        return historyRecords.stream().map(this::fromHistory).toList();
    }

    private EhrFolderRecord fromHistory(EhrFolderHistoryRecord historyRecord) {
        return historyRecord.into(EHR_FOLDER);
    }

    /**
     * Get the all folders of the latest active (not deleted) Version from the DB for a given Ehr.
     *
     * @param ehrId
     * @param ehrFoldersIdx
     * @return
     */
    public Result<EhrFolderRecord> getFolderHead(UUID ehrId, int ehrFoldersIdx) {
        return context.selectFrom(EHR_FOLDER)
                .where(EHR_FOLDER.EHR_ID.eq(ehrId), EHR_FOLDER.EHR_FOLDERS_IDX.eq(ehrFoldersIdx))
                .fetch();
    }

    /**
     * Get the latest root folder from the History in the DB for a given Ehr.
     *
     * @param ehrid
     * @return
     */
    public Optional<EhrFolderHistoryRecord> getLatestHistoryRoot(UUID ehrid, int ehrFoldersIdx) {
        return context.selectFrom(EHR_FOLDER_HISTORY)
                .where(
                        EHR_FOLDER_HISTORY.EHR_ID.eq(ehrid),
                        EHR_FOLDER_HISTORY.EHR_FOLDERS_IDX.eq(ehrFoldersIdx),
                        EHR_FOLDER_HISTORY.ROW_NUM.eq(0))
                .orderBy(EHR_FOLDER_HISTORY.SYS_VERSION.desc())
                .limit(1)
                .fetchOptional();
    }

    /**
     * Delete a  Folder in the DB
     *
     * @param ehrId
     * @param rootFolderId
     * @param version        Version to be deleted. Must match latest.
     * @param ehrFoldersIdx
     * @param contributionId If <code>null</code> default contribution will be created {@link ContributionRepository#createDefault(UUID, ContributionDataType, ContributionChangeType)}
     * @param auditId        If <code>null</code> default audit will be created {@link ContributionRepository#createDefaultAudit(ContributionChangeType)}
     */
    @Transactional
    public void delete(
            UUID ehrId, UUID rootFolderId, int version, int ehrFoldersIdx, UUID contributionId, UUID auditId) {

        Result<EhrFolderRecord> headFolders = getFolderHead(ehrId, ehrFoldersIdx);
        if (headFolders.isEmpty()) {
            throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
        }

        EhrFolderRecord headRoot = findRoot(headFolders);
        if (headRoot.getSysVersion() != version || !headRoot.getId().equals(rootFolderId)) {
            throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
        }

        // timestamp for sysPeriod
        OffsetDateTime now = createCurrentTime(headRoot.getSysPeriodLower());

        List<EhrFolderHistoryRecord> historyRecords =
                headFolders.stream().map(r -> toHistory(r, now)).toList();

        // copy head to history
        RepositoryHelper.executeBulkInsert(context, historyRecords, EHR_FOLDER_HISTORY);

        if (contributionId == null) {
            contributionId = contributionRepository.createDefault(
                    ehrId, ContributionDataType.folder, ContributionChangeType.deleted);
        }

        if (auditId == null) {
            auditId = contributionRepository.createDefaultAudit(ContributionChangeType.creation);
        }

        // add delete entry to history
        EhrFolderHistoryRecord delRecord = headRoot.into(EHR_FOLDER_HISTORY);
        delRecord.setSysVersion(version + 1);
        delRecord.setSysPeriodUpper(null);
        delRecord.setSysPeriodLower(now);
        delRecord.setSysDeleted(true);
        delRecord.setContributionId(contributionId);
        delRecord.setAuditId(auditId);
        // reset unused fields
        delRecord.setArchetypeNodeId(null);
        delRecord.setItems(null);
        delRecord.setFields(null);

        context.executeInsert(delRecord);

        // delete from head
        int deleteCount = context.deleteFrom(EHR_FOLDER)
                .where(
                        EHR_FOLDER.EHR_ID.eq(ehrId),
                        EHR_FOLDER.EHR_FOLDERS_IDX.eq(ehrFoldersIdx),
                        EHR_FOLDER.SYS_VERSION.eq(version))
                .execute();

        if (deleteCount == 0) {
            // concurrent modification
            throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
        }
    }

    public Result<EhrFolderHistoryRecord> getByVersion(UUID ehrId, int version) {

        SelectConditionStep<Record> headQuery =
                headQuery(context).where(EHR_FOLDER.EHR_ID.eq(ehrId), EHR_FOLDER.SYS_VERSION.eq(version));

        Field<?>[] fields = convertToEhrFolderHistoryFields(headQuery.fields());

        SelectConditionStep<Record> historyQuery = context.select(fields)
                .from(EHR_FOLDER_HISTORY)
                .where(
                        EHR_FOLDER_HISTORY.EHR_ID.eq(ehrId),
                        EHR_FOLDER_HISTORY.SYS_VERSION.eq(version),
                        EHR_FOLDER_HISTORY.SYS_DELETED.isFalse());

        return headQuery.unionAll(historyQuery).fetch().into(EHR_FOLDER_HISTORY);
    }

    /**
     * Converts an array of JOOQ {@link Field}s to an array of JOOQ {@link Field}s of type {@code EHR_FOLDER_HISTORY},
     * applying a given {@link Function} to each field.
     *
     * @param fields the array of JOOQ {@link Field}s to be converted
     * @return an array of JOOQ {@link Field}s of type {@code EHR_FOLDER_HISTORY}
     * */
    public Field<?>[] convertToEhrFolderHistoryFields(Field<?>[] fields) {
        return Arrays.stream(fields).map(EHR_FOLDER_HISTORY::field).toArray(Field[]::new);
    }

    private static SelectJoinStep<Record> headQuery(DSLContext context) {
        return context.select(EHR_FOLDER.fields())
                .select(
                        DSL.field("null").as(EHR_FOLDER_HISTORY.SYS_PERIOD_UPPER.getName()),
                        DSL.field("false").as(EHR_FOLDER_HISTORY.SYS_DELETED.getName()))
                .from(EHR_FOLDER);
    }

    public Result<EhrFolderHistoryRecord> getByTime(UUID ehrId, OffsetDateTime time) {

        SelectConditionStep<Record> headQuery =
                headQuery(context).where(EHR_FOLDER.EHR_ID.eq(ehrId), EHR_FOLDER.SYS_PERIOD_LOWER.lessOrEqual(time));

        Field<?>[] fields = convertToEhrFolderHistoryFields(headQuery.fields());

        SelectConditionStep<Record> historyQuery = context.select(fields)
                .from(EHR_FOLDER_HISTORY)
                .where(
                        EHR_FOLDER_HISTORY.EHR_ID.eq(ehrId),
                        EHR_FOLDER_HISTORY.SYS_PERIOD_LOWER.lessOrEqual(time),
                        EHR_FOLDER_HISTORY
                                .SYS_PERIOD_UPPER
                                .greaterThan(time)
                                .or(EHR_FOLDER_HISTORY.SYS_PERIOD_UPPER.isNull()),
                        EHR_FOLDER_HISTORY.SYS_DELETED.isFalse());

        return headQuery.unionAll(historyQuery).fetch().into(EHR_FOLDER_HISTORY);
    }

    public Folder from(List<EhrFolderRecord> ehrFolderRecords) {

        Map<List<String>, EhrFolderRecord> byPathMap = ehrFolderRecords.stream()
                .collect(Collectors.toMap(
                        ehrFolderRecord ->
                                Arrays.stream(ehrFolderRecord.getPath()).toList(),
                        Function.identity()));

        return from(
                byPathMap.keySet().stream().filter(l -> l.size() == 1).findAny().orElseThrow(), byPathMap);
    }

    private static Folder from(List<String> path, Map<List<String>, EhrFolderRecord> byPathMap) {

        Folder folder =
                new CanonicalJson().unmarshal(byPathMap.get(path).getFields().data(), Folder.class);

        byPathMap.keySet().stream().filter(l -> l.size() == path.size() + 1).forEach(nextPath -> {
            Folder subFolder = from(
                    nextPath,
                    byPathMap.entrySet().stream()
                            .filter(e -> e.getKey().size() >= nextPath.size()
                                    && e.getKey().subList(0, nextPath.size()).equals(nextPath))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

            folder.addFolder(subFolder);
        });

        return folder;
    }

    public boolean hasDirectory(UUID ehrId) {
        var headQuery = context.selectOne()
                .from(EHR_FOLDER)
                .where(EHR_FOLDER.EHR_ID.eq(ehrId), EHR_FOLDER.EHR_FOLDERS_IDX.eq(1));
        var historyQuery = context.selectOne()
                .from(EHR_FOLDER_HISTORY)
                .where(EHR_FOLDER_HISTORY.EHR_ID.eq(ehrId), EHR_FOLDER_HISTORY.EHR_FOLDERS_IDX.eq(1));
        return context.fetchExists(headQuery.unionAll(historyQuery));
    }

    public List<EhrFolderRecord> toRecord(UUID ehrId, Folder folder) {

        List<Triple<List<String>, List<Integer>, Folder>> flatten = flatten(folder);
        List<EhrFolderRecord> ehrFolderRecords = IntStream.range(0, flatten.size())
                .mapToObj(i -> toRecord(i, flatten.get(i), ehrId))
                .toList();

        if (folder.getUid() instanceof ObjectVersionId objectVersionId) {
            ehrFolderRecords.forEach(r -> r.setSysVersion(
                    Integer.valueOf(objectVersionId.getVersionTreeId().getValue())));
        }

        return ehrFolderRecords;
    }

    private EhrFolderRecord toRecord(int rowNum, Triple<List<String>, List<Integer>, Folder> flattened, UUID ehrId) {

        EhrFolderRecord folder2Record = context.newRecord(EHR_FOLDER);

        folder2Record.setEhrId(ehrId);
        // For now there is only one Folder hierarchy per ehr.
        folder2Record.setEhrFoldersIdx(1);
        folder2Record.setRowNum(rowNum);

        List<String> path = flattened.getLeft();
        folder2Record.setPath(path.toArray(String[]::new));

        Folder folder = flattened.getRight();
        folder2Record.setId(UUID.fromString(folder.getUid().getRoot().getValue()));
        folder2Record.setArchetypeNodeId(folder.getArchetypeNodeId());

        folder2Record.setItems(findItems(folder));

        List<Integer> indexList = flattened.getMiddle();
        // Add index for root
        indexList.add(0, 0);
        folder2Record.setHierarchyIdx(encodeIndex(indexList, false));
        folder2Record.setHierarchyIdxCap(encodeIndex(indexList, true));
        folder2Record.setHierarchyIdxLen(indexList.size());

        // Exclude folders from JSON record
        folder.setFolders(null);
        folder2Record.setFields(JSONB.valueOf(new CanonicalJson().marshal(folder)));

        return folder2Record;
    }

    private static String encodeIndex(List<Integer> index, boolean addCap) {
        return index.stream().map(Objects::toString).collect(Collectors.joining(",", "", addCap ? ",~" : ","));
    }

    private UUID[] findItems(Folder folder) {
        UUID[] value = null;
        if (folder.getItems() != null) {

            value = folder.getItems().stream()
                    .map(ObjectRef::getId)
                    .map(ObjectId::getValue)
                    .map(UUID::fromString)
                    .toArray(UUID[]::new);
        }

        return value;
    }

    /**
     * For each folder in the hierarchy a triple (name path, index path, Folder) is added to the list
     * @param folder
     * @return
     */
    private static List<Triple<List<String>, List<Integer>, Folder>> flatten(Folder folder) {

        // List of Triple<name path, index path, Folder>
        List<Triple<List<String>, List<Integer>, Folder>> flattened = new ArrayList<>();

        {
            // add a root entry for this path
            List<String> namePath = new ArrayList<>();
            namePath.add(folder.getNameAsString());
            List<Integer> indexPath = new ArrayList<>();
            flattened.add(Triple.of(namePath, indexPath, folder));
        }

        if (folder.getFolders() != null) {

            IntStream.range(0, folder.getFolders().size())
                    // for each subfolder: flatten & prefix each entry with the path of this folder
                    .forEach(i -> flatten(folder.getFolders().get(i)).forEach(p -> {
                        List<String> namePath = new ArrayList<>();
                        namePath.add(folder.getNameAsString());
                        namePath.addAll(p.getLeft());
                        List<Integer> indexPath = p.getMiddle();
                        indexPath.add(0, i);
                        flattened.add(Triple.of(namePath, indexPath, p.getRight()));
                    }));
        }

        return flattened;
    }

    @Transactional
    public void adminDelete(UUID ehrId, Integer ehrFoldersIdx) {
        context.deleteFrom(EHR_FOLDER).where(EHR_FOLDER.EHR_ID.eq(ehrId)).execute();
        DeleteConditionStep<EhrFolderHistoryRecord> deleteQuery =
                context.deleteFrom(EHR_FOLDER_HISTORY).where(EHR_FOLDER_HISTORY.EHR_ID.eq(ehrId));

        if (ehrFoldersIdx != null) {
            deleteQuery = deleteQuery.and(EHR_FOLDER_HISTORY.EHR_FOLDERS_IDX.eq(ehrFoldersIdx));
        }

        deleteQuery.execute();
    }

    public List<ObjectVersionId> findForContribution(UUID ehrId, UUID contributionId) {

        var headQuery = context.select(EHR_FOLDER.ID, EHR_FOLDER.SYS_VERSION)
                .from(EHR_FOLDER)
                .where(
                        EHR_FOLDER.EHR_ID.eq(ehrId),
                        EHR_FOLDER.ROW_NUM.eq(0),
                        EHR_FOLDER.EHR_FOLDERS_IDX.eq(1),
                        EHR_FOLDER.CONTRIBUTION_ID.eq(contributionId));
        var historyQuery = context.select(EHR_FOLDER_HISTORY.ID, EHR_FOLDER_HISTORY.SYS_VERSION)
                .from(EHR_FOLDER_HISTORY)
                .where(
                        EHR_FOLDER_HISTORY.EHR_ID.eq(ehrId),
                        EHR_FOLDER_HISTORY.ROW_NUM.eq(0),
                        EHR_FOLDER_HISTORY.EHR_FOLDERS_IDX.eq(1),
                        EHR_FOLDER_HISTORY.CONTRIBUTION_ID.eq(contributionId));
        return headQuery.unionAll(historyQuery).stream()
                .map(r -> new ObjectVersionId(
                        r.value1().toString() + "::" + serverConfig.getNodename() + "::" + r.value2()))
                .toList();
    }
}
