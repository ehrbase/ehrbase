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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.net.HttpHeaders;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.dto.experimental.ItemTagDto;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.service.experimental.ItemTag;
import org.ehrbase.api.service.experimental.ItemTagService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

class ItemTagControllerTest {

    private static final String SAMPLE_EHR_ID = "d7a57443-20ee-4950-8c72-9bced2aa9881";

    private final ItemTagService mockItemTagService = mock();

    private final ItemTagController spyController = spy(new ItemTagController(mockItemTagService));

    private ItemTagController controller() {
        return spyController;
    }

    private ItemTagDto itemTagDto(ItemTag.ItemTagRMType type, String targetId) {
        return new ItemTagDto(
                UUID.randomUUID(),
                UUID.fromString(SAMPLE_EHR_ID),
                UUID.fromString(targetId),
                type,
                "/content",
                "a::key",
                "some value");
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(mockItemTagService, spyController);
        doReturn("https://openehr.test.item-tag.com/rest").when(spyController).getContextPath();
    }

    @Test
    void conditionalEnabled() {
        ConditionalOnProperty annotation =
                AnnotationUtils.findAnnotation(controller().getClass(), ConditionalOnProperty.class);
        Assertions.assertNotNull(annotation);
        assertEquals("ehrbase.rest.experimental.tags.enabled", annotation.name()[0]);
        assertEquals("true", annotation.havingValue());
    }

    // --- UPSERT ---

    @Test
    void upsertItemTagsCommonUsed() {

        String targetId = "1073d3bd-1490-4f3b-b2ac-95202d18c41d";
        ItemTagDto tag = itemTagDto(ItemTag.ItemTagRMType.COMPOSITION, targetId);

        // @format:off
        ItemTagController controller = controller();
        controller.upsertCompositionItemTags(null, null, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE, null, SAMPLE_EHR_ID, targetId, List.of(tag));
        controller.upsertEhrStatusItemTags(null, null, MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_XML_VALUE, null, SAMPLE_EHR_ID, targetId, List.of(tag));

        verify(controller, times(1)).upsertItemTags(MediaType.APPLICATION_JSON_VALUE, null, SAMPLE_EHR_ID, targetId, ItemTag.ItemTagRMType.COMPOSITION, "composition", List.of(tag));
        verify(controller, times(1)).upsertItemTags(MediaType.APPLICATION_XML_VALUE, null, SAMPLE_EHR_ID, targetId, ItemTag.ItemTagRMType.EHR_STATUS, "ehr_status", List.of(tag));
        // @format:on
    }

    @Test
    void upsertItemTagsEmptyError() {

        UnprocessableEntityException exception =
                assertThrowsExactly(UnprocessableEntityException.class, () -> controller()
                        .upsertItemTags(
                                MediaType.APPLICATION_JSON_VALUE,
                                null,
                                SAMPLE_EHR_ID,
                                "21bfba85-aaa6-4190-b6dd-4968cb84b549",
                                ItemTag.ItemTagRMType.EHR_STATUS,
                                "ehr_status",
                                List.of()));
        assertEquals("ItemTags are empty", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
                EHR_STATUS|application/json||87ef3397-8939-4c18-9bb3-d5d9a80d740d
                COMPOSITION|application/xml|return=minimal|98c09fbf-54ca-496f-9aa1-d9778cc487a8
            """,
            delimiterString = "|")
    void upsertItemTagsPreferMinimal(String type, String accept, String prefer, String targetId) {

        ItemTagDto tag = itemTagDto(ItemTag.ItemTagRMType.valueOf(type), targetId);
        List<Object> tags = runUpsertItemTagsBaseTest(type, accept, prefer, targetId, tag);

        assertEquals(1, tags.size());
        assertEquals(tag.id(), tags.getFirst());
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
                EHR_STATUS|application/xml|87ef3397-8939-4c18-9bb3-d5d9a80d740d
                COMPOSITION|application/json|98c09fbf-54ca-496f-9aa1-d9778cc487a8
            """,
            delimiterString = "|")
    void upsertItemTagsPreferRepresentation(String type, String accept, String targetId) {

        ItemTagDto tag = itemTagDto(ItemTag.ItemTagRMType.valueOf(type), targetId);
        List<Object> tags = runUpsertItemTagsBaseTest(type, accept, "return=representation", targetId, tag);

        assertEquals(1, tags.size());
        assertEquals(tag, tags.getFirst());
    }

    @SuppressWarnings("unchecked")
    private List<Object> runUpsertItemTagsBaseTest(
            String type, String accept, String prefer, String targetId, ItemTagDto... tagList) {

        List<ItemTagDto> tags = Arrays.stream(tagList).toList();

        doReturn(tags.stream().map(ItemTagDto::id).toList())
                .when(mockItemTagService)
                .bulkUpsert(any(), any(), any(), any());
        doReturn(tags).when(mockItemTagService).findItemTag(any(), any(), any(), any(), any());

        ResponseEntity<Object> response = controller()
                .upsertItemTags(
                        accept,
                        prefer,
                        SAMPLE_EHR_ID,
                        targetId,
                        ItemTag.ItemTagRMType.valueOf(type),
                        type.toLowerCase(),
                        tags);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "https://openehr.test.item-tag.com/rest/ehr/%s/%s/%s/item_tag"
                        .formatted(SAMPLE_EHR_ID, type.toLowerCase(), targetId),
                response.getHeaders().getFirst(HttpHeaders.LOCATION));

        return Optional.ofNullable(response.getBody())
                .map(it -> (List<Object>) it)
                .orElseThrow();
    }

