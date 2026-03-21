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
package org.ehrbase.repository.composition;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datastructures.Element;
import com.nedap.archie.rm.datavalues.DataValue;
import com.nedap.archie.rm.datavalues.DvBoolean;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.DvURI;
import com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia;
import com.nedap.archie.rm.datavalues.encapsulated.DvParsable;
import com.nedap.archie.rm.datavalues.quantity.DvCount;
import com.nedap.archie.rm.datavalues.quantity.DvOrdinal;
import com.nedap.archie.rm.datavalues.quantity.DvProportion;
import com.nedap.archie.rm.datavalues.quantity.DvQuantity;
import com.nedap.archie.rm.datavalues.quantity.DvScale;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDate;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDuration;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;
import com.nedap.archie.rminfo.RMAttributeInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplateNode;
import org.ehrbase.repository.schema.TemplateTableMetadata;
import org.ehrbase.schemagen.ColumnNamer;

/**
 * Walks an RM Composition tree in parallel with its WebTemplate to extract column values
 * for INSERT into normalized template tables.
 *
 * <p>Mirrors the tree walk of {@link org.ehrbase.schemagen.TemplateAnalyzer} to ensure
 * column names match exactly. Uses {@link ColumnNamer} for name generation.
 */
public final class RmTreeWalker {

    private static final Set<String> STRUCTURE_NODES = Set.of(
            "COMPOSITION",
            "SECTION",
            "OBSERVATION",
            "EVALUATION",
            "INSTRUCTION",
            "ACTION",
            "ADMIN_ENTRY",
            "ACTIVITY",
            "HISTORY",
            "EVENT",
            "POINT_EVENT",
            "INTERVAL_EVENT",
            "ITEM_TREE",
            "ITEM_LIST",
            "ITEM_SINGLE",
            "ITEM_TABLE");

    private RmTreeWalker() {}

    /**
     * Extracts column values from an RM Composition, ready for INSERT into template tables.
     *
     * @param composition   the Archie RM Composition
     * @param webTemplate   the WebTemplate for this template
     * @param metadata      table metadata from TemplateSchemaResolver
     * @return extracted column data for main and child tables
     */
    public static CompositionTableData extract(
            Composition composition, WebTemplate webTemplate, TemplateTableMetadata metadata) {

        Map<String, Object> mainValues = new LinkedHashMap<>();
        Map<String, List<Map<String, Object>>> childValues = new HashMap<>();

        WebTemplateNode root = webTemplate.getTree();
        walkNode(root, composition, mainValues, childValues, "", metadata);

        // Participations JSONB — from composition context
        if (composition.getContext() != null
                && composition.getContext().getParticipations() != null
                && !composition.getContext().getParticipations().isEmpty()) {
            mainValues.put("participations", serializeToJson(composition.getContext().getParticipations()));
        }

        return new CompositionTableData(mainValues, childValues);
    }

    private static void walkNode(
            WebTemplateNode node,
            Object rmObject,
            Map<String, Object> values,
            Map<String, List<Map<String, Object>>> childValues,
            String pathPrefix,
            TemplateTableMetadata metadata) {

        if (rmObject == null) {
            return;
        }

        List<Object> rmChildren = getRmChildren(rmObject);
        for (WebTemplateNode childNode : node.getChildren()) {
            String rmType = childNode.getRmType();
            String childId = childNode.getId();

            // Find matching RM object(s) for this WebTemplate node
            List<Object> matchingRm = findMatchingRm(rmChildren, childNode);

            if (isRepeatingCluster(childNode)) {
                // Repeating CLUSTER -> separate child table
                String childTableName = metadata.tableName() + "_" + ColumnNamer.toColumnName(childId);
                List<Map<String, Object>> childRows =
                        childValues.computeIfAbsent(childTableName, k -> new ArrayList<>());
                for (Object rmChild : matchingRm) {
                    Map<String, Object> childRow = new LinkedHashMap<>();
                    walkNode(childNode, rmChild, childRow, childValues, "", metadata);
                    childRows.add(childRow);
                }
            } else if ("ELEMENT".equals(rmType)) {
                for (Object rmChild : matchingRm) {
                    handleElement(childNode, rmChild, values, pathPrefix);
                }
            } else if ("CLUSTER".equals(rmType)) {
                String newPrefix = buildPrefix(pathPrefix, childId);
                for (Object rmChild : matchingRm) {
                    walkNode(childNode, rmChild, values, childValues, newPrefix, metadata);
                }
            } else if (STRUCTURE_NODES.contains(rmType)) {
                for (Object rmChild : matchingRm) {
                    walkNode(childNode, rmChild, values, childValues, pathPrefix, metadata);
                }
            }
        }
    }

