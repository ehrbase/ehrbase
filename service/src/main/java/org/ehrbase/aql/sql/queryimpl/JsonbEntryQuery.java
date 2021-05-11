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
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.serialisation.dbencoding.CompositionSerializer;
import org.ehrbase.service.IntrospectService;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_ITERATIVE_MARKER;
import static org.ehrbase.jooq.pg.Tables.*;

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

//    private String jsonbItemPath;

    public static final String TAG_ACTIVITIES = "/" + ACTIVITIES;
    public static final String TAG_EVENTS = "/" + EVENTS;
    public static final String TAG_COMPOSITION = "/" + COMPOSITION;
    public static final String TAG_CONTENT = "/" + CONTENT;
    public static final String TAG_ITEMS = "/" + ITEMS;

//    private boolean containsJqueryPath = false; //true if at leas one AQL path is contained in expression
    private boolean ignoreUnresolvedIntrospect = false;

    private static String ENV_IGNORE_UNRESOLVED_INTROSPECT = "aql.ignoreUnresolvedIntrospect";

    private IntrospectService introspectCache;

    public JsonbEntryQuery(I_DomainAccess domainAccess, IntrospectService introspectCache, PathResolver pathResolver) {
        super(domainAccess, pathResolver);
        this.introspectCache = introspectCache;
        ignoreUnresolvedIntrospect = Boolean.parseBoolean(System.getProperty(ENV_IGNORE_UNRESOLVED_INTROSPECT, "false"));
    }

    public enum PATH_PART {IDENTIFIER_PATH_PART, VARIABLE_PATH_PART}

    public enum OTHER_ITEM {OTHER_DETAILS, OTHER_CONTEXT}


    private int retrieveIndex(String nodeId) {
        if (nodeId.contains("#")) {
            return Integer.parseInt((nodeId.split("#")[1]).split("']")[0]);
        }
        return 0;
    }

    @Override
    public MultiFields makeField(String templateId, String identifier, I_VariableDefinition variableDefinition, Clause clause) {
        boolean setReturningFunctionInWhere = false; //if true, use a subselect
        boolean isRootContent = false; //that is a query path on a full composition starting from the root content
        DataType castTypeAs = null;

        if (pathResolver.entryRoot(templateId) == null) //case of (invalid) composition with null entry!
            return null;

        Set<String> pathSet;
        if (variableDefinition.getPath() != null && variableDefinition.getPath().startsWith(CONTENT)) {
            pathSet = new MultiPath().asSet("/" + variableDefinition.getPath());
            isRootContent = true;
        } else
            //TODO: create multiple fields!
            pathSet = pathResolver.pathOf(templateId, variableDefinition.getIdentifier());

        String alias = clause.equals(Clause.WHERE) ? null : variableDefinition.getAlias();

        if (pathSet.isEmpty()) {
            return new MultiFields(variableDefinition, new NullField(variableDefinition, alias).instance(), templateId);
        }

        //traverse the set of paths and create the corresponding fields
        List<QualifiedAqlField> fieldList = new ArrayList<>();

        for (String path: pathSet) {
            List<String> itemPathArray = new ArrayList<>();
            itemPathArray.add(pathResolver.entryRoot(templateId));

            if (!path.startsWith(TAG_COMPOSITION) && !isRootContent)
                itemPathArray.addAll(new JqueryPath(PATH_PART.IDENTIFIER_PATH_PART, path, "0").evaluate());

            JqueryPath jqueryPath = new JqueryPath(PATH_PART.VARIABLE_PATH_PART, variableDefinition.getPath(), "0");
            itemPathArray.addAll(jqueryPath.evaluate());

            try {
                IterativeNode iterativeNode = new IterativeNode(domainAccess, templateId, introspectCache);
                Integer[] pos = iterativeNode.iterativeAt(itemPathArray);
                itemPathArray = iterativeNode.clipInIterativeMarker(itemPathArray, pos);
                if (clause.equals(Clause.WHERE))
                    setReturningFunctionInWhere = true;
            } catch (Exception e) {
                ;
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


            DataTypeFromTemplate dataTypeFromTemplate = new DataTypeFromTemplate(introspectCache, ignoreUnresolvedIntrospect);

            dataTypeFromTemplate.evaluate(templateId, referenceItemPathArray);

            if (!jqueryPath.isJsonDataBlock())
                castTypeAs = dataTypeFromTemplate.getIdentifiedType();

            Field<?> fieldPathItem;
            if (clause.equals(Clause.SELECT)) {
                if (StringUtils.isNotEmpty(alias))
                    fieldPathItem = buildFieldWithCast(itemPath, castTypeAs, alias);
                else {
                    String tempAlias = DefaultColumnId.value(variableDefinition);
                    fieldPathItem = buildFieldWithCast(itemPath, castTypeAs, tempAlias);
                }
            } else if (clause.equals(Clause.WHERE)) {
                fieldPathItem = buildFieldWithCast(itemPath, castTypeAs, null);
                ;
                if (itemPathArray.contains(AQL_NODE_ITERATIVE_MARKER))
                    fieldPathItem = DSL.field(DSL.select(fieldPathItem));
            } else
                throw new IllegalStateException("Unhandled clause:" + clause);

//            if (jqueryPath.isJsonDataBlock()) {
//                jsonbItemPath = toAqlPath(itemPathArray);
//            }

            if (setReturningFunctionInWhere)
                fieldPathItem = DSL.select(fieldPathItem).asField();

            QualifiedAqlField aqlField = new QualifiedAqlField(fieldPathItem,
                                                dataTypeFromTemplate.getItemType(),
                                                dataTypeFromTemplate.getItemCategory(),
                                                jqueryPath.isJsonDataBlock(),
                                true,
                                                toAqlPath(itemPathArray));

            fieldList.add(aqlField);
        }

        return new MultiFields(variableDefinition, fieldList, templateId);
    }

    private Field<?> buildFieldWithCast(String itemPath, DataType castTypeAs, String alias){
        Field fieldPathItem;

        if (castTypeAs != null) {
            fieldPathItem = DSL.field(itemPath, String.class).cast(castTypeAs).as(alias);
        }
        else {
            fieldPathItem = DSL.field(itemPath, String.class).as(alias);
        }

        if (alias != null)
            fieldPathItem = fieldPathItem.as(alias);

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
    public MultiFields whereField(String templateId, String identifier, I_VariableDefinition variableDefinition) {
        //TODO: create multiple fields!
        Set<String> pathSet = pathResolver.pathOf(templateId, variableDefinition.getIdentifier());

        //traverse the set of paths and create the corresponding fields
        List<QualifiedAqlField> fieldList = new ArrayList<>();

        for (String path: pathSet) {
            List<String> itemPathArray = new ArrayList<>();

            if (pathResolver.entryRoot(templateId) == null) {
                throw new IllegalArgumentException("a " + NAME + "/" + VALUE + " expression for " + COMPOSITION + " must be specified, where clause cannot be built without");
            }

            itemPathArray.add(pathResolver.entryRoot(templateId));
            if (path != null && !path.startsWith(TAG_COMPOSITION))
                itemPathArray.addAll(new JqueryPath(PATH_PART.IDENTIFIER_PATH_PART, path, "#").evaluate());
            JqueryPath jqueryPath = new JqueryPath(PATH_PART.VARIABLE_PATH_PART, variableDefinition.getPath(), "#");
            itemPathArray.addAll(jqueryPath.evaluate());

            StringBuilder jsqueryPath = new StringBuilder();

            for (int i = 0; i < itemPathArray.size(); i++) {
                if (!itemPathArray.get(i).equals("#") && !itemPathArray.get(i).equals("0"))
                    jsqueryPath.append("\"").append(itemPathArray.get(i)).append("\"");
                else if (itemPathArray.get(i).equals("0")) { //case /name/value -> /name,0,value
                    jsqueryPath.append("#");
                } else
                    jsqueryPath.append(itemPathArray.get(i));
                if (i < itemPathArray.size() - 1)
                    jsqueryPath.append(".");
            }

            Field<?> fieldPathItem = DSL.field(jsqueryPath.toString(), String.class);
            QualifiedAqlField qualifiedAqlField = new QualifiedAqlField(fieldPathItem);
            qualifiedAqlField.setContainsJqueryPath(true);
            fieldList.add(qualifiedAqlField);
        }
        return new MultiFields(variableDefinition, fieldList, templateId);
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
}
