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
package org.ehrbase.api.service.experimental;

import java.util.Collection;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.ehrbase.api.dto.experimental.ItemTagDto;
import org.ehrbase.api.dto.experimental.ItemTagDto.ItemTagRMType;

/**
 * Service manages the <a href="https://specifications.openehr.org/releases/RM/latest/common.html#tags">ITEM_TAG</a>
 * <code>Class</code>.
 * </br>
 * All operations on tags are implemented as bulk operations.
 */
public interface ItemTagService {

    /**
     * Performs a <code>bulk</code> update/create operation for the given <code>ItemTag</code> into the tag list
     * of the <code>owner</code>.
     *
     * @param ownerId       Identifier of owner object, such as EHR.
     * @param targetId      VERSIONED_OBJECT&lt;T&gt; Identifier of target.
     * @param targetType    Type of the target object.
     * @param itemTagsDto   Content of the <code>ItemTag</code> containing the <code>key, value</code> parameter.
     * @return tagUUIDs     of the update/create <code>ItemTag</code>s
     */
    Collection<UUID> bulkUpsert(
            @Nonnull UUID ownerId,
            @Nonnull UUID targetId,
            @Nonnull ItemTagRMType targetType,
            @Nonnull Collection<ItemTagDto> itemTagsDto);

    /**
     * Performs a <code>bulk</code> get operation for the given <code>ItemTag</code> <code>IDs</code> and/or
     * <code>keys</code>.
     *
     * @param ownerId       Identifier of owner object, such as EHR.
     * @param targetId      VERSIONED_OBJECT&lt;T&gt; Identifier of target.
     * @param targetType    Type of the target object.
     * @param ids           Identifier <code>ItemTag</code> to search for.
     * @param keys          <code>ItemTag</code> keys to search for.
     * @return tags         Matching the <code>ownerId, targetId</code> and optional <code>ids, keys</code>
     */
    Collection<ItemTagDto> findItemTag(
            @Nonnull UUID ownerId,
            @Nonnull UUID targetId,
            @Nonnull ItemTagRMType targetType,
            @Nonnull Collection<UUID> ids,
            @Nonnull Collection<String> keys);

    /**
     * Performs a <code>bulk</code> delete operation for the given <code>ItemTag</code> <code>ids</code>. This method
     * will simply return in cases where the given IDs does not exist.
     *
     * @param ownerId       Identifier of owner object, such as EHR.
     * @param targetId      VERSIONED_OBJECT&lt;T&gt; Identifier of target.
     * @param targetType    Type of the target object.
     * @param ids           Identifier <code>ItemTag</code> to delete.
     */
    void bulkDelete(
            @Nonnull UUID ownerId,
            @Nonnull UUID targetId,
            @Nonnull ItemTagRMType targetType,
            @Nonnull Collection<UUID> ids);
}