    private static void handleElement(
            WebTemplateNode elementNode, Object rmObject, Map<String, Object> values, String pathPrefix) {

        if (!(rmObject instanceof Element element)) {
            return;
        }

        String colBase = buildColumnName(pathPrefix, elementNode.getId());
        DataValue value = element.getValue();

        if (value == null) {
            // null_flavour for optional elements
            if (element.getNullFlavour() != null && elementNode.getMin() == 0) {
                values.put(colBase + "_null_flavour", element.getNullFlavour().getValue());
            }
            return;
        }

        extractDvValue(value, colBase, values);

        // null_flavour column if element is optional
        if (elementNode.getMin() == 0) {
            values.put(colBase + "_null_flavour", null);
        }
    }

    /**
     * Extracts column values from a DataValue according to TypeMapper conventions.
     */
    static void extractDvValue(DataValue value, String colBase, Map<String, Object> values) {
        switch (value) {
            case DvBoolean dv -> values.put(colBase, dv.getValue());

            case DvQuantity dv -> {
                values.put(colBase + "_magnitude", dv.getMagnitude());
                values.put(colBase + "_units", dv.getUnits());
                values.put(colBase + "_precision", dv.getPrecision());
            }

            case DvCount dv -> values.put(colBase, dv.getMagnitude());

            case DvCodedText dv -> {
                values.put(colBase + "_value", dv.getValue());
                values.put(
                        colBase + "_code",
                        dv.getDefiningCode() != null ? dv.getDefiningCode().getCodeString() : null);
                values.put(
                        colBase + "_terminology",
                        dv.getDefiningCode() != null && dv.getDefiningCode().getTerminologyId() != null
                                ? dv.getDefiningCode().getTerminologyId().getValue()
                                : null);
            }

            case DvText dv -> values.put(colBase, dv.getValue());

            case DvDateTime dv -> {
                values.put(colBase, dv.getValue() != null ? dv.getValue().toString() : null);
                values.put(colBase + "_magnitude", dv.getMagnitude());
            }

            case DvDate dv -> {
                values.put(colBase, dv.getValue() != null ? dv.getValue().toString() : null);
                values.put(colBase + "_magnitude", dv.getMagnitude());
            }

            case DvTime dv -> {
                values.put(colBase, dv.getValue() != null ? dv.getValue().toString() : null);
                values.put(colBase + "_magnitude", dv.getMagnitude());
            }

            case DvDuration dv -> {
                values.put(colBase, dv.getValue() != null ? dv.getValue().toString() : null);
                values.put(colBase + "_magnitude", dv.getMagnitude());
            }

            case DvProportion dv -> {
                values.put(colBase + "_numerator", dv.getNumerator());
                values.put(colBase + "_denominator", dv.getDenominator());
                values.put(colBase + "_type", dv.getType());
            }

            case DvOrdinal dv -> {
                values.put(colBase + "_value", dv.getValue());
                values.put(
                        colBase + "_symbol_value",
                        dv.getSymbol() != null ? dv.getSymbol().getValue() : null);
                values.put(
                        colBase + "_symbol_code",
                        dv.getSymbol() != null && dv.getSymbol().getDefiningCode() != null
                                ? dv.getSymbol().getDefiningCode().getCodeString()
                                : null);
            }

            case DvScale dv -> {
                values.put(colBase + "_value", dv.getValue());
                values.put(
                        colBase + "_symbol_value",
                        dv.getSymbol() != null ? dv.getSymbol().getValue() : null);
                values.put(
                        colBase + "_symbol_code",
                        dv.getSymbol() != null && dv.getSymbol().getDefiningCode() != null
                                ? dv.getSymbol().getDefiningCode().getCodeString()
                                : null);
            }

            case DvIdentifier dv -> {
                values.put(colBase + "_id", dv.getId());
                values.put(colBase + "_issuer", dv.getIssuer());
                values.put(colBase + "_assigner", dv.getAssigner());
                values.put(colBase + "_type", dv.getType());
            }

            case DvURI dv ->
                values.put(colBase, dv.getValue() != null ? dv.getValue().toString() : null);

            case DvMultimedia dv -> {
                values.put(
                        colBase + "_uri",
                        dv.getUri() != null ? dv.getUri().getValue().toString() : null);
                values.put(
                        colBase + "_media_type",
                        dv.getMediaType() != null ? dv.getMediaType().getCodeString() : null);
            }

            case DvParsable dv -> {
                values.put(colBase + "_value", dv.getValue());
                values.put(colBase + "_formalism", dv.getFormalism());
            }

            // JSONB fallback for unmapped types
            default -> values.put(colBase, serializeToJson(value));
        }
    }

