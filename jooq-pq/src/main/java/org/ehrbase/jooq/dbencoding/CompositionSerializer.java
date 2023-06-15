/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.jooq.dbencoding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.composition.Action;
import com.nedap.archie.rm.composition.Activity;
import com.nedap.archie.rm.composition.AdminEntry;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.composition.ContentItem;
import com.nedap.archie.rm.composition.Evaluation;
import com.nedap.archie.rm.composition.Instruction;
import com.nedap.archie.rm.composition.Observation;
import com.nedap.archie.rm.composition.Section;
import com.nedap.archie.rm.datastructures.Cluster;
import com.nedap.archie.rm.datastructures.Element;
import com.nedap.archie.rm.datastructures.Event;
import com.nedap.archie.rm.datastructures.History;
import com.nedap.archie.rm.datastructures.Item;
import com.nedap.archie.rm.datastructures.ItemList;
import com.nedap.archie.rm.datastructures.ItemSingle;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datastructures.ItemTable;
import com.nedap.archie.rm.datastructures.ItemTree;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.integration.GenericEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.map.MultiValueMap;
import org.ehrbase.jooq.dbencoding.attributes.ActionAttributes;
import org.ehrbase.jooq.dbencoding.attributes.ActivityAttributes;
import org.ehrbase.jooq.dbencoding.attributes.AdminEntryAttributes;
import org.ehrbase.jooq.dbencoding.attributes.ClusterAttributes;
import org.ehrbase.jooq.dbencoding.attributes.ElementAttributes;
import org.ehrbase.jooq.dbencoding.attributes.EvaluationAttributes;
import org.ehrbase.jooq.dbencoding.attributes.EventAttributes;
import org.ehrbase.jooq.dbencoding.attributes.HistoryAttributes;
import org.ehrbase.jooq.dbencoding.attributes.InstructionAttributes;
import org.ehrbase.jooq.dbencoding.attributes.ItemStructureAttributes;
import org.ehrbase.jooq.dbencoding.attributes.ObservationAttributes;
import org.ehrbase.jooq.dbencoding.attributes.SectionAttributes;
import org.ehrbase.openehr.sdk.serialisation.exception.MarshalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sequential Event Processor for Composition.
 *
 * <p>Takes an RM composition and serialize it as a Maps of Maps and Arrays with WrappedElements.
 * since some duplicate entries have been noticed, the node id contains type:name:archetype_id
 *
 * @author Christian Chevalley
 */
public class CompositionSerializer {

    public enum WalkerOutputMode {
        PATH,
        NAMED,
        EXPANDED,
        RAW
    }

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    protected ItemStack itemStack = new ItemStack();

    private final WalkerOutputMode tagMode; // default

