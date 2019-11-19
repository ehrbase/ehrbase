/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 * This file is part of project EHRbase
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
package org.ehrbase.aql.sql.queryImpl.attribute.composition;

import org.ehrbase.aql.sql.binding.I_JoinBinder;
import org.ehrbase.aql.sql.queryImpl.I_QueryImpl;
import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.I_RMObjectAttribute;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;
import org.jooq.Field;
import org.jooq.SelectQuery;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;

import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.COMPOSITION_HISTORY;

public class CompositionUidValue extends CompositionAttribute {

    public CompositionUidValue(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField(){
        if (fieldContext.getClause() == I_QueryImpl.Clause.WHERE)
            filterSetup.setCompositionIdFiltered(true);
        else
            compositionIdFieldSetup.setCompositionIdField(true);

        joinSetup.setJoinComposition(true);

        if (fieldContext.isWithAlias())
            return uid();
        else
            return rawUid();
    }

    @Override
    public I_RMObjectAttribute forTableField(TableField tableField) {
        return this;
    }

    private Field<?> uid() {

        //use inline SQL as it seems coalesce is not going through with POSTGRES dialect
        SelectQuery<?> subSelect = fieldContext.getContext().selectQuery();
        subSelect.addSelect(DSL.count());
        subSelect.addFrom(COMPOSITION_HISTORY);
        subSelect.addConditions(I_JoinBinder.compositionRecordTable.field("id", UUID.class).eq(COMPOSITION_HISTORY.ID));
        subSelect.addGroupBy(COMPOSITION_HISTORY.ID);

        String coalesceVersion = "1 + COALESCE(\n(" + subSelect + "), 0)";

        Field<?> select = aliased(DSL.field(I_JoinBinder.compositionRecordTable.field("id")
                        + "||"
                        + DSL.val("::")
                        + "||"
                        + DSL.val(fieldContext.getServerNodeId())
                        + "||"
                        + DSL.val("::")
                        + "||"
                        + DSL.field(coalesceVersion)
                , SQLDataType.VARCHAR));

        return select;
    }

    private Field<?> rawUid() {
        return as(DSL.field(I_JoinBinder.compositionRecordTable.field("id", UUID.class)));
    }
}
