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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.jooq.DataType;

public class I_VariableDefinitionHelper {
    private I_VariableDefinitionHelper() {}

    public static void checkEqualWithoutFuncParameters(I_VariableDefinition actual, I_VariableDefinition expected) {
        assertThat(actual.getAlias()).isEqualTo(expected.getAlias());
        assertThat(actual.getIdentifier()).isEqualTo(expected.getIdentifier());
        assertThat(actual.getPath()).isEqualTo(expected.getPath());
        assertThat(actual.isDistinct()).isEqualTo(expected.isDistinct());
        assertThat(actual.isExtension()).isEqualTo(expected.isExtension());
        assertThat(actual.isFunction()).isEqualTo(expected.isFunction());
    }

    public static I_VariableDefinition build(
            String path, String alias, String identifier, boolean distinct, boolean function, boolean extension) {
        return new I_VariableDefinition() {
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
            public void setLateralJoinTable(String templateId, LateralJoinDefinition lateralJoinDefinition) {}

            @Override
            public boolean isDistinct() {
                return distinct;
            }

            @Override
            public boolean isFunction() {
                return function;
            }

            @Override
            public boolean isExtension() {
                return extension;
            }

            @Override
            public boolean isHidden() {
                return false;
            }

            @Override
            public List<FuncParameter> getFuncParameters() {
                return null;
            }

            @Override
            public I_VariableDefinition duplicate() {
                return I_VariableDefinitionHelper.build(path, alias, identifier, distinct, function, extension);
            }

            @Override
            public void setPath(String path) {}

            @Override
            public void setDistinct(boolean distinct) {}

            @Override
            public void setHidden(boolean hidden) {}

            @Override
            public void setAlias(String alias) {}

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
            public void setSelectType(DataType castTypeAs) {}

            @Override
            public DataType getSelectType() {
                return null;
            }

            @Override
            public boolean isVoidAlias() {
                return false;
            }

            @Override
            public void setVoidAlias(boolean isVoidAlias) {}
        };
    }
}
