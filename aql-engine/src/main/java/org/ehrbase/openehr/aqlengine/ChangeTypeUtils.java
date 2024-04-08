/*
 * Copyright (c) 2024 vitasystems GmbH.
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
package org.ehrbase.openehr.aqlengine;

import java.util.Map;
import java.util.Optional;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.bidimap.UnmodifiableBidiMap;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;

public final class ChangeTypeUtils {
    public static final BidiMap<ContributionChangeType, String> JOOQ_CHANGE_TYPE_TO_CODE =
            UnmodifiableBidiMap.unmodifiableBidiMap(new DualHashBidiMap<>(Map.of(
                    ContributionChangeType.creation, "249",
                    ContributionChangeType.amendment, "250",
                    ContributionChangeType.modification, "251",
                    ContributionChangeType.synthesis, "252",
                    ContributionChangeType.Unknown, "253",
                    ContributionChangeType.deleted, "523")));

    private ChangeTypeUtils() {}

    public static ContributionChangeType getJooqChangeTypeByCode(String code) {
        return Optional.ofNullable(code).map(JOOQ_CHANGE_TYPE_TO_CODE::getKey).orElse(null);
    }

    public static String getCodeByJooqChangeType(ContributionChangeType cct) {
        return Optional.ofNullable(cct).map(JOOQ_CHANGE_TYPE_TO_CODE::get).orElse(null);
    }
}
