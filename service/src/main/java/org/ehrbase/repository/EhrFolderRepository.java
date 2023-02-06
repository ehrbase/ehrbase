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

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.jooq.pg.tables.EhrFolder;
import org.ehrbase.jooq.pg.tables.EhrFolderHistory;
import org.ehrbase.jooq.pg.tables.records.EhrFolderHistoryRecord;
import org.ehrbase.jooq.pg.tables.records.EhrFolderRecord;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Loader;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stefan Spiska
 */
@Repository
public class EhrFolderRepository {

    private final DSLContext context;
    private final ServerConfig serverConfig;

    private final TenantService tenantService;

    public EhrFolderRepository(DSLContext context, ServerConfig serverConfig, TenantService tenantService) {
        this.context = context;
        this.serverConfig = serverConfig;
        this.tenantService = tenantService;
    }

    @Transactional
    public void commit(List<EhrFolderRecord> folderRecordList) {

        folderRecordList.forEach(r -> {
            r.setSysPeriodLower(OffsetDateTime.now());
            r.setNamespace(tenantService.getCurrentTenantIdentifier());
        });
        executeInsert(folderRecordList, EhrFolder.EHR_FOLDER);
    }

    private <T extends Record> void executeInsert(List<T> folderRecordList, Table<?> table) {
        try {
            Loader<?> execute = context.loadInto(table)
                    .bulkAfter(500)
                    .loadRecords(folderRecordList)
                    .fields(table.fields())
                    .execute();

            if (!execute.result().errors().isEmpty()) {

                throw new InternalServerException(execute.result().errors().stream()
                        .map(e -> e.exception().getMessage())
                        .collect(Collectors.joining(";")));
            }
        } catch (IOException e) {
            throw new InternalServerException(e);
        }
    }

    @Transactional
    public void update(List<EhrFolderRecord> folderRecordList) {

        UUID ehrId = folderRecordList.get(0).getEhrId();
        Result<EhrFolderRecord> old = getLatest(ehrId);

        if (old.isEmpty()
                || findRoot(old).getSysVersion() + 1
                        != findRoot(folderRecordList).getSysVersion()
                || !findRoot(old).getId().equals(findRoot(folderRecordList).getId())) {
            throw new PreconditionFailedException("If-Match version_uid does not match latest version.");
        }

        int execute = context.deleteFrom(EhrFolder.EHR_FOLDER)
                .where(EhrFolder.EHR_FOLDER.EHR_ID.eq(ehrId))
                .and(EhrFolder.EHR_FOLDER.SYS_VERSION.eq(old.get(0).getSysVersion()))
                .execute();

        if (execute == 0) {
            throw new PreconditionFailedException("If-Match version_uid does not match latest version.");
        }

        commit(folderRecordList);

        List<EhrFolderHistoryRecord> historyRecords =
                old.stream().map(this::toHistory).toList();

        executeInsert(historyRecords, EhrFolderHistory.EHR_FOLDER_HISTORY);
    }

    EhrFolderRecord findRoot(List<EhrFolderRecord> folderRecordList) {

        return folderRecordList.stream()
                .filter(r -> r.getPath().length == 1)
                .findAny()
                .orElseThrow();
    }

    private EhrFolderHistoryRecord toHistory(EhrFolderRecord record) {
        EhrFolderHistoryRecord historyRecord = new EhrFolderHistoryRecord();

        historyRecord.setId(record.getId());
        historyRecord.setEhrId(record.getEhrId());
        historyRecord.setContributionId(record.getContributionId());
        historyRecord.setArchetypeNodeId(record.getArchetypeNodeId());
        historyRecord.setPath(record.getPath());
        historyRecord.setContains(record.getContains());
        historyRecord.setFields(record.getFields());
        historyRecord.setNamespace(record.getNamespace());
        historyRecord.setSysVersion(record.getSysVersion());
        historyRecord.setSysPeriodLower(record.getSysPeriodLower());
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
        EhrFolderRecord folderRecord = new EhrFolderRecord();

        folderRecord.setId(historyRecord.getId());
        folderRecord.setEhrId(historyRecord.getEhrId());
        folderRecord.setContributionId(historyRecord.getContributionId());
        folderRecord.setArchetypeNodeId(historyRecord.getArchetypeNodeId());
        folderRecord.setPath(historyRecord.getPath());
        folderRecord.setContains(historyRecord.getContains());
        folderRecord.setFields(historyRecord.getFields());
        folderRecord.setNamespace(historyRecord.getNamespace());
        folderRecord.setSysVersion(historyRecord.getSysVersion());
        folderRecord.setSysPeriodLower(historyRecord.getSysPeriodLower());

        return folderRecord;
    }

