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

    public List<FuncParameter> getParameters() {
        return parameters;
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
    public List<FuncParameter> getFuncParameters() {
        return parameters;
    }

    @Override
    public I_VariableDefinition clone() {
        return new FunctionDefinition(this.identifier, this.alias, this.path, this.parameters);
    }

    @Override
    public void setPath(String path) {
        this.path = path;
    }
}
