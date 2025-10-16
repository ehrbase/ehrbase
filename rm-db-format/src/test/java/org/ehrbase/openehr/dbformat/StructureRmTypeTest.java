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
package org.ehrbase.openehr.dbformat;

import static org.assertj.core.api.Assertions.assertThat;

import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.demographic.Actor;
import com.nedap.archie.rm.ehr.EhrAccess;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import com.nedap.archie.rminfo.RMTypeInfo;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StructureRmTypeTest {

    @ParameterizedTest
    @EnumSource(StructureRmType.class)
    void byTypeName(StructureRmType type) {
        assertThat(StructureRmType.byTypeName(type.name())).contains(type);
    }

    /**
     * check hierarchy against RM model
     */
    @ParameterizedTest
    @EnumSource(StructureRmType.class)
    void rmHierarchy(StructureRmType type) {

        ArchieRMInfoLookup rmInfos = ArchieRMInfoLookup.getInstance();

        RMTypeInfo typeInfo = rmInfos.getTypeInfo(type.type);
        Set<RMTypeInfo> ownTypeInfos = typeInfo.getAllParentClasses();
        ownTypeInfos.add(typeInfo);

        // find all attributes referencing this type or an ancestor
        // and compile set of concrete owner classes
        Set<RMTypeInfo> referencingTypes = rmInfos.getAllTypes().stream()
                .filter(t -> Actor.class.getPackage() != t.getJavaClass().getPackage())
                .filter(t -> t.getJavaClass() != EhrAccess.class)
                .filter(t -> t.getAttributes().values().stream()
                        .filter(att -> !att.isComputed())
                        .anyMatch(att -> ownTypeInfos.contains(rmInfos.getTypeInfo(att.getTypeInCollection()))))
                .collect(Collectors.toSet());
        referencingTypes.removeIf(t -> Modifier.isAbstract(t.getJavaClass().getModifiers()));

        Set<StructureRmType> referencingStructureRmTypes = referencingTypes.stream()
                .map(t -> StructureRmType.byTypeName(t.getRmName())
                        .orElseThrow(() -> new IllegalArgumentException(t.getRmName())))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(StructureRmType.class)));

        // make sure parents is correct
        assertThat(type.getParents()).isEqualTo(referencingStructureRmTypes);
    }

    @ParameterizedTest
    @EnumSource(StructureRmType.class)
    void structureEntry(StructureRmType type) {
        switch (type) {
            case EVENT_CONTEXT, FEEDER_AUDIT ->
                assertThat(type.isStructureEntry()).isTrue();
            default -> assertThat(type.isStructureEntry()).isEqualTo(Locatable.class.isAssignableFrom(type.type));
        }
    }

    @Test
    void distinguishing() {

        Map<StructureRmType, Set<StructureRmType>> reachableFrom = new EnumMap<>(StructureRmType.class);

        // add parents
        Arrays.stream(StructureRmType.values()).forEach(s -> {
            Set<StructureRmType> parents = EnumSet.of(s);
            parents.addAll(s.getParents());
            reachableFrom.put(s, parents);
        });

        Set<StructureRmType> todo = EnumSet.allOf(StructureRmType.class);

        while (!todo.isEmpty()) {
            var it = todo.iterator();
            StructureRmType s = it.next();
            it.remove();
            Set<StructureRmType> predecessors = reachableFrom.get(s);

            var newPredecessors = predecessors.stream()
                    .map(reachableFrom::get)
                    .flatMap(Set::stream)
                    .distinct()
                    .filter(p -> !predecessors.contains(p))
                    .toList();

            if (!newPredecessors.isEmpty()) {
                predecessors.addAll(newPredecessors);
                // set children dirty
                reachableFrom.entrySet().stream()
                        .filter(e -> e.getKey() != s)
                        .filter(e -> e.getValue().contains(s))
                        .forEach(e -> todo.add(e.getKey()));
            }
        }

        Set<StructureRmType> roots =
                EnumSet.of(StructureRmType.COMPOSITION, StructureRmType.EHR_STATUS, StructureRmType.FOLDER);

        Arrays.stream(StructureRmType.values()).forEach(s ->
        // distinguishing means it os only reachable from one root
        {
            Set<StructureRmType> belongsToRoots = SetUtils.intersection(reachableFrom.get(s), roots);
            assertThat(s.isDistinguishing())
                    .as("%s(distinguishing(=%s) is reachable from %s", s, s.isDistinguishing(), belongsToRoots)
                    .isEqualTo(belongsToRoots.size() == 1);
        });
    }
}
