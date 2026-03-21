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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.service.RequestContext;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * REST API v1 controller for item tags (experimental, behind feature flag).
 */
@RestController
@RequestMapping("/api/v1/tags")
@ConditionalOnProperty(name = "ehrbase.features.item-tags", havingValue = "true")
@Tag(name = "Item Tags (Experimental)", description = "Key-value tagging on compositions and EHRs")
public class ItemTagController extends BaseApiController {

    private static final org.jooq.Table<?> ITEM_TAG = table(name("ehr_system", "item_tag"));

    private final DSLContext dsl;
    private final RequestContext requestContext;

    public ItemTagController(DSLContext dsl, RequestContext requestContext) {
        this.dsl = dsl;
        this.requestContext = requestContext;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create tag on a composition or EHR")
    public ResponseEntity<Map<String, Object>> createTag(@RequestBody Map<String, String> body) {
        UUID targetId = parseUuid(body.get("target_id"), "target");
        String targetType = body.getOrDefault("target_type", "composition");
        String key = body.get("key");
        String value = body.get("value");

        dsl.execute(
                "INSERT INTO ehr_system.item_tag (target_id, target_type, key, value, owner_id, sys_tenant) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                targetId,
                targetType,
                key,
                value,
                requestContext.getUserId(),
                requestContext.getTenantId());

        return ResponseEntity.status(201)
                .body(Map.of("target_id", targetId.toString(), "target_type", targetType, "key", key, "value", value));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get tags for an object")
    public ResponseEntity<List<Map<String, Object>>> getTags(@RequestParam("target_id") String targetIdStr) {
        UUID targetId = parseUuid(targetIdStr, "target");

        List<Map<String, Object>> tags =
                dsl
                        .select()
                        .from(ITEM_TAG)
                        .where(field(name("target_id"), UUID.class).eq(targetId))
                        .fetch()
                        .stream()
                        .map(Record::intoMap)
                        .toList();

        return ResponseEntity.ok(tags);
    }

    @DeleteMapping("/{tag_id}")
    @Operation(summary = "Delete tag")
    public ResponseEntity<Void> deleteTag(@PathVariable("tag_id") String tagIdStr) {
        UUID tagId = parseUuid(tagIdStr, "tag");

        dsl.deleteFrom(ITEM_TAG).where(field(name("id"), UUID.class).eq(tagId)).execute();

        return ResponseEntity.noContent().build();
    }
}
