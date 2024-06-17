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

import static org.ehrbase.jooq.pg.tables.EhrItemTag.EHR_ITEM_TAG;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.experimental.ItemTag;
import org.ehrbase.jooq.pg.enums.EhrItemTagTargetType;
import org.ehrbase.jooq.pg.tables.records.EhrItemTagRecord;
import org.ehrbase.openehr.sdk.aql.webtemplatepath.AqlPath;
import org.ehrbase.repository.RepositoryHelper;
import org.ehrbase.service.TimeProvider;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository responsible for <code>ITEM_TAG</code> persistence.
 */
@Repository
public class ItemTagRepository {

    private final DSLContext context;

    private final TimeProvider timeProvider;

    public ItemTagRepository(DSLContext context, TimeProvider timeProvider) {

        this.context = context;
        this.timeProvider = timeProvider;
    }

    /**
     * Stores the given @{@link ItemTag}s by performing an <code>Insert</code> for tags without an {@link ItemTag#id()}
     * or <code>Update</code> with existence check for tags with an {@link ItemTag#id()}
     *
     * @param itemTags  @{@link ItemTag}s store using <code>Insert</code> or <code>Update</code>.
     * @return tagIDs   Sored {@link ItemTag#id()}s.
     */
    @Transactional
    public Collection<UUID> bulkStore(@NonNull Collection<ItemTag> itemTags) {

        List<EhrItemTagRecord> newTags = itemTags.stream()
                .filter(tag -> tag.id() == null)
                .map(this::createInsertTagAsRecord)
                .toList();

        Map<UUID, ItemTag> existingTagsById = itemTags.stream()
                .filter(tag -> tag.id() != null)
                .map(tag -> new AbstractMap.SimpleImmutableEntry<>(tag.id(), tag))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (newTags.isEmpty() && existingTagsById.isEmpty()) {
            return List.of();
        }

        List<EhrItemTagRecord> existingTags =
                context
                        .select(EHR_ITEM_TAG)
                        .from(EHR_ITEM_TAG)
                        .where(EHR_ITEM_TAG.ID.in(existingTagsById.keySet()))
                        .stream()
                        .map(dbRecord -> mapExistingTagAsRecord(existingTagsById, dbRecord))
                        .toList();

        if (!existingTagsById.isEmpty()) {
            throw new ObjectNotFoundException(
                    ItemTag.class.getSimpleName(),
                    "ItemTag(s) with ID(s) %s does not exist".formatted(existingTagsById.keySet()));
        }

        return Stream.concat(bulkUpdate(existingTags), bulkInsert(newTags)).toList();
    }

    /**
     * Search @{@link ItemTag}s of <code>ownerId</code>, for the latest version of the given <code>targetVoId</code>
     * by applying the optional filter for <code>ids, keys</code>.
     *
     * @param ownerId       Identifier of owner object, such as EHR.
     * @param targetVoId    VERSIONED_OBJECT&lt;T&gt; Identifier of target.
     * @param targetType    Type of the target object.
     * @param ids           Identifier <code>ItemTag</code> to search for.
     * @param keys          <code>ItemTag</code> keys to search for.
     * @return itemTgs      <code>ItemTag</code> .
     */
    @Transactional
    public Collection<ItemTag> findForLatestTargetVersion(
            @NonNull UUID ownerId,
            @NonNull UUID targetVoId,
            @NonNull ItemTag.ItemTagRMType targetType,
            @NonNull Collection<UUID> ids,
            @NonNull Collection<String> keys) {

        SelectConditionStep<Record1<EhrItemTagRecord>> query = context.select(EHR_ITEM_TAG)
                .from(EHR_ITEM_TAG)
                .where(EHR_ITEM_TAG.TARGET_TYPE.eq(itemTargetTypeToEnum(targetType)))
                .and(EHR_ITEM_TAG.EHR_ID.eq(ownerId).and(EHR_ITEM_TAG.TARGET_VO_ID.eq(targetVoId)));

        if (!ids.isEmpty()) {
            query = query.and(EHR_ITEM_TAG.ID.in(ids));
        }
        if (!keys.isEmpty()) {
            query = query.and(EHR_ITEM_TAG.KEY.in(keys));
        }
        return query.fetch().map(ItemTagRepository::recordAsItemTag);
    }

