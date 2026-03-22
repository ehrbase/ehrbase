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
package org.ehrbase.rest.api.controller;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.service.RequestContext;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API v2 controller for folder/directory operations using ltree paths.
 */
@RestController
@RequestMapping("/api/v2/ehrs/{ehr_id}/directory")
@Tag(name = "Directory", description = "Folder hierarchy operations with ltree path queries")
public class DirectoryController extends BaseApiController {

    private final DSLContext dsl;
    private final EhrService ehrService;
    private final RequestContext requestContext;

    public DirectoryController(DSLContext dsl, EhrService ehrService, RequestContext requestContext) {
        this.dsl = dsl;
        this.ehrService = ehrService;
        this.requestContext = requestContext;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get folder hierarchy", description = "Supports ?path= for ltree subtree queries")
    public ResponseEntity<List<Map<String, Object>>> getFolderHierarchy(
            @PathVariable("ehr_id") String ehrIdStr, @RequestParam(value = "path", required = false) String path) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExists(ehrId);
        requestContext.setEhrId(ehrId);

        var query = dsl.select()
                .from(table(name("ehr_views", "v_folder_tree")))
                .where(field(name("ehr_id"), UUID.class).eq(ehrId));

        if (path != null && !path.isBlank()) {
            query = query.and(org.jooq.impl.DSL.condition("path <@ ?::ltree", path));
        }

        List<Map<String, Object>> folders = query.orderBy(field(name("depth"))).fetch().stream()
                .map(Record::intoMap)
                .toList();

        return ResponseEntity.ok(folders);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create folder")
    public ResponseEntity<Map<String, Object>> createFolder(
            @PathVariable("ehr_id") String ehrIdStr, @RequestBody Map<String, Object> body) {

        UUID ehrId = parseEhrId(ehrIdStr);
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        requestContext.setEhrId(ehrId);

        String folderName = extractName(body.get("name"));
        String archetypeNodeId = (String) body.get("archetype_node_id");
        String pathLabel = folderName.replaceAll("[^a-zA-Z0-9_]", "_");
        String path = (String) body.getOrDefault("path", "root." + pathLabel);
        String committerName = requestContext.getUserId();

        Record result = dsl.resultQuery(
                        "INSERT INTO ehr_system.ehr_folder "
                                + "(ehr_id, path, name, archetype_node_id, committer_name, committer_id, sys_tenant) "
                                + "VALUES (?, ?::ltree, ?, ?, ?, ?, ?) RETURNING id",
                        ehrId,
                        path,
                        folderName,
                        archetypeNodeId,
                        committerName,
                        committerName,
                        requestContext.getTenantId())
                .fetchOne();

        UUID folderId = result != null ? result.get(0, UUID.class) : null;
        URI location = locationUri("api", "v2", "ehrs", ehrId.toString(), "directory", String.valueOf(folderId));

        return ResponseEntity.created(location)
                .body(Map.of(
                        "id", String.valueOf(folderId), "name", folderName, "path", path, "ehr_id", ehrId.toString()));
    }

    @org.springframework.web.bind.annotation.PutMapping(
            value = "/{folder_id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update folder")
    public ResponseEntity<Map<String, Object>> updateFolder(
            @PathVariable("ehr_id") String ehrIdStr,
            @PathVariable("folder_id") String folderIdStr,
            @RequestBody Map<String, Object> body) {

        UUID ehrId = parseEhrId(ehrIdStr);
        UUID folderId = parseUuid(folderIdStr, "folder");
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        requestContext.setEhrId(ehrId);

        String newName = (String) body.get("name");
        if (newName != null) {
            dsl.execute("UPDATE ehr_system.ehr_folder SET name = ? WHERE id = ?", newName, folderId);
        }

        return ResponseEntity.ok(Map.of("folder_id", folderId.toString(), "name", newName != null ? newName : ""));
    }

    @DeleteMapping("/{folder_id}")
    @Operation(summary = "Delete folder")
    public ResponseEntity<Void> deleteFolder(
            @PathVariable("ehr_id") String ehrIdStr, @PathVariable("folder_id") String folderIdStr) {

        UUID ehrId = parseEhrId(ehrIdStr);
        UUID folderId = parseUuid(folderIdStr, "folder");
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        requestContext.setEhrId(ehrId);

        dsl.deleteFrom(table(name("ehr_system", "ehr_folder_item")))
                .where(field(name("folder_id"), UUID.class).eq(folderId))
                .execute();
        dsl.deleteFrom(table(name("ehr_system", "ehr_folder")))
                .where(field(name("id"), UUID.class).eq(folderId))
                .execute();

        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/{folder_id}/items",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add composition to folder")
    public ResponseEntity<Void> addItem(
            @PathVariable("ehr_id") String ehrIdStr,
            @PathVariable("folder_id") String folderIdStr,
            @RequestBody Map<String, String> body) {

        UUID ehrId = parseEhrId(ehrIdStr);
        UUID folderId = parseUuid(folderIdStr, "folder");
        UUID compositionId = parseUuid(body.get("composition_id"), "composition");
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        requestContext.setEhrId(ehrId);

        dsl.execute(
                "INSERT INTO ehr_system.ehr_folder_item (folder_id, composition_id, sys_tenant) VALUES (?, ?, ?)",
                folderId,
                compositionId,
                requestContext.getTenantId());

        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{folder_id}/items/{composition_id}")
    @Operation(summary = "Remove composition from folder")
    public ResponseEntity<Void> removeItem(
            @PathVariable("ehr_id") String ehrIdStr,
            @PathVariable("folder_id") String folderIdStr,
            @PathVariable("composition_id") String compositionIdStr) {

        UUID ehrId = parseEhrId(ehrIdStr);
        UUID folderId = parseUuid(folderIdStr, "folder");
        UUID compositionId = parseUuid(compositionIdStr, "composition");
        ehrService.checkEhrExistsAndIsModifiable(ehrId);
        requestContext.setEhrId(ehrId);

        dsl.deleteFrom(table(name("ehr_system", "ehr_folder_item")))
                .where(field(name("folder_id"), UUID.class).eq(folderId))
                .and(field(name("composition_id"), UUID.class).eq(compositionId))
                .execute();

        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    private static String extractName(Object nameField) {
        if (nameField instanceof String s) {
            return s;
        }
        if (nameField instanceof Map<?, ?> map) {
            Object value = map.get("value");
            if (value instanceof String s) {
                return s;
            }
        }
        throw new IllegalArgumentException("Invalid 'name' field: expected String or {\"value\": \"...\"}");
    }
}
