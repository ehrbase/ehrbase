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
package org.ehrbase.openehr.dbformat.jooq.prototypes;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import org.jooq.Table;
import org.jooq.impl.UpdatableRecordImpl;

public abstract class AbstractRecordPrototype<P extends AbstractRecordPrototype<P>> extends UpdatableRecordImpl<P> {

    private static final long serialVersionUID = 1L;

    protected final void set(FieldPrototype f, Object value) {
        super.set(columnIndex(f), value);
    }

    public final Object get(FieldPrototype f) {
        return super.get(columnIndex(f));
    }

    protected abstract int columnIndex(FieldPrototype f);

    protected AbstractRecordPrototype(Table<P> table, Object... values) {
        super(table);
        for (int i = 0; i < values.length; i++) {
            set(i, values[i]);
        }
        resetChangedOnNotNull();
    }

    protected static EnumMap<FieldPrototype, Integer> determineColumns(boolean version, boolean head) {
        EnumMap<FieldPrototype, Integer> columns = new EnumMap<>(FieldPrototype.class);

        Iterator<FieldPrototype> it = Arrays.stream(FieldPrototype.values())
                .filter(f -> f.isAvailable(version, head))
                .iterator();
        int pos = 0;
        while (it.hasNext()) {
            columns.put(it.next(), pos++);
        }
        return columns;
    }
}
