/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

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

package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.containment.IdentifierMapper;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.definition.VariableDefinition;
import org.ehrbase.aql.sql.queryImpl.CompositionAttributeQuery;
import org.ehrbase.aql.sql.queryImpl.I_QueryImpl;
import org.ehrbase.aql.sql.queryImpl.JsonbEntryQuery;
import org.ehrbase.aql.sql.queryImpl.VariablePath;
import org.ehrbase.aql.sql.queryImpl.value_field.ISODateTime;
import org.ehrbase.serialisation.CompositionSerializer;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.*;

/**
 * Bind the abstract WHERE clause parameters into a SQL expression
 * Created by christian on 5/20/2016.
 */
public class WhereBinder {

    private static final String JSQUERY_EXPR_VALUE = "\"value\"";
    //from AQL grammar
    private static final Set<String> sqloperators = new HashSet<>(Arrays.asList("=", "!=", ">", ">=", "<", "<=", "MATCHES", "EXISTS", "NOT", "(", ")", "{", "}"));

    private JsonbEntryQuery jsonbEntryQuery;
    private CompositionAttributeQuery compositionAttributeQuery;
    private final List whereClause;
    private IdentifierMapper mapper;
    private Condition initialCondition;
    private boolean isWholeComposition = false;
    private String compositionName = null;
    private String sqlSetStatementRegexp = "(?i)(like|ilike|in|not in)"; //list of subquery and operators

    private enum Operator {OR, XOR, AND, NOT, EXISTS}

    private boolean usePgExtensions = true;

    public WhereBinder(JsonbEntryQuery jsonbEntryQuery, CompositionAttributeQuery compositionAttributeQuery, List whereClause, IdentifierMapper mapper) {
        this.jsonbEntryQuery = jsonbEntryQuery;
        this.compositionAttributeQuery = compositionAttributeQuery;
        this.whereClause = whereClause;
        this.mapper = mapper;

    }

    private TaggedStringBuilder encodeWhereVariable(String templateId, UUID comp_id, I_VariableDefinition variableDefinition, boolean forceSQL, String compositionName) {
        String identifier = variableDefinition.getIdentifier();
        String className = mapper.getClassName(identifier);
        if (className == null)
            throw new IllegalArgumentException("Could not bind identifier in WHERE clause:'" + identifier + "'");
        Field<?> field;
        //EHR-327: if force SQL is set to true via environment, jsquery extension is not required
        //this allows to deploy on AWS since jsquery is not supported by this provider
        if (forceSQL || !usePgExtensions) {
            //EHR-327: also supports EHR attributes in WHERE clause
            if (className.equals("COMPOSITION") || className.equals("EHR")) {
                field = compositionAttributeQuery.whereField(templateId, comp_id, identifier, variableDefinition);
            } else { //should be removed (?)
                //TODO: identify a method to avoid using Set Returning Function (jsonb_array_element) in WHERE (unsupported in PG10+) while still filtering values in a set
                field = jsonbEntryQuery.makeField(templateId, comp_id, identifier, variableDefinition, I_QueryImpl.Clause.WHERE);
            }
            if (field == null)
                return null;
            return new TaggedStringBuilder(field.toString(), I_TaggedStringBuilder.TagField.SQLQUERY);

        } else {
            switch (className) {
                case "COMPOSITION":
                    if (variableDefinition.getPath().startsWith("content")) {
                        field = jsonbEntryQuery.whereField(templateId, comp_id, identifier, variableDefinition);
                        TaggedStringBuilder taggedStringBuilder = new TaggedStringBuilder(field.toString(), I_TaggedStringBuilder.TagField.JSQUERY);
                        if (compositionName != null && taggedStringBuilder.startWith(CompositionSerializer.TAG_COMPOSITION)) {
                            //add the composition name into the composition predicate
                            taggedStringBuilder.replace("]", " and name/value='" + compositionName + "']");
                        }
                        return taggedStringBuilder;
                    }
                case "EHR":
                    field = compositionAttributeQuery.whereField(templateId, comp_id, identifier, variableDefinition);
                    if (field == null)
                        return null;
                    return new TaggedStringBuilder(field.toString(), I_TaggedStringBuilder.TagField.SQLQUERY);

                default:
                    field = jsonbEntryQuery.whereField(templateId, comp_id, identifier, variableDefinition);
                    return new TaggedStringBuilder(field.toString(), I_TaggedStringBuilder.TagField.JSQUERY);
            }
        }
    }

