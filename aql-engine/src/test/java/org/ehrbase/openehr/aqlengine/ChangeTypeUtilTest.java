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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.service.ContributionService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.junit.jupiter.api.Test;

class ChangeTypeUtilTest {

    @Test
    void ensureJooqChangeTypeToCodeMappingsMatch() {
        Arrays.stream(ContributionChangeType.values())
                .map(jct -> Pair.of(
                        Integer.toString(ContributionService.ContributionChangeType.valueOf(
                                        jct.getLiteral().toUpperCase())
                                .getCode()),
                        jct))
                .forEach(p -> {
                    assertEquals(p.getLeft(), ChangeTypeUtils.getCodeByJooqChangeType(p.getRight()));
                    assertEquals(p.getRight(), ChangeTypeUtils.getJooqChangeTypeByCode(p.getLeft()));
                });
    }
}
