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
package org.ehrbase.aql.sql.binding;

import java.util.*;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.LateralJoinDefinition;
import org.ehrbase.aql.definition.LateralVariable;
import org.ehrbase.aql.definition.VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.queryimpl.*;
import org.ehrbase.aql.sql.queryimpl.value_field.ISODateTime;
import org.jooq.*;
import org.jooq.impl.DSL;

/**
 * Bind the abstract WHERE clause parameters into a SQL expression
 * Created by christian on 5/20/2016.
 */
// ignore cognitive complexity flag as this code as evolved historically and adjusted depending on use cases
@SuppressWarnings({"java:S3776", "java:S135"})
public class WhereBinder {

    private static final String VALUETYPE_EXPR_VALUE = "/value,value";
    public static final String EXISTS = "EXISTS";
    public static final String MATCHES = "MATCHES";
    public static final String NOT = "NOT";
    public static final String IS = "IS";
    public static final String TRUE = "TRUE";
    public static final String FALSE = "FALSE";
    public static final String NULL = "NULL";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String DISTINCT = "DISTINCT";
    public static final String FROM = "FROM";
    public static final String BETWEEN = "BETWEEN";

    // from AQL grammar
    private static final Set<String> SQL_OPERATORS = Set.of(
            "=", "!=", ">", ">=", "<", "<=", MATCHES, EXISTS, NOT, IS, TRUE, FALSE, NULL, UNKNOWN, DISTINCT, FROM,
            BETWEEN, "(", ")", "{", "}");
    /**
     * list of subquery and operators
     */
    private static final Pattern SQL_CONDITIONAL_FUNCTIONAL_OPERATOR_PATTERN =
            Pattern.compile("(?i)(like|ilike|substr|in|not in)");

    public static final String OR = "OR";
    public static final String XOR = "XOR";
    public static final String AND = "AND";
    public static final String IN = "IN";
    public static final String ANY = "ANY";
    public static final String SOME = "SOME";
    public static final String ALL = "ALL";

    enum ExistsMode {
        NOT_EXISTS,
        EXISTS,
        UNSET
    }

    private final List<Object> whereClause;
    private boolean requiresJSQueryClosure = false;
    private boolean isFollowedBySQLConditionalOperator = false;

    private final WhereVariable whereVariable;

    public WhereBinder(List<Object> whereClause, PathResolver pathResolver) {
        this.whereClause = whereClause;
        whereVariable = new WhereVariable(pathResolver);
    }

    private TaggedStringBuilder buildWhereCondition(
            ExistsMode existsMode,
            int whereCursor,
            MultiFieldsMap multiFieldsMap,
            int selectCursor,
            MultiFieldsMap multiSelectFieldsMap,
            TaggedStringBuilder taggedBuffer,
            List<Object> item)
            throws UnknownVariableException {
        for (Object part : item) {
            if (part instanceof String) taggedBuffer.append((String) part);
            else if (part instanceof VariableDefinition) {
                // substitute the identifier
                TaggedStringBuilder taggedStringBuilder = whereVariable.encodeWhereVariable(
                        existsMode,
                        isFollowedBySQLConditionalOperator,
                        whereCursor,
                        multiFieldsMap,
                        selectCursor,
                        multiSelectFieldsMap,
                        (VariableDefinition) part,
                        null);
                isFollowedBySQLConditionalOperator = whereVariable.isFollowedBySQLConditionalOperator();

                if (taggedStringBuilder != null) {
                    taggedBuffer.append(taggedStringBuilder.toString());
                    taggedBuffer.setTagField(taggedStringBuilder.getTagField());
                }
            } else if (part instanceof List) {
                TaggedStringBuilder taggedStringBuilder = buildWhereCondition(
                        existsMode,
                        whereCursor,
                        multiFieldsMap,
                        selectCursor,
                        multiSelectFieldsMap,
                        taggedBuffer,
                        (List) part);
                taggedBuffer.append(taggedStringBuilder.toString());
                taggedBuffer.setTagField(taggedStringBuilder.getTagField());
            }
        }
        return taggedBuffer;
    }