    public static final String TAG_META = "/meta";
    public static final String TAG_CONTENT = "/content";
    public static final String TAG_PROTOCOL = "/protocol";
    public static final String TAG_DATA = "/data";
    public static final String TAG_STATE = "/state";
    public static final String TAG_DESCRIPTION = "/description";
    public static final String TAG_TIME = "/time";
    public static final String TAG_WIDTH = "/width";
    public static final String TAG_MATH_FUNCTION = "/math_function";
    public static final String TAG_INSTRUCTION = "/instruction";
    public static final String TAG_NARRATIVE = "/narrative";
    public static final String TAG_EXPIRY_TIME = "/expiry_time";
    public static final String TAG_ITEMS = "/items";
    public static final String TAG_OTHER_CONTEXT = "/context/other_context";
    public static final String TAG_ACTIVITIES = "/activities";
    public static final String TAG_ACTIVITY = "/activity";
    public static final String TAG_VALUE = "/value";
    public static final String TAG_NULL_FLAVOUR = "/null_flavour";
    public static final String TAG_FEEDER_AUDIT = "/feeder_audit";
    public static final String TAG_EVENTS = "/events";
    public static final String TAG_ORIGIN = "/origin";
    public static final String TAG_SUMMARY = "/summary";
    public static final String TAG_TIMING = "/timing";
    public static final String TAG_COMPOSITION = "/composition";
    public static final String TAG_ENTRY = "/entry";
    public static final String TAG_EVALUATION = "/evaluation";
    public static final String TAG_OBSERVATION = "/observation";
    public static final String TAG_ACTION = "/action";
    public static final String TAG_SUBJECT = "/subject";
    public static final String TAG_LANGUAGE = "/language";
    public static final String TAG_ENCODING = "/encoding";
    public static final String TAG_ISM_TRANSITION = "/ism_transition";
    public static final String TAG_CURRENT_STATE = "/current_state";
    public static final String TAG_CAREFLOW_STEP = "/careflow_step";
    public static final String TAG_ISM_TRANSITION_REASON = "/careflow_step";
    public static final String TAG_TRANSITION = "/transition";
    public static final String TAG_WORKFLOW_ID = "/workflow_id";
    public static final String TAG_WF_DEFINITION = "/wf_definition";
    public static final String TAG_GUIDELINE_ID = "/guideline_id";
    public static final String TAG_OTHER_PARTICIPATIONS = "/other_participations";
    public static final String TAG_PROVIDER = "/provider"; // care entry provider
    public static final String TAG_UID = "/uid";
    public static final String TAG_OTHER_DETAILS = "/other_details";
    public static final String TAG_INSTRUCTION_DETAILS = "/instruction_details";
    public static final String TAG_ACTIVITY_ID = "/action_id";
    public static final String TAG_INSTRUCTION_ID = "/instruction_id";
    public static final String TAG_PATH = "/$PATH$";
    public static final String TAG_CLASS = "/$CLASS$";
    public static final String TAG_NAME = "/name";
    public static final String TAG_DEFINING_CODE = "/defining_code";
    public static final String INNER_CLASS_LIST = "$INNER_CLASS_LIST$";
    public static final String TAG_ACTION_ARCHETYPE_ID = "/action_archetype_id";
    public static final String TAG_ARCHETYPE_NODE_ID = "/archetype_node_id";
    public static final String TAG_LINKS = "/links";
    public static final String DEFAULT_NARRATIVE = "DEFAULT_NARRATIVE";
    public static final String TAG_ARCHETYPE_DETAILS = "/archetype_details";
    public static final String EPOCH_OFFSET = "epoch_offset";
    public static final DvText NO_NAME = null;

    public CompositionSerializer() {
        this.tagMode = WalkerOutputMode.PATH;
    }

    /**
     * main entry method, process a composition.
     *
     * @param composition
     * @return
     */
    //    @Override
    private Map<String, Object> traverse(Composition composition) {
        Map<String, Object> ctree = PathMap.getInstance();
        if (composition == null /* CHC 170426: no content is legit... */) {
            return null;
        }

        Map<String, Object> ltree = MultiMap.getInstance();

        if (composition.getContent() != null && !composition.getContent().isEmpty()) {
            for (ContentItem item : composition.getContent()) {
                ltree = new EntrySerialTree(ltree, tagMode).insert(item, TAG_CONTENT, traverse(item, TAG_CONTENT));
            }
        }

        ltree.remove(TAG_CLASS);
        ltree.put(TAG_CLASS, new SimpleClassName(composition).toString()); // force the classname
        itemStack.popStacks();

        ctree = new EntrySerialTree(ctree, tagMode).insert(composition, TAG_COMPOSITION, ltree);

        return ctree;
    }

