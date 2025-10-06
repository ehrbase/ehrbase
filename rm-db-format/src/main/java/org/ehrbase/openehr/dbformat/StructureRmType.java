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
import com.nedap.archie.rm.archetyped.FeederAudit;
import com.nedap.archie.rm.archetyped.FeederAuditDetails;
import com.nedap.archie.rm.composition.Action;
import com.nedap.archie.rm.composition.Activity;
import com.nedap.archie.rm.composition.AdminEntry;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.composition.Evaluation;
import com.nedap.archie.rm.composition.EventContext;
import com.nedap.archie.rm.composition.Instruction;
import com.nedap.archie.rm.composition.InstructionDetails;
import com.nedap.archie.rm.composition.Observation;
import com.nedap.archie.rm.composition.Section;
import com.nedap.archie.rm.datastructures.Cluster;
import com.nedap.archie.rm.datastructures.Element;
import com.nedap.archie.rm.datastructures.History;
import com.nedap.archie.rm.datastructures.IntervalEvent;
import com.nedap.archie.rm.datastructures.ItemList;
import com.nedap.archie.rm.datastructures.ItemSingle;
import com.nedap.archie.rm.datastructures.ItemTable;
import com.nedap.archie.rm.datastructures.ItemTree;
import com.nedap.archie.rm.datastructures.PointEvent;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.integration.GenericEntry;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Describes the RM types structuring the Versioned Object
 */
public enum StructureRmType {
    COMPOSITION("CO", StructureRoot.COMPOSITION, Composition.class, false), // , "content", "context"),
    FOLDER("F", StructureRoot.FOLDER, Folder.class, true),
    EHR_STATUS("ES", StructureRoot.EHR_STATUS, EhrStatus.class, false),

    EVENT_CONTEXT("EC", StructureRoot.COMPOSITION, EventContext.class, true, false, COMPOSITION), // , "otherContext"),

    SECTION("SE", StructureRoot.COMPOSITION, Section.class, true, true, COMPOSITION), // , "items"),
    GENERIC_ENTRY("GE", StructureRoot.COMPOSITION, GenericEntry.class, true, false, COMPOSITION, SECTION), // ,"data"),
    ADMIN_ENTRY("AE", StructureRoot.COMPOSITION, AdminEntry.class, true, false, COMPOSITION, SECTION), // ,"data"),
    OBSERVATION(
            "OB",
            StructureRoot.COMPOSITION,
            Observation.class,
            true,
            false,
            COMPOSITION,
            SECTION), // ,"data", "state", "protocol"),
    INSTRUCTION(
            "IN",
            StructureRoot.COMPOSITION,
            Instruction.class,
            true,
            false,
            COMPOSITION,
            SECTION), // ,"activities", "protocol"),
    ACTION(
            "AN",
            StructureRoot.COMPOSITION,
            Action.class,
            true,
            false,
            COMPOSITION,
            SECTION), // ,"description", "instructionDetails", "protocol"),
    EVALUATION(
            "EV",
            StructureRoot.COMPOSITION,
            Evaluation.class,
            true,
            false,
            COMPOSITION,
            SECTION), // ,"data", "protocol"),

    INSTRUCTION_DETAILS(
            "ID", StructureRoot.COMPOSITION, InstructionDetails.class, false, false, ACTION), // , "wf_details"),
    ACTIVITY("AY", StructureRoot.COMPOSITION, Activity.class, true, false, INSTRUCTION), // , "description"),

    HISTORY("HI", StructureRoot.COMPOSITION, History.class, true, false, OBSERVATION), // , "events", "summary"),
    POINT_EVENT("PE", StructureRoot.COMPOSITION, PointEvent.class, true, false, HISTORY), // , "data", "state"),
    INTERVAL_EVENT("IE", StructureRoot.COMPOSITION, IntervalEvent.class, true, false, HISTORY), // , "data", "state"),

