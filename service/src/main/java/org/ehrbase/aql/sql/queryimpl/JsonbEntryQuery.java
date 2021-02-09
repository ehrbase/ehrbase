/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Stefan Spiska (Vitasystems GmbH).

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.aql.sql.queryimpl;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.value_field.NodePredicate;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.ehr.util.LocatableHelper;
import org.ehrbase.serialisation.dbencoding.CompositionSerializer;
import org.ehrbase.service.IntrospectService;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_ITERATIVE_MARKER;
import static org.ehrbase.jooq.pg.Tables.ENTRY;
import static org.ehrbase.jooq.pg.Tables.EVENT_CONTEXT;
import static org.ehrbase.jooq.pg.Tables.STATUS;

/**
 * Generate an SQL field corresponding to a JSONB data value query
 * Created by christian on 5/6/2016.
 */
@SuppressWarnings({"java:S3776","java:S3740","java:S1452","java:S1075"})
public class JsonbEntryQuery extends ObjectQuery implements IQueryImpl {

    public static final String MAGNITUDE = "magnitude";
    Logger logger = LogManager.getLogger(JsonbEntryQuery.class);

    private static final String JSONB_PATH_SELECTOR_EXPR = " #>> '{";
    private static final String JSONB_AT_AT_SELECTOR_EXPR = " @@ '";
    private static final String JSONB_SELECTOR_COMPOSITION_OPEN = ENTRY.ENTRY_ + JSONB_PATH_SELECTOR_EXPR;
    public static final String JSQUERY_COMPOSITION_OPEN = ENTRY.ENTRY_ + JSONB_AT_AT_SELECTOR_EXPR;


    //OTHER_DETAILS (Ehr Status Query)
    private static final String SELECT_EHR_OTHER_DETAILS_MACRO = JoinBinder.statusRecordTable.field(STATUS.OTHER_DETAILS) + "->('" + CompositionSerializer.TAG_OTHER_DETAILS + "')";
    private static final String JSONB_SELECTOR_EHR_OTHER_DETAILS_OPEN = SELECT_EHR_OTHER_DETAILS_MACRO + JSONB_PATH_SELECTOR_EXPR;


    //OTHER_CONTEXT (Composition context other_context Query)
    private static final String SELECT_EHR_OTHER_CONTEXT_MACRO = EVENT_CONTEXT.OTHER_CONTEXT + "->('" + CompositionSerializer.TAG_OTHER_CONTEXT + "[at0001]" + "')";
    private static final String JSONB_SELECTOR_EHR_OTHER_CONTEXT_OPEN = SELECT_EHR_OTHER_CONTEXT_MACRO + JSONB_PATH_SELECTOR_EXPR;
    public static final String JSQUERY_EHR_OTHER_CONTEXT_OPEN = SELECT_EHR_OTHER_CONTEXT_MACRO + JSONB_AT_AT_SELECTOR_EXPR;

    public static final String COMPOSITION = "composition";
    public static final String CONTENT = "content";
    public static final String ACTIVITIES = "activities";
    public static final String EVENTS = "events";
    public static final String ITEMS = "items";


    public static final String PROTOCOL = "protocol";
    public static final String DATA = "data";
    public static final String DESCRIPTION = "description";
    public static final String INSTRUCTION = "instruction";
    public static final String ACTIVITY = "activity";
    public static final String ENTRY1 = "entry";
    public static final String EVALUATION = "evaluation";
    public static final String OBSERVATION = "observation";
    public static final String ACTION = "action";

    public static final String VALUE = "value";
    public static final String DEFINING_CODE = "definingCode";
    public static final String TIME = "time";
    public static final String NAME = "name";
    public static final String ORIGIN = "origin";
    public static final String MAPPINGS = "mappings";
    public static final String PURPOSE = "purpose";
    public static final String TARGET = "target";
    public static final String TERMINOLOGY_ID = "terminologyId";