    /**
     * domain level: Observation, evaluation, instruction, action. section, admin etc.
     *
     * @param item
     * @param tag
     * @throws Exception
     */
    private Map<String, Object> traverse(ContentItem item, String tag) {

        Map<String, Object> retmap = null;

        if (item == null) {
            return null;
        }

        log.debug(
                "traverse element of class: {}, tag: {}, nodeid: {}", item.getClass(), tag, item.getArchetypeNodeId());

        if (item.getArchetypeNodeId() == null || item.getArchetypeNodeId().isEmpty()) {
            throw new IllegalArgumentException(
                    "ContentItem mandatory attribute archetype_node_id null or empty, item:" + item);
        }

        if (item.getName() == null || item.getName().getValue().isEmpty()) {
            throw new IllegalArgumentException(
                    "ContentItem mandatory attribute name is null or empty, item:" + item.getArchetypeNodeId());
        }

        itemStack.pushStacks(
                tag + "[" + item.getArchetypeNodeId() + "]", item.getName().getValue());

        if (item instanceof Observation) {
            Observation observation = (Observation) item;
            Map<String, Object> ltree = PathMap.getInstance();

            if (observation.getProtocol() != null) {
                Object protocol = traverse(observation.getProtocol(), TAG_PROTOCOL);
                if (protocol != null) {
                    ltree = new EntrySerialTree(ltree, tagMode).insert(observation, TAG_PROTOCOL, protocol);
                }
            }

            if (observation.getData() != null) {
                ltree = new EntrySerialTree(ltree, tagMode)
                        .insert(observation, TAG_DATA, traverse(observation.getData(), TAG_DATA));
            }

            if (observation.getState() != null) {
                ltree = new EntrySerialTree(ltree, tagMode)
                        .insert(observation, TAG_STATE, traverse(observation.getState(), TAG_STATE));
            }

            ltree = new ObservationAttributes(this, itemStack, ltree).toMap(observation);

            if (ltree.size() > 0) {
                retmap = ltree;
            }

        } else if (item instanceof Evaluation) {
            Evaluation evaluation = (Evaluation) item;
            Map<String, Object> ltree = PathMap.getInstance();

            if (evaluation.getProtocol() != null) {
                Object protocol = traverse(evaluation.getProtocol(), TAG_PROTOCOL);
                if (protocol != null) {
                    ltree = new EntrySerialTree(ltree, tagMode).insert(evaluation, TAG_PROTOCOL, protocol);
                }
            }

            if (evaluation.getData() != null) {
                ltree = new EntrySerialTree(ltree, tagMode)
                        .insert(evaluation, TAG_DATA, traverse(evaluation.getData(), TAG_DATA));
            }

            ltree = new EvaluationAttributes(this, itemStack, ltree).toMap(evaluation);

            if (ltree.size() > 0) {
                retmap = ltree;
            }

        } else if (item instanceof Instruction) {
            Map<String, Object> ltree = PathMap.getInstance();

            Instruction instruction = (Instruction) item;

            if (instruction.getProtocol() != null) {
                Object protocol = traverse(instruction.getProtocol(), TAG_PROTOCOL);
                if (protocol != null) {
                    ltree = new SerialTree(ltree)
                            .insert(
                                    instruction,
                                    new NodeEncoding(tagMode)
                                            .tag(TAG_PROTOCOL, ((Instruction) item).getProtocol(), ltree),
                                    protocol);
                }
            }

            ltree = new InstructionAttributes(this, itemStack, ltree).toMap(instruction);

            if (instruction.getActivities() != null) {

                Map<String, Object> activities = MultiMap.getInstance();
                for (Activity activity : instruction.getActivities()) {
                    itemStack.pushStacks(
                            TAG_ACTIVITIES + "[" + activity.getArchetypeNodeId() + "]",
                            activity.getName().getValue());
                    activities = new EntrySerialTree(activities, tagMode)
                            .insert(activity, TAG_ACTIVITIES, traverse(activity, TAG_DESCRIPTION));
                    itemStack.popStacks();
                }

                ltree = new EntrySerialTree(ltree, tagMode).insert(instruction, TAG_ACTIVITIES, activities);
            }

            if (ltree.size() > 0) {
                retmap = ltree;
            }

        } else if (item instanceof Action) {
            Map<String, Object> ltree = PathMap.getInstance();

            Action action = (Action) item;

            if (action.getProtocol() != null) {
                Object protocol = traverse(action.getProtocol(), TAG_PROTOCOL);
                if (protocol != null) {
                    ltree = new EntrySerialTree(ltree, tagMode).insert(action, TAG_PROTOCOL, protocol);
                }
            }

            if (action.getDescription() != null) {
                Object description = traverse(action.getDescription(), TAG_DESCRIPTION);
                if (description != null) {
                    ltree = new EntrySerialTree(ltree, tagMode).insert(action, TAG_DESCRIPTION, description);
                }
            } else {
                // this should not occur except in test scenario as this is rejected by the validation
                if (log.isWarnEnabled()) {
                    log.warn("ACTION requires attribute 'description' at node: {}", itemStack.pathStackDump());
                }
            }

            ltree = new ActionAttributes(this, itemStack, ltree).toMap(action);
            ltree.computeIfAbsent(TAG_CLASS, value -> new SimpleClassName(item).toString());

            retmap = ltree;

        } else if (item instanceof Section) {

            Map<String, Object> ltree = MultiMap.getInstance();

            for (ContentItem contentItem : ((Section) item).getItems()) {
                ltree = new SerialTree(ltree)
                        .insert(
                                contentItem,
                                new NodeEncoding(tagMode).tag(TAG_ITEMS, contentItem, ltree),
                                traverse(contentItem, TAG_ITEMS));
                log.debug("ltree now: {}", ltree != null);
            }

            ltree = new SectionAttributes(this, itemStack, ltree).toMap((Section) item);

            ltree.remove(TAG_CLASS);
            ltree.put(TAG_CLASS, new SimpleClassName(item).toString()); // force the classname
            ltree = fixLocatableAttributes(ltree);
            retmap = ltree;

        } else if (item instanceof AdminEntry) {
            AdminEntry adminEntry = (AdminEntry) item;
            Map<String, Object> ltree = PathMap.getInstance();

            if (adminEntry.getData() != null) {
                ltree = new SerialTree(ltree)
                        .insert(
                                adminEntry,
                                new NodeEncoding(tagMode).tag(TAG_DATA, adminEntry.getData(), ltree),
                                traverse(adminEntry.getData(), TAG_DATA));
            }

            ltree = new AdminEntryAttributes(this, itemStack, ltree).toMap(adminEntry);

            if (ltree.size() > 0) {
                retmap = ltree;
            }

        } else if (item instanceof GenericEntry) {
            Map<String, Object> ltree = PathMap.getInstance();

            GenericEntry genericEntry = (GenericEntry) item;

            ltree = new SerialTree(ltree)
                    .insert(
                            genericEntry,
                            new NodeEncoding(tagMode).tag(TAG_DATA, genericEntry.getData(), ltree),
                            traverse(genericEntry.getData(), TAG_DATA));

            if (ltree.size() > 0) {
                retmap = ltree;
            }

        } else {
            log.warn("This item is not handled! {}", item.getNameAsString());
        }

        itemStack.popStacks();
        return retmap;
    }

