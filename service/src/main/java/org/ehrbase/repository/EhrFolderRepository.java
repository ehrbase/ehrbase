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
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.tables.records.EhrFolderHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.EhrFolderRecord;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Result;
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

        store(folderRecordList, contributionId, ContributionChangeType.creation, auditId);
    }

    private void store(
            List<EhrFolderRecord> folderRecordList,
            UUID contributionId,
            ContributionChangeType contributionChangeType,
            UUID auditId) {

        if (contributionId == null) {

            contributionId = contributionRepository.createDefault(
                    folderRecordList.get(0).getEhrId(), ContributionDataType.folder, contributionChangeType);
        }

        if (auditId == null) {

            auditId = contributionRepository.createDefaultAudit(ContributionChangeType.creation);
        }

        UUID finalContributionId = contributionId;
        UUID finalAuditId = auditId;
        folderRecordList.forEach(r -> {
            r.setSysPeriodLower(OffsetDateTime.now());
            r.setNamespace(tenantService.getCurrentTenantIdentifier());
            r.setContributionId(finalContributionId);
            r.setAuditId(finalAuditId);
        });

        RepostoryHelper.executeBulkInsert(context, folderRecordList, EHR_FOLDER);
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

        UUID ehrId = folderRecordList.get(0).getEhrId();
        Result<EhrFolderRecord> old = getLatest(ehrId);

        boolean isDeleted = false;
        if (old.isEmpty()) {

            Result<EhrFolderHistoryRecord> history = getLatestHistory(ehrId);

            if (history.isNotEmpty() && BooleanUtils.isTrue(history.get(0).getSysDeleted())) {

                old = history.into(EHR_FOLDER);
                isDeleted = true;
            }
        }

        if (old.isEmpty()
                || findRoot(old).getSysVersion() + 1
                        != findRoot(folderRecordList).getSysVersion()
                || !findRoot(old).getId().equals(findRoot(folderRecordList).getId())) {
            throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
        }
        if (!isDeleted) {

            context.deleteFrom(EHR_FOLDER)
                    .where(EHR_FOLDER.EHR_ID.eq(ehrId))
                    .and(EHR_FOLDER.SYS_VERSION.eq(old.get(0).getSysVersion()))
                    .execute();

            List<EhrFolderHistoryRecord> historyRecords =
                    old.stream().map(this::toHistory).toList();

            RepostoryHelper.executeBulkInsert(context, historyRecords, EHR_FOLDER_HISTORY);
        }

        store(folderRecordList, contributionId, ContributionChangeType.modification, auditId);
    }

    private EhrFolderRecord findRoot(List<EhrFolderRecord> folderRecordList) {

        return folderRecordList.stream()
                .filter(r -> r.getPath().length == 1)
                .findAny()
                .orElseThrow();
    }

    private EhrFolderHistoryRecord toHistory(EhrFolderRecord ehrFolderRecord) {

        EhrFolderHistoryRecord historyRecord = ehrFolderRecord.into(EHR_FOLDER_HISTORY);

        historyRecord.setSysPeriodUpper(OffsetDateTime.now());
        historyRecord.setSysDeleted(false);
        if (!historyRecord.getSysPeriodUpper().isAfter(historyRecord.getSysPeriodLower())) {
            historyRecord.setSysPeriodUpper(historyRecord.getSysPeriodLower().plusNanos(1));
        }

        return historyRecord;
    }

    public List<EhrFolderRecord> fromHistory(List<EhrFolderHistoryRecord> historyRecords) {

        return historyRecords.stream().map(this::fromHistory).toList();
    }

    private EhrFolderRecord fromHistory(EhrFolderHistoryRecord historyRecord) {

        return historyRecord.into(EHR_FOLDER);
    }

    /**
     * Get the latest aktive (not deleted) Version from the DB for a given Ehr.
     *
     * @param ehrId
     * @return
     */
    public Result<EhrFolderRecord> getLatest(UUID ehrId) {

        return context.selectFrom(EHR_FOLDER).where(EHR_FOLDER.EHR_ID.eq(ehrId)).fetch();
    }

    /**
     * Get the latest Version from the History in the DB for a given Ehr.
     *
     * @param ehrid
     * @return
     */
    public Result<EhrFolderHistoryRecord> getLatestHistory(UUID ehrid) {

        return context.selectFrom(EHR_FOLDER_HISTORY)
                .where(EHR_FOLDER_HISTORY.EHR_ID.eq(ehrid))
                .and(EHR_FOLDER_HISTORY.SYS_VERSION.eq(
                        context.select(DSL.coalesce(DSL.max(EHR_FOLDER_HISTORY.SYS_VERSION), 0))
                                .from(EHR_FOLDER_HISTORY)
                                .where(EHR_FOLDER_HISTORY.EHR_ID.eq(ehrid))
                                .getQuery()))
                .fetch();
    }

    /**
     * Delete a  Folder in the DB
     *
     * @param ehrId
     * @param rootFolderId
     * @param version        Version to be deleted. Must match latest.
     * @param contributionId If <code>null</code> default contribution will be created {@link ContributionRepository#createDefault(UUID, ContributionDataType, ContributionChangeType)}
     * @param auditId        If <code>null</code> default audit will be created {@link ContributionRepository#createDefaultAudit(ContributionChangeType)}
     */
    @Transactional
    public void delete(UUID ehrId, UUID rootFolderId, int version, UUID contributionId, UUID auditId) {

        Result<EhrFolderRecord> old = getLatest(ehrId);

        if (old.isEmpty()
                || findRoot(old).getSysVersion() != version
                || !findRoot(old).getId().equals(rootFolderId)) {
            throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
        }

        int execute = context.deleteFrom(EHR_FOLDER)
                .where(EHR_FOLDER.EHR_ID.eq(ehrId))
                .and(EHR_FOLDER.SYS_VERSION.eq(old.get(0).getSysVersion()))
                .execute();

        if (execute == 0) {
            throw new PreconditionFailedException(NOT_MATCH_LATEST_VERSION);
        }

        List<EhrFolderHistoryRecord> historyRecords =
                old.stream().map(this::toHistory).toList();

        RepostoryHelper.executeBulkInsert(context, historyRecords, EHR_FOLDER_HISTORY);

        if (contributionId == null) {
            contributionId = contributionRepository.createDefault(
                    ehrId, ContributionDataType.folder, ContributionChangeType.deleted);
        }

        if (auditId == null) {

            auditId = contributionRepository.createDefaultAudit(ContributionChangeType.creation);
        }

        UUID finalContributionId = contributionId;
        UUID finalAuditId = auditId;
        historyRecords.forEach(h -> {
            h.setSysVersion(h.getSysVersion() + 1);
            h.setSysDeleted(true);
            h.setContributionId(finalContributionId);
            h.setAuditId(finalAuditId);
        });

        RepostoryHelper.executeBulkInsert(context, historyRecords, EHR_FOLDER_HISTORY);
    }

    public Result<EhrFolderHistoryRecord> getByVersion(UUID ehrId, int version) {

        return context.select(EHR_FOLDER.fields())
                .select(DSL.field("null").as(EHR_FOLDER_HISTORY.SYS_PERIOD_UPPER.getName()))
                .select(DSL.field("false").as(EHR_FOLDER_HISTORY.SYS_DELETED.getName()))
                .from(EHR_FOLDER)
                .where(EHR_FOLDER.EHR_ID.eq(ehrId))
                .and(EHR_FOLDER.SYS_VERSION.eq(version))
                .unionAll(context.selectFrom(EHR_FOLDER_HISTORY)
                        .where(EHR_FOLDER_HISTORY.EHR_ID.eq(ehrId))
                        .and(EHR_FOLDER_HISTORY.SYS_VERSION.eq(version))
                        .and(EHR_FOLDER_HISTORY.SYS_DELETED.isFalse()))
                .fetch()
                .into(EHR_FOLDER_HISTORY);
    }

    public Result<EhrFolderHistoryRecord> getByTime(UUID ehrId, OffsetDateTime time) {

        return context.select(EHR_FOLDER.fields())
                .select(DSL.field("null").as(EHR_FOLDER_HISTORY.SYS_PERIOD_UPPER.getName()))
                .select(DSL.field("false").as(EHR_FOLDER_HISTORY.SYS_DELETED.getName()))
                .from(EHR_FOLDER)
                .where(EHR_FOLDER.EHR_ID.eq(ehrId))
                .and(EHR_FOLDER.SYS_PERIOD_LOWER.lessOrEqual(time))
                .unionAll(context.selectFrom(EHR_FOLDER_HISTORY)
                        .where(EHR_FOLDER_HISTORY.EHR_ID.eq(ehrId))
                        .and(EHR_FOLDER_HISTORY.SYS_PERIOD_LOWER.lessOrEqual(time))
                        .and(EHR_FOLDER_HISTORY
                                .SYS_PERIOD_UPPER
                                .greaterThan(time)
                                .or(EHR_FOLDER_HISTORY.SYS_PERIOD_UPPER.isNull()))
                        .and(EHR_FOLDER_HISTORY.SYS_DELETED.isFalse()))
                .fetch()
                .into(EHR_FOLDER_HISTORY);
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

    private Folder from(List<String> path, Map<List<String>, EhrFolderRecord> byPathMap) {

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

        return context.fetchExists(context.selectOne()
                .from(EHR_FOLDER)
                .where(EHR_FOLDER.EHR_ID.eq(ehrId))
                .unionAll(context.selectOne().from(EHR_FOLDER_HISTORY).where(EHR_FOLDER_HISTORY.EHR_ID.eq(ehrId))));
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

    private EhrFolderRecord toRecord(int i, Triple<List<String>, List<Integer>, Folder> pair, UUID ehrId) {

        EhrFolderRecord folder2Record = context.newRecord(EHR_FOLDER);

        folder2Record.setEhrId(ehrId);
        // For now there is only one Folder hierarchy per ehr.
        folder2Record.setEhrFoldersIdx(1);
        folder2Record.setRowNum(i);

        List<String> uuids = pair.getLeft();
        folder2Record.setPath(uuids.toArray(new String[0]));

        Folder folder = pair.getRight();
        folder2Record.setId(UUID.fromString(folder.getUid().getRoot().getValue()));
        folder2Record.setArchetypeNodeId(folder.getArchetypeNodeId());

        folder2Record.setItems(findItems(folder));

        List<Integer> indexList = pair.getMiddle();
        // Add index for root
        indexList.add(0, 0);
        folder2Record.setHierarchyIdx(encode(indexList, false));
        folder2Record.setHierarchyIdxCap(encode(indexList, true));
        folder2Record.setHierarchyIdxLen(indexList.size());

        // do not save hierarchy
        folder.setFolders(null);
        folder2Record.setFields(JSONB.valueOf(new CanonicalJson().marshal(folder)));

        return folder2Record;
    }

    private String encode(List<Integer> index, boolean addCap) {

        String indexString = index.stream().map(Objects::toString).collect(Collectors.joining(","));
        return addCap ? indexString + ",~" : indexString + ",";
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

    private List<Triple<List<String>, List<Integer>, Folder>> flatten(Folder folder) {

        ArrayList<Triple<List<String>, List<Integer>, Folder>> pairs = new ArrayList<>();

        ArrayList<String> left = new ArrayList<>();
        left.add(folder.getNameAsString());
        ArrayList<Integer> index = new ArrayList<>();
        pairs.add(Triple.of(left, index, folder));

        if (folder.getFolders() != null) {

            IntStream.range(0, folder.getFolders().size())
                    .forEach(i -> flatten(folder.getFolders().get(i)).forEach(p -> {
                        List<String> uuids = new ArrayList<>();
                        uuids.add(folder.getNameAsString());
                        uuids.addAll(p.getLeft());
                        List<Integer> middle = p.getMiddle();
                        middle.add(0, i);
                        pairs.add(Triple.of(uuids, middle, p.getRight()));
                    }));
        }

        return pairs;
    }

    @Transactional
    public void adminDelete(UUID ehrId) {

        context.deleteFrom(EHR_FOLDER).where(EHR_FOLDER.EHR_ID.eq(ehrId)).execute();
        context.deleteFrom(EHR_FOLDER_HISTORY)
                .where(EHR_FOLDER_HISTORY.EHR_ID.eq(ehrId))
                .execute();
    }

    public List<ObjectVersionId> findForContribution(UUID ehrId, UUID contributionId) {

        return context
                .select(EHR_FOLDER.ID, EHR_FOLDER.SYS_VERSION)
                .from(EHR_FOLDER)
                .where(EHR_FOLDER.EHR_ID.eq(ehrId))
                .and(EHR_FOLDER.CONTRIBUTION_ID.eq(contributionId))
                .unionAll(context.select(EHR_FOLDER_HISTORY.ID, EHR_FOLDER_HISTORY.SYS_VERSION)
                        .from(EHR_FOLDER_HISTORY)
                        .where(EHR_FOLDER_HISTORY.EHR_ID.eq(ehrId))
                        .and(EHR_FOLDER_HISTORY.CONTRIBUTION_ID.eq(contributionId)))
                .fetch()
                .stream()
                .map(r -> new ObjectVersionId(
                        r.value1().toString() + "::" + serverConfig.getNodename() + "::" + r.value2()))
                .toList();
    }
}
