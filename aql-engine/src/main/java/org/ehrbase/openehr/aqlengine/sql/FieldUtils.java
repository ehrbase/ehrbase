/*
 * Copyright (c) 2024 vitasystems GmbH.
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

import java.util.Iterator;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslVirtualField;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;

final class FieldUtils {

    private FieldUtils() {}

    public static <T> Field<T> field(
            Table<?> sqlProvider,
            AslQuery aslProvider,
            AslQuery owner,
            String fieldName,
            Class<T> type,
            boolean aliased) {
        return field(sqlProvider, findFieldByOwnerAndName(aslProvider, owner, fieldName), type, aliased);
    }

    private static AslColumnField findFieldByOwnerAndName(AslQuery src, AslQuery owner, String columnName) {
        Iterator<AslColumnField> fieldsIt = src.getSelect().stream()
                .filter(AslColumnField.class::isInstance)
                .map(AslColumnField.class::cast)
                .filter(f -> owner == f.getOwner())
                .filter(f -> f.getColumnName().equals(columnName))
                .iterator();

        if (!fieldsIt.hasNext()) {
            throw new IllegalArgumentException("field with columnName %s not present".formatted(columnName));
        }
        AslColumnField field = fieldsIt.next();
        if (fieldsIt.hasNext()) {
            throw new IllegalArgumentException("found multiple fields with columnName %s".formatted(columnName));
        }
        return field;
    }

    public static Field<?> field(Table<?> table, AslVirtualField aslField, String fieldName, boolean aliased) {
        return table.field(aliased ? aslField.aliasedName(fieldName) : fieldName);
    }

    public static <T> Field<T> field(
            Table<?> table, AslVirtualField aslField, String fieldName, Class<T> type, boolean aliased) {
        return table.field(aliased ? aslField.aliasedName(fieldName) : fieldName, type);
    }

    public static Field<?> field(Table<?> table, AslColumnField aslField, boolean aliased) {
        return table.field(aslField.getName(aliased));
    }

    public static <T> Field<T> field(Table<?> table, AslColumnField aslField, Class<T> type, boolean aliased) {
        return table.field(aslField.getName(aliased), type);
    }

    public static <T> Field<T> aliasedField(Table<?> target, AslDataQuery aslData, TableField<?, T> fieldTemplate) {
        return field(
                target, aslData.getBase(), aslData.getBase(), fieldTemplate.getName(), fieldTemplate.getType(), true);
    }

    public static <T> Field<T> aliasedField(
            Table<?> target, AslDataQuery aslData, String fieldName, Class<T> fieldType) {
        return field(target, aslData.getBase(), aslData.getBase(), fieldName, fieldType, true);
    }

    public static Field<?> virtualAliasedField(
            Table<?> target, Field<?> field, AslVirtualField column, String columnName) {
        return DSL.field("{0}.{1}", target, field).as(column.aliasedName(columnName));
    }
}
