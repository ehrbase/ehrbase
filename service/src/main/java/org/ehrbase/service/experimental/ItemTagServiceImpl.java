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
import java.util.UUID;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.dto.experimental.ItemTagDto;
import org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.api.service.experimental.ItemTagService;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPathUtil;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
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
            @Nonnull ItemTagRMType targetType,
            @Nonnull Collection<ItemTagDto> itemTags) {

        if (itemTags.isEmpty()) {
            return List.of();
        }

        // sanity check for existing EHR version
        ehrService.checkEhrExists(ownerId);

        // validate and collect errors
        itemTags.forEach(dto -> fillAndValidateDto(dto, ownerId, targetId, targetType));

        return itemTagRepository.bulkStore(itemTags);
    }

    @Override
    public Collection<ItemTagDto> findItemTag(
            @Nonnull UUID ownerId,
            @Nonnull UUID targetVoId,
            @Nonnull ItemTagRMType targetType,
            @Nonnull Collection<UUID> ids,
            @Nonnull Collection<String> keys) {

        // sanity check for existing EHR version
        ehrService.checkEhrExists(ownerId);

        return itemTagRepository.findForLatestTargetVersion(ownerId, targetVoId, targetType, ids, keys).stream()
                .map(itemTag -> itemTag)
                .toList();
    }

    @Override
    public void bulkDelete(
            @Nonnull UUID ownerId,
            @Nonnull UUID targetVoId,
            @Nonnull ItemTagRMType targetType,
            @Nonnull Collection<UUID> ids) {

        if (ids.isEmpty()) {
            return;
        }

        // sanity check for existing EHR version
        ehrService.checkEhrExists(ownerId);

        itemTagRepository.bulkDelete(ownerId, targetVoId, targetType, ids);
    }

    private static void fillAndValidateDto(ItemTagDto dto, UUID ownerId, UUID targetVoId, ItemTagRMType targetType) {

        final String key = dto.getKey();
        final String value = dto.getValue();
        final String targetPath = dto.getTargetPath();

        // Changing the owner is not supported - we keep the EHR for the tag
        if (dto.getOwnerId() != null && ObjectUtils.notEqual(dto.getOwnerId(), ownerId)) {
            throw new UnprocessableEntityException(
                    "Owner mismatch for ItemTag '%s': %s vs. %s".formatted(key, dto.getOwnerId(), ownerId));
        }

        // tag validation
        validateTagKey(key);
        validateTagValue(key, value);
        validateTargetPath(key, targetPath);
        validateTargetType(dto, targetType);

        dto.setOwnerId(ownerId);
        dto.setTarget(targetVoId);
        dto.setTargetType(targetType);
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
        if (StringUtils.isBlank(key)) {
            throw new UnprocessableEntityException("ItemTag must have a key that must not be blank");
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
        if (StringUtils.isBlank(value) && value != null) {
            throw new UnprocessableEntityException("ItemTag '%s' value must not be blank".formatted(key));
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
    static void validateTargetPath(String key, String targetPath) {

        if (targetPath == null) {
            return;
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

        AqlObjectPath path = AqlObjectPath.parse(targetPath);
        path.getPathNodes().forEach(node -> {
            if (node.getPredicateOrOperands().size() > 1) {
                throw new UnprocessableEntityException(
                        "ItemTag '%s' target_path '%s': OR predicates are not supported".formatted(key, targetPath));
            }
            node.getPredicateOrOperands().stream()
                    .map(AndOperatorPredicate::getOperands)
                    .flatMap(Collection::stream)
                    .map(ComparisonOperatorPredicate::getPath)
                    .filter(p ->
                            !AqlObjectPathUtil.ARCHETYPE_NODE_ID.equals(p) && !AqlObjectPathUtil.NAME_VALUE.equals(p))
                    .findFirst()
                    .ifPresent(__ -> {
                        throw new UnprocessableEntityException(
                                "ItemTag '%s' target_path '%s': only predicates on archetype_node_id and name/value are supported"
                                        .formatted(key, targetPath));
                    });
        });
    }

    private static void validateTargetType(ItemTagDto itemTag, ItemTagRMType targetType) {
        ItemTagRMType tagType = itemTag.getTargetType();
        if (tagType != null && !Objects.equals(tagType, targetType)) {
            throw new ValidationException("target_type does not match %s".formatted(targetType.name()));
        }
        ;
    }
}
