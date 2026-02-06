/*
 * Copyright (c) 2026 vitasystems GmbH.
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
package org.ehrbase.api.util;

import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.support.identification.ObjectId;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.UID;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import com.nedap.archie.rm.support.identification.VersionTreeId;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NonNull;

public final class LocatableUtils {
    private LocatableUtils() {}

    public static String getTemplateId(Locatable archetyped) {
        return Optional.ofNullable(archetyped)
                .map(Locatable::getArchetypeDetails)
                .map(Archetyped::getTemplateId)
                .map(ObjectId::getValue)
                .orElse(null);
    }

    public static @NonNull Optional<String> getUidRootString(Locatable root) {
        return Optional.ofNullable(root)
                .map(Locatable::getUid)
                .map(UIDBasedId::getRoot)
                .map(UID::getValue);
    }

    public static @NonNull Optional<String> getUidRootString(UIDBasedId uid) {
        return Optional.ofNullable(uid).map(UIDBasedId::getRoot).map(UID::getValue);
    }

    public static UUID getUuid(UIDBasedId uid) {
        return getUidRootString(uid).map(UUID::fromString).orElse(null);
    }

    public static UUID getUuid(Locatable archetyped) {
        return getUidRootString(archetyped).map(UUID::fromString).orElse(null);
    }

    public static int getUidVersion(Locatable root) {
        return getUidVersion(root.getUid());
    }

    public static int getUidVersion(UIDBasedId uid) {
        return Integer.parseInt(Optional.of(uid)
                .filter(ObjectVersionId.class::isInstance)
                .map(ObjectVersionId.class::cast)
                .map(ObjectVersionId::getVersionTreeId)
                .map(VersionTreeId::getValue)
                .orElseThrow());
    }
}