    public Result<EhrFolderRecord> getLatest(UUID ehrId) {

        return context.selectFrom(EhrFolder.EHR_FOLDER)
                .where(EhrFolder.EHR_FOLDER.EHR_ID.eq(ehrId))
                .fetch();
    }

    public Result<EhrFolderHistoryRecord> getVersion(UUID ehrId, int version) {

        return context.select(EhrFolder.EHR_FOLDER.fields())
                .select(DSL.field("null").as(EhrFolderHistory.EHR_FOLDER_HISTORY.SYS_PERIOD_UPPER.getName()))
                .select(DSL.field("false").as(EhrFolderHistory.EHR_FOLDER_HISTORY.SYS_DELETED.getName()))
                .from(EhrFolder.EHR_FOLDER)
                .where(EhrFolder.EHR_FOLDER.EHR_ID.eq(ehrId))
                .and(EhrFolder.EHR_FOLDER.SYS_VERSION.eq(version))
                .unionAll(context.selectFrom(EhrFolderHistory.EHR_FOLDER_HISTORY)
                        .where(EhrFolderHistory.EHR_FOLDER_HISTORY.EHR_ID.eq(ehrId))
                        .and(EhrFolderHistory.EHR_FOLDER_HISTORY.SYS_VERSION.eq(version)))
                .fetch()
                .into(EhrFolderHistory.EHR_FOLDER_HISTORY);
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

        return context.fetchExists(EhrFolder.EHR_FOLDER.where(EhrFolder.EHR_FOLDER.EHR_ID.eq(ehrId)));
    }

    public List<EhrFolderRecord> to(UUID ehrId, Folder folder) {

        List<EhrFolderRecord> ehrFolderRecords =
                flatten(folder).stream().map(p -> to(p, ehrId)).toList();

        if (folder.getUid() instanceof ObjectVersionId objectVersionId) {
            ehrFolderRecords.forEach(r -> r.setSysVersion(
                    Integer.valueOf(objectVersionId.getVersionTreeId().getValue())));
        }

        return ehrFolderRecords;
    }

    private EhrFolderRecord to(Pair<List<String>, Folder> pair, UUID ehrId) {

        EhrFolderRecord folder2Record = new EhrFolderRecord();

        folder2Record.setEhrId(ehrId);

        List<String> uuids = pair.getKey();
        folder2Record.setPath(uuids.toArray(new String[0]));

        Folder folder = pair.getValue();
        folder2Record.setId(UUID.fromString(folder.getUid().getRoot().getValue()));
        folder2Record.setArchetypeNodeId(folder.getArchetypeNodeId());

        folder2Record.setContains(findItems(folder));
        // do not save hierarchy
        folder.setFolders(null);
        folder2Record.setFields(JSONB.valueOf(new CanonicalJson().marshal(folder)));

        return folder2Record;
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

        if (folder.getFolders() != null) {
            List<UUID[]> collect =
                    folder.getFolders().stream().map(this::findItems).collect(Collectors.toList());

            for (UUID[] a : collect) {

                value = ArrayUtils.addAll(value, a);
            }
        }
        return value;
    }

    private List<Pair<List<String>, Folder>> flatten(Folder folder) {

        ArrayList<Pair<List<String>, Folder>> pairs = new ArrayList<>();

        ArrayList<String> left = new ArrayList<>();
        left.add(folder.getNameAsString());
        pairs.add(Pair.of(left, folder));

        if (folder.getFolders() != null) {

            folder.getFolders().stream()
                    .map(this::flatten)
                    .flatMap(List::stream)
                    .forEach(p -> {
                        List<String> uuids = new ArrayList<>();
                        uuids.add(folder.getNameAsString());
                        uuids.addAll(p.getLeft());
                        pairs.add(Pair.of(uuids, p.getRight()));
                    });
        }

        return pairs;
    }
}
