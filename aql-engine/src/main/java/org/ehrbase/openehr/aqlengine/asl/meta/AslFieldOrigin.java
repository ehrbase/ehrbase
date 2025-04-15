/*
 * Copyright (c) 2019-2025 vitasystems GmbH.
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
package org.ehrbase.openehr.aqlengine.asl.meta;

import javax.annotation.Nonnull;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;

/**
 * Contains backtracking information for the original AQL query field.
 *
 * @param path of the original AQL query select or where clause where this field originates from. Can be used for
 *             tracking field usage, post-processing and/or debugging purposes.
 */
public record AslFieldOrigin(@Nonnull IdentifiedPath path) {

    public static AslFieldOrigin of(IdentifiedPath path) {
        return new AslFieldOrigin(path);
    }

    @Override
    public String toString() {
        return "AslFieldOrigin[" + path.render() + ']';
    }
}