    public Condition bind(
            String templateId,
            int whereCursor,
            MultiFieldsMap multiWhereFieldsMap,
            int selectCursor,
            MultiFieldsMap multiSelectFieldsMap)
            throws UnknownVariableException {

        boolean unresolvedVariable = false;

        if (whereClause.isEmpty()) return null;

        TaggedStringBuilder taggedBuffer = new TaggedStringBuilder();

        // work on a copy since Exist is destructive
        List<Object> whereItems = new ArrayList<>(whereClause);
        boolean notExists = false;
        boolean inSubqueryOperator = false;
        ExistsMode inExists = ExistsMode.UNSET;

        for (int cursor = 0; cursor < whereItems.size(); cursor++) {
            Object item = whereItems.get(cursor);
            if (item instanceof String) {
                switch (((String) item).trim().toUpperCase()) {
                    case OR:
                    case XOR:
                    case AND:
                        taggedBuffer = new WhereJsQueryExpression(
                                        taggedBuffer, requiresJSQueryClosure, isFollowedBySQLConditionalOperator)
                                .closure();
                        taggedBuffer.append(" " + item + " ");
                        break;

                    case NOT:
                        // check if precedes 'EXISTS'
                        if (whereItems.get(cursor + 1).toString().equalsIgnoreCase(EXISTS)) {
                            notExists = true;
                            inExists = ExistsMode.NOT_EXISTS;
                        } else {
                            taggedBuffer = new WhereJsQueryExpression(
                                            taggedBuffer, requiresJSQueryClosure, isFollowedBySQLConditionalOperator)
                                    .closure();
                            taggedBuffer.append(" " + item + " ");
                        }
                        break;

                    case IN:
                    case ANY:
                    case SOME:
                    case ALL:
                        if (((String) item).trim().toUpperCase().matches("ANY|SOME|ALL")) inSubqueryOperator = true;
                        taggedBuffer.append((String) item);
                        break;

                    case EXISTS:
                        // add the comparison to null after the variable expression
                        whereItems.add(cursor + 2, notExists ? "IS " : "IS NOT ");
                        whereItems.add(cursor + 3, "NULL");
                        notExists = false;
                        if (inExists.equals(ExistsMode.UNSET)) inExists = ExistsMode.EXISTS;
                        break;

                    default:
                        ISODateTime isoDateTime = new ISODateTime(((String) item).replace("'", ""));
                        if (isoDateTime.isValidDateTimeExpression()) {
                            long timestamp = isoDateTime.toTimeStamp()
                                    / 1000; // this to align with epoch_offset in the DB, ignore ms
                            int lastValuePos = taggedBuffer.lastIndexOf(VALUETYPE_EXPR_VALUE);
                            if (lastValuePos > 0) {
                                taggedBuffer.replaceLast(VALUETYPE_EXPR_VALUE, "/value,epoch_offset");
                            }
                            isFollowedBySQLConditionalOperator = true;
                            item = hackItem(taggedBuffer, Long.toString(timestamp), "numeric");
                            taggedBuffer.append((String) item);
                        } else {
                            item = hackItem(taggedBuffer, (String) item, null);
                            taggedBuffer.append((String) item);
                        }
                        break;
                }
            } else if (item instanceof Long) {
                item = hackItem(taggedBuffer, item.toString(), null);
                taggedBuffer.append(item.toString());
            } else if (item instanceof I_VariableDefinition) {
                // look ahead and check if followed by a sql operator
                TaggedStringBuilder taggedStringBuilder = new TaggedStringBuilder();
                if (isFollowedBySQLConditionalOperator(cursor)) {
                    TaggedStringBuilder encodedVar = whereVariable.encodeWhereVariable(
                            inExists,
                            isFollowedBySQLConditionalOperator,
                            whereCursor,
                            multiWhereFieldsMap,
                            selectCursor,
                            multiSelectFieldsMap,
                            (I_VariableDefinition) item,
                            null);
                    isFollowedBySQLConditionalOperator = whereVariable.isFollowedBySQLConditionalOperator();
                    inExists = whereVariable.inExists();

                    String expanded =
                            expandForLateral(templateId, encodedVar, (I_VariableDefinition) item, multiSelectFieldsMap);

                    if (StringUtils.isNotBlank(expanded)) taggedStringBuilder.append(expanded);
                    else {
                        unresolvedVariable = true;
                        break;
                    }
                } else {
                    // if the path contains node predicate expression uses a SQL syntax instead of jsquery
                    if (new VariablePath(((I_VariableDefinition) item).getPath()).hasPredicate()) {
                        TaggedStringBuilder variableSQL = whereVariable.encodeWhereVariable(
                                inExists,
                                isFollowedBySQLConditionalOperator,
                                whereCursor,
                                multiWhereFieldsMap,
                                selectCursor,
                                multiSelectFieldsMap,
                                (I_VariableDefinition) item,
                                null);
                        isFollowedBySQLConditionalOperator = whereVariable.isFollowedBySQLConditionalOperator();
                        inExists = whereVariable.inExists();

                        String expanded = expandForLateral(
                                templateId, variableSQL, (I_VariableDefinition) item, multiSelectFieldsMap);
                        if (StringUtils.isNotBlank(expanded)) {
                            taggedStringBuilder.append(encodeForSubquery(expanded, inSubqueryOperator));
                            inSubqueryOperator = false;
                        } else {
                            unresolvedVariable = true;
                            break;
                        }
                        isFollowedBySQLConditionalOperator = true;
                        requiresJSQueryClosure = false;
                    } else {
                        // use select variable path used for this selectCursor if applicable!
                        String expanded = expandForLateral(
                                templateId,
                                whereVariable.encodeWhereVariable(
                                        inExists,
                                        isFollowedBySQLConditionalOperator,
                                        whereCursor,
                                        multiWhereFieldsMap,
                                        selectCursor,
                                        multiSelectFieldsMap,
                                        (I_VariableDefinition) item,
                                        null),
                                (I_VariableDefinition) item,
                                multiSelectFieldsMap);
                        isFollowedBySQLConditionalOperator = whereVariable.isFollowedBySQLConditionalOperator();
                        inExists = whereVariable.inExists();

                        if (StringUtils.isNotBlank(expanded)) {
                            taggedStringBuilder.append(encodeForSubquery(expanded, inSubqueryOperator));
                            inSubqueryOperator = false;
                        } else {
                            unresolvedVariable = true;
                            break;
                        }
                    }
                }

                taggedBuffer.append(taggedStringBuilder.toString());
                taggedBuffer.setTagField(taggedStringBuilder.getTagField());

            } else if (item instanceof List) {
                TaggedStringBuilder taggedStringBuilder = buildWhereCondition(
                        inExists,
                        whereCursor,
                        multiWhereFieldsMap,
                        selectCursor,
                        multiSelectFieldsMap,
                        taggedBuffer,
                        (List) item);
                taggedBuffer.append(taggedStringBuilder.toString());
                taggedBuffer.setTagField(taggedStringBuilder.getTagField());
            }
        }

        if (!unresolvedVariable) {
            taggedBuffer = new WhereJsQueryExpression(
                            taggedBuffer, requiresJSQueryClosure, isFollowedBySQLConditionalOperator)
                    .closure(); // termination

            return DSL.condition(taggedBuffer.toString());
        } else return DSL.falseCondition();
    }

