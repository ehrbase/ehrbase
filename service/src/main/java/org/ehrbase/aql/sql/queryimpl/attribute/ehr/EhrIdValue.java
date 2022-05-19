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
package org.ehrbase.aql.sql.queryimpl.attribute.ehr;

import static org.ehrbase.jooq.pg.Tables.EHR_;

import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.IRMObjectAttribute;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.DSL;

public class EhrIdValue extends EhrAttribute {

    public EhrIdValue(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        ehrSetup.setContainsEhrId(true);
        ehrSetup.setEhrIdAlias(effectiveAlias());
        if (fieldContext.getPathResolver().hasPathExpression()) {
            joinSetup.setJoinEhr(true);
            if (fieldContext.isWithAlias()) {
                return aliased(DSL.field("{0}", JoinBinder.ehrRecordTable.field(EHR_.ID.getName())));
            } else
                return defaultAliased(
                        DSL.field(JoinBinder.ehrRecordTable.field(JoinBinder.ehrRecordTable.field(EHR_.ID.getName()))));
        } else if (!joinSetup.isContainsEhrStatus()) {
            joinSetup.setJoinEhr(true);
            if (fieldContext.isWithAlias()) {
                return aliased(DSL.field("{0}", JoinBinder.ehrRecordTable.field(EHR_.ID.getName())));
            } else return defaultAliased(DSL.field(JoinBinder.ehrRecordTable.field(EHR_.ID.getName())));
        } else {
            if (fieldContext.isWithAlias()) {
                return aliased(DSL.field("{0}", JoinBinder.ehrRecordTable.field(EHR_.ID.getName())));
            } else return defaultAliased(DSL.field(JoinBinder.ehrRecordTable.field(EHR_.ID.getName())));
        }
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        return this;
    }
}
