/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
package org.ehrbase.openehr.aqlengine.sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.ehrbase.openehr.aqlengine.asl.model.field.AslFolderItemIdValuesColumnField;
import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.QOM;
import org.junit.jupiter.api.Test;

@SuppressWarnings("UnstableApiUsage")
class FieldUtilsTest {

    @Test
    void virtualAliasedField() {

        String columnName = "items_id_value";

        AslFolderItemIdValuesColumnField aslField = mock(AslFolderItemIdValuesColumnField.class);
        doReturn("aliased_" + columnName).when(aslField).aliasedName(columnName);

        Field<?> field = FieldUtils.virtualAliasedField(
                DSL.table("test_table"), DSL.field("some_field_on_table"), aslField, columnName);

        assertThat(field)
                .hasToString("\"aliased_items_id_value\"")
                .isInstanceOf(QOM.FieldAlias.class)
                .satisfies(aliasedField -> assertThat(((QOM.FieldAlias<?>) field).$aliased())
                        .hasToString("test_table.some_field_on_table"));
    }
}