    private String encodeForSubquery(String sqlExpression, boolean inSubqueryOperator) {
        if (inSubqueryOperator) return "(SELECT " + sqlExpression + ")";
        else return sqlExpression;
    }

    // look ahead for a SQL operator
    private boolean isFollowedBySQLConditionalOperator(int cursor) {
        if (cursor < whereClause.size() - 1) {
            Object nextToken = whereClause.get(cursor + 1);
            if (nextToken instanceof String
                    && SQL_CONDITIONAL_FUNCTIONAL_OPERATOR_PATTERN
                            .matcher(((String) nextToken).trim())
                            .matches()) {
                isFollowedBySQLConditionalOperator = true;
                return true;
            }
        }
        isFollowedBySQLConditionalOperator = false;
        return false;
    }

    // look ahead for a SQL operator
    private String compositionNameValue(String symbol) {

        String token = null;
        int lcursor; // skip the current variable

        for (lcursor = 0; lcursor < whereClause.size() - 1; lcursor++) {
            if (whereClause.get(lcursor) instanceof VariableDefinition
                    && (((VariableDefinition) whereClause.get(lcursor))
                            .getIdentifier()
                            .equals(symbol))
                    && (((VariableDefinition) whereClause.get(lcursor))
                            .getPath()
                            .equals("name/value"))) {
                Object nextToken = whereClause.get(lcursor + 1); // check operator
                if (nextToken instanceof String && !nextToken.equals("="))
                    throw new IllegalArgumentException("name/value for CompositionAttribute must be an equality");
                nextToken = whereClause.get(lcursor + 2);
                if (nextToken instanceof String) {
                    token = (String) nextToken;
                    break;
                }
            }
        }

        return token;
    }

