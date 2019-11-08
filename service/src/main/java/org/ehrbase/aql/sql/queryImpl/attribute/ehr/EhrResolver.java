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
package org.ehrbase.aql.sql.queryImpl.attribute.ehr;

import org.ehrbase.aql.sql.binding.I_JoinBinder;
import org.ehrbase.aql.sql.queryImpl.attribute.*;
import org.ehrbase.aql.sql.queryImpl.attribute.composition.SimpleCompositionAttribute;
import org.ehrbase.aql.sql.queryImpl.attribute.ehr.ehrstatus.StatusResolver;
import org.ehrbase.aql.sql.queryImpl.attribute.system.SystemResolver;
import org.ehrbase.aql.sql.queryImpl.value_field.GenericJsonField;
import org.ehrbase.aql.sql.queryImpl.value_field.SimpleAttribute;
import org.ehrbase.jooq.pg.tables.Ehr;
import org.jooq.Field;

import static org.ehrbase.jooq.pg.Ehr.EHR;
import static org.ehrbase.jooq.pg.Tables.EHR_;

public class EhrResolver extends AttributeResolver
{

    public EhrResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
    }

    public Field<?> sqlField(String path){

        if (path.startsWith("ehr_status")) {
            return new StatusResolver(fieldResolutionContext, joinSetup).sqlField(new AttributePath("ehr_status").redux(path));
        }
        else if (path.startsWith("system_id"))
            return new SystemResolver(fieldResolutionContext, joinSetup).sqlField(new AttributePath("system_id").redux(path));

        switch (path){
            case "ehr_id":
                joinSetup.setJoinEhr(true);
                return new GenericJsonField(fieldResolutionContext, joinSetup).jsonField("HIER_OBJECT_ID", "ehr.js_canonical_hier_object_id", I_JoinBinder.ehrRecordTable.field(EHR_.ID));
            case "ehr_id/value":
                return new EhrIdValue(fieldResolutionContext, joinSetup).forTableField(NULL_FIELD).sqlField();
            case "time_created":
                joinSetup.setJoinEhr(true);
                return new GenericJsonField(fieldResolutionContext, joinSetup)
                        .jsonField("DV_DATE_TIME", "ehr.js_dv_date_time", I_JoinBinder.ehrRecordTable.field(EHR_.DATE_CREATED), I_JoinBinder.ehrRecordTable.field(EHR_.DATE_CREATED_TZID));
            case "time_created/value":
                joinSetup.setJoinEhr(true);
                return new SimpleAttribute(fieldResolutionContext, joinSetup)
                        .forTableField("TEXT", I_JoinBinder.ehrRecordTable.field(EHR_.DATE_CREATED))
                        .sqlField();

        }
        throw new IllegalArgumentException("Unresolved ehr attribute path:"+path);
    }
}