    private static List<Object> getRmChildren(Object rmObject) {
        if (rmObject instanceof com.nedap.archie.rm.archetyped.Locatable locatable) {
            // Use Archie's reflection to get all RM children
            List<Object> children = new ArrayList<>();
            try {
                var info = com.nedap.archie.rminfo.ArchieRMInfoLookup.getInstance();
                var typeInfo = info.getTypeInfo(rmObject.getClass());
                if (typeInfo != null) {
                    for (RMAttributeInfo attrInfo : typeInfo.getAttributes().values()) {
                        Object attr = attrInfo.getGetMethod().invoke(rmObject);
                        if (attr instanceof List<?> list) {
                            children.addAll(list);
                        } else if (attr instanceof com.nedap.archie.rm.archetyped.Locatable) {
                            children.add(attr);
                        }
                    }
                }
            } catch (Exception e) {
                // fallback: empty
            }
            return children;
        }
        return List.of();
    }

    private static List<Object> findMatchingRm(List<Object> rmChildren, WebTemplateNode templateNode) {
        List<Object> matching = new ArrayList<>();
        String nodeId = templateNode.getNodeId();
        for (Object child : rmChildren) {
            if (child instanceof com.nedap.archie.rm.archetyped.Locatable locatable) {
                if (nodeId != null && nodeId.equals(locatable.getArchetypeNodeId())) {
                    matching.add(child);
                }
            }
        }
        // If no archetype node match, try RM type match
        if (matching.isEmpty()) {
            String rmType = templateNode.getRmType();
            for (Object child : rmChildren) {
                if (child != null && child.getClass().getSimpleName().equalsIgnoreCase(rmType)) {
                    matching.add(child);
                }
            }
        }
        return matching;
    }

    private static boolean isRepeatingCluster(WebTemplateNode node) {
        return "CLUSTER".equals(node.getRmType()) && node.getMax() == -1;
    }

    private static String buildPrefix(String existing, String id) {
        if (existing.isEmpty()) return id;
        return existing + "_" + id;
    }

    private static String buildColumnName(String prefix, String id) {
        String colName = ColumnNamer.toColumnName(id);
        if (prefix.isEmpty()) return colName;
        String prefixClean = ColumnNamer.toColumnName(prefix);
        return prefixClean + "_" + colName;
    }

    private static String serializeToJson(Object value) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