    // do some temporary hacking for unsupported features
    private Object hackItem(TaggedStringBuilder taggedBuffer, String item, String castAs) {
        // this deals with post type casting required f.e. for date comparisons with epoch_offset
        if (castAs != null) {
            if (isFollowedBySQLConditionalOperator) { // at the moment, only for epoch_offset
                int variableClosure = taggedBuffer.lastIndexOf("}'");
                if (variableClosure > 0) {
                    int variableInitial = taggedBuffer.lastIndexOf("\"ehr\".\"entry\".\"entry\" #>>");
                    if (variableInitial >= 0 && variableInitial < variableClosure) {
                        taggedBuffer.insert(variableClosure + "}'".length(), ")::" + castAs);
                        taggedBuffer.insert(variableInitial, "(");
                    }
                }
            }
            return item;
        }

        if (SQL_OPERATORS.contains(item.toUpperCase())) return " " + item + " ";
        if (taggedBuffer.toString().contains(JoinBinder.COMPOSITION_JOIN) && item.contains("::"))
            return item.split("::")[0] + "'";
        if (requiresJSQueryClosure
                && !isFollowedBySQLConditionalOperator
                && taggedBuffer.indexOf("#") > 0
                && item.contains("'")) { // conventionally double quote for jsquery
            return item.replace("'", "\"");
        }
        return item;
    }

    private String expandForCondition(TaggedStringBuilder taggedStringBuilder) {

        if (taggedStringBuilder == null) return null;

        // perform the condition query wrapping depending on the dialect jsquery or sql
        String wrapped;

        switch (taggedStringBuilder.getTagField()) {
            case JSQUERY:
                wrapped = JsonbEntryQuery.JSQUERY_COMPOSITION_OPEN + taggedStringBuilder;
                requiresJSQueryClosure = true;
                break;
            case SQLQUERY:
                wrapped = taggedStringBuilder.toString();
                break;

            default:
                throw new IllegalArgumentException("Uninitialized tag passed in query expression");
        }

        return wrapped;
    }

    private String expandForLateral(
            String templateId,
            TaggedStringBuilder encodedVar,
            I_VariableDefinition variableDefinition,
            MultiFieldsMap multiSelectFieldsMap) {
        String expanded = expandForCondition(encodedVar);
        final boolean isAlreadyCast;

        if (SetReturningFunction.isUsed(expanded)) {
            isAlreadyCast = false;
            // check if this variable is already defined as a lateral join from the projection (SELECT)
            MultiFields selectFields =
                    multiSelectFieldsMap.get(variableDefinition.getIdentifier(), variableDefinition.getPath());

            if (selectFields != null
                    && selectFields.getVariableDefinition().getLateralJoinDefinitions(templateId) != null) {
                // TODO: get the matching lateral join... not the LAST! XXX??
                LateralJoinDefinition lateralJoinDefinition =
                        reconciliateWithAliasedTable(expanded, selectFields.getVariableDefinition(), templateId);
                if (lateralJoinDefinition == null) return null;
                variableDefinition.setLateralJoinTable(templateId, lateralJoinDefinition);
                // NB: white space at the end is required since the clause is built with a string builder and space(s)
                // is important!
                variableDefinition.setAlias(new LateralVariable(
                                lateralJoinDefinition.getTable().getName(), lateralJoinDefinition.getLateralVariable())
                        .alias());
            } else {
                // check for an existing lateral join for this template
                multiSelectFieldsMap
                        .matchingLateralJoin(templateId, expanded)
                        .ifPresentOrElse(
                                lj -> LateralJoins.reuse(lj, templateId, variableDefinition),
                                () -> LateralJoins.create(
                                        templateId, encodedVar, variableDefinition, IQueryImpl.Clause.WHERE));
            }

            expanded = variableDefinition.getAlias();
        } else {
            isAlreadyCast = true;
        }

        if (whereVariable.hasRightMostJsonbExpression())
            expanded = expanded + whereVariable.getRightMostJsonbExpression();

        if (variableDefinition.getSelectType() != null && !isAlreadyCast) {
            expanded =
                    "(" + expanded + ")::" + variableDefinition.getSelectType().getCastTypeName() + " ";
        }

        return expanded;
    }

    private LateralJoinDefinition reconciliateWithAliasedTable(
            String expanded, I_VariableDefinition variableDefinition, String templateId) {

        Set<LateralJoinDefinition> definedLateralJoins = variableDefinition.getLateralJoinDefinitions(templateId);

        for (LateralJoinDefinition lateralJoinDefinition : definedLateralJoins) {
            if (lateralJoinDefinition
                    .getSqlExpression()
                    .replace("\n", "")
                    .replace(" ", "")
                    .contains(expanded.substring(0, expanded.length() - 1)
                            .substring(1)
                            .replace("\n", "")
                            .replace(" ", ""))) return lateralJoinDefinition;
        }

        return null;
    }
}
