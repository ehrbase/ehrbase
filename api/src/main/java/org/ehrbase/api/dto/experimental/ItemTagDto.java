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
package org.ehrbase.api.dto.experimental;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.ehrbase.api.service.experimental.ItemTag;

/**
 * ItemTag DTO used as a PUT or DELETE input as well as output for GET.
 *
 * @param id          Identifier of the tag
 * @param ownerId     Identifier of owner object, such as EHR.
 * @param target      Identifier of target, which may be a VERSIONED_OBJECT&lt;T&gt; or a VERSION&lt;T&gt;.
 * @param targetType  RM type of the tag
 * @param targetPath  Optional archetype (i.e. AQL) or RM path within target, used to tag a fine-grained element.
 * @param key         The tag key. May not be empty or contain leading or trailing whitespace.
 * @param value       The value. If set, may not be empty.
 */
@JsonRootName("ITEM_TAG")
public record ItemTagDto(
        @JsonProperty(value = "id") @Nullable UUID id,
        @JsonProperty(value = "owner_id") @Nullable UUID ownerId, // obtained by path for PUT
        @JsonProperty(value = "target") @Nullable UUID target, // obtained by path for PUT
        @JsonProperty(value = "target_type") @Nullable ItemTag.ItemTagRMType targetType, // obtained by path for PUT
        @JsonProperty(value = "target_path") @Nullable String targetPath,
        @JsonProperty(value = "key") @Nonnull String key,
        @JsonProperty(value = "value") @Nullable String value) {}