    //Generic stuff
    private static final String JSONB_SELECTOR_CLOSE = "}'";
    public static final String JSQUERY_CLOSE = " '::jsquery";

    private String jsonbItemPath;

    public static final String TAG_ACTIVITIES = "/" + ACTIVITIES;
    public static final String TAG_EVENTS = "/" + EVENTS;
    public static final String TAG_COMPOSITION = "/" + COMPOSITION;
    public static final String TAG_CONTENT = "/" + CONTENT;
    public static final String TAG_ITEMS = "/" + ITEMS;

    private static final String[] listIdentifier = {
            TAG_CONTENT,
            TAG_ITEMS,
            TAG_ACTIVITIES,
            TAG_EVENTS
    };

    private boolean containsJqueryPath = false; //true if at leas one AQL path is contained in expression
    private boolean ignoreUnresolvedIntrospect = false;

    private static String ENV_IGNORE_UNRESOLVED_INTROSPECT = "aql.ignoreUnresolvedIntrospect";

    private IntrospectService introspectCache;

    public JsonbEntryQuery(I_DomainAccess domainAccess, IntrospectService introspectCache, PathResolver pathResolver) {
        super(domainAccess, pathResolver);
        this.introspectCache = introspectCache;
        ignoreUnresolvedIntrospect = Boolean.parseBoolean(System.getProperty(ENV_IGNORE_UNRESOLVED_INTROSPECT, "false"));
    }

    private static boolean isList(String predicate) {
        if (predicate.equals(TAG_ACTIVITIES))
            return false;
        for (String identifier : listIdentifier)
            if (predicate.startsWith(identifier)) return true;
        return false;
    }

    public enum PATH_PART {IDENTIFIER_PATH_PART, VARIABLE_PATH_PART}

    public enum OTHER_ITEM {OTHER_DETAILS, OTHER_CONTEXT}

    //deals with special tree based entities
    //this is required to encode structure like events of events
    //the same is applicable to activities. These are in fact pseudo arrays.
    private static void encodeTreeMapNodeId(List<String> jqueryPath, String nodeId) {
        if (nodeId.startsWith(TAG_EVENTS)) {
            //this is an exception since events are represented in an event tree
            jqueryPath.add(TAG_EVENTS);
        } else if (nodeId.startsWith(TAG_ACTIVITIES)) {
            jqueryPath.add(TAG_ACTIVITIES);
        }
    }