    // --- GET ---

    private ResponseEntity<Collection<ItemTagDto>> getItemTags(
            String accept,
            String targetId,
            ItemTag.ItemTagRMType type,
            @Nullable List<String> ids,
            @Nullable List<String> keys) {

        return controller()
                .getItemTag(accept, SAMPLE_EHR_ID, targetId, type, type.name().toLowerCase(), ids, keys);
    }

    @Test
    void getItemTagsCommonUsed() {

        String targetId = "1073d3bd-1490-4f3b-b2ac-95202d18c41d";

        // @format:off
        ItemTagController controller = controller();
        controller.getCompositionItemTags(null, null, MediaType.APPLICATION_JSON_VALUE, SAMPLE_EHR_ID, targetId, List.of(), List.of());
        controller.getEhrStatusItemTags(null, null, MediaType.APPLICATION_XML_VALUE, SAMPLE_EHR_ID, targetId, List.of(), List.of());

        verify(controller, times(1)).getItemTag(MediaType.APPLICATION_JSON_VALUE, SAMPLE_EHR_ID, targetId, ItemTag.ItemTagRMType.COMPOSITION, "composition", List.of(), List.of());
        verify(controller, times(1)).getItemTag(MediaType.APPLICATION_XML_VALUE, SAMPLE_EHR_ID, targetId, ItemTag.ItemTagRMType.EHR_STATUS, "ehr_status", List.of(), List.of());
        // @format:on
    }

