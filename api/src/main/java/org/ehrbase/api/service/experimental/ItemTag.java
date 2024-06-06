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

import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.ehrbase.openehr.sdk.aql.webtemplatepath.AqlPath;

// TODO needs to be part of the rm spec - archie does not contain ItemTag for now - possible move to OpenEHR_SDK
/**
 * @param id          Identifier of the tag
 * @param ownerId     Identifier of owner object, such as EHR.
 * @param target      Identifier of target, which may be a VERSIONED_OBJECT&lt;T&gt; or a VERSION&lt;T&gt;.
 * @param targetType  RM type of the tag (EHR_STATUS, COMPOSITION)
 * @param targetPath  Optional archetype (i.e. AQL) or RM path within target, used to tag a fine-grained element.
 * @param key         The tag key. May not be empty or contain leading or trailing whitespace.
 * @param value       The value. If set, may not be empty.
 */
public record ItemTag(
        @Nullable UUID id,
        @Nonnull UUID ownerId,
        @Nonnull UUID target,
        @Nonnull ItemTagRMType targetType,
        @Nullable AqlPath targetPath,
        @Nonnull String key,
        @Nullable String value) {

    public enum ItemTagRMType {
        EHR_STATUS,
        COMPOSITION
    }
}
