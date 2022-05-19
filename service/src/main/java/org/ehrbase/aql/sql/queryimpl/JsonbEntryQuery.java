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
package org.ehrbase.aql.sql.queryimpl;

import static org.ehrbase.aql.sql.queryimpl.QueryImplConstants.AQL_NODE_ITERATIVE_MARKER;
import static org.ehrbase.jooq.pg.Tables.ENTRY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.service.IntrospectService;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate an SQL field corresponding to a JSONB data value query
 * Created by christian on 5/6/2016.
 */
@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452", "java:S1075"})
public class JsonbEntryQuery extends ObjectQuery implements IQueryImpl {

    public static final String MAGNITUDE = "magnitude";
    Logger logger = LoggerFactory.getLogger(JsonbEntryQuery.class);

    private static final String JSONB_PATH_SELECTOR_EXPR = " #>> '{";
    private static final String JSONB_AT_AT_SELECTOR_EXPR = " @@ '";
    private static final String JSONB_SELECTOR_COMPOSITION_OPEN = ENTRY.ENTRY_ + JSONB_PATH_SELECTOR_EXPR;
    public static final String JSQUERY_COMPOSITION_OPEN = ENTRY.ENTRY_ + JSONB_AT_AT_SELECTOR_EXPR;

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

    // Generic stuff
    private static final String JSONB_SELECTOR_CLOSE = "}'";
    public static final String JSQUERY_CLOSE = " '::jsquery";

    public static final String TAG_ACTIVITIES = "/" + ACTIVITIES;
    public static final String TAG_EVENTS = "/" + EVENTS;
    public static final String TAG_COMPOSITION = "/" + COMPOSITION;
    public static final String TAG_CONTENT = "/" + CONTENT;
    public static final String TAG_ITEMS = "/" + ITEMS;

    private static final String[] listIdentifier = {TAG_CONTENT, TAG_ITEMS, TAG_ACTIVITIES, TAG_EVENTS};

    private boolean ignoreUnresolvedIntrospect = false;

    private static String ENV_IGNORE_UNRESOLVED_INTROSPECT = "aql.ignoreUnresolvedIntrospect";

    private IntrospectService introspectCache;

    public JsonbEntryQuery(I_DomainAccess domainAccess, IntrospectService introspectCache, PathResolver pathResolver) {
        super(domainAccess, pathResolver);
        this.introspectCache = introspectCache;
        ignoreUnresolvedIntrospect =
                Boolean.parseBoolean(System.getProperty(ENV_IGNORE_UNRESOLVED_INTROSPECT, "false"));
    }

    public enum PATH_PART {
        IDENTIFIER_PATH_PART,
        VARIABLE_PATH_PART
    }

    public enum OTHER_ITEM {
        OTHER_DETAILS,
        OTHER_CONTEXT
    }

    private int retrieveIndex(String nodeId) {
        if (nodeId.contains("#")) {
            return Integer.parseInt((nodeId.split("#")[1]).split("']")[0]);
        }
        return 0;
    }