    @Test
    void getItemTagsEhrNotFound() {

        doThrow(new ObjectNotFoundException("ehr", "ehr not exist"))
                .when(mockItemTagService)
                .findItemTag(any(), any(), any(), any(), any());

        assertThrowsExactly(
                ObjectNotFoundException.class,
                () -> getItemTags(
                        MediaType.APPLICATION_JSON_VALUE,
                        "4074c093-64c5-4511-a10f-813f78a5e1fc",
                        ItemTag.ItemTagRMType.EHR_STATUS,
                        List.of(),
                        List.of()));
        assertThrowsExactly(
                ObjectNotFoundException.class,
                () -> getItemTags(
                        MediaType.APPLICATION_XML_VALUE,
                        "4074c093-64c5-4511-a10f-813f78a5e1fc",
                        ItemTag.ItemTagRMType.COMPOSITION,
                        List.of(),
                        List.of()));
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
                EHR_STATUS|application/xml||
                EHR_STATUS|application/json||1::key,2::key
                COMPOSITION|application/xml|0df4ff20-be70-4416-ae12-c7681459f46e,02fbaf93-b2f1-4880-920c-e6716ca54e03|
                COMPOSITION|application/json|6f0eec75-e547-4fc1-ada1-ab872456e000|2::key
            """,
            delimiterString = "|")
    void getItemTags(String rawType, String accept, String rawIds, String rawKeys) {

        String targetId = "c143d292-fb07-41f4-8715-ff88f11240d4";
        ItemTag.ItemTagRMType type = ItemTag.ItemTagRMType.valueOf(rawType);
        List<String> keys = Optional.ofNullable(rawKeys)
                .map(it -> Arrays.stream(it.split(",")).toList())
                .orElse(List.of());
        List<String> ids = Optional.ofNullable(rawIds)
                .map(it -> Arrays.stream(it.split(",")).toList())
                .orElse(List.of());

        ItemTagDto itemTagDto = itemTagDto(type, targetId);
        doReturn(List.of(itemTagDto))
                .when(mockItemTagService)
                .findItemTag(
                        UUID.fromString(SAMPLE_EHR_ID),
                        UUID.fromString(targetId),
                        type,
                        ids.stream().map(UUID::fromString).toList(),
                        keys);

        ResponseEntity<Collection<ItemTagDto>> response = getItemTags(accept, targetId, type, ids, keys);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "https://openehr.test.item-tag.com/rest/ehr/%s/%s/%s/item_tag"
                        .formatted(SAMPLE_EHR_ID, type.name().toLowerCase(), targetId),
                response.getHeaders().getFirst(HttpHeaders.LOCATION));

        List<ItemTagDto> tags =
                Objects.requireNonNull(response.getBody()).stream().toList();
        assertEquals(1, tags.size());
        assertSame(itemTagDto, tags.getFirst());
    }

    // --- DELETE ---

    @Test
    void deleteItemTagsCommonUsed() {

        String targetId = "a377d88c-3374-4cea-8149-697b7837de17";
        String id = "248c21df-09ad-476f-87d7-72ca8f58e89f";

        // @format:off
        ItemTagController controller = controller();
        controller.deleteCompositionItemTags(null, null, MediaType.APPLICATION_JSON_VALUE, SAMPLE_EHR_ID, targetId, List.of(id));
        controller.deleteEhrStatusItemTags(null, null, MediaType.APPLICATION_XML_VALUE, SAMPLE_EHR_ID, targetId, List.of(id));

        verify(controller, times(1)).deleteTags(SAMPLE_EHR_ID, targetId, ItemTag.ItemTagRMType.COMPOSITION, List.of(id));
        verify(controller, times(1)).deleteTags(SAMPLE_EHR_ID, targetId, ItemTag.ItemTagRMType.EHR_STATUS, List.of(id));
        // @format:on
    }

    @Test
    void deleteItemTagsEmptyError() {

        UnprocessableEntityException exception =
                assertThrowsExactly(UnprocessableEntityException.class, () -> controller()
                        .deleteTags(
                                SAMPLE_EHR_ID,
                                "a377d88c-3374-4cea-8149-697b7837de17",
                                ItemTag.ItemTagRMType.EHR_STATUS,
                                List.of()));
        assertEquals("ItemTags are empty", exception.getMessage());
    }

    @ParameterizedTest
    @EnumSource(ItemTag.ItemTagRMType.class)
    void deleteItemTagsByIDs(ItemTag.ItemTagRMType type) {

        String id = "95c1b874-be9e-47d7-97a7-0a647e03e0a6";
        String targetId = "a377d88c-3374-4cea-8149-697b7837de17";

        ResponseEntity<Void> response = controller().deleteTags(SAMPLE_EHR_ID, targetId, type, List.of(id));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(mockItemTagService, times(1))
                .bulkDelete(
                        UUID.fromString(SAMPLE_EHR_ID), UUID.fromString(targetId), type, List.of(UUID.fromString(id)));
    }

    @ParameterizedTest
    @EnumSource(ItemTag.ItemTagRMType.class)
    void deleteItemTagsByDto(ItemTag.ItemTagRMType type) {

        String targetId = "a377d88c-3374-4cea-8149-697b7837de17";
        ItemTagDto tagDto = itemTagDto(type, targetId);

        ResponseEntity<Void> response = controller()
                .deleteTags(
                        SAMPLE_EHR_ID,
                        targetId,
                        type,
                        List.of(Map.of("id", Objects.requireNonNull(tagDto.id()).toString())));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(mockItemTagService, times(1))
                .bulkDelete(
                        UUID.fromString(SAMPLE_EHR_ID),
                        UUID.fromString(targetId),
                        type,
                        List.of(Objects.requireNonNull(tagDto.id())));
    }
}