    /**
     * Bulk delete @{@link ItemTag}s with the given <code>ids</code>.
     *
     * @param ids  Identifier of <code>ItemTag</code> to delete.
     */
    @Transactional
    public void bulkDelete(UUID ownerId, UUID targetId, Collection<UUID> ids) {

        if (ids.isEmpty()) {
            return;
        }
        context.delete(EHR_ITEM_TAG)
                .where(EHR_ITEM_TAG.EHR_ID.eq(ownerId))
                .and(EHR_ITEM_TAG.TARGET_VO_ID.eq(targetId))
                .and(EHR_ITEM_TAG.ID.in(ids))
                .execute();
    }

    @Transactional
    public void adminDelete(UUID targetId) {
        context.delete(EHR_ITEM_TAG)
                .where(EHR_ITEM_TAG.TARGET_VO_ID.eq(targetId))
                .execute();
    }

    @Transactional
    public void adminDeleteAll(UUID ehrId) {
        context.delete(EHR_ITEM_TAG).where(EHR_ITEM_TAG.EHR_ID.eq(ehrId)).execute();
    }

    private Stream<UUID> bulkInsert(Collection<EhrItemTagRecord> records) {

        if (records.isEmpty()) {
            return Stream.empty();
        }
        try {
            RepositoryHelper.executeBulkInsert(context.dsl(), records.stream(), EHR_ITEM_TAG);
        } catch (DataIntegrityViolationException e) {

            String[] details = e.getMessage().split("[(|)]");
            throw new ObjectNotFoundException(
                    "EHR", "EHR with id '%s' does not exist".formatted(details[details.length - 2]));
        }
        return records.stream().map(EhrItemTagRecord::getId);
    }

    private Stream<UUID> bulkUpdate(Collection<EhrItemTagRecord> records) {

        if (records.isEmpty()) {
            return Stream.empty();
        }

        context.batchUpdate(records).execute();
        return records.stream().map(EhrItemTagRecord::getId);
    }

    private EhrItemTagRecord createInsertTagAsRecord(ItemTag itemTag) {

        EhrItemTagRecord itemTagRecord = context.newRecord(EHR_ITEM_TAG);
        mapItemTag(itemTag, itemTagRecord);
        itemTagRecord.setCreationDate(timeProvider.getNow());
        return itemTagRecord;
    }

    private EhrItemTagRecord mapExistingTagAsRecord(
            Map<UUID, ItemTag> existingTagsById, Record1<EhrItemTagRecord> dbRecord) {
        EhrItemTagRecord itemTagRecord = dbRecord.component1();
        ItemTag itemTag = existingTagsById.remove(itemTagRecord.getId());
        mapItemTag(itemTag, itemTagRecord);
        return itemTagRecord;
    }

    private void mapItemTag(ItemTag itemTag, EhrItemTagRecord itemTagRecord) {
        itemTagRecord.setId(Optional.ofNullable(itemTag.id()).orElseGet(UuidGenerator::randomUUID));
        itemTagRecord.setEhrId(itemTag.ownerId());
        itemTagRecord.setTargetVoId(itemTag.target());
        itemTagRecord.setTargetType(itemTargetTypeToEnum(itemTag.targetType()));
        itemTagRecord.setKey(itemTag.key());
        itemTagRecord.setValue(itemTag.value());
        itemTagRecord.setTargetPath(
                Optional.ofNullable(itemTag.targetPath()).map(AqlPath::getPath).orElse(null));
        itemTagRecord.setSysPeriodLower(timeProvider.getNow());
    }

    private static ItemTag recordAsItemTag(Record1<EhrItemTagRecord> dbRecord) {
        EhrItemTagRecord itemTagRecord = dbRecord.component1();
        return new ItemTag(
                itemTagRecord.getId(),
                itemTagRecord.getEhrId(),
                itemTagRecord.getTargetVoId(),
                switch (itemTagRecord.getTargetType()) {
                    case ehr_status -> ItemTag.ItemTagRMType.EHR_STATUS;
                    case composition -> ItemTag.ItemTagRMType.COMPOSITION;
                },
                Optional.ofNullable(itemTagRecord.getTargetPath())
                        .map(AqlPath::parse)
                        .orElse(null),
                itemTagRecord.getKey(),
                itemTagRecord.getValue());
    }

    private static EhrItemTagTargetType itemTargetTypeToEnum(ItemTag.ItemTagRMType type) {
        return switch (type) {
            case EHR_STATUS -> EhrItemTagTargetType.ehr_status;
            case COMPOSITION -> EhrItemTagTargetType.composition;
        };
    }
}
