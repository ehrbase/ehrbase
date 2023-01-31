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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.jooq.pg.tables.EhrFolder;
import org.ehrbase.jooq.pg.tables.records.EhrFolderRecord;
import org.ehrbase.serialisation.jsonencoding.CanonicalJson;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Loader;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stefan Spiska
 */
@Repository
public class EhrFolderRepository {

    private final DSLContext context;
    private final ServerConfig serverConfig;

    public EhrFolderRepository(DSLContext context, ServerConfig serverConfig) {
        this.context = context;
        this.serverConfig = serverConfig;
    }

    @Transactional
    public void commit(List<EhrFolderRecord> folderRecordList) {
        try {
            Loader<EhrFolderRecord> execute = context.loadInto(EhrFolder.EHR_FOLDER)
                    .bulkAfter(500)
                    .loadRecords(folderRecordList)
                    .fields(EhrFolder.EHR_FOLDER.fields())
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

    public boolean hasDirectory(UUID ehrId) {

        return context.fetchExists(EhrFolder.EHR_FOLDER.where(EhrFolder.EHR_FOLDER.EHR_ID.eq(ehrId)));
    }

    public List<EhrFolderRecord> to(UUID ehrId, Folder folder) {

        return flatten(folder).stream().map(p -> to(p, ehrId)).collect(Collectors.toList());
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
