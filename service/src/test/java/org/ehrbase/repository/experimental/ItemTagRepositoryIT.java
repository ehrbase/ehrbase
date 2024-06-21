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
package org.ehrbase.repository.experimental;

import static org.ehrbase.api.service.experimental.ItemTag.ItemTagRMType.COMPOSITION;
import static org.ehrbase.api.service.experimental.ItemTag.ItemTagRMType.EHR_STATUS;
import static org.ehrbase.jooq.pg.tables.EhrItemTag.EHR_ITEM_TAG;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.experimental.ItemTag;
import org.ehrbase.api.service.experimental.ItemTag.ItemTagRMType;
import org.ehrbase.repository.CompositionRepository;
import org.ehrbase.repository.EhrRepository;
import org.ehrbase.test.ServiceIntegrationTest;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

@ServiceIntegrationTest
class ItemTagRepositoryIT {

    @Autowired
    EhrRepository ehrRepository;

    @Autowired
    CompositionRepository compositionRepository;

    @Autowired
    ItemTagRepository itemTagRepository;

    @Autowired
    DSLContext context;

    private final UUID ehrId = UuidGenerator.randomUUID();
    private final UUID compId = UuidGenerator.randomUUID();
    private final UUID ehrStatusId = ehrId;

    @BeforeEach
    void setUp() {

        EhrStatus status = new EhrStatus();
        status.setUid(new ObjectVersionId(ehrStatusId.toString(), "integration-test", "1"));
        status.setArchetypeNodeId("openEHR-EHR-EHR_STATUS.generic.v1");
        status.setName(new DvText("EHR Status"));
        status.setSubject(new PartySelf(null));
        status.setModifiable(true);
        status.setQueryable(true);

        ehrRepository.commit(ehrId, status, null, null);
    }

    @AfterEach
    void tearDown() {
        // cleanup all remaining tags
        itemTagRepository.adminDeleteAll(ehrId);
    }

    private ItemTag newItemTag(UUID ownerId, UUID target, ItemTagRMType type, String key) {
        return new ItemTag(null, ownerId, target, type, null, key, null);
    }

    private ItemTag copyItemTag(UUID id, ItemTag itemTag, String value) {
        return new ItemTag(
                id,
                itemTag.ownerId(),
                itemTag.target(),
                itemTag.targetType(),
                itemTag.targetPath(),
                itemTag.key(),
                value != null ? value : itemTag.value());
    }

    // --- bulkStore ---

    private Collection<UUID> bulkStore(ItemTag... itemTags) {
        return itemTagRepository.bulkStore(List.of(itemTags));
    }

    @Test
    void bulkStoreEmptyNop() {

        Collection<UUID> ids = bulkStore();
        assertEquals(0, ids.size());
    }

    @Test
    void bulkStoreInsertTargetNotExist() {

        Collection<UUID> ids =
                bulkStore(newItemTag(ehrId, UuidGenerator.randomUUID(), EHR_STATUS, "insert:comp:not_exist:1"));
        assertEquals(1, ids.size(), "There should be one inserted ItemTag id");
    }

    @Test
    void bulkStoreInsertTargetExist() {

        Collection<UUID> ids = bulkStore(newItemTag(ehrId, compId, COMPOSITION, "insert:comp:exist:2"));
        assertEquals(1, ids.size(), "There should be one inserted ItemTag id");
    }

    @Test
    void bulkStoreUpdateTargetExist() {

        ItemTag itemTag = newItemTag(ehrId, compId, COMPOSITION, "update:comp:not_exist:1");

        Collection<UUID> insertIds = bulkStore(itemTag);
        assertEquals(1, insertIds.size(), "There should be one inserted ItemTag id");

        UUID tagId = insertIds.stream().findFirst().orElseThrow();

        ItemTag itemTagToUpdate = copyItemTag(tagId, itemTag, "new value");
        Collection<UUID> updateIds = bulkStore(itemTagToUpdate);

        assertEquals(1, updateIds.size(), "There should be one updated ItemTag id");
        assertEquals(tagId, updateIds.stream().findFirst().orElse(null), "Updated item must have the same id");
    }

    @Test
    void bulkStoreInsertEhrNotExist() {

        List<ItemTag> itemTags = List.of(new ItemTag(
                null,
                UUID.fromString("7eb0db46-b72e-4db0-9955-05bb91275951"),
                compId,
                COMPOSITION,
                null,
                "update:comp:not_exist:1",
                null));
        ObjectNotFoundException exec =
                assertThrows(ObjectNotFoundException.class, () -> itemTagRepository.bulkStore(itemTags));
        assertEquals("EHR with id '7eb0db46-b72e-4db0-9955-05bb91275951' does not exist", exec.getMessage());
    }

    @Test
    void bulkStoreUpdateItemTagExistError() {

        ItemTag itemTag = copyItemTag(
                UUID.fromString("7eb0db46-b72e-4db0-9955-05bb91275951"),
                newItemTag(ehrId, compId, COMPOSITION, "update:comp:not_exist:1"),
                null);

        ObjectNotFoundException exec = assertThrows(ObjectNotFoundException.class, () -> bulkStore(itemTag));
        assertEquals("ItemTag(s) with ID(s) [7eb0db46-b72e-4db0-9955-05bb91275951] does not exist", exec.getMessage());
    }

    // --- findForLatestTargetVersion ---

    private Collection<ItemTag> findForLatestTargetVersion(
            UUID target, ItemTagRMType type, Collection<UUID> ids, Collection<String> keys) {
        return itemTagRepository.findForLatestTargetVersion(ehrId, target, type, ids, keys);
    }

