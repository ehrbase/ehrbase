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

package org.ehrbase.aql.definition;

import org.jooq.Table;

import java.util.ArrayList;
import java.util.List;

/**
 * Container of a variable (symbol) with its path and alias (AS 'alias')
 * Created by christian on 5/3/2016.
 */
public class ConstantDefinition implements I_VariableDefinition {

    private String alias;
    private String path;
    private Object value;

    public ConstantDefinition(Object value, String alias) {
        this.value = value;
        this.alias = alias;
        if (alias != null)
            path = alias;
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
        return null;
    }

    @Override
    public void setLateralJoinTable(Table lateralJoinTable) {
        // n/a
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public boolean isLateralJoin() {
        return false;
    }

    @Override
    public Table getLateralJoinTable() {
        return null;
    }

    @Override
    public boolean isDistinct() {
        return false;
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
        return false;
    }

    @Override
    public List<FuncParameter> getFuncParameters() {
        return new ArrayList<>();
    }

    @Override
    public void setPath(String path){
        this.path = path;
    }

    @Override
    public void setDistinct(boolean distinct){
        // n/a
    }

    @Override
    public void setHidden(boolean hidden){
        // n/a
    }

    @Override
    public I_VariableDefinition duplicate() {
        return new ConstantDefinition(this.value, this.alias);
    }

    @Override
    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setValue(Object value){ this.value = value;}

    public Object getValue() {
        return value;
    }
}
