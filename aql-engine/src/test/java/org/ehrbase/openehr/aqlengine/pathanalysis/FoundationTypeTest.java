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
package org.ehrbase.openehr.aqlengine.pathanalysis;

import static org.assertj.core.api.Assertions.assertThat;

import com.nedap.archie.rminfo.RMTypeInfo;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FoundationTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {"STRING", "LONG", "TEMPORAL"})
    void hasFoundationType(String name) {
        assertThat(FoundationType.valueOf(name)).isNotNull();
    }

    @Test
    void foundationTypesComplete() {

        // make sure that FoundationType contains all needed for Compositions

        Queue<RMTypeInfo> remainingTypes = new LinkedList<>();
        remainingTypes.add(PathAnalysis.RM_INFOS.getTypeInfo(RmConstants.COMPOSITION));

        Set<RMTypeInfo> seen = new HashSet<>();
        seen.add(remainingTypes.peek());

        Set<String> typeNames = new HashSet<>();

        while (!remainingTypes.isEmpty()) {
            RMTypeInfo typeInfo = remainingTypes.poll();

            typeInfo.getDirectDescendantClasses().stream().filter(seen::add).forEach(remainingTypes::add);

            if (!Modifier.isAbstract(typeInfo.getJavaClass().getModifiers())) {
                typeInfo.getAttributes().values().stream()
                        .filter(ti -> !ti.isComputed())
                        .map(ai -> {
                            String typeName = ai.getTypeNameInCollection();

                            RMTypeInfo ti = PathAnalysis.RM_INFOS.getTypeInfo(typeName);
                            if (ti == null) {
                                typeNames.add(typeName);
                            }

                            return typeName;
                        })
                        .map(PathAnalysis.RM_INFOS::getTypeInfo)
                        .filter(Objects::nonNull)
                        .filter(seen::add)
                        .forEach(remainingTypes::add);
            }
        }

        assertThat(Arrays.stream(FoundationType.values()).map(Enum::name))
                .containsExactlyInAnyOrderElementsOf(typeNames);
    }
}