    FEEDER_AUDIT(
            "FA",
            null,
            FeederAudit.class,
            true,
            false,
            COMPOSITION,
            FOLDER,
            EHR_STATUS,
            SECTION,
            GENERIC_ENTRY,
            ADMIN_ENTRY,
            OBSERVATION,
            INSTRUCTION,
            ACTION,
            EVALUATION,
            ACTIVITY,
            HISTORY,
            POINT_EVENT,
            INTERVAL_EVENT), // Locatable.feederAudit
    FEEDER_AUDIT_DETAILS("FD", null, FeederAuditDetails.class, false, false, FEEDER_AUDIT),

    ITEM_LIST(
            "IL",
            null,
            ItemList.class,
            true,
            false,
            FOLDER,
            EHR_STATUS,
            FEEDER_AUDIT_DETAILS,
            EVENT_CONTEXT,
            ADMIN_ENTRY,
            OBSERVATION,
            INSTRUCTION,
            ACTION,
            EVALUATION,
            INSTRUCTION_DETAILS,
            ACTIVITY,
            HISTORY,
            POINT_EVENT,
            INTERVAL_EVENT), // , "items"),

    ITEM_SINGLE(
            "IS",
            null,
            ItemSingle.class,
            true,
            false,
            FOLDER,
            EHR_STATUS,
            FEEDER_AUDIT_DETAILS,
            EVENT_CONTEXT,
            ADMIN_ENTRY,
            OBSERVATION,
            INSTRUCTION,
            ACTION,
            EVALUATION,
            INSTRUCTION_DETAILS,
            ACTIVITY,
            HISTORY,
            POINT_EVENT,
            INTERVAL_EVENT), // , "item"),
    ITEM_TABLE(
            "TA",
            null,
            ItemTable.class,
            true,
            false,
            FOLDER,
            EHR_STATUS,
            FEEDER_AUDIT_DETAILS,
            EVENT_CONTEXT,
            ADMIN_ENTRY,
            OBSERVATION,
            INSTRUCTION,
            ACTION,
            EVALUATION,
            INSTRUCTION_DETAILS,
            ACTIVITY,
            HISTORY,
            POINT_EVENT,
            INTERVAL_EVENT), // , "rows"),
    ITEM_TREE(
            "TR",
            null,
            ItemTree.class,
            true,
            false,
            FOLDER,
            EHR_STATUS,
            FEEDER_AUDIT_DETAILS,
            EVENT_CONTEXT,
            GENERIC_ENTRY,
            ADMIN_ENTRY,
            OBSERVATION,
            INSTRUCTION,
            ACTION,
            EVALUATION,
            INSTRUCTION_DETAILS,
            ACTIVITY,
            HISTORY,
            POINT_EVENT,
            INTERVAL_EVENT), // , "items"),
    CLUSTER("CL", null, Cluster.class, true, true, ITEM_TABLE, ITEM_TREE), // , "items"),
    ELEMENT("E", null, Element.class, true, false, ITEM_LIST, ITEM_SINGLE, ITEM_TREE, CLUSTER); // , "items", "item");

    private final String alias;
    public final Class<? extends RMObject> type;

    private final boolean structureEntry;

    /**
     * If it is the case, the only queryable versioned object type (COMPOSITION, EHR_STATUS, FOLDER) that can contain this structure.
     * This means that in AQL CONTAINS it can stand for itself and does not need to appear as a sub-contains of a different explicit type.
     */
    private final StructureRoot structureRoot;

    private final boolean isStructureRoot;

    /**
     * Types that can contain entries of this type
     */
    private final Set<StructureRmType> parentModifiable;

    /**
     * Types that can contain entries of this type
     */
    private final Set<StructureRmType> parents;

    private static final Map<Class<? extends RMObject>, StructureRmType> BY_TYPE =
            Arrays.stream(values()).collect(Collectors.toMap(v -> v.type, v -> v));
    private static final Map<String, StructureRmType> BY_TYPE_NAME = Arrays.stream(values())
            .collect(Collectors.toMap(
                    v -> ArchieRMInfoLookup.getInstance().getTypeInfo(v.type).getRmName(), v -> v));

    static {
        // post-op: add circular references
        FEEDER_AUDIT.parentModifiable.addAll(Set.of(ITEM_LIST, ITEM_SINGLE, ITEM_TABLE, ITEM_TREE, CLUSTER, ELEMENT));
    }

