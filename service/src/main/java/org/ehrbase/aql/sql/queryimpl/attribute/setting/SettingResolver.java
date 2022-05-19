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
package org.ehrbase.aql.sql.queryimpl.attribute.setting;

import static org.ehrbase.jooq.pg.tables.EventContext.EVENT_CONTEXT;

import org.ehrbase.aql.sql.queryimpl.QueryImplConstants;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributeResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.attribute.eventcontext.EventContextJson;
import org.jooq.Field;

@SuppressWarnings({"java:S3740", "java:S1452"})
public class SettingResolver extends AttributeResolver {

    public static final String MAPPINGS = "mappings";

    public SettingResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
    }

    public Field<?> sqlField(String path) {

        Field<?> retField;

        if (path.isEmpty())
            return new EventContextJson(fieldResolutionContext, joinSetup)
                    .forJsonPath("setting")
                    .sqlField();

        if (!path.equals(MAPPINGS) && path.startsWith(MAPPINGS)) {
            path = path.substring(path.indexOf(MAPPINGS) + MAPPINGS.length() + 1);
            // we insert a tag to indicate that the path operates on a json array
            fieldResolutionContext.setUsingSetReturningFunction(true); // to generate lateral join
            retField = new EventContextJson(fieldResolutionContext, joinSetup)
                    .forJsonPath("setting/mappings/" + QueryImplConstants.AQL_NODE_ITERATIVE_MARKER + "/" + path)
                    .forTableField(EVENT_CONTEXT.SETTING)
                    .sqlField();
        } else {
            retField = new EventContextJson(fieldResolutionContext, joinSetup)
                    .forJsonPath("setting/" + path)
                    .forTableField(EVENT_CONTEXT.SETTING)
                    .sqlField();
        }

        return retField;
    }
}
