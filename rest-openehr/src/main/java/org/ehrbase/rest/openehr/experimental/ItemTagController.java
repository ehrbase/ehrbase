/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
package org.ehrbase.rest.openehr.experimental;

import com.google.common.annotations.VisibleForTesting;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.service.experimental.ItemTag;
import org.ehrbase.api.service.experimental.ItemTagDto;
import org.ehrbase.api.service.experimental.ItemTagService;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.experimental.ItemTagApiSpecification;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Experimental REST interface for {@link <a href="https://specifications.openehr.org/releases/RM/latest/ehr.html#tags">4.2.6. Tags</a>}
 */
@ConditionalOnMissingBean(name = "primaryopenehritemtagcontroller")
@RestController
@RequestMapping(
        path = "${ehrbase.rest.experimental.tags.context-path:/rest/experimental/tags}/ehr",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@ConditionalOnProperty(name = "ehrbase.rest.experimental.tags.enabled", havingValue = "true")
public class ItemTagController extends BaseController implements ItemTagApiSpecification {

    private final ItemTagService itemTagService;

    public ItemTagController(ItemTagService itemTagService) {
        this.itemTagService = itemTagService;
    }

    // --- EHR_STATUS ---

    @PutMapping(
            value = "/{ehr_id}/ehr_status/{versioned_object_uid}/item_tag",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<Object> upsertEhrStatusItemTags(
            @RequestHeader(value = OPENEHR_VERSION, required = false) String openehrVersion,
            @RequestHeader(value = OPENEHR_AUDIT_DETAILS, required = false) String openehrAuditDetails,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE) String contentType,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @RequestHeader(value = PREFER, required = false) String prefer,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid,
            @RequestBody Collection<ItemTagDto> itemTags) {

        return upsertItemTags(
                accept,
                prefer,
                ehrIdString,
                versionedObjectUid,
                ItemTag.ItemTagRMType.EHR_STATUS,
                EHR_STATUS,
                itemTags);
    }

    @GetMapping(value = "/{ehr_id}/ehr_status/{versioned_object_uid}/item_tag")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<Collection<ItemTagDto>> getEhrStatusItemTags(
            @RequestHeader(value = OPENEHR_VERSION, required = false) String openehrVersion,
            @RequestHeader(value = OPENEHR_AUDIT_DETAILS, required = false) String openehrAuditDetails,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid,
            @RequestParam(value = "ids", required = false) List<String> ids,
            @RequestParam(value = "keys", required = false) List<String> keys) {

        return getItemTag(
                accept, ehrIdString, versionedObjectUid, ItemTag.ItemTagRMType.EHR_STATUS, EHR_STATUS, ids, keys);
    }

    @DeleteMapping(value = "/{ehr_id}/ehr_status/{versioned_object_uid}/item_tag")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteEhrStatusItemTags(
            @RequestHeader(value = OPENEHR_VERSION, required = false) String openehrVersion,
            @RequestHeader(value = OPENEHR_AUDIT_DETAILS, required = false) String openehrAuditDetails,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE) String contentType,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid,
            @RequestBody List<Object> itemTagsOrUUIDs) {

        return deleteTags(ehrIdString, versionedObjectUid, ItemTag.ItemTagRMType.EHR_STATUS, itemTagsOrUUIDs);
    }

    // --- COMPOSITION ---

    @PutMapping(
            value = "/{ehr_id}/composition/{versioned_object_uid}/item_tag",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<Object> upsertCompositionItemTags(
            @RequestHeader(value = OPENEHR_VERSION, required = false) String openehrVersion,
            @RequestHeader(value = OPENEHR_AUDIT_DETAILS, required = false) String openehrAuditDetails,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE) String contentType,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @RequestHeader(value = PREFER, required = false) String prefer,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid,
            @RequestBody Collection<ItemTagDto> itemTags) {

        return upsertItemTags(
                accept,
                prefer,
                ehrIdString,
                versionedObjectUid,
                ItemTag.ItemTagRMType.COMPOSITION,
                COMPOSITION,
                itemTags);
    }

    @GetMapping(value = "/{ehr_id}/composition/{versioned_object_uid}/item_tag")
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<Collection<ItemTagDto>> getCompositionItemTags(
            @RequestHeader(value = OPENEHR_VERSION, required = false) String openehrVersion,
            @RequestHeader(value = OPENEHR_AUDIT_DETAILS, required = false) String openehrAuditDetails,
            @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid,
            @RequestParam(value = "ids", required = false) List<String> ids,
            @RequestParam(value = "keys", required = false) List<String> keys) {

        return getItemTag(
                accept, ehrIdString, versionedObjectUid, ItemTag.ItemTagRMType.COMPOSITION, COMPOSITION, ids, keys);
    }

    @DeleteMapping(value = "/{ehr_id}/composition/{versioned_object_uid}/item_tag")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteCompositionItemTags(
            @RequestHeader(value = OPENEHR_VERSION, required = false) String openehrVersion,
            @RequestHeader(value = OPENEHR_AUDIT_DETAILS, required = false) String openehrAuditDetails,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE) String contentType,
            @PathVariable(value = "ehr_id") String ehrIdString,
            @PathVariable(value = "versioned_object_uid") String versionedObjectUid,
            @RequestBody List<Object> itemTagsOrUUIDs) {

        return deleteTags(ehrIdString, versionedObjectUid, ItemTag.ItemTagRMType.COMPOSITION, itemTagsOrUUIDs);
    }

    // --- Common Implementation

    @VisibleForTesting
    ResponseEntity<Object> upsertItemTags(
            String accept,
            String prefer,
            String ehrIdString,
            String versionedObjectUid,
            ItemTag.ItemTagRMType itemTagType,
            String locationPart,
            Collection<ItemTagDto> itemTags) {

        // obtain path parameter
        UUID ehrId = getEhrUuid(ehrIdString);
        UUID compositionUid = extractVersionedObjectUidFromVersionUid(versionedObjectUid);

        // Obtain header parameter
        MediaType mediaType = resolveContentType(accept);

        // sanity check for input
        if (itemTags.isEmpty()) {
            throw new UnprocessableEntityException("ItemTags are empty");
        }

        // perform bulk creation and return based on preferred response type
        Collection<UUID> tagIds = itemTagService.bulkUpsert(ehrId, compositionUid, itemTagType, itemTags);

        URI uri = createLocationUri(EHR, ehrId.toString(), locationPart, versionedObjectUid, "item_tag");
        ResponseEntity.BodyBuilder bodyBuilder =
                ResponseEntity.ok().contentType(mediaType).location(uri);

        if (RETURN_REPRESENTATION.equals(prefer)) {
            Collection<ItemTagDto> tags =
                    itemTagService.findItemTag(ehrId, compositionUid, itemTagType, tagIds, List.of());
            return bodyBuilder.body(tags);
        } else {
            return bodyBuilder.body(tagIds);
        }
    }

    @VisibleForTesting
    ResponseEntity<Collection<ItemTagDto>> getItemTag(
            String accept,
            String ehrIdString,
            String versionedObjectUid,
            ItemTag.ItemTagRMType itemTagType,
            String locationPart,
            @Nullable List<String> ids,
            @Nullable List<String> keys) {

        // obtain path parameter
        UUID ehrId = getEhrUuid(ehrIdString);
        UUID compositionUid = extractVersionedObjectUidFromVersionUid(versionedObjectUid);
        List<String> tagKeys = Optional.ofNullable(keys).orElseGet(List::of);
        List<UUID> tagIDs = Optional.ofNullable(ids)
                .map(it -> it.stream().map(UUID::fromString).toList())
                .orElseGet(List::of);

        // Obtain header parameter
        MediaType mediaType = resolveContentType(accept);

        Collection<ItemTagDto> itemTags =
                itemTagService.findItemTag(ehrId, compositionUid, itemTagType, tagIDs, tagKeys);

        URI uri = createLocationUri(EHR, ehrId.toString(), locationPart, versionedObjectUid, "item_tag");
        return ResponseEntity.ok().contentType(mediaType).location(uri).body(itemTags);
    }

    @VisibleForTesting
    ResponseEntity<Void> deleteTags(
            String ehrIdString,
            String versionedObjectUid,
            ItemTag.ItemTagRMType itemTagType,
            @Nonnull List<Object> itemTagsOrUUIDs) {

        if (itemTagsOrUUIDs.isEmpty()) {
            throw new UnprocessableEntityException("ItemTags are empty");
        }

        // obtain path parameter
        UUID ehrId = getEhrUuid(ehrIdString);
        UUID compositionUid = extractVersionedObjectUidFromVersionUid(versionedObjectUid);

        List<UUID> tagIDs = itemTagsOrUUIDs.stream()
                .map(entry -> switch (entry) {
                    case String s -> s;
                    case Map<?, ?> map -> Optional.ofNullable(map.get("id"))
                            .map(Object::toString)
                            .orElseThrow(() ->
                                    new UnprocessableEntityException("Expected ItemTag entry to contain an 'id'"));
                    default -> throw new UnprocessableEntityException(
                            "Expected array entry to be ItemTag or UUID String");
                })
                .map(UUID::fromString)
                .toList();

        itemTagService.bulkDelete(ehrId, compositionUid, itemTagType, tagIDs);

        return ResponseEntity.noContent().build();
    }
}
