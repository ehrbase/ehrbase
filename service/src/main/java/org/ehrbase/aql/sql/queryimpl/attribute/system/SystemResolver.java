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
package org.ehrbase.aql.sql.queryimpl.attribute.system;

import static org.ehrbase.jooq.pg.Tables.EHR_;
import static org.ehrbase.jooq.pg.tables.System.SYSTEM;

import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributeResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.value_field.GenericJsonField;
import org.jooq.Field;

@SuppressWarnings({"java:S3740", "java:S1452"})
public class SystemResolver extends AttributeResolver {

    public SystemResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
        joinSetup.setJoinSystem(true);
        joinSetup.setJoinEhr(true);
    }

    public Field<?> sqlField(String path) {

        if (path.isEmpty()) {
            return new GenericJsonField(fieldResolutionContext, joinSetup)
                    .hierObjectId(JoinBinder.ehrRecordTable.field(EHR_.SYSTEM_ID));
        }

        switch (path) {
            case "value":
                return new GenericJsonField(fieldResolutionContext, joinSetup)
                        .forJsonPath("value")
                        .hierObjectId(JoinBinder.ehrRecordTable.field(EHR_.SYSTEM_ID));
            case "description":
                return new SystemAttribute(fieldResolutionContext, joinSetup)
                        .forTableField(SYSTEM.DESCRIPTION)
                        .sqlField();
            default:
                throw new IllegalArgumentException("Unresolved system attribute path:" + path);
        }
    }
}
