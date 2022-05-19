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
package org.ehrbase.aql.sql.queryimpl.attribute.concept;

import java.util.Arrays;
import org.ehrbase.aql.sql.queryimpl.QueryImplConstants;
import org.ehrbase.aql.sql.queryimpl.attribute.AttributeResolver;
import org.ehrbase.aql.sql.queryimpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryimpl.attribute.JoinSetup;
import org.jooq.Field;
import org.jooq.TableField;

@SuppressWarnings({"java:S3776", "java:S3740", "java:S1452"})
public class ConceptResolver extends AttributeResolver {

    public static final String MAPPINGS = "mappings";
    TableField tableField;

    public ConceptResolver(FieldResolutionContext fieldResolutionContext, JoinSetup joinSetup) {
        super(fieldResolutionContext, joinSetup);
    }

    public Field<?> sqlField(String path) {

        if (path.isEmpty())
            return new ConceptJson(fieldResolutionContext, joinSetup)
                    .forTableField(tableField)
                    .sqlField();

        if (!path.equals(MAPPINGS) && path.startsWith(MAPPINGS)) {
            path = path.substring(path.indexOf(MAPPINGS) + MAPPINGS.length() + 1);
            // we insert a tag to indicate that the path operates on a json array
            fieldResolutionContext.setUsingSetReturningFunction(true); // to generate lateral join
            return new ConceptJson(fieldResolutionContext, joinSetup)
                    .forJsonPath("mappings/" + QueryImplConstants.AQL_NODE_ITERATIVE_MARKER + "/" + path)
                    .forTableField(tableField)
                    .sqlField();
        } else if (Arrays.asList(
                        "value",
                        MAPPINGS,
                        "defining_code",
                        "defining_code/terminology_id",
                        "defining_code/terminology_id/value",
                        "defining_code/code_string")
                .contains(path)) {
            Field sqlField = new ConceptJson(fieldResolutionContext, joinSetup)
                    .forJsonPath(path)
                    .forTableField(tableField)
                    .sqlField();
            if (path.equals("defining_code/terminology_id/value") || path.equals("value"))
                fieldResolutionContext.setJsonDatablock(false);
            return sqlField;
        } else throw new IllegalArgumentException("Unresolved concept attribute path:" + path);
    }

    public ConceptResolver forTableField(TableField tableField) {
        this.tableField = tableField;
        return this;
    }
}