    private Map<String, Object> traverse(Activity activity, String tag) {
        if (activity == null) {
            return null;
        }

        log.debug("traverse activity: {}", activity);

        if (activity.getDescription() == null) {
            throw new IllegalArgumentException("Invalid activity, no description found:" + activity.getNameAsString());
        }

        Map<String, Object> ltree = PathMap.getInstance();

        ltree = new ActivityAttributes(this, itemStack, ltree).toMap(activity);

        itemStack.pushStacks(
                tag + "[" + activity.getDescription().getArchetypeNodeId() + "]",
                activity.getDescription().getName().getValue());

        if (log.isDebugEnabled()) {
            log.debug(
                    "{}{}[{}]={}",
                    itemStack.pathStackDump(),
                    TAG_DESCRIPTION,
                    activity.getArchetypeNodeId(),
                    activity.getDescription());
        }

        ltree = new EntrySerialTree(ltree, tagMode)
                .insert(
                        activity,
                        TAG_DESCRIPTION,
                        traverse(
                                activity.getDescription(),
                                null)); // don't add a /data in path for description (don't ask me why...)

        itemStack.popStacks();
        return ltree;
    }

    /**
     * History level in composition
     *
     * @param history
     * @param tag
     * @throws Exception
     */
    private Map<String, Object> traverse(History<?> history, String tag) {
        if (history == null) {
            return null;
        }

        log.debug("traverse history: {}", history);

        itemStack.pushStacks(
                tag + "[" + history.getArchetypeNodeId() + "]",
                history.getName().getValue());

        Map<String, Object> ltree = PathMap.getInstance();

        if (log.isDebugEnabled()) {
            log.debug(
                    "{}{}[{}]={}",
                    itemStack.pathStackDump(),
                    TAG_ORIGIN,
                    history.getArchetypeNodeId(),
                    history.getOrigin());
        }

        ltree = new HistoryAttributes(this, itemStack, ltree).toMap(history);

        if (history.getSummary() != null) {
            ltree = new EntrySerialTree(ltree, tagMode)
                    .insert(history, TAG_SUMMARY, traverse(history.getSummary(), TAG_SUMMARY));
        }

        if (history.getEvents() != null) {

            Map<String, Object> eventtree = MultiMap.getInstance();

            for (Event<?> event : history.getEvents()) {
                itemStack.pushStacks(
                        TAG_EVENTS + "[" + event.getArchetypeNodeId() + "]",
                        event.getName().getValue());

                Map<String, Object> subtree = PathMap.getInstance();

                if (log.isDebugEnabled()) {
                    log.debug(
                            "{}{}[{}]={}",
                            itemStack.pathStackDump(),
                            TAG_TIME,
                            event.getArchetypeNodeId(),
                            event.getTime());
                }

                subtree = new EventAttributes(this, itemStack, subtree).toMap(event);

                if (event.getData() != null) {
                    subtree = new EntrySerialTree(subtree, tagMode)
                            .insert(event, TAG_DATA, traverse(event.getData(), TAG_DATA));
                }
                if (event.getState() != null) {
                    subtree = new EntrySerialTree(subtree, tagMode)
                            .insert(event, TAG_STATE, traverse(event.getState(), TAG_STATE));
                }

                if (!subtree.containsKey(TAG_CLASS)) {
                    log.warn("Inserting class type, potentially a test case?");
                    subtree.put(TAG_CLASS, new SimpleClassName(event).toString());
                }

                itemStack.popStacks();

                eventtree = new SerialTree(eventtree)
                        .insert(null, event, new NodeEncoding(tagMode).tag(TAG_EVENTS, event, eventtree), subtree);
            }

            ltree = new EntrySerialTree(ltree, tagMode).insert(history, TAG_EVENTS, eventtree);
        }

        itemStack.popStacks();
        return ltree;
    }

