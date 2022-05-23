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

import java.util.List;
import java.util.Set;
import org.jooq.DataType;

/**
 * Created by christian on 9/20/2016.
 */
public class FunctionDefinition implements I_VariableDefinition {

    private String identifier;
    private String alias;
    private String path;
    private List<FuncParameter> parameters;

    public FunctionDefinition(String identifier, String alias, String path, List<FuncParameter> parameters) {
        this.identifier = identifier;
        this.alias = alias;
        this.path = path;
        this.parameters = parameters;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public LateralJoinDefinition getLateralJoinDefinition(String templateId, int index) {
        return null;
    }

    @Override
    public int getLateralJoinsSize(String templateId) {
        return 0;
    }

    @Override
    public boolean isLateralJoinsEmpty(String templateId) {
        return false;
    }

    @Override
    public LateralJoinDefinition getLastLateralJoin(String templateId) {
        return null;
    }

    @Override
    public void setLateralJoinTable(String templateId, LateralJoinDefinition lateralJoinDefinition) {
        // n/a
    }

    @Override
    public boolean isDistinct() {
        return false;
    }

    @Override
    public boolean isFunction() {
        return true;
    }

    @Override
    public boolean isExtension() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public List<FuncParameter> getFuncParameters() {
        return parameters;
    }

    @Override
    public I_VariableDefinition duplicate() {
        return new FunctionDefinition(this.identifier, this.alias, this.path, this.parameters);
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void setDistinct(boolean distinct) {
        // n/a
    }

    @Override
    public void setHidden(boolean hidden) {
        // n/a
    }

    @Override
    public void setAlias(String alias) {
        // n/a
    }

    @Override
    public boolean isConstant() {
        return false;
    }

    @Override
    public boolean isLateralJoin(String templateId) {
        return false;
    }

    @Override
    public Set<LateralJoinDefinition> getLateralJoinDefinitions(String templateId) {
        return null;
    }

    @Override
    public PredicateDefinition getPredicateDefinition() {
        return null;
    }

    @Override
    public void setSubstituteFieldVariable(String variableAlias) {
        // na
    }

    @Override
    public String getSubstituteFieldVariable() {
        return null;
    }

    @Override
    public void setSelectType(DataType castTypeAs) {
        // na
    }

    @Override
    public DataType getSelectType() {
        return null;
    }

    @Override
    public boolean isVoidAlias() {
        return false;
    }

    @Override
    public void setVoidAlias(boolean isVoidAlias) {
        // na
    }
}
