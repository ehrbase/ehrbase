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

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.experimental.ItemTag;
import org.ehrbase.api.service.experimental.ItemTagDto;
import org.ehrbase.api.service.experimental.ItemTagService;
import org.ehrbase.openehr.sdk.aql.webtemplatepath.AqlPath;
import org.ehrbase.repository.experimental.ItemTagRepository;
import org.springframework.stereotype.Service;

/**
 * {@link ItemTagService} implementation.
 */
@Service
public class ItemTagServiceImpl implements ItemTagService {

    private final ItemTagRepository itemTagRepository;

    private final EhrService ehrService;

    public ItemTagServiceImpl(ItemTagRepository itemTagRepository, EhrService ehrService) {
        this.itemTagRepository = itemTagRepository;
        this.ehrService = ehrService;
    }

    public Collection<UUID> bulkUpsert(
            @Nonnull UUID ownerId,
            @Nonnull UUID targetId,
            @Nonnull ItemTag.ItemTagRMType targetType,
            @Nonnull Collection<ItemTagDto> itemTagsDto) {

        if (itemTagsDto.isEmpty()) {
            return List.of();
        }

        // sanity check for existing EHR version
        ehrService.checkEhrExists(ownerId);

        // validate and collect errors
        List<ItemTag> itemTags = itemTagsDto.stream()
                .map(dto -> itemTagFromDto(ownerId, targetId, targetType, dto))
                .toList();

        return itemTagRepository.bulkStore(itemTags);
    }

    @Override
    public Collection<ItemTagDto> findItemTag(
            @Nonnull UUID ownerId,
            @Nonnull UUID targetVoId,
            @Nonnull ItemTag.ItemTagRMType targetType,
            @Nonnull Collection<UUID> ids,
            @Nonnull Collection<String> keys) {

        // sanity check for existing EHR version
        ehrService.checkEhrExists(ownerId);

        return itemTagRepository.findForLatestTargetVersion(ownerId, targetVoId, targetType, ids, keys).stream()
                .map(ItemTagServiceImpl::itemTagToDto)
                .toList();
    }

    @Override
    public void bulkDelete(
            @Nonnull UUID ownerId,
            @Nonnull UUID targetVoId,
            @Nonnull ItemTag.ItemTagRMType targetType,
            @Nonnull Collection<UUID> ids) {

        if (ids.isEmpty()) {
            return;
        }

        // sanity check for existing EHR version
        ehrService.checkEhrExists(ownerId);

        itemTagRepository.bulkDelete(ownerId, targetVoId, targetType, ids);
    }

    private static ItemTag itemTagFromDto(
            UUID ownerId, UUID targetVoId, ItemTag.ItemTagRMType targetType, ItemTagDto dto) {

        final String key = dto.key();
        final String value = dto.value();
        final String targetPath = dto.targetPath();

        // Changing the owner is not supported - we keep the EHR for the tag
        if (dto.ownerId() != null && !Objects.equals(dto.ownerId(), ownerId)) {
            throw new UnprocessableEntityException(
                    "Can not change owner of ItemTag '%s' from %s to %s".formatted(key, dto.ownerId(), ownerId));
        }

        // tag validation
        validateTagKey(key);
        validateTagValue(key, value);
        AqlPath path = validateTargetPath(key, targetPath);

        // target path not empty
        return new ItemTag(dto.id(), ownerId, targetVoId, targetType, path, key, value);
    }

    /**
     * ITEM_TAG.key: May not be empty or contain leading or trailing whitespace.
     * <p>
     * In order to ensure compatibility with potential constraints in the openEHR specs, we propose the following
     * defensive formatting rules:
     * <ul>
     *      <li>not empty</li>
     *      <li>no whitespace</li>
     *      <li>alphanumerics, minus, slash, underscore</li>
     * </ul>
     * </p>
     */
    @VisibleForTesting
    static void validateTagKey(String key) {
        // validate given properties
        if (key == null || key.isEmpty() || key.isBlank()) {
            throw new UnprocessableEntityException("ItemTag must have a key that can not be empty or blank");
        }
        if (!key.matches("^[a-zA-Z0-9/\\-_:]*$")) {
            throw new UnprocessableEntityException(
                    "ItemTag key '%s' contains invalid characters, only alphanumerics, minus, slash, underscore are allowed"
                            .formatted(key));
        }
    }

    /**
     * ITEM_TAG.value: If set, may not be empty.
     */
    @VisibleForTesting
    static void validateTagValue(String key, String value) {
        if (value != null && (value.isEmpty() || value.isBlank())) {
            throw new UnprocessableEntityException("ItemTag '%s' value can not be empty or blank".formatted(key));
        }
    }

    /**
     * ITEM_TAG.target_path: Optional archetype (i.e. AQL) or RM path within target, used to tag a fine-grained element.
     * <p>
     * Simple PATHABLE/LOCATABLE path:
     * <ul>
     *✅    <li>starts at the root of the data contained in the VERSION contained in the VERSIONED_OBJECT referenced by the ITEM_TAG</li>
     *     <li>only archetype_node_id with optional name as predicates</li>
     *     <li>no additional AND or OR predicates</li>
     *     <li>all path segments, but the last must be either PATHABLE or LOCATABLE (or fall into the STRUCTURE_INTERMEDIATE category)</li>
     *     <li>the last path segment can be an RM object or a primitive attribute, but must not be an array</li>
     *     <li>must be valid within the openEHR RM</li>
     * </ul>
     * </p>
     */
    @VisibleForTesting
    static AqlPath validateTargetPath(String key, String targetPath) {

        if (targetPath == null) {
            return null;
        }

        if (targetPath.contains("|")) {
            throw new UnprocessableEntityException(
                    "ItemTag '%s' target_path '%s' attributes are not supported".formatted(key, targetPath));
        }
        // validate given properties
        if (!targetPath.startsWith("/")) {
            throw new UnprocessableEntityException(
                    "ItemTag '%s' target_path '%s' does not start at root".formatted(key, targetPath));
        }

        AqlPath path = AqlPath.parse(targetPath);
        path.getNodes().forEach(node -> {
            if (node.getName().contains(" ")) {
                throw new UnprocessableEntityException(
                        "ItemTag '%s' target_path '%s' can not contain blank lines".formatted(key, targetPath));
            }

            node.getOtherPredicate().getValues().forEach(value -> {
                if (!value.getStatement().equals("archetype_node_id")) {
                    throw new UnprocessableEntityException(
                            "ItemTag '%s' target_path '%s' additional AND or OR predicates are not supported"
                                    .formatted(key, targetPath));
                }
            });
        });
        return path;
    }

    private static ItemTagDto itemTagToDto(ItemTag itemTag) {
        return new ItemTagDto(
                itemTag.id(),
                itemTag.ownerId(),
                itemTag.target(),
                itemTag.targetType(),
                Optional.ofNullable(itemTag.targetPath()).map(AqlPath::getPath).orElse(null),
                itemTag.key(),
                itemTag.value());
    }
}
