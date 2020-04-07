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
package org.ehrbase.aql.sql.queryImpl.attribute;

import org.ehrbase.aql.sql.queryImpl.attribute.eventcontext.SimpleEventContextAttribute;
import org.jooq.Field;
import org.jooq.TableField;
import org.jooq.impl.DSL;

public class TemporalWithTimeZone extends SimpleEventContextAttribute {

    private Field timeZoneField;

    public TemporalWithTimeZone(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    public Field<?> sqlField() {
        return as(DSL.field("timezone(COALESCE(" + timeZoneField + "::text,'UTC')," + tableField + "::timestamp)"));
    }

    public TemporalWithTimeZone useTimeZone(TableField tableField){
        this.timeZoneField = tableField;
        return this;
    }

    @Override
    public I_RMObjectAttribute forTableField(TableField tableField) {
        this.tableField = tableField;
        if (timeZoneField == null){
            String tzFieldName = tableField.getName().toUpperCase()+"_TZID"; //conventionally
            timeZoneField = DSL.field(tableField.getTable().getName()+"."+tzFieldName);
        }
        return this;
    }


}
