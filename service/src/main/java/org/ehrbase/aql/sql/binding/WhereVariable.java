/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.PathResolver;
import org.ehrbase.aql.sql.queryimpl.*;
import org.jooq.Field;
import org.jooq.impl.DSL;

public class WhereVariable {

    public static final String COMPOSITION = "COMPOSITION";
    public static final String CONTENT = "content";
    public static final String EHR = "EHR";
    private boolean isFollowedBySQLConditionalOperator = false;
    private WhereBinder.ExistsMode inExists;
    private String rightMostJsonbExpression = null;

    private final PathResolver pathResolver;

    public WhereVariable(PathResolver pathResolver) {
        this.pathResolver = pathResolver;
    }

    public TaggedStringBuilder encodeWhereVariable(
            WhereBinder.ExistsMode inExists,
            boolean isFollowedBySQLConditionalOperator,
            int whereCursor,
            MultiFieldsMap multiWhereFieldsMap,
            int selectCursor,
            MultiFieldsMap multiSelectFieldsMap,
            I_VariableDefinition variableDefinition,
            String compositionName)
            throws UnknownVariableException {

        this.inExists = inExists;
        this.isFollowedBySQLConditionalOperator = isFollowedBySQLConditionalOperator;

        TaggedStringBuilder variableSQL;

        try {
            variableSQL = _encodeWhereVariable(
                    whereCursor,
                    multiWhereFieldsMap,
                    selectCursor,
                    multiSelectFieldsMap,
                    variableDefinition,
                    compositionName);
        } catch (UnknownVariableException e) {
            if (!inExists.equals(WhereBinder.ExistsMode.UNSET)) {
                variableSQL = new TaggedStringBuilder(
                        " " + DSL.val((Byte) null) + " ", I_TaggedStringBuilder.TagField.SQLQUERY);
                this.inExists = WhereBinder.ExistsMode.UNSET;
            } else throw new UnknownVariableException(e.toString());
        }

        return variableSQL;
    }

    private TaggedStringBuilder _encodeWhereVariable(
            int whereCursor,
            MultiFieldsMap multiFieldsMap,
            int selectCursor,
            MultiFieldsMap multiSelectFieldsMap,
            I_VariableDefinition variableDefinition,
            String compositionName)
            throws UnknownVariableException {
        String identifier = variableDefinition.getIdentifier();

        // check if we have already resolved this variable in the select clause
        String alreadyResolvedSQL = lookUpFromSelect(variableDefinition, selectCursor, multiSelectFieldsMap);
        if (alreadyResolvedSQL != null) {
            rightMostJsonbExpression = null; // reinit
            return new TaggedStringBuilder(alreadyResolvedSQL, I_TaggedStringBuilder.TagField.SQLQUERY);
        }

        String className = pathResolver.classNameOf(identifier);
        if (className == null)
            throw new IllegalArgumentException("Could not bind identifier in WHERE clause:'" + identifier + "'");

        MultiFields multiFields = multiFieldsMap.get(variableDefinition.getIdentifier(), variableDefinition.getPath());

        if (multiFields == null) throw new UnknownVariableException(variableDefinition.toString());

        QualifiedAqlField qualifiedAqlField = multiFields.getQualifiedFieldOrLast(whereCursor);

        Field<?> field = qualifiedAqlField.getSQLField();
        rightMostJsonbExpression = qualifiedAqlField.getRightMostJsonbExpression();

        if (field == null) return null;

        return new TaggedStringBuilder(field.toString(), I_TaggedStringBuilder.TagField.SQLQUERY);
    }

    private String lookUpFromSelect(
            I_VariableDefinition variableDefinition, int selectCursor, MultiFieldsMap multiSelectFieldsMap)
            throws UnknownVariableException {

        // TODO: remove when we support historical queries.
        if (pathResolver.classNameOf(variableDefinition.getIdentifier()).equals("COMPOSITION")
                && variableDefinition.getPath().equals("uid/value")) return null;

        MultiFields multiFields =
                multiSelectFieldsMap.get(variableDefinition.getIdentifier(), variableDefinition.getPath());
        if (multiFields == null) return null;
        QualifiedAqlField aqlField = multiFields.getQualifiedFieldOrLast(selectCursor);

        // There is an issue with jOOQ regarding the formatting of interval. Hence, the actual varchar format is not
        // preserved.
        if (aqlField.getItemType() != null && aqlField.getItemType().equals("DV_DURATION")) return null;

        return DSL.select(aqlField.getSQLField())
                .getSQL()
                .replace("select ", "")
                .replace("\"" + aqlField.getSQLField().getName() + "\"", "");
    }

    public boolean isFollowedBySQLConditionalOperator() {
        return isFollowedBySQLConditionalOperator;
    }

    public WhereBinder.ExistsMode inExists() {
        return inExists;
    }

    public String getRightMostJsonbExpression() {
        return rightMostJsonbExpression;
    }

    public boolean hasRightMostJsonbExpression() {
        return rightMostJsonbExpression != null;
    }
}