    private TaggedStringBuilder buildWhereCondition(String templateId, UUID comp_id, TaggedStringBuilder taggedBuffer, List item) {
        for (Object part : item) {
            if (part instanceof String)
                taggedBuffer.append((String) part);
            else if (part instanceof VariableDefinition) {
                //substitute the identifier
                TaggedStringBuilder taggedStringBuilder = encodeWhereVariable(templateId, comp_id, (VariableDefinition) part, false, null);
                taggedBuffer.append(taggedStringBuilder.toString());
                taggedBuffer.setTagField(taggedStringBuilder.getTagField());
            } else if (part instanceof List) {
                TaggedStringBuilder taggedStringBuilder = buildWhereCondition(templateId, comp_id, taggedBuffer, (List) part);
                taggedBuffer.append(taggedStringBuilder.toString());
                taggedBuffer.setTagField(taggedStringBuilder.getTagField());
            }
        }
        return taggedBuffer;
    }

    private Condition wrapInCondition(Condition condition, TaggedStringBuilder taggedStringBuilder, Deque<Operator> operators) {
        //perform the condition query wrapping depending on the dialect jsquery or sql
        String wrapped;

        switch (taggedStringBuilder.getTagField()) {
            case JSQUERY:
                wrapped = JsonbEntryQuery.Jsquery_COMPOSITION_OPEN + taggedStringBuilder.toString() + JsonbEntryQuery.Jsquery_CLOSE;
                break;
            case SQLQUERY:
                wrapped = taggedStringBuilder.toString();
                break;

            default:
                throw new IllegalArgumentException("Uninitialized tag passed in query expression");
        }

        if (condition == null)
            condition = DSL.condition(wrapped);
        else {
            if (operators.isEmpty()) //assumes AND
                condition = condition.and(wrapped);
            else {
                Operator operator = operators.pop();
                switch (operator) {
                    case OR:
                        condition = condition.or(wrapped);
                        break;
                    case XOR:
                        throw new IllegalArgumentException("XOR is not supported yet...");

                    case AND:
                        condition = condition.and(wrapped);
                        break;
                    case NOT:
                        Condition condition1 = DSL.condition(wrapped);
                        if (!operators.isEmpty()) {
                            operator = operators.pop();
                            switch (operator) {
                                case OR:
                                    condition = condition.orNot(condition1);
                                    break;
                                case AND:
                                    condition = condition.andNot(condition1);
                                    break;
                                default:
                                    condition = condition.andNot(condition1);
                            }
                        } else
                            condition = condition.andNot(condition1);
                        break;
                }
            }
        }

        return condition;
    }