    /**
     * identify if the entry is a value singleton. If so, compact the entry to be "KEY/Value=entry" if
     * not, use the usual convention of hash of hash...
     *
     * @param target
     * @throws Exception
     */
    private Map<String, Object> compactEntry(
            Object node, Map<String, Object> target, String key, Map<String, Object> entry) {
        // if entry is null, ignore, the dirty bit is not set...
        if (entry != null && !entry.isEmpty()) {
            if (entry.keySet().size() == 1 && entry.get(TAG_VALUE) != null) {
                Object o = entry.get(TAG_VALUE);
                // TAG_VALUE is not required in the properties map representation
                target = new SerialTree(target).insert(null, null, key, o);
            } else {
                target = new SerialTree(target)
                        .insert(new SimpleClassName(node).toString(), null, key, entry); // unchanged and uncompacted
            }
        }

        return target;
    }

    /**
     * ItemStructure: single, tree or table
     *
     * @param item
     * @param uppertag
     */
    private Map<String, Object> traverse(ItemStructure item, String uppertag) {

        Map<String, Object> retmap = null;

        log.debug("traverse itemstructure: {}", item);

        if (item == null) {
            return null;
        }

        if (uppertag != null) {
            itemStack.pushStacks(uppertag + "[" + item.getArchetypeNodeId() + "]", item.getNameAsString());
        }

        if (item instanceof ItemSingle) {
            Map<String, Object> ltree = PathMap.getInstance();

            ItemSingle itemSingle = (ItemSingle) item;

            if (itemSingle.getItem() != null) {
                ltree = compactEntry(
                        itemSingle,
                        ltree,
                        new NodeEncoding(tagMode).tag(TAG_ITEMS, itemSingle, ltree),
                        traverse(itemSingle.getItem(), TAG_ITEMS));
            }
            if (ltree.size() > 0) {
                retmap = ltree;
            }
        } else if (item instanceof ItemList) {
            Map<String, Object> ltree = MultiMap.getInstance();

            ItemList list = (ItemList) item;

            if (list.getItems() != null) {

                for (Item listItem : list.getItems()) {
                    ltree = compactEntry(
                            listItem,
                            ltree,
                            new NodeEncoding(tagMode).tag(TAG_ITEMS, listItem, ltree),
                            traverse(listItem, TAG_ITEMS));
                }
            }
            if (ltree.size() > 0) {
                retmap = ltree;
            }

        } else if (item instanceof ItemTree) {
            Map<String, Object> ltree = MultiMap.getInstance();

            ItemTree tree = (ItemTree) item;

            if (tree.getItems() != null) {

                for (Item subItem : tree.getItems()) {
                    ltree = compactEntry(
                            subItem,
                            ltree,
                            new NodeEncoding(tagMode).tag(TAG_ITEMS, subItem, ltree),
                            traverse(subItem, TAG_ITEMS));
                }
            }
            if (ltree.size() > 0) {
                retmap = ltree;
            }

        } else if (item instanceof ItemTable) {
            Map<String, Object> ltree = MultiMap.getInstance();

            ItemTable table = (ItemTable) item;

            if (table.getRows() != null) {

                for (Item subItem : table.getRows()) {
                    ltree = compactEntry(
                            subItem,
                            ltree,
                            new NodeEncoding(tagMode).tag(TAG_ITEMS, subItem, ltree),
                            traverse(subItem, TAG_ITEMS));
                }
            }
            if (ltree.size() > 0) {
                retmap = ltree;
            }
        }

        if (uppertag != null) {
            itemStack.popStacks();
        }

        if (retmap != null) {
            if (retmap.containsKey(TAG_CLASS)) {
                retmap.remove(CompositionSerializer.TAG_CLASS); // this will come out as an array...
            }
            retmap.put(TAG_CLASS, new SimpleClassName(item).toString()); // this will come out as an array...
            retmap = new ItemStructureAttributes(this, itemStack, retmap).toMap(item);
            retmap = fixLocatableAttributes(retmap);
        }
        return retmap;
    }

