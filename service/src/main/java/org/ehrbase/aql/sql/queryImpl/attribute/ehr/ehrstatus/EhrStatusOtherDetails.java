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
package org.ehrbase.aql.sql.queryImpl.attribute.ehr.ehrstatus;


import org.ehrbase.aql.sql.queryImpl.JsonbEntryQuery;
import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.I_RMObjectAttribute;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;
import org.jooq.Field;
import org.jooq.TableField;

public class EhrStatusOtherDetails extends EhrStatusAttribute {

    public EhrStatusOtherDetails(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
        joinSetup.setContainsEhrStatus(true);
    }

    @Override
    public Field<?> sqlField(){
        fieldContext.setJsonDatablock(true);
        String variablePath = fieldContext.getVariableDefinition().getPath().substring("ehr_status/other_details".length() + 1);
        Field<?> field = new JsonbEntryQuery(fieldContext.getContext(), fieldContext.getIntrospectCache(), fieldContext.getPathResolver(), fieldContext.getEntry_root())
                .makeField(JsonbEntryQuery.OTHER_ITEM.OTHER_DETAILS, null, fieldContext.getVariableDefinition().getAlias(), variablePath, fieldContext.isWithAlias());
        return field;
    }

    @Override
    public I_RMObjectAttribute forTableField(TableField tableField) {
        return this;
    }
}
