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
 * Represents a AQL Select Variable
 * Created by christian on 9/22/2017.
 */
public interface I_VariableDefinition {
    String getPath();

    String getAlias();

    String getIdentifier();

    boolean isDistinct();

    boolean isFunction();

    boolean isExtension();

    List<FuncParameter> getFuncParameters();

    I_VariableDefinition clone();

    void setPath(String path); //used to modify the path in case of struct query (canonical json).
}