    private Map<String, Object> fixLocatableAttributes(Map<String, Object> map) {
        if (map instanceof MultiValueMap) {
            Map<String, Object> newMap = new LinkedHashMap<>();

            map.forEach((k, v) -> {
                if (List.of(TAG_UID, TAG_FEEDER_AUDIT, TAG_LINKS).contains(k)
                        && v instanceof List
                        && !((List<?>) v).isEmpty()) {
                    newMap.put(k, ((List<?>) v).get(0));
                } else {
                    newMap.put(k, v);
                }
            });

            return newMap;
        } else {
            return map;
        }
    }

    /**
     * Element level, normally cannot go deeper...
     *
     * @param item
     * @param tag
     * @throws Exception
     */
    private Map<String, Object> traverse(Item item, String tag) {
        Map<String, Object> retmap = PathMap.getInstance();

        log.debug("traverse item: {}", item);

        if (item == null) {
            return null;
        }

        if (item instanceof Element) { // NB. Element value and null flavour are optional
            itemStack.pushStacks(tag + "[" + item.getArchetypeNodeId() + "]", null);
            retmap = new ElementAttributes(this, itemStack, retmap).toMap((Element) item);
            itemStack.popStacks();
        } else if (item instanceof Cluster) {
            Map<String, Object> ltree = MultiMap.getInstance();
            itemStack.pushStacks(tag + "[" + item.getArchetypeNodeId() + "]", item.getNameAsString());

            Cluster cluster = (Cluster) item;
            boolean hasContent = false;

            if (cluster.getItems() != null) {

                // CHC:160914: fixed issue with cluster encoding as items (generated /value {/name...
                // /value... /$PATH$... $CLASS$})
                // this caused inconsistencies when running AQL queries
                for (Item clusterItem : cluster.getItems()) {
                    Map<String, Object> clusterItems = traverse(clusterItem, TAG_ITEMS);

                    if (clusterItems != null) {
                        clusterItems.computeIfPresent(
                                TAG_CLASS, (key, value) -> item.getClass().getSimpleName());
                        ltree.put(
                                new NodeEncoding(tagMode).tag(TAG_ITEMS, clusterItem, ltree),
                                clusterItems.getOrDefault(TAG_VALUE, clusterItems));
                        //
                    }
                }
                if (ltree.size() > 0) {
                    hasContent = true;
                }

                ltree = new ClusterAttributes(this, itemStack, ltree).toMap(cluster);

                ltree.computeIfAbsent(TAG_CLASS, value -> Cluster.class.getSimpleName());
            }
            if (hasContent) {
                ltree = fixLocatableAttributes(ltree);
                retmap = ltree;
            } else {
                retmap = null;
            }

            itemStack.popStacks();
        }

        return retmap;
    }

    //    @Override
    public String dbEncode(RMObject rmObject) {

        Map<String, Object> objectMap;
        if (rmObject instanceof Composition) {
            objectMap = traverse((Composition) rmObject);
        } else if (rmObject instanceof Item) {
            objectMap = traverse((Item) rmObject, TAG_ITEMS);
        } else if (rmObject instanceof ItemStructure) {
            objectMap = traverse((ItemStructure) rmObject, TAG_ITEMS);
            if (objectMap != null
                    && !objectMap.containsKey(TAG_ARCHETYPE_NODE_ID)
                    && ((ItemStructure) rmObject).getArchetypeNodeId() != null) {
                objectMap.put(
                        CompositionSerializer.TAG_ARCHETYPE_NODE_ID, ((ItemStructure) rmObject).getArchetypeNodeId());
            }
            if (objectMap != null
                    && !objectMap.containsKey(CompositionSerializer.TAG_NAME)
                    && ((ItemStructure) rmObject).getName() != null) {
                objectMap.put(CompositionSerializer.TAG_NAME, ((ItemStructure) rmObject).getName());
            }
        } else {
            throw new MarshalException(String.format("Class %s not supported ", rmObject.getClass()), null);
        }

        GsonBuilder builder = EncodeUtilArchie.getGsonBuilderInstance();
        Gson gson = builder.setPrettyPrinting().create();
        return gson.toJson(objectMap);
    }

    public Map<String, String> getLtreeMap() {
        return itemStack.getLtreeMap();
    }

    public WalkerOutputMode tagMode() {
        return tagMode;
    }
}
