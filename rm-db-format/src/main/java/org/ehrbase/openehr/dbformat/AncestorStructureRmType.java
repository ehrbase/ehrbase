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

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.composition.CareEntry;
import com.nedap.archie.rm.composition.ContentItem;
import com.nedap.archie.rm.composition.Entry;
import com.nedap.archie.rm.datastructures.Event;
import com.nedap.archie.rm.datastructures.Item;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import com.nedap.archie.rminfo.RMTypeInfo;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Describes relevant abstract RM structure types
 */
public enum AncestorStructureRmType {
    CONTENT_ITEM(ContentItem.class),
    ENTRY(Entry.class),
    CARE_ENTRY(CareEntry.class),
    EVENT(Event.class),
    ITEM_STRUCTURE(ItemStructure.class),
    ITEM(Item.class),
    ;

    private final Class<? extends RMObject> type;

    /**
     * If none of the other queryable versioned objects (COMPOSITION, EHR_STATUS, FOLDER) can also contain this structure.
     * This means that in AQL CONTAINS it can stand for itself and does not need to appear as a sub-contains of a different explicit type.
     */
    private final StructureRoot structureRoot;

    /**
     * Types that can contain entries of this type
     */
    private final Set<Class<? extends RMObject>> nonStructureDescendants;

    /**
     * Types that can contain entries of this type
     */
    private final Set<StructureRmType> descendants;

    private static final Map<Class<? extends RMObject>, AncestorStructureRmType> byType =
            Arrays.stream(values()).collect(Collectors.toMap(v -> v.type, v -> v));
    private static final Map<String, AncestorStructureRmType> byTypeName = Arrays.stream(values())
            .collect(Collectors.toMap(
                    v -> ArchieRMInfoLookup.getInstance().getTypeInfo(v.type).getRmName(), v -> v));

    AncestorStructureRmType(Class<? extends RMObject> type) {
        this.type = type;
        Set<Class<? extends RMObject>> nonAbstractDescendants = nonAbstractDescendants(type);

        Stream.Builder<StructureRmType> descendantStream = Stream.builder();

        nonAbstractDescendants.removeIf(c -> {
            StructureRmType.byType(c).ifPresent(descendantStream::add);
            return StructureRmType.byType(c).isPresent();
        });

        this.nonStructureDescendants =
                unmodifiableOrderedSet(nonAbstractDescendants.stream(), Comparator.comparing(Class::getCanonicalName));
        this.descendants =
                unmodifiableOrderedSet(descendantStream.build(), Comparator.comparing(StructureRmType::name));
        List<StructureRoot> structureRoots = descendants.stream()
                .map(StructureRmType::getStructureRoot)
                .distinct()
                .toList();
        this.structureRoot = structureRoots.size() == 1 ? structureRoots.getFirst() : null;
    }

    private static <T> Set<T> unmodifiableOrderedSet(Stream<T> values, Comparator<T> comparator) {
        Set<T> collect = values.sorted(comparator).collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(collect);
    }

    public StructureRoot getStructureRoot() {
        return structureRoot;
    }

    private static Set<Class<? extends RMObject>> nonAbstractDescendants(Class<? extends RMObject> type) {
        return ArchieRMInfoLookup.getInstance().getTypeInfo(type).getAllDescendantClasses().stream()
                .map(RMTypeInfo::getJavaClass)
                .filter(t -> !Modifier.isAbstract(t.getModifiers()))
                .map(c -> (Class<? extends RMObject>) c)
                .collect(Collectors.toSet());
    }

    public static Optional<AncestorStructureRmType> byType(Class<? extends RMObject> rmType) {
        return Optional.ofNullable(rmType).map(byType::get);
    }

    public static Optional<AncestorStructureRmType> byTypeName(String rmTypeName) {
        return Optional.ofNullable(rmTypeName).map(byTypeName::get);
    }

    public Set<StructureRmType> getDescendants() {
        return descendants;
    }

    public Set<Class<? extends RMObject>> getNonStructureDescendants() {
        return nonStructureDescendants;
    }
}
