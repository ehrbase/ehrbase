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
package org.ehrbase.aql.definition;

import java.util.*;
import org.jooq.DataType;

/**
 * Container of a variable (symbol) with its path and alias (AS 'alias')
 * Created by christian on 5/3/2016.
 */
public class VariableDefinition implements I_VariableDefinition {

    private String path;
    private String alias;
    private boolean isVoidAlias = true;
    private String identifier;
    private String substituteFieldVariable;
    private boolean isDistinct;
    private boolean isHidden;
    private Map<String, Set<LateralJoinDefinition>> lateralJoinDefinitions = new HashMap<>();
    private PredicateDefinition predicateDefinition;
    private DataType selectDataType;

    public VariableDefinition(String path, String alias, String identifier, boolean isDistinct) {
        this.path = path;
        this.alias = alias;
        this.identifier = identifier;
        this.isDistinct = isDistinct;
        this.isHidden = false;
    }

    public VariableDefinition(
            String path, String alias, String identifier, boolean isDistinct, PredicateDefinition predicateDefinition) {
        this.path = path;
        this.alias = alias;
        this.identifier = identifier;
        this.isDistinct = isDistinct;
        this.isHidden = false;
        this.predicateDefinition = predicateDefinition;
    }

    /**
     * used whenever a variable is added for technical reason (f.e. a order by field not present in the select clause)
     * @param path
     * @param alias
     * @param identifier
     * @param isDistinct
     * @param isHidden
     */
    public VariableDefinition(String path, String alias, String identifier, boolean isDistinct, boolean isHidden) {
        this.path = path;
        this.alias = alias;
        this.identifier = identifier;
        this.isDistinct = isDistinct;
        this.isHidden = isHidden;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toString() {
        return identifier + "::" + path;
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public boolean isLateralJoin(String templateId) {
        return !lateralJoinDefinitions.isEmpty() && lateralJoinDefinitions.get(templateId) != null;
    }

    @Override
    public Set<LateralJoinDefinition> getLateralJoinDefinitions(String templateId) {
        return lateralJoinDefinitions.get(templateId);
    }

    @Override
    public LateralJoinDefinition getLateralJoinDefinition(String templateId, int index) {
        Set<LateralJoinDefinition> definitions = getLateralJoinDefinitions(templateId);
        if (index > definitions.size() - 1) return null;
        return definitions.toArray(new LateralJoinDefinition[] {})[index];
    }

    @Override
    public int getLateralJoinsSize(String templateId) {
        if (isLateralJoinsEmpty(templateId)) return 0;
        return lateralJoinDefinitions.get(templateId).size();
    }

    @Override
    public boolean isLateralJoinsEmpty(String templateId) {
        if (lateralJoinDefinitions.isEmpty()
                || lateralJoinDefinitions.get(templateId) == null
                || lateralJoinDefinitions.get(templateId).isEmpty()) return true;
        return false;
    }

    @Override
    public LateralJoinDefinition getLastLateralJoin(String templateId) {
        if (isLateralJoinsEmpty(templateId)) return null;
        return Arrays.asList(lateralJoinDefinitions.get(templateId).toArray(new LateralJoinDefinition[] {}))
                .get(getLateralJoinsSize(templateId) - 1);
    }

    @Override
    public void setLateralJoinTable(String templateId, LateralJoinDefinition lateralJoinDefinition) {
        lateralJoinDefinitions.computeIfAbsent(templateId, k -> new HashSet<>());

        // do not add duplicate join
        this.lateralJoinDefinitions.get(templateId).add(lateralJoinDefinition);
    }

    @Override
    public boolean isDistinct() {
        return isDistinct;
    }

    @Override
    public boolean isFunction() {
        return false;
    }

    @Override
    public boolean isExtension() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return isHidden;
    }

    @Override
    public List<FuncParameter> getFuncParameters() {
        return new ArrayList<>();
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void setDistinct(boolean distinct) {
        this.isDistinct = distinct;
    }

    @Override
    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }

    @Override
    public I_VariableDefinition duplicate() {
        return new VariableDefinition(this.path, this.alias, this.identifier, this.isDistinct, this.isHidden);
    }

    @Override
    public void setAlias(String alias) {
        this.alias = alias;
    }

    public PredicateDefinition getPredicateDefinition() {
        return predicateDefinition;
    }

    public String getSubstituteFieldVariable() {
        return substituteFieldVariable;
    }

    public void setSubstituteFieldVariable(String substituteFieldVariable) {
        this.substituteFieldVariable = substituteFieldVariable;
    }

    public void setSelectType(DataType sqlDataType) {
        this.selectDataType = sqlDataType;
    }

    public DataType getSelectType() {
        return this.selectDataType;
    }

    @Override
    public boolean isVoidAlias() {
        return isVoidAlias;
    }

    @Override
    public void setVoidAlias(boolean isVoidAlias) {
        this.isVoidAlias = isVoidAlias;
    }
}
