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

import org.ehrbase.aql.sql.queryImpl.I_QueryImpl;
import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.I_RMObjectAttribute;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import static org.ehrbase.jooq.pg.Tables.ENTRY;

public class CompositionName extends CompositionAttribute {

    public CompositionName(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField(){
        //extract the composition name from the jsonb root key
        String trimName = "trim(LEADING '''' FROM (trim(TRAILING ''']' FROM\n" +
                " (regexp_split_to_array((select root_json_key from jsonb_object_keys(" + ENTRY.ENTRY_ + ") root_json_key where root_json_key like '/composition%')," +
                " 'and name/value=')) [2])))";
        //postgresql equivalent expression
        if (fieldContext.isWithAlias()) {
            Field<?> select = aliased(DSL.field(trimName));
            return select;
        } else {
            if (fieldContext.getClause().equals(I_QueryImpl.Clause.WHERE)) {
                trimName = "(SELECT " + trimName + ")";
            }
            return defaultAliased(DSL.field(trimName));
        }
    }

    @Override
    public I_RMObjectAttribute forTableField(TableField tableField) {
        return this;
    }
}
