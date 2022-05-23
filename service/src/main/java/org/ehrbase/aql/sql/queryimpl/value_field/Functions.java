/*
 * Copyright (c) 2020-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.queryimpl.value_field;

import java.util.function.Function;
import org.jooq.Field;
import org.jooq.Function2;
import org.jooq.Function3;
import org.jooq.Function4;
import org.jooq.TableField;

/**
 * @author Christian Chevalley
 * @since 1.0
 */
@SuppressWarnings("java:S3740")
public class Functions {

    private Functions() {}

    public static Field apply(Object function, TableField<?, ?>... tableField) {
        if (function == null) {
            return tableField[0];
        }

        switch (tableField.length) {
            case 1:
                return applyFunction1((Function) function, tableField);
            case 2:
                return applyFunction2((Function2) function, tableField);
            case 3:
                return applyFunction3((Function3) function, tableField);
            case 4:
                return applyFunction4((Function4) function, tableField);
            default:
                throw new IllegalStateException("Unsupported argument cardinality:" + tableField.length);
        }
    }

    private static Field applyFunction1(Function function, TableField... tableField) {
        return (Field) function.apply(tableField[0]);
    }

    private static Field applyFunction2(Function2 function, TableField... tableField) {
        return (Field) function.apply(tableField[0], tableField[1]);
    }

    private static Field applyFunction3(Function3 function, TableField... tableField) {
        return (Field) function.apply(tableField[0], tableField[1], tableField[2]);
    }

    private static Field applyFunction4(Function4 function, TableField... tableField) {
        return (Field) function.apply(tableField[0], tableField[1], tableField[2], tableField[3]);
    }
}
