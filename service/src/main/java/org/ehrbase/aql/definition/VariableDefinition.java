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

import java.util.List;

/**
 * Container of a variable (symbol) with its path and alias (AS 'alias')
 * Created by christian on 5/3/2016.
 */
public class VariableDefinition implements I_VariableDefinition {

    private String path;
    private String alias;
    private String identifier;
    private boolean isDistinct = false;

    public VariableDefinition(String path, String alias, String identifier, boolean isDistinct) {
        this.path = path;
        this.alias = alias;
        this.identifier = identifier;
        this.isDistinct = isDistinct;
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
    public List<FuncParameter> getFuncParameters() {
        return null;
    }

    @Override
    public I_VariableDefinition clone(){
        return new VariableDefinition(this.path, this.alias, this.identifier, this.isDistinct);
    }

    @Override
    public void setPath(String path){
        this.path = path;
    }
}
