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
 * Represents a AQL Select Variable
 * Created by christian on 9/22/2017.
 */
public interface I_VariableDefinition extends Cloneable {
    String getPath();

    String getAlias();

    String getIdentifier();

    LateralJoinDefinition getLateralJoinDefinition(String templateId, int index);

    int getLateralJoinsSize(String templateId);

    boolean isLateralJoinsEmpty(String templateId);

    LateralJoinDefinition getLastLateralJoin(String templateId);

    void setLateralJoinTable(String templateId, LateralJoinDefinition lateralJoinDefinition);

    boolean isDistinct();

    boolean isFunction();

    boolean isExtension();

    boolean isHidden();

    List<FuncParameter> getFuncParameters();

    I_VariableDefinition duplicate();

    void setPath(String path); // used to modify the path in case of struct query (canonical json).

    void setDistinct(boolean distinct);

    void setHidden(boolean hidden);

    void setAlias(String alias);

    String toString();

    boolean isConstant();

    boolean isLateralJoin(String templateId);

    Set<LateralJoinDefinition> getLateralJoinDefinitions(String templateId);

    PredicateDefinition getPredicateDefinition();

    void setSubstituteFieldVariable(String variableAlias);

    String getSubstituteFieldVariable();

    void setSelectType(DataType castTypeAs);

    DataType getSelectType();

    boolean isVoidAlias();

    void setVoidAlias(boolean isVoidAlias);
}
