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
package org.ehrbase.aql.sql.queryimpl.attribute.composition;

import static org.ehrbase.jooq.pg.Tables.COMPOSITION_HISTORY;

import java.util.UUID;
import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.IQueryImpl;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.IRMObjectAttribute;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.jooq.Field;
import org.jooq.SelectQuery;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

public class CompositionUidValue extends CompositionAttribute {

    public CompositionUidValue(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        if (fieldContext.getClause() == IQueryImpl.Clause.WHERE) filterSetup.setCompositionIdFiltered(true);
        else compositionIdFieldSetup.setCompositionIdField(true);

        joinSetup.setJoinComposition(true);

        if (fieldContext.isWithAlias()) return uid();
        else return rawUid();
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        return this;
    }

    private Field<?> uid() {

        // use inline SQL as it seems coalesce is not going through with POSTGRES dialect
        SelectQuery<?> subSelect = fieldContext.getContext().selectQuery();
        subSelect.addSelect(DSL.count());
        subSelect.addFrom(COMPOSITION_HISTORY);
        subSelect.addConditions(
                JoinBinder.compositionRecordTable.field("id", UUID.class).eq(COMPOSITION_HISTORY.ID));
        subSelect.addGroupBy(COMPOSITION_HISTORY.ID);

        String coalesceVersion = "1 + COALESCE(\n(" + subSelect + "), 0)";

        return aliased(DSL.field(
                JoinBinder.compositionRecordTable.field("id")
                        + "||"
                        + DSL.val("::")
                        + "||"
                        + DSL.val(fieldContext.getServerNodeId())
                        + "||"
                        + DSL.val("::")
                        + "||"
                        + DSL.field(coalesceVersion),
                SQLDataType.VARCHAR));
    }

    private Field<?> rawUid() {
        return as(DSL.field(JoinBinder.compositionRecordTable.field("id", UUID.class)));
    }
}
