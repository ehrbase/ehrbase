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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.datavalues.quantity.DvProportion;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDate;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDuration;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.ehrbase.openehr.dbformat.json.RmDbJson;
import org.ehrbase.openehr.sdk.util.OpenEHRDateTimeSerializationUtils;
import org.ehrbase.openehr.sdk.webtemplate.parser.NodeId;

public final class VersionedObjectDataStructure {

    /**
     * Synthetic field for DV_ORDERDED.magnitude() that contains the calculated magnitude.
     */
    public static final String MAGNITUDE_FIELD = "_magnitude";

    public static final ObjectMapper MARSHAL_OM = CanonicalJson.MARSHAL_OM
            .copy()
            .setDefaultTyping(
                    new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.EVERYTHING) {
                        @Override
                        public boolean useForType(JavaType t) {
                            return OpenEHRBase.class.isAssignableFrom(t.getRawClass());
                        }
                    }.init(JsonTypeInfo.Id.NAME, new CanonicalJson.CJOpenEHRTypeNaming())
                            .typeProperty(RmAttribute.OBJ_TYPE.attribute())
                            .typeIdVisibility(true)
                            .inclusion(JsonTypeInfo.As.PROPERTY));

    private VersionedObjectDataStructure() {}

    public static List<StructureNode> createDataStructure(RMObject rmObject) {
        JsonNode jsonNode = RmDbJson.MARSHAL_OM.valueToTree(rmObject);
        fillInMagnitudes(jsonNode);

        var root = createStructureDto(
                null, jsonNode, StructureRmType.byType(rmObject.getClass()).orElseThrow(), null);

        List<StructureNode> roots = new ArrayList<>();
        roots.add(root);

        handleSubStructure(root, root.getJsonNode(), null, roots);

        // set num and parentNum fields
        {
            int num = 0;
            root.setParentNum(0);
            for (StructureNode r : roots) {
                // skip intermediates
                if (r.getStructureRmType().isStructureEntry()) {
                    r.setNum(num++);
                    r.getChildren().stream()
                            .filter(c -> c.getStructureRmType().isStructureEntry())
                            .forEach(c -> c.setParentNum(r.getNum()));
                }
            }
        }
        return roots;
    }

    /**
     * Depth-first stream of object nodes
     *
     * @param jsonNode root node
     */
    private static Stream<ObjectNode> streamObjectNodes(JsonNode jsonNode) {
        if (!jsonNode.isContainerNode()) {
            return Stream.empty();
        }
        Stream<ObjectNode> childContainers = StreamSupport.stream(jsonNode.spliterator(), false)
                .flatMap(VersionedObjectDataStructure::streamObjectNodes);
        if (jsonNode.isObject()) {
            return Stream.concat(childContainers, Stream.of((ObjectNode) jsonNode));
        } else {
            return childContainers;
        }
    }

    public static void fillInMagnitudes(JsonNode jsonNode) {
        streamObjectNodes(jsonNode)
                .filter(JsonNode::isObject)
                .forEach(n -> addMagnitudeAttribute(
                        n.get(RmAttribute.OBJ_TYPE.attribute()).textValue(), n));
    }

    private static void addMagnitudeAttribute(String type, ObjectNode object) {
        try {
            switch (type) {
                case "DV_DATE_TIME" -> object.put(
                        MAGNITUDE_FIELD,
                        OpenEHRDateTimeSerializationUtils.toMagnitude(
                                RmDbJson.MARSHAL_OM.treeToValue(object, DvDateTime.class)));
                case "DV_DATE" -> object.put(
                        MAGNITUDE_FIELD,
                        OpenEHRDateTimeSerializationUtils.toMagnitude(
                                RmDbJson.MARSHAL_OM.treeToValue(object, DvDate.class)));
                case "DV_TIME" -> object.put(
                        MAGNITUDE_FIELD,
                        OpenEHRDateTimeSerializationUtils.toMagnitude(
                                RmDbJson.MARSHAL_OM.treeToValue(object, DvTime.class)));
                case "DV_DURATION" -> object.put(
                        MAGNITUDE_FIELD,
                        RmDbJson.MARSHAL_OM
                                .treeToValue(object, DvDuration.class)
                                .getMagnitude());
                case "DV_PROPORTION" -> object.put(
                        MAGNITUDE_FIELD,
                        RmDbJson.MARSHAL_OM
                                .treeToValue(object, DvProportion.class)
                                .getMagnitude());
                default -> {
                    /* do not add magnitude */
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Walks the JSON tree and removes nodes that are structure roots.
     * Root nodes are only present in arrays.
     * Arrays never hold a mix of root and non-root nodes.
     * Empty arrays are removed.
     *
     * @param currentNode
     * @param currentJson
     * @param arrayParentName
     * @param roots gathers all structure roots
     * @return if the child needs to remain in the parent data structure
     */
    private static boolean handleSubStructure(
            StructureNode currentNode, JsonNode currentJson, String arrayParentName, List<StructureNode> roots) {
        if (!currentJson.isContainerNode()) {
            return true;
        }

        if (currentJson.isObject()) {
            handleSubObject(currentNode, (ObjectNode) currentJson, roots);
            return true;

        } else if (currentJson.isArray()) {
            boolean hasRemainingChildren = false;
            for (int i = 0; i < currentJson.size(); i++) {
                boolean remainingChild =
                        handleArrayElement(currentNode, (ArrayNode) currentJson, arrayParentName, roots, i);
                if (i == 0) {
                    hasRemainingChildren = remainingChild;
                } else if (hasRemainingChildren != remainingChild) {
                    throw new IllegalArgumentException(
                            "Structure elements must not be mixed with non-structure elements");
                }
            }
            return hasRemainingChildren;
        } else {
            throw new IllegalStateException("Unexpected container type: " + currentJson.getNodeType());
        }
    }

    private static boolean handleArrayElement(
            StructureNode root, ArrayNode parentArray, String parentName, List<StructureNode> roots, int childIndex) {
        var child = parentArray.get(childIndex);
        Optional<StructureRmType> structureRmType = getType(child).flatMap(StructureRmType::byTypeName);
        var childRoot = structureRmType.map(t -> {
            StructureNode newRoot = createStructureDto(root, child, t, StructureIndex.Node.of(parentName, childIndex));
            roots.add(newRoot);
            return newRoot;
        });
        handleSubStructure(childRoot.orElse(root), child, null, roots);

        return structureRmType.filter(StructureRmType::isStructureEntry).isEmpty();
    }

    private static void handleSubObject(StructureNode root, ObjectNode jsonNode, List<StructureNode> roots) {
        Iterator<Map.Entry<String, JsonNode>> fieldIt = jsonNode.fields();
        while (fieldIt.hasNext()) {
            Map.Entry<String, JsonNode> e = fieldIt.next();
            String attribute = e.getKey();
            JsonNode child = e.getValue();
            Optional<StructureNode> childRoot = getType(child)
                    .flatMap(StructureRmType::byTypeName)
                    .map(structureRmType -> {
                        if (structureRmType.isStructureEntry()
                                // Feeder audit is kept for element
                                && !(StructureRmType.ELEMENT.equals(root.getStructureRmType())
                                        && StructureRmType.FEEDER_AUDIT.equals(
                                                structureRmType))) { // structureRmType.isDataRoot()) {
                            fieldIt.remove();
                        }
                        StructureNode newRoot = createStructureDto(
                                root, child, structureRmType, StructureIndex.Node.of(attribute, null));
                        roots.add(newRoot);
                        return newRoot;
                    });
            boolean keepChild = handleSubStructure(childRoot.orElse(root), child, attribute, roots);
            // remove empty arrays
            if (!keepChild) {
                fieldIt.remove();
            }
        }
    }

    private static StructureNode createStructureDto(
            StructureNode parent, JsonNode jsonNode, StructureRmType structureRmType, StructureIndex.Node idx) {
        StructureNode newRoot = parent == null ? new StructureNode() : new StructureNode(parent);
        newRoot.setEntityIdx(
                parent == null ? new StructureIndex() : parent.getEntityIdx().createChild(idx));
        newRoot.setStructureRmType(structureRmType);

        var jn = Optional.of(jsonNode);
        jn.map(n -> n.get("name"))
                .map(n -> n.get("value"))
                .map(JsonNode::asText)
                .ifPresent(newRoot::setEntityName);
        jn.map(n -> n.get("archetype_node_id")).map(JsonNode::asText).ifPresent(newRoot::setArchetypeNodeId);
        newRoot.setJsonNode((ObjectNode) jsonNode);

        /* the archetype parent of the node.
         * e.g. for c/content[at0001]/data/events[at0002]/items[at003]/items[openEHR-EHR-CLUSTER.myCluster.v1]/items[at002]/items[openEHR-EHR-ELEMENT.myElement.v1, 'Data']
         * this would be items[openEHR-EHR-CLUSTER.myCluster.v1]
         */
        StructureNode contentItem;
        if (parent == null) {
            contentItem = null;
        } else if (Optional.of(parent)
                .map(StructureNode::getArchetypeNodeId)
                .map(NodeId::new)
                .filter(NodeId::isArchetypeId)
                .isPresent()) {
            contentItem = parent;
        } else {
            contentItem = parent.getContentItem();
        }
        newRoot.setContentItem(contentItem);

        return newRoot;
    }

    private static Optional<String> getType(JsonNode childNode) {
        return Optional.of(childNode)
                .filter(JsonNode::isObject)
                .map(n -> n.get(RmAttribute.OBJ_TYPE.attribute()))
                .map(JsonNode::asText);
    }

    public static ObjectNode applyRmAliases(ObjectNode jsonNode) {
        ObjectNode newNode = jsonNode.objectNode();

        jsonNode.fields().forEachRemaining(e -> {
            String alias = RmAttribute.getAlias(e.getKey());
            JsonNode child = e.getValue();
            if (child.isObject()) {
                child = applyRmAliases((ObjectNode) child);
            } else if (child.isArray()) {
                ArrayNode newArray = jsonNode.arrayNode(child.size());
                child.forEach(c -> {
                    if (c.isObject()) {
                        newArray.add(applyRmAliases((ObjectNode) c));
                    } else {
                        newArray.add(c);
                    }
                });
                child = newArray;
            } else if (child.isTextual() && e.getKey().equals(RmAttribute.OBJ_TYPE.attribute())) {
                String rmNameAlias = RmType.getAlias(child.textValue());
                child = new TextNode(rmNameAlias);
            }
            newNode.putIfAbsent(alias, child);
        });

        return newNode;
    }
}