    @Test
    void findForLatestTargetVersion() {

        List<UUID> insertIds = bulkStore(
                        newItemTag(ehrId, compId, COMPOSITION, "find:composition:tag"),
                        newItemTag(ehrId, compId, COMPOSITION, "other:composition:tag"),
                        newItemTag(ehrId, UuidGenerator.randomUUID(), COMPOSITION, "find:composition:tag"),
                        newItemTag(ehrId, ehrStatusId, EHR_STATUS, "find:ehr_status:tag"),
                        newItemTag(ehrId, ehrStatusId, EHR_STATUS, "other:ehr_status:tag"))
                .stream()
                .toList();
        assertEquals(5, insertIds.size(), "There must be 5 inserted ItemTags");

        assertEquals(
                0,
                itemTagRepository
                        .findForLatestTargetVersion(
                                UuidGenerator.randomUUID(), compId, COMPOSITION, List.of(), List.of())
                        .size(),
                "There should be not match for non existing EHR");

        List<ItemTag> compIdMatch = findForLatestTargetVersion(compId, COMPOSITION, List.of(), List.of()).stream()
                .toList();
        assertEquals(2, compIdMatch.size());
        assertEquals(insertIds.get(0), compIdMatch.get(0).id());
        assertEquals(insertIds.get(1), compIdMatch.get(1).id());

        List<ItemTag> compTagIdMatch =
                findForLatestTargetVersion(compId, COMPOSITION, List.of(), List.of("find:composition:tag")).stream()
                        .toList();
        assertEquals(1, compTagIdMatch.size());
        assertEquals(insertIds.get(0), compIdMatch.getFirst().id());

        List<ItemTag> compTagIdIdMatch =
                findForLatestTargetVersion(compId, COMPOSITION, List.of(insertIds.get(1)), List.of()).stream()
                        .toList();
        assertEquals(1, compTagIdIdMatch.size());
        assertEquals(insertIds.get(1), compIdMatch.get(1).id());

        List<ItemTag> ehrStatusMatch =
                findForLatestTargetVersion(ehrStatusId, EHR_STATUS, List.of(), List.of()).stream()
                        .toList();
        assertEquals(2, ehrStatusMatch.size());
        assertEquals(insertIds.get(3), ehrStatusMatch.get(0).id());
        assertEquals(insertIds.get(4), ehrStatusMatch.get(1).id());
    }

    // --- bulkDelete ---

    @Test
    void bulkDeleteEmptyNop() {

        // nop
        assertDoesNotThrow(
                () -> itemTagRepository.bulkDelete(ehrId, UuidGenerator.randomUUID(), COMPOSITION, List.of()));
        // deletion of non-existing simply does nothing
        assertDoesNotThrow(() -> itemTagRepository.bulkDelete(
                ehrId, UuidGenerator.randomUUID(), EHR_STATUS, List.of(UuidGenerator.randomUUID())));
    }

    @Test
    void bulkDeleteNop() {

        Collection<UUID> insertIds = bulkStore(
                newItemTag(ehrId, UuidGenerator.randomUUID(), COMPOSITION, "some:composition:tag"),
                newItemTag(ehrId, UuidGenerator.randomUUID(), EHR_STATUS, "some:ehr_status:tag"));
        assertEquals(2, insertIds.size(), "There should be two inserted ItemTag ids");

        itemTagRepository.bulkDelete(ehrId, UuidGenerator.randomUUID(), COMPOSITION, insertIds);
        itemTagRepository.bulkDelete(ehrId, UuidGenerator.randomUUID(), EHR_STATUS, insertIds);

        assertEquals(
                0,
                findForLatestTargetVersion(compId, COMPOSITION, List.of(), List.of())
                        .size(),
                "The should be COMPOSITION tags");
        assertEquals(
                0,
                findForLatestTargetVersion(ehrStatusId, EHR_STATUS, List.of(), List.of())
                        .size(),
                "The should be EHR_STATUS tags");
    }

    @ParameterizedTest
    @CsvSource(
            textBlock =
                    """
            EHR_STATUS|ee77096d-8aae-41ae-8c80-1c14e8e66792
            COMPOSITION|a8019d32-af8b-49ec-a8b8-9df87acb857c
            """,
            delimiterString = "|")
    void adminDelete(String type, String id) {

        UUID targetId = UUID.fromString(id);
        ItemTagRMType tagType = ItemTagRMType.valueOf(type);

        Collection<UUID> insertIds = bulkStore(
                newItemTag(ehrId, targetId, tagType, "some:%s:tag".formatted(type.toLowerCase())),
                newItemTag(ehrId, UuidGenerator.randomUUID(), COMPOSITION, "some:composition:tag"),
                newItemTag(ehrId, UuidGenerator.randomUUID(), EHR_STATUS, "some:ehr_status:tag"));
        assertEquals(3, insertIds.size(), "There should be two inserted ItemTag ids");

        itemTagRepository.adminDelete(targetId);
        assertEquals(
                0,
                itemTagRepository
                        .findForLatestTargetVersion(ehrId, targetId, tagType, List.of(), List.of())
                        .size());
    }

    @Test
    void adminDeleteAll() {

        Collection<UUID> insertIds = bulkStore(
                newItemTag(ehrId, UuidGenerator.randomUUID(), COMPOSITION, "some:composition:tag"),
                newItemTag(ehrId, UuidGenerator.randomUUID(), EHR_STATUS, "some:ehr_status:tag"));
        assertEquals(2, insertIds.size(), "There should be two inserted ItemTag ids");

        itemTagRepository.adminDeleteAll(ehrId);

        assertEquals(0, context.fetchCount(EHR_ITEM_TAG, EHR_ITEM_TAG.EHR_ID.eq(ehrId)));
    }
}
