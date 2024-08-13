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
package org.ehrbase.service.experimental;

import static org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType.COMPOSITION;
import static org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType.EHR_STATUS;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.ehrbase.api.dto.experimental.ItemTagDto;
import org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.experimental.ItemTagService;
import org.ehrbase.repository.experimental.ItemTagRepository;
import org.ehrbase.util.UuidGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class ItemTagServiceTest {

    private static final UUID SAMPLE_EHR_ID = UUID.fromString("90867c82-8498-4536-b2b6-16ea4e62818f");

    private final ItemTagRepository mockIemTagRepository = Mockito.mock();
    private final EhrService mockEhrService = Mockito.mock();

    @BeforeEach
    void setUp() {
        Mockito.reset(mockIemTagRepository, mockEhrService);
    }

    private ItemTagService service() {
        return new ItemTagServiceImpl(mockIemTagRepository, mockEhrService);
    }

    private org.ehrbase.api.dto.experimental.ItemTagDto itemTagDto(String key) {
        return new org.ehrbase.api.dto.experimental.ItemTagDto(null, null, null, null, null, key, null);
    }

    @Test
    void bulkUpsertEmptyListNop() {

        Collection<UUID> ids = service()
                .bulkUpsert(
                        SAMPLE_EHR_ID, UUID.fromString("6d4622d6-ecbb-456a-981a-59423a374a2e"), COMPOSITION, List.of());
        assertEquals(0, ids.size());
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
            EHR_STATUS|90867c82-8498-4536-b2b6-16ea4e62818f
            COMPOSITION|6d4622d6-ecbb-456a-981a-59423a374a2e
            """,
            delimiterString = "|")
    void bulkUpsertEhrNotExistError(String type, String targetId) {

        UUID target = UUID.fromString(targetId);
        ItemTagRMType targetType = org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType.valueOf(type);

        ItemTagService service = service();
        assertThrowsObjectNotFound(
                () -> service.bulkUpsert(SAMPLE_EHR_ID, target, targetType, List.of(itemTagDto("some:key"))));
    }

    @Test
    void bulkUpdaterChangeOfOwnerError() {

        List<org.ehrbase.api.dto.experimental.ItemTagDto> itemTags =
                List.of(new org.ehrbase.api.dto.experimental.ItemTagDto(
                        UUID.randomUUID(),
                        UUID.fromString("1dc42f91-094a-43e7-8c26-3ca6d43b8833"),
                        SAMPLE_EHR_ID,
                        EHR_STATUS,
                        null,
                        "a:key",
                        null));

        ItemTagService service = service();
        UnprocessableEntityException exception = assertThrows(
                UnprocessableEntityException.class,
                () -> service.bulkUpsert(SAMPLE_EHR_ID, SAMPLE_EHR_ID, EHR_STATUS, itemTags));
        assertEquals(
                "Owner mismatch for ItemTag 'a:key': 1dc42f91-094a-43e7-8c26-3ca6d43b8833 vs. %s"
                        .formatted(SAMPLE_EHR_ID.toString()),
                exception.getMessage());
    }

    @Test
    void buildUpdateTargetTypeMissmatch() {

        List<org.ehrbase.api.dto.experimental.ItemTagDto> itemTags =
                List.of(new org.ehrbase.api.dto.experimental.ItemTagDto(
                        UUID.randomUUID(),
                        SAMPLE_EHR_ID,
                        UUID.fromString("f5fe8b05-2fe3-4962-a0b7-d443e0b53304"),
                        COMPOSITION,
                        null,
                        "some:key",
                        null));

        ItemTagService service = service();
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> service.bulkUpsert(
                        SAMPLE_EHR_ID, UUID.fromString("f5fe8b05-2fe3-4962-a0b7-d443e0b53304"), EHR_STATUS, itemTags));
        assertEquals("target_type does not match EHR_STATUS", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
            EHR_STATUS|1a598f7a-fbc2-4762-9fae-6480390b9bb5
            COMPOSITION|9448b3b8-866f-4415-97d2-d5605a553e45
            """,
            delimiterString = "|")
    void bulkUpsert(String type, String targetId) {

        UUID target = UUID.fromString(targetId);
        ItemTagRMType targetType = org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType.valueOf(type);

        doNothing().when(mockEhrService).checkEhrExists(SAMPLE_EHR_ID);

        UUID tagId = UUID.fromString("49532718-9a4f-4c3c-a1ad-86af95a1751c");
        List<org.ehrbase.api.dto.experimental.ItemTagDto> itemTags = List.of(
                itemTagDto("1:new:key"),
                new org.ehrbase.api.dto.experimental.ItemTagDto(tagId, null, null, null, null, "2:existing:key", null));

        Collection<UUID> fixtureIDs = List.of(UuidGenerator.randomUUID(), tagId);
        doReturn(fixtureIDs).when(mockIemTagRepository).bulkStore(any());

        Collection<UUID> ids = service().bulkUpsert(SAMPLE_EHR_ID, target, targetType, itemTags);
        assertSame(fixtureIDs, ids);

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<ItemTagDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockIemTagRepository, times(1)).bulkStore(captor.capture()); // for instance

        List<ItemTagDto> storedTags = captor.getValue().stream()
                .sorted(Comparator.comparing(ItemTagDto::getKey))
                .toList();
        assertEquals(2, storedTags.size());
        assertEquals(
                new ItemTagDto(null, SAMPLE_EHR_ID, target, targetType, null, "1:new:key", null), storedTags.get(0));
        assertEquals(
                new ItemTagDto(tagId, SAMPLE_EHR_ID, target, targetType, null, "2:existing:key", null),
                storedTags.get(1));
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
            EHR_STATUS|422f7ddf-5646-4db8-b984-2df23d5021da
            COMPOSITION|6d4622d6-ecbb-456a-981a-59423a374a2e
            """,
            delimiterString = "|")
    void findEhrNotExistError(String type, String targetId) {

        UUID target = UUID.fromString(targetId);
        ItemTagRMType targetType = org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType.valueOf(type);

        ItemTagService service = service();
        assertThrowsObjectNotFound(() -> service.findItemTag(SAMPLE_EHR_ID, target, targetType, List.of(), List.of()));
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
            EHR_STATUS|90867c82-8498-4536-b2b6-16ea4e62818f|tag::1
            COMPOSITION|7eb105ca-d671-4220-9103-d08db0403dc0|tag::2
            """,
            delimiterString = "|")
    void findItemByTag(String type, String targetId, String key) {

        UUID target = UUID.fromString(targetId);
        ItemTagRMType targetType = org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType.valueOf(type);
        ItemTagService service = service();

        ItemTagDto itemTag = new ItemTagDto(UUID.randomUUID(), SAMPLE_EHR_ID, target, targetType, null, key, null);
        doReturn(List.of(itemTag))
                .when(mockIemTagRepository)
                .findForOwnerAndTarget(SAMPLE_EHR_ID, target, targetType, List.of(), List.of(key));

        Collection<org.ehrbase.api.dto.experimental.ItemTagDto> result =
                service.findItemTag(SAMPLE_EHR_ID, target, targetType, List.of(), List.of(key));
        assertEquals(1, result.size());
        assertEquals(
                new org.ehrbase.api.dto.experimental.ItemTagDto(
                        itemTag.getId(), SAMPLE_EHR_ID, target, targetType, null, key, null),
                result.stream().findFirst().orElseThrow());
    }

    @Test
    void bulkDeleteNop() {

        doThrow(new ObjectNotFoundException("test", "test"))
                .when(mockEhrService)
                .checkEhrExists(SAMPLE_EHR_ID);
        assertDoesNotThrow(() -> service().bulkDelete(SAMPLE_EHR_ID, UUID.randomUUID(), EHR_STATUS, List.of()));
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
            EHR_STATUS|422f7ddf-5646-4db8-b984-2df23d5021da
            COMPOSITION|6d4622d6-ecbb-456a-981a-59423a374a2e
            """,
            delimiterString = "|")
    void bulkDeleteEhrNotExistError(String type, String targetId) {

        UUID target = UUID.fromString(targetId);
        ItemTagRMType targetType = org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType.valueOf(type);

        ItemTagService service = service();
        assertThrowsObjectNotFound(
                () -> service.bulkDelete(SAMPLE_EHR_ID, target, targetType, List.of(UUID.randomUUID())));
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
            EHR_STATUS|b84b5a08-e6fa-4988-b40a-82f162a8f068
            COMPOSITION|f1251117-66e2-49e5-884b-c69641ad36e3
            """,
            delimiterString = "|")
    void bulkDelete(String type, String targetId) {

        UUID target = UUID.fromString(targetId);
        ItemTagRMType targetType = org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType.valueOf(type);
        ItemTagService service = service();

        UUID id = UUID.fromString("247d5155-a4bd-4ad6-ae9a-a8112d1fcd25");
        service.bulkDelete(SAMPLE_EHR_ID, target, targetType, List.of(id));

        verify(mockIemTagRepository, times(1)).bulkDelete(SAMPLE_EHR_ID, target, targetType, List.of(id));
    }

    @TestFactory
    Collection<DynamicTest> validTagKey() {

        String allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789/-_:";

        return allowed.chars()
                .mapToObj(c -> DynamicTest.dynamicTest(
                        "Key [%c]".formatted(c),
                        () -> assertDoesNotThrow(() -> ItemTagServiceImpl.validateTagKey("%c".formatted(c)))))
                .toList();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "", " "})
    void invalidTagKeyEmptyOrBlank(String key) {

        var tagKey = "null".equals(key) ? null : key;
        UnprocessableEntityException exception =
                assertThrows(UnprocessableEntityException.class, () -> ItemTagServiceImpl.validateTagKey(tagKey));
        assertEquals("ItemTag key must not be blank", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ä", "&", "=", "\\", "\"",
            })
    void invalidTagKey(String key) {

        UnprocessableEntityException exception =
                assertThrows(UnprocessableEntityException.class, () -> ItemTagServiceImpl.validateTagKey(key));
        assertEquals(
                "ItemTag key '%s' contains invalid characters, only alphanumerics, minus, slash, underscore are allowed"
                        .formatted(key),
                exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void invalidTagValueEmptyOrBlank(String value) {

        UnprocessableEntityException exception = assertThrows(
                UnprocessableEntityException.class, () -> ItemTagServiceImpl.validateTagValue("key", value));
        assertEquals("ItemTag 'key' value must not be blank", exception.getMessage());
    }

    @Test
    void validTagValue() {

        assertDoesNotThrow(() -> ItemTagServiceImpl.validateTagValue("key", "Some tag, lorem ipsum? Dolor et amer!"));
        assertDoesNotThrow(
                () -> ItemTagServiceImpl.validateTagValue(
                        "key",
                        """
                {
                    "could": "also be json"
                }
                """));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/content",
                "/content/value/name",
                "/item[at001]/value/item[at002]",
                "/content[openEHR-EHR-SECTION.medications.v1]"
            })
    void validTargetPathDeclaration(String path) {

        assertDoesNotThrow(() -> ItemTagServiceImpl.validateTargetPath("key:path", path));
    }

    // --- Assertions ---

    private void assertThrowsObjectNotFound(Executable executable) {

        ObjectNotFoundException expectedException = new ObjectNotFoundException("test", "test");
        doThrow(expectedException).when(mockEhrService).checkEhrExists(SAMPLE_EHR_ID);

        ObjectNotFoundException exception = Assertions.assertThrows(ObjectNotFoundException.class, executable);
        assertSame(expectedException, exception);
    }
}