    @Override
    public MultiFields makeField(
            String templateId, String identifier, I_VariableDefinition variableDefinition, Clause clause) {
        boolean setReturningFunctionInWhere = false; // if true, use a subselect
        boolean isRootContent = false; // that is a query path on a full composition starting from the root content
        DataType castTypeAs = null;

        if (pathResolver.entryRoot(templateId) == null) // case of (invalid) composition with null entry!
        return null;

        Set<String> pathSet;
        if (variableDefinition.getPath() != null && variableDefinition.getPath().startsWith(CONTENT)) {
            pathSet = new MultiPath().asSet("/" + variableDefinition.getPath());
            isRootContent = true;
        } else pathSet = pathResolver.pathOf(templateId, variableDefinition.getIdentifier());

        String alias = clause.equals(Clause.WHERE) ? null : variableDefinition.getAlias();

        if (pathSet == null || pathSet.isEmpty()) {
            return MultiFields.asNull(variableDefinition, templateId, clause);
        }

        // traverse the set of paths and create the corresponding fields
        List<QualifiedAqlField> fieldList = new ArrayList<>();

        for (String path : pathSet) {
            List<String> itemPathArray = new ArrayList<>();
            itemPathArray.add(pathResolver.entryRoot(templateId));

            if (!path.startsWith(TAG_COMPOSITION) && !isRootContent)
                itemPathArray.addAll(new JqueryPath(PATH_PART.IDENTIFIER_PATH_PART, path, "0").evaluate());

            JqueryPath jqueryPath = new JqueryPath(PATH_PART.VARIABLE_PATH_PART, variableDefinition.getPath(), "0");
            itemPathArray.addAll(new NormalizedRmAttributePath(jqueryPath.evaluate()).transformStartingAt(1));

            try {
                IterativeNode iterativeNode = new IterativeNode(domainAccess, templateId, introspectCache);
                Integer[] pos = iterativeNode.iterativeAt(itemPathArray);
                itemPathArray = iterativeNode.clipInIterativeMarker(itemPathArray, pos);
                if (clause.equals(Clause.WHERE)) setReturningFunctionInWhere = true;
            } catch (Exception e) {
                // do nothing
            }

            resolveArrayIndex(itemPathArray);

            List<String> referenceItemPathArray = new ArrayList<>();
            referenceItemPathArray.addAll(itemPathArray);
            Collections.replaceAll(referenceItemPathArray, AQL_NODE_ITERATIVE_MARKER, "0");

            if (itemPathArray.contains(QueryImplConstants.AQL_NODE_NAME_PREDICATE_MARKER))
                itemPathArray = new NodePredicateCall(itemPathArray).resolve();
            else if (itemPathArray.contains(AQL_NODE_ITERATIVE_MARKER)) {
                itemPathArray = new JsonbFunctionCall(
                                itemPathArray,
                                AQL_NODE_ITERATIVE_MARKER,
                                QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION)
                        .resolve();
            }

            String itemPath = StringUtils.join(itemPathArray.toArray(new String[] {}), ",");

            if (!itemPath.startsWith(QueryImplConstants.AQL_NODE_NAME_PREDICATE_FUNCTION)
                    && !itemPath.contains(QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION))
                itemPath = wrapQuery(itemPath, JSONB_SELECTOR_COMPOSITION_OPEN, JSONB_SELECTOR_CLOSE);

            DataTypeFromTemplate dataTypeFromTemplate =
                    new DataTypeFromTemplate(introspectCache, ignoreUnresolvedIntrospect, clause);

            dataTypeFromTemplate.evaluate(templateId, referenceItemPathArray);

            castTypeAs = dataTypeFromTemplate.getIdentifiedType();

            Field<?> fieldPathItem;
            if (clause.equals(Clause.SELECT)) {
                // set the determined type with the variable
                variableDefinition.setSelectType(castTypeAs);

                if (StringUtils.isNotEmpty(alias)) fieldPathItem = buildFieldWithCast(itemPath, castTypeAs, alias);
                else {
                    String tempAlias = DefaultColumnId.value(variableDefinition);
                    fieldPathItem = buildFieldWithCast(itemPath, castTypeAs, tempAlias);
                }
            } else if (clause.equals(Clause.WHERE)) {
                fieldPathItem = buildFieldWithCast(itemPath, castTypeAs, null);
                if (itemPathArray.contains(AQL_NODE_ITERATIVE_MARKER))
                    fieldPathItem = DSL.field(DSL.select(fieldPathItem));
            } else throw new IllegalStateException("Unhandled clause:" + clause);

            if (setReturningFunctionInWhere)
                fieldPathItem = DSL.select(fieldPathItem).asField();

            QualifiedAqlField aqlField = new QualifiedAqlField(
                    fieldPathItem, dataTypeFromTemplate.getItemType(), dataTypeFromTemplate.getItemCategory());

            fieldList.add(aqlField);
        }

        return new MultiFields(variableDefinition, fieldList, templateId);
    }

    private Field<?> buildFieldWithCast(String itemPath, DataType castTypeAs, String alias) {
        Field fieldPathItem;

        if (castTypeAs != null) {
            fieldPathItem = DSL.field(itemPath, String.class).cast(castTypeAs).as(alias);
        } else {
            fieldPathItem = DSL.field(itemPath, String.class).as(alias);
        }

        if (alias != null) fieldPathItem = fieldPathItem.as(alias);

        return fieldPathItem;
    }

    @Override
    public MultiFields whereField(String templateId, String identifier, I_VariableDefinition variableDefinition) {
        Set<String> pathSet = pathResolver.pathOf(templateId, variableDefinition.getIdentifier());

        // traverse the set of paths and create the corresponding fields
        List<QualifiedAqlField> fieldList = new ArrayList<>();

        for (String path : pathSet) {
            List<String> itemPathArray = new ArrayList<>();

            if (pathResolver.entryRoot(templateId) == null) {
                throw new IllegalArgumentException("a " + NAME + "/" + VALUE + " expression for " + COMPOSITION
                        + " must be specified, where clause cannot be built without");
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
                else if (itemPathArray.get(i).equals("0")) { // case /name/value -> /name,0,value
                    jsqueryPath.append("#");
                } else jsqueryPath.append(itemPathArray.get(i));
                if (i < itemPathArray.size() - 1) jsqueryPath.append(".");
            }

            Field<?> fieldPathItem = DSL.field(jsqueryPath.toString(), String.class);
            QualifiedAqlField qualifiedAqlField = new QualifiedAqlField(fieldPathItem);
            fieldList.add(qualifiedAqlField);
        }
        return new MultiFields(variableDefinition, fieldList, templateId);
    }

    private void resolveArrayIndex(List<String> itemPathArray) {

        for (int i = 0; i < itemPathArray.size(); i++) {
            String nodeId = itemPathArray.get(i);
            if (nodeId.contains("#")) {
                int index = retrieveIndex(nodeId);
                // change the default index of the previous one
                if (i - 1 >= 0) {
                    itemPathArray.set(i - 1, Integer.toString(index));
                }

                itemPathArray.set(i, nodeId);
            }
        }
    }

    private static String wrapQuery(String itemPath, String open, String close) {
        if (itemPath.contains("/item_count")) {
            // trim the last array index in the prefix
            // look ahead for an index expression: ','<nnn>','
            String[] segments = itemPath.split("(?=(,[0-9]*,))");
            // trim the last index expression
            String pathPart = StringUtils.join(ArrayUtils.subarray(segments, 0, segments.length - 1));
            return QueryImplConstants.AQL_NODE_ITERATIVE_FUNCTION + "(" + CONTENT + " #> '{" + pathPart + "}')";
        } else return open + itemPath + close;
    }
}
