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
package org.ehrbase.aql.sql.queryImpl.attribute.system;

import org.ehrbase.aql.sql.queryImpl.attribute.AttributeResolver;
import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;
import org.jooq.Field;

import static org.ehrbase.jooq.pg.tables.System.SYSTEM;

public class SystemResolver extends AttributeResolver
{

    public SystemResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
        joinSetup.setJoinSystem(true);
        joinSetup.setJoinEhr(true);
    }

    public Field<?> sqlField(String path){

        switch (path){
            case "value":
                return new SystemAttribute(fieldResolutionContext, joinSetup).forTableField(SYSTEM.SETTINGS).sqlField();
            case "description":
                return new SystemAttribute(fieldResolutionContext, joinSetup).forTableField(SYSTEM.DESCRIPTION).sqlField();
        }
        throw new IllegalArgumentException("Unresolved system attribute path:"+path);
    }
}
