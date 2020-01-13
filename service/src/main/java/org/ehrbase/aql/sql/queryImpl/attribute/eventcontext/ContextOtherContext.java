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
package org.ehrbase.aql.sql.queryImpl.attribute.eventcontext;


import org.ehrbase.aql.sql.queryImpl.JsonbEntryQuery;
import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.I_RMObjectAttribute;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;
import org.jooq.Field;
import org.jooq.TableField;

import static org.ehrbase.jooq.pg.Tables.EVENT_CONTEXT;

public class ContextOtherContext extends EventContextAttribute {

    public ContextOtherContext(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        String variablePath = fieldContext.getVariableDefinition().getPath().substring("context/other_context".length());


        if (variablePath.startsWith("/"))
            variablePath = variablePath.substring(1);
        return new EventContextJson(fieldContext, joinSetup).forJsonPath("other_context/"+variablePath).forTableField(EVENT_CONTEXT.OTHER_CONTEXT).sqlField();
    }

    @Override
    public I_RMObjectAttribute forTableField(TableField tableField) {
        return this;
    }
}