    private static final Set<StructureRmType> STRUCTURE_LEAFS;

    static {
        STRUCTURE_LEAFS = EnumSet.allOf(StructureRmType.class);
        // XXX handle non-structure nodes
        Arrays.stream(values()).forEach(s -> STRUCTURE_LEAFS.removeAll(s.parents));
    }

    StructureRmType(String alias, StructureRoot structureRoot, Class<? extends RMObject> type, boolean ownParent) {
        if (!structureRoot.name().equals(this.name())) {
            throw new IllegalArgumentException("The constructor is only intended for root structures");
        }
        this.alias = alias;
        this.type = type;
        this.structureEntry = true;
        this.parentModifiable = null;
        if (ownParent) {
            this.parents = Set.of(this);
        } else {
            this.parents = Set.of();
        }
        this.structureRoot = structureRoot;
        this.isStructureRoot = true;
    }

    StructureRmType(
            String alias,
            StructureRoot structureRoot,
            Class<? extends RMObject> type,
            boolean structureEntry,
            boolean ownParent,
            StructureRmType... parents) {
        this.alias = alias;
        this.type = type;
        this.structureEntry = structureEntry;
        if (type == FeederAudit.class) {
            // prepare for modification in static initializer
            this.parentModifiable = new LinkedHashSet<>(Arrays.asList(parents));
            this.parents = Collections.unmodifiableSet(parentModifiable);
        } else if (ownParent) {
            this.parentModifiable = null;
            this.parents = Set.of(ArrayUtils.add(parents, this));
        } else {
            this.parentModifiable = null;
            this.parents = Set.of(parents);
        }
        this.structureRoot = structureRoot;
        this.isStructureRoot = false;
    }

    public static String getAliasOrTypeName(String typeName) {
        return StructureRmType.byTypeName(typeName)
                .map(StructureRmType::getAlias)
                .orElse(typeName);
    }

    public boolean isDistinguishing() {
        return structureRoot != null;
    }

    public StructureRoot getStructureRoot() {
        return structureRoot;
    }

    public boolean isStructureRoot() {
        return isStructureRoot;
    }

    public String getAlias() {
        return alias;
    }

    public static Optional<StructureRmType> byType(Class<? extends RMObject> rmType) {
        return Optional.ofNullable(rmType).map(BY_TYPE::get);
    }

    public static Optional<StructureRmType> byTypeName(String rmTypeName) {
        return Optional.ofNullable(switch (rmTypeName) {
            case "COMPOSITION" -> COMPOSITION;
            case "FOLDER" -> FOLDER;
            case "EHR_STATUS" -> EHR_STATUS;
            case "EVENT_CONTEXT" -> EVENT_CONTEXT;
            case "SECTION" -> SECTION;
            case "GENERIC_ENTRY" -> GENERIC_ENTRY;
            case "ADMIN_ENTRY" -> ADMIN_ENTRY;
            case "OBSERVATION" -> OBSERVATION;
            case "INSTRUCTION" -> INSTRUCTION;
            case "ACTION" -> ACTION;
            case "EVALUATION" -> EVALUATION;
            case "INSTRUCTION_DETAILS" -> INSTRUCTION_DETAILS;
            case "ACTIVITY" -> ACTIVITY;
            case "HISTORY" -> HISTORY;
            case "POINT_EVENT" -> POINT_EVENT;
            case "INTERVAL_EVENT" -> INTERVAL_EVENT;
            case "FEEDER_AUDIT" -> FEEDER_AUDIT;
            case "FEEDER_AUDIT_DETAILS" -> FEEDER_AUDIT_DETAILS;
            case "ITEM_LIST" -> ITEM_LIST;
            case "ITEM_SINGLE" -> ITEM_SINGLE;
            case "ITEM_TABLE" -> ITEM_TABLE;
            case "ITEM_TREE" -> ITEM_TREE;
            case "CLUSTER" -> CLUSTER;
            case "ELEMENT" -> ELEMENT;
            default -> null;
        });
    }

    public Set<StructureRmType> getParents() {
        return parents;
    }

    public boolean isStructureEntry() {
        return structureEntry;
    }
}