    public Condition bind(String templateId, UUID comp_id) {
        Deque<Operator> operators = new ArrayDeque<>();
        TaggedStringBuilder taggedBuffer = new TaggedStringBuilder();
        Condition condition = initialCondition;

//        List whereItems = new WhereResolver(whereClause).resolveDateCondition();
        List whereItems = whereClause;

        for (int cursor = 0; cursor < whereItems.size(); cursor++) {
            Object item = whereItems.get(cursor);
            if (item instanceof String) {
                switch ((String) item) {
                    case "OR":
                    case "or":
                        operators.push(Operator.OR);
                        break;

                    case "XOR":
                    case "xor":
                        operators.push(Operator.XOR);
                        break;

                    case "AND":
                    case "and":
                        operators.push(Operator.AND);
                        break;

                    case "NOT":
                    case "not":
                        operators.push(Operator.NOT);
                        break;

                    default:
                        ISODateTime isoDateTime = new ISODateTime(((String) item).replaceAll("'", ""));
                        if (isoDateTime.isValidDateTimeExpression()) {
                            Long timestamp = isoDateTime.toTimeStamp();
                            int lastValuePos = taggedBuffer.lastIndexOf(JSQUERY_EXPR_VALUE);
                            if (lastValuePos > 0) {
                                taggedBuffer.replaceLast(JSQUERY_EXPR_VALUE, "\"epoch_offset\"");
                            }
                            item = hackItem(taggedBuffer, timestamp.toString());
                            taggedBuffer.append((String) item);
                        } else {
                            item = hackItem(taggedBuffer, (String) item);
                            taggedBuffer.append((String) item);
                        }
                        break;

                }
            } else if (item instanceof Long) {
                item = hackItem(taggedBuffer, item.toString());
                taggedBuffer.append(item.toString());
            } else if (item instanceof I_VariableDefinition) {
                if (taggedBuffer.length() > 0) {
                    condition = wrapInCondition(condition, taggedBuffer, operators);
                    taggedBuffer = new TaggedStringBuilder();
                }
                //look ahead and check if followed by a sql operator
                TaggedStringBuilder taggedStringBuilder = null;
                if (isFollowedBySQLSetOperator(cursor))
                    taggedStringBuilder = encodeWhereVariable(templateId, comp_id, (I_VariableDefinition) item, true, null);
                else {
                    if (((I_VariableDefinition) item).getPath() != null && isWholeComposition) {
                        //assume a composition
                        //look ahead for name/value condition (used to produce the composition root)
                        if (compositionName == null)
                            compositionName = compositionNameValue(((I_VariableDefinition) item).getIdentifier());

                        if (compositionName != null) {
                            taggedStringBuilder = encodeWhereVariable(templateId, comp_id, (I_VariableDefinition) item, false, compositionName);
                        } else
                            throw new IllegalArgumentException("A composition name/value is required to resolve where statement when querying for a whole composition");
                    } else {
                        //if the path contains node predicate expression uses a SQL syntax instead of jsquery
                        if (new VariablePath(((I_VariableDefinition) item).getPath()).hasPredicate()) {
                            taggedStringBuilder = encodeWhereVariable(templateId, comp_id, (I_VariableDefinition) item, true, null);
                        } else {
                            taggedStringBuilder = encodeWhereVariable(templateId, comp_id, (I_VariableDefinition) item, false, null);
                        }
                    }
                }

                if (taggedStringBuilder != null) {
                    taggedBuffer.append(taggedStringBuilder.toString());
                    taggedBuffer.setTagField(taggedStringBuilder.getTagField());
                }
                //check for a composition name if applicable
//                if (((VariableDefinition) item).getAlias())
//                condition = wrapInCondition(condition, stringBuffer, operators);
            } else if (item instanceof List) {
                TaggedStringBuilder taggedStringBuilder = buildWhereCondition(templateId, comp_id, taggedBuffer, (List) item);
                taggedBuffer.append(taggedStringBuilder.toString());
                taggedBuffer.setTagField(taggedStringBuilder.getTagField());
                condition = wrapInCondition(condition, taggedBuffer, operators);
            }

        }

        if (taggedBuffer.length() != 0) {
            condition = wrapInCondition(condition, taggedBuffer, operators);
        }

        return condition;
    }


    //look ahead for a SQL operator
    private boolean isFollowedBySQLSetOperator(int cursor) {
        if (cursor < whereClause.size() - 1) {
            Object nextToken = whereClause.get(cursor + 1);
            if (nextToken instanceof String && ((String) nextToken).matches(sqlSetStatementRegexp))
                return true;
        }
        return false;
    }

    //look ahead for a SQL operator
    private String compositionNameValue(String symbol) {

        String token = null;
        int lcursor; //skip the current variable

        for (lcursor = 0; lcursor < whereClause.size() - 1; lcursor++) {
            if (whereClause.get(lcursor) instanceof VariableDefinition
                    && (((VariableDefinition) whereClause.get(lcursor)).getIdentifier().equals(symbol))
                    && (((VariableDefinition) whereClause.get(lcursor)).getPath().equals("name/value"))
            ) {
                Object nextToken = whereClause.get(lcursor + 1); //check operator
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


    //do some temporary hacking for unsupported features
    private Object hackItem(TaggedStringBuilder taggedBuffer, String item) {
        if (sqloperators.contains(item.toUpperCase()))
            return item;
        if (taggedBuffer.toString().contains(I_JoinBinder.COMPOSITION_JOIN) && item.contains("::"))
            return item.split("::")[0] + "'";
        if (taggedBuffer.indexOf("#>>") > 0) {
            return item;
        }
        if (taggedBuffer.indexOf("#") > 0 && item.contains("'")) { //conventionally double quote for jsquery
            return item.replaceAll("'", "\"");
        }
        return item;
    }

    public void setInitialCondition(Condition initialCondition) {
        this.initialCondition = initialCondition;
    }

    public void setIsWholeComposition() {
        isWholeComposition = true;
    }

    public WhereBinder setUsePgExtensions(boolean usePgExtensions) {
        this.usePgExtensions = usePgExtensions;
        return this;
    }
}
