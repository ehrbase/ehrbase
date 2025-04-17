/*
 * Copyright (c) 2019-2025 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.openehr.aqlengine.asl.model.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.junit.jupiter.api.Test;

public class AslExternalQueryTest {

    private class AslExternalTestQuery extends AslExternalQuery {
        protected AslExternalTestQuery(String alias, List<? extends AslField> fields) {
            super(alias, fields);
        }
    }

    @Test
    void newInstance() {

        AslExternalQuery query = new AslExternalTestQuery("ext", List.of());
        assertThat(query.getSelect()).isEmpty();
        assertThat(query.getAlias()).isEqualTo("ext");
        assertThat(query).hasToString("AslExternalQuery[ext]");
    }

    @Test
    void doesNotSupportJoinConditions() {

        AslExternalQuery query = new AslExternalTestQuery("ext", List.of());
        assertThatThrownBy(query::joinConditionsForFiltering).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void addField() {

        AslExternalQuery query = new AslExternalTestQuery("ext", List.of());
        AslColumnField field = query.addField(new AslColumnField(String.class, "test"));
        assertThat(field.getOwner()).isSameAs(query);
        assertThat(query.getSelect()).containsExactly(field);
    }
}
