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
package org.ehrbase.aql.sql.queryimpl.attribute.ehr.ehrstatus;

import static org.ehrbase.jooq.pg.Tables.STATUS;

import org.ehrbase.aql.sql.queryimpl.attribute.AttributePath;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributeResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.ehrbase.aql.sql.queryimpl.attribute.ehr.ehrstatus.subject.SubjectResolver;
import org.jooq.Field;

@SuppressWarnings({"java:S3740", "java:S1452"})
public class StatusResolver extends AttributeResolver {

    public StatusResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
    }

    public Field<?> sqlField(String path) {

        try {
            if (path.startsWith("other_details")) {
                return new EhrStatusOtherDetails(fieldResolutionContext, joinSetup)
                        .forTableField(NULL_FIELD)
                        .sqlField();
            } else if (path.startsWith("subject")) {
                return new SubjectResolver(fieldResolutionContext, joinSetup)
                        .sqlField(new AttributePath("subject").redux(path));
            } else if (path.isEmpty()) {
                return new EhrStatusJson(fieldResolutionContext, joinSetup).sqlField();
            } else if (path.equals("is_queryable")) {
                return new SimpleEhrStatusAttribute(fieldResolutionContext, joinSetup)
                        .forTableField(STATUS.IS_QUERYABLE)
                        .sqlField();
            } else if (path.equals("is_modifiable")) {
                return new SimpleEhrStatusAttribute(fieldResolutionContext, joinSetup)
                        .forTableField(STATUS.IS_MODIFIABLE)
                        .sqlField();
            } else
                return new EhrStatusJson(fieldResolutionContext, joinSetup)
                        .forJsonPath(path)
                        .sqlField();
        } catch (IllegalArgumentException e) {
            return new EhrStatusJson(fieldResolutionContext, joinSetup)
                    .forJsonPath(path)
                    .sqlField();
        }
    }
}