    public List<String> jqueryPath(PATH_PART pathPart, String path, String defaultIndex) {
        //CHC 160607: this offset (1 or 0) was required due to a bug in generating the containment table
        //from a PL/pgSQL script. this is no more required

        if (path == null) { //partial path
            jsonDataBlock = true;
            return new ArrayList<>();
        }

        jsonDataBlock = false;
        int offset = 0;
        List<String> segments = LocatableHelper.dividePathIntoSegments(path);
        List<String> jqueryPath = new ArrayList<>();
        String nodeId = null;
        for (int i = offset; i < segments.size(); i++) {
            nodeId = segments.get(i);
            nodeId = "/" + nodeId;

            encodeTreeMapNodeId(jqueryPath, nodeId);

            //CHC, 180502. See CR#95 for more on this.
            //IDENTIFIER_PATH_PART is provided by CONTAINMENT.
            NodePredicate nodePredicate = new NodePredicate(nodeId);
            if (pathPart.equals(PATH_PART.IDENTIFIER_PATH_PART)) {
                nodeId = nodePredicate.removeNameValuePredicate();
                jqueryPath.add(nodeId);
            }
            //VARIABLE_PATH_PART is provided by the user. It may contain name/value node predicate
            //see http://www.openehr.org/releases/QUERY/latest/docs/AQL/AQL.html#_node_predicate
            else if (pathPart.equals(PATH_PART.VARIABLE_PATH_PART)) {

                if (nodePredicate.hasPredicate()) {
                    //do the formatting to allow name/value node predicate processing
                    jqueryPath = new NodeNameValuePredicate(nodePredicate).path(jqueryPath, nodeId);
                } else {
                    nodeId = nodePredicate.removeNameValuePredicate();
                    jqueryPath.add(nodeId);
                }
            }

            if (isList(nodeId) && ((pathPart.equals(PATH_PART.VARIABLE_PATH_PART) && !(i == segments.size() - 1))||pathPart.equals(PATH_PART.IDENTIFIER_PATH_PART)))
                jqueryPath.add(defaultIndex);
        }

        if (pathPart.equals(PATH_PART.VARIABLE_PATH_PART)) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = jqueryPath.size() - 1; i >= 0; i--) {
                if (jqueryPath.get(i).matches("[0-9]*|#") || jqueryPath.get(i).contains("[at"))
                    break;
                String item = jqueryPath.remove(i);
                stringBuilder.insert(0, item);
            }
            nodeId = EntryAttributeMapper.map(stringBuilder.toString());
            if (nodeId != null) {
                if (defaultIndex.equals("#")) { //jsquery
                    if (nodeId.contains(",")) {
                        String[] parts = nodeId.split(",");
                        jqueryPath.addAll(Arrays.asList(parts));
                    } else {
                        jqueryPath.add(nodeId);
                    }
                } else {
                    jqueryPath.add(nodeId);
                }
            }
        }

        //CHC 191018 EHR-163 '/value' for an ELEMENT will return a structure
        if (pathPart.equals(PATH_PART.VARIABLE_PATH_PART)) {
            jsonDataBlock = new JsonDataBlockCheck(jqueryPath).isJsonBlock();
        }

        return jqueryPath;
    }

    private int retrieveIndex(String nodeId) {
        if (nodeId.contains("#")) {
            return Integer.parseInt((nodeId.split("#")[1]).split("']")[0]);
        }
        return 0;
    }


    public Field<?> makeField(OTHER_ITEM type, String path, String alias, String variablePath, boolean withAlias) {
        List<String> itemPathArray = new ArrayList<>();

        if (path != null)
            itemPathArray.addAll(jqueryPath(PATH_PART.IDENTIFIER_PATH_PART, path, "0"));
        itemPathArray.addAll(jqueryPath(PATH_PART.VARIABLE_PATH_PART, variablePath, "0"));

        resolveArrayIndex(itemPathArray);

        String itemPath = StringUtils.join(itemPathArray.toArray(new String[]{}), ",");

        itemPath = wrapQuery(itemPath, type.equals(OTHER_ITEM.OTHER_DETAILS) ? JSONB_SELECTOR_EHR_OTHER_DETAILS_OPEN : JSONB_SELECTOR_EHR_OTHER_CONTEXT_OPEN, JSONB_SELECTOR_CLOSE);

        if (itemPathArray.get(itemPathArray.size() - 1).contains(MAGNITUDE)) { //force explicit type cast for DvQuantity
            itemPath = "(" + itemPath + ")::float";
        }

        Field<?> fieldPathItem;
        if (withAlias) {
            if (StringUtils.isNotEmpty(alias))
                fieldPathItem = DSL.field(itemPath, String.class).as(alias);
            else {
                String tempAlias = "FIELD_" + getSerial();
                fieldPathItem = DSL.field(itemPath, String.class).as(tempAlias);
            }
        } else
            fieldPathItem = DSL.field(itemPath, String.class);

        containsJqueryPath = true;
        return fieldPathItem;
    }


    @Override
    public Field<?> makeField(String templateId, String identifier, I_VariableDefinition variableDefinition, Clause clause) {

        boolean isRootContent = false; //that is a query path on a full composition starting from the root content

        if (pathResolver.entryRoot(templateId) == null) //case of (invalid) composition with null entry!
            return null;

        String path;
        if (variableDefinition.getPath() != null && variableDefinition.getPath().startsWith(CONTENT)) {
            path = "/" + variableDefinition.getPath();
            isRootContent = true;
        } else
            path = pathResolver.pathOf(templateId, variableDefinition.getIdentifier());

        String alias = clause.equals(Clause.WHERE) ? null : variableDefinition.getAlias();

        if (path == null) {
            //return a null field
            String cast = "";
            //force explicit type cast for DvQuantity
            if (variableDefinition.getPath() != null && variableDefinition.getPath().endsWith(MAGNITUDE))
                cast = "::numeric";

            if (alias != null)
                return DSL.field(DSL.val((String) null) + cast).as(variableDefinition.getAlias());
            else
                return DSL.field(DSL.val((String) null) + cast);
        }

        List<String> itemPathArray = new ArrayList<>();
        itemPathArray.add(pathResolver.entryRoot(templateId));
        if (!path.startsWith(TAG_COMPOSITION) && !isRootContent)
            itemPathArray.addAll(jqueryPath(PATH_PART.IDENTIFIER_PATH_PART, path, "0"));
        itemPathArray.addAll(jqueryPath(PATH_PART.VARIABLE_PATH_PART, variableDefinition.getPath(), "0"));

        //EHR-327: do not use array expression in WHERE clause
        if (clause.equals(Clause.SELECT)) {
            try {
                IterativeNode iterativeNode = new IterativeNode(domainAccess, templateId, introspectCache);
                Integer[] pos = iterativeNode.iterativeAt(itemPathArray);
                itemPathArray = iterativeNode.clipInIterativeMarker(itemPathArray, pos);
            } catch (Exception e) {
                ;
            }
        }

        resolveArrayIndex(itemPathArray);

        List<String> referenceItemPathArray = new ArrayList<>();
        referenceItemPathArray.addAll(itemPathArray);
        Collections.replaceAll(referenceItemPathArray, AQL_NODE_ITERATIVE_MARKER, "0");

        if (itemPathArray.contains(QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER))
            itemPathArray = new NodePredicateCall(itemPathArray).resolve();
        else if (itemPathArray.contains(AQL_NODE_ITERATIVE_MARKER)) {
            itemPathArray = new JsonbFunctionCall(itemPathArray, AQL_NODE_ITERATIVE_MARKER, QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION).resolve();
        }

        String itemPath = StringUtils.join(itemPathArray.toArray(new String[]{}), ",");

        if (!itemPath.startsWith(QueryImplConstants.AQL_NODE_NAME_PREDICATE_FUNCTION) && !itemPath.contains(QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION))
            itemPath = wrapQuery(itemPath, JSONB_SELECTOR_COMPOSITION_OPEN, JSONB_SELECTOR_CLOSE);

        //type casting from introspected data value type
        try {
            if (introspectCache == null)
                throw new IllegalArgumentException("MetaDataCache is not initialized");
            String reducedItemPathArray = new SegmentedPath(referenceItemPathArray).reduce();
            if (reducedItemPathArray != null && !reducedItemPathArray.isEmpty()) {
                ItemInfo info = introspectCache.getInfo(templateId, reducedItemPathArray);

                itemType = info.getItemType();
                itemCategory = info.getItemCategory();
                if (itemType != null) {
                    String pgType = new PGType(itemPathArray).forRmType(itemType);
                    if (pgType != null)
                        itemPath = "(" + itemPath + ")::" + pgType;
                }
            }
        } catch (Exception e) {
            if (!ignoreUnresolvedIntrospect)
                throw new IllegalArgumentException("Unresolved type, missing template?(" + templateId + "), reason:" + e);
            else
                logger.warn("Ignoring unresolved introspect (reason:" + e + ")");
        }
        if (itemPathArray.get(itemPathArray.size() - 1).contains(MAGNITUDE)) { //force explicit type cast for DvQuantity
            itemPath = "(" + itemPath + ")::numeric";
        }


        Field<?> fieldPathItem;
        if (clause.equals(Clause.SELECT)) {
            if (StringUtils.isNotEmpty(alias))
                fieldPathItem = DSL.field(itemPath, String.class).as(alias);
            else {
                String tempAlias = DefaultColumnId.value(variableDefinition);
                fieldPathItem = DSL.field(itemPath, String.class).as(tempAlias);
            }
        } else if (clause.equals(Clause.WHERE)) {
            fieldPathItem = DSL.field(itemPath, String.class);
            if (itemPathArray.contains(AQL_NODE_ITERATIVE_MARKER))
                fieldPathItem = DSL.field(DSL.select(fieldPathItem));
        }
        else
            throw new IllegalStateException("Unhandled clause:"+clause);


        containsJqueryPath = true;

        if (isJsonDataBlock()) {
            jsonbItemPath = toAqlPath(itemPathArray);
        }

        return fieldPathItem;
    }

    private String toAqlPath(List<String> itemPathArray) {
        List<String> aqlPath = new ArrayList<>();
        for (String path : itemPathArray) {
            if (!path.startsWith(TAG_COMPOSITION) && !path.matches("[0-9]*")) {
                aqlPath.add(path);
            }
        }
        return StringUtils.join(aqlPath.toArray(new String[]{}));
    }

    @Override
    public Field<?> whereField(String templateId, String identifier, I_VariableDefinition variableDefinition) {
        String path = pathResolver.pathOf(templateId, variableDefinition.getIdentifier());

        List<String> itemPathArray = new ArrayList<>();

        if (pathResolver.entryRoot(templateId) == null) {
            throw new IllegalArgumentException("a " + NAME + "/" + VALUE + " expression for " + COMPOSITION + " must be specified, where clause cannot be built without");
        }

        itemPathArray.add(pathResolver.entryRoot(templateId));
        if (path != null && !path.startsWith(TAG_COMPOSITION))
            itemPathArray.addAll(jqueryPath(PATH_PART.IDENTIFIER_PATH_PART, path, "#"));
        itemPathArray.addAll(jqueryPath(PATH_PART.VARIABLE_PATH_PART, variableDefinition.getPath(), "#"));

        StringBuilder jsqueryPath = new StringBuilder();

        for (int i = 0; i < itemPathArray.size(); i++) {
            if (!itemPathArray.get(i).equals("#") && !itemPathArray.get(i).equals("0"))
                jsqueryPath.append("\"").append(itemPathArray.get(i)).append("\"");
            else if (itemPathArray.get(i).equals("0")){ //case /name/value -> /name,0,value
                jsqueryPath.append("#");
            }
            else
                jsqueryPath.append(itemPathArray.get(i));
            if (i < itemPathArray.size() - 1)
                jsqueryPath.append(".");
        }

        Field<?> fieldPathItem = DSL.field(jsqueryPath.toString(), String.class);

        containsJqueryPath = true;
        return fieldPathItem;
    }

    private void resolveArrayIndex(List<String> itemPathArray) {

        for (int i = 0; i < itemPathArray.size(); i++) {
            String nodeId = itemPathArray.get(i);
            if (nodeId.contains("#")) {
                int index = retrieveIndex(nodeId);
                //change the default index of the previous one
                if (i - 1 >= 0) {
                    itemPathArray.set(i - 1, Integer.toString(index));
                }

                itemPathArray.set(i, nodeId);
            }
        }
    }


    private static String wrapQuery(String itemPath, String open, String close) {
        if (itemPath.contains("/item_count")) {
            //trim the last array index in the prefix
            //look ahead for an index expression: ','<nnn>','
            String[] segments = itemPath.split("(?=(,[0-9]*,))");
            //trim the last index expression
            String pathPart = StringUtils.join(ArrayUtils.subarray(segments, 0, segments.length - 1));
            return QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION+ "(" + CONTENT + " #> '{" + pathPart + "}')";
        } else
            return open + itemPath + close;

    }

    @Override
    public boolean isContainsJqueryPath() {
        return containsJqueryPath;
    }


    @Override
    public String getJsonbItemPath() {
        return jsonbItemPath;
    }
}
