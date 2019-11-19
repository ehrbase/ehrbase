/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) Ripple Foundation CIC Ltd, UK, 2017
 * Author: Christian Chevalley
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
public class ExtensionDefinition implements I_VariableDefinition {

    private String context;
    private String parsableExpression;
    private String alias;

    public ExtensionDefinition(String context, String parsableExpression, String alias) {
        this.context = context;
        this.parsableExpression = parsableExpression;
        this.alias = alias;
    }


    @Override
    public String getAlias() {
        return alias;
    }


    @Override
    public String getPath() {
        return null;
    }


    @Override
    public String getIdentifier() {
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
        return true;
    }

    @Override
    public List<FuncParameter> getFuncParameters() {
        return null;
    }

    @Override
    public I_VariableDefinition clone() {
        return new ExtensionDefinition(this.context, this.parsableExpression, this.alias);
    }

    @Override
    public void setPath(String path) {

    }
}
