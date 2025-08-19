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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.ehrbase.api.dto.experimental.ItemTagDto;
import org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.jooq.pg.enums.EhrItemTagTargetType;
import org.ehrbase.jooq.pg.tables.records.EhrItemTagRecord;
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
     * Stores the given @{@link ItemTagDto}s by performing an <code>Insert</code> for tags without an {@link ItemTagDto#getId()}
     * or <code>Update</code> with existence check for tags with an {@link ItemTagDto#getId()}
     *
     * @param itemTags  @{@link ItemTagDto}s store using <code>Insert</code> or <code>Update</code>.
     * @return tagIDs   Sored {@link ItemTagDto#getId()}s.
     */
    @Transactional
    public List<UUID> bulkStore(@NonNull List<ItemTagDto> itemTags) {

        if (itemTags.isEmpty()) {
            return List.of();
        }

        List<EhrItemTagRecord> newTags = itemTags.stream()
                .filter(tag -> tag.getId() == null)
                .map(this::newRecordForTag)
                .toList();

        Map<UUID, ItemTagDto> existingTagsById = itemTags.stream()
                .filter(tag -> tag.getId() != null)
                .map(tag -> new AbstractMap.SimpleImmutableEntry<>(tag.getId(), tag))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<EhrItemTagRecord> existingTags = context.selectFrom(EHR_ITEM_TAG)
                .where(EHR_ITEM_TAG.ID.in(existingTagsById.keySet()))
                .collect(Collectors.mapping(
                        dbRecord -> {
                            ItemTagDto itemTag = existingTagsById.remove(dbRecord.getId());
                            mapItemTag(itemTag, dbRecord);
                            return dbRecord;
                        },
                        Collectors.toList()));

        if (!existingTagsById.isEmpty()) {
            throw new ObjectNotFoundException(
                    ItemTagDto.class.getSimpleName(),
                    "ItemTag(s) with ID(s) %s not found".formatted(existingTagsById.keySet()));
        }
        bulkInsert(newTags);
        context.batchUpdate(existingTags).execute();

        // retain order of itemTags parameter
        Iterator<EhrItemTagRecord> it = newTags.iterator();
        return itemTags.stream()
                .map(t -> Optional.of(t).map(ItemTagDto::getId).orElseGet(() -> it.next()
                        .getId()))
                .toList();
    }

    /**
     * Search @{@link ItemTagDto}s of <code>ownerId</code>, for the latest version of the given <code>targetVoId</code>
     * by applying the optional filter for <code>ids, keys</code>.
     *
     * @param ownerId       Identifier of owner object, such as EHR.
     * @param targetVoId    VERSIONED_OBJECT&lt;T&gt; Identifier of target.
     * @param targetType    Type of the target object.
     * @param ids           Identifier <code>ItemTag</code> to search for.
     * @param keys          <code>ItemTag</code> keys to search for.
     * @return itemTags      <code>ItemTag</code> .
     */
    @Transactional
    public Collection<ItemTagDto> findForOwnerAndTarget(
            @NonNull UUID ownerId,
            @NonNull UUID targetVoId,
            @NonNull ItemTagRMType targetType,
            @NonNull Collection<UUID> ids,
            @NonNull Collection<String> keys) {

        SelectConditionStep<Record1<EhrItemTagRecord>> query = context.select(EHR_ITEM_TAG)
                .from(EHR_ITEM_TAG)
                .where(EHR_ITEM_TAG.EHR_ID.eq(ownerId))
                .and(EHR_ITEM_TAG.TARGET_TYPE.eq(itemTargetTypeToDbEnum(targetType)))
                .and(EHR_ITEM_TAG.TARGET_VO_ID.eq(targetVoId));

        if (!ids.isEmpty()) {
            query = query.and(EHR_ITEM_TAG.ID.in(ids));
        }
        if (!keys.isEmpty()) {
            query = query.and(EHR_ITEM_TAG.KEY.in(keys));
        }
        return query.fetch().map(ItemTagRepository::recordAsItemTag);
    }

    /**
     * Bulk delete @{@link ItemTagDto}s with the given <code>ids</code>.
     *
     * @param ids  Identifier of <code>ItemTag</code> to delete.
     */
    @Transactional
    public int bulkDelete(
            @NonNull UUID ownerId,
            @NonNull UUID targetVoId,
            @NonNull ItemTagRMType targetType,
            @NonNull Collection<UUID> ids) {

        if (ids.isEmpty()) {
            return 0;
        }
        return context.delete(EHR_ITEM_TAG)
                .where(EHR_ITEM_TAG.EHR_ID.eq(ownerId))
                .and(EHR_ITEM_TAG.TARGET_TYPE.eq(itemTargetTypeToDbEnum(targetType)))
                .and(EHR_ITEM_TAG.TARGET_VO_ID.eq(targetVoId))
                .and(EHR_ITEM_TAG.ID.in(ids))
                .execute();
    }

    @Transactional
    public void adminDelete(UUID targetId, ItemTagRMType targetType) {
        context.delete(EHR_ITEM_TAG)
                .where(EHR_ITEM_TAG.TARGET_VO_ID.eq(targetId))
                .and(EHR_ITEM_TAG.TARGET_TYPE.eq(itemTargetTypeToDbEnum(targetType)))
                .execute();
    }

    @Transactional
    public void adminDeleteAll(UUID ehrId) {
        context.delete(EHR_ITEM_TAG).where(EHR_ITEM_TAG.EHR_ID.eq(ehrId)).execute();
    }

    private void bulkInsert(List<EhrItemTagRecord> records) {

        if (records.isEmpty()) {
            return;
        }
        try {
            RepositoryHelper.executeBulkInsert(context.dsl(), records.stream(), EHR_ITEM_TAG);
        } catch (DataIntegrityViolationException e) {

            String[] details = e.getMessage().split("[(|)]");
            throw new ObjectNotFoundException(
                    "EHR", "EHR with id '%s' does not exist".formatted(details[details.length - 2]));
        }
    }

    private EhrItemTagRecord newRecordForTag(ItemTagDto itemTag) {

        EhrItemTagRecord itemTagRecord = context.newRecord(EHR_ITEM_TAG);
        mapItemTag(itemTag, itemTagRecord);
        itemTagRecord.setCreationDate(timeProvider.getNow());
        return itemTagRecord;
    }

    private void mapItemTag(ItemTagDto itemTag, EhrItemTagRecord itemTagRecord) {
        itemTagRecord.setId(Optional.ofNullable(itemTag.getId()).orElseGet(UuidGenerator::randomUUID));
        itemTagRecord.setEhrId(itemTag.getOwnerId());
        itemTagRecord.setTargetVoId(itemTag.getTarget());
        itemTagRecord.setTargetType(itemTargetTypeToDbEnum(itemTag.getTargetType()));
        itemTagRecord.setKey(itemTag.getKey());
        itemTagRecord.setValue(itemTag.getValue());
        itemTagRecord.setTargetPath(itemTag.getTargetPath());
        itemTagRecord.setSysPeriodLower(timeProvider.getNow());
    }

    public static ItemTagDto recordAsItemTag(Record1<EhrItemTagRecord> dbRecord) {
        EhrItemTagRecord itemTagRecord = dbRecord.component1();
        return new ItemTagDto(
                itemTagRecord.getId(),
                itemTagRecord.getEhrId(),
                itemTagRecord.getTargetVoId(),
                dbEnumToTargetType(itemTagRecord.getTargetType()),
                itemTagRecord.getTargetPath(),
                itemTagRecord.getKey(),
                itemTagRecord.getValue());
    }

    private static @Nonnull ItemTagRMType dbEnumToTargetType(final EhrItemTagTargetType dbEnum) {
        return switch (dbEnum) {
            case ehr_status -> ItemTagRMType.EHR_STATUS;
            case composition -> ItemTagRMType.COMPOSITION;
        };
    }

    private static EhrItemTagTargetType itemTargetTypeToDbEnum(ItemTagRMType type) {
        return switch (type) {
            case EHR_STATUS -> EhrItemTagTargetType.ehr_status;
            case COMPOSITION -> EhrItemTagTargetType.composition;
        };
    }
}
