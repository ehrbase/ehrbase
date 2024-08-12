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
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ItemTagDto {
    private UUID id;
    private UUID ownerId;
    private UUID target;
    private ItemTagRMType targetType;
    private String targetPath;
    private String key;
    private String value;

    /**
     * @param id          Identifier of the tag
     * @param ownerId     Identifier of owner object, such as EHR.
     * @param target      Identifier of target, which may be a VERSIONED_OBJECT&lt;T&gt; or a VERSION&lt;T&gt;.
     * @param targetType  RM type of the tag
     * @param targetPath  Optional archetype (i.e. AQL) or RM path within target, used to tag a fine-grained element.
     * @param key         The tag key. May not be empty or contain leading or trailing whitespace.
     * @param value       The value. If set, may not be empty.
     */
    public ItemTagDto(
            UUID id,
            UUID ownerId, // obtained by path for PUT
            UUID target, // obtained by path for PUT
            ItemTagRMType targetType, // obtained by path for PUT
            String targetPath,
            String key,
            String value) {
        this.id = id;
        this.ownerId = ownerId;
        this.target = target;
        this.targetType = targetType;
        this.targetPath = targetPath;
        this.key = key;
        this.value = value;
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public void setId(@Nullable final UUID id) {
        this.id = id;
    }

    @JsonProperty(value = "owner_id")
    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(@Nullable final UUID ownerId) {
        this.ownerId = ownerId;
    }

    @Nullable
    public UUID getTarget() {
        return target;
    }

    public void setTarget(@Nullable final UUID target) {
        this.target = target;
    }

    @JsonProperty(value = "target_type")
    @Nullable
    public ItemTagRMType getTargetType() {
        return targetType;
    }

    public void setTargetType(@Nullable final ItemTagRMType targetType) {
        this.targetType = targetType;
    }

    @JsonProperty(value = "target_path")
    @Nullable
    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(@Nullable final String targetPath) {
        this.targetPath = targetPath;
    }

    @Nonnull
    public String getKey() {
        return key;
    }

    public void setKey(@Nonnull final String key) {
        this.key = key;
    }

    @Nullable
    public String getValue() {
        return value;
    }

    public void setValue(@Nullable final String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ItemTagDto) obj;
        return Objects.equals(this.id, that.id)
                && Objects.equals(this.ownerId, that.ownerId)
                && Objects.equals(this.target, that.target)
                && Objects.equals(this.targetType, that.targetType)
                && Objects.equals(this.targetPath, that.targetPath)
                && Objects.equals(this.key, that.key)
                && Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerId, target, targetType, targetPath, key, value);
    }

    @Override
    public String toString() {
        return "ItemTagDto[" + "id="
                + id + ", " + "ownerId="
                + ownerId + ", " + "target="
                + target + ", " + "targetType="
                + targetType + ", " + "targetPath="
                + targetPath + ", " + "key="
                + key + ", " + "value="
                + value + ']';
    }

    public enum ItemTagRMType {
        EHR_STATUS,
        COMPOSITION
    }
}
