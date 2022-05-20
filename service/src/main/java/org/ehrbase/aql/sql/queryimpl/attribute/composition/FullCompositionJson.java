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
package org.ehrbase.aql.sql.queryimpl.attribute.composition;

import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.jsonpathItemAsText;
import static org.ehrbase.aql.sql.queryimpl.AqlRoutines.jsonpathParameters;
import static org.ehrbase.jooq.pg.Routines.jsComposition2;
import static org.ehrbase.jooq.pg.Tables.COMPOSITION;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.aql.sql.binding.JoinBinder;
import org.ehrbase.aql.sql.queryimpl.attribute.*;
import org.jooq.Configuration;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.TableField;
import org.jooq.impl.DSL;

@SuppressWarnings({"java:S3776", "java:S3740"})
public class FullCompositionJson extends CompositionAttribute {

    protected TableField tableField = COMPOSITION.ID;
    protected Optional<String> jsonPath = Optional.empty();
    private boolean isJsonDataBlock = true; // by default, can be overriden

    public FullCompositionJson(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        fieldContext.setJsonDatablock(true);
        fieldContext.setRmType("COMPOSITION");
        // to retrieve DB dialect
        Configuration configuration = fieldContext.getContext().configuration();

        // query the json representation of EVENT_CONTEXT and cast the result as TEXT
        Field jsonFullComposition;

        if (jsonPath.isPresent()) {
            jsonFullComposition = DSL.field(jsonpathItemAsText(
                    configuration,
                    jsComposition2(
                                    DSL.field(JoinBinder.compositionRecordTable.getName() + "." + tableField.getName())
                                            .cast(UUID.class),
                                    DSL.val(fieldContext.getServerNodeId()))
                            .cast(JSONB.class),
                    jsonpathParameters(jsonPath.get())));
        } else
            jsonFullComposition = DSL.field(jsComposition2(
                            DSL.field(JoinBinder.compositionRecordTable.getName() + "." + tableField.getName())
                                    .cast(UUID.class),
                            DSL.val(fieldContext.getServerNodeId()))
                    .cast(String.class));

        if (fieldContext.isWithAlias()) return aliased(DSL.field(jsonFullComposition));
        else return defaultAliased(jsonFullComposition);
    }

    @Override
    public IRMObjectAttribute forTableField(TableField tableField) {
        this.tableField = tableField;
        return this;
    }

    public FullCompositionJson forJsonPath(String jsonPath) {
        if (jsonPath == null || jsonPath.isEmpty()) {
            this.jsonPath = Optional.empty();
            return this;
        }
        this.jsonPath = Optional.of(new GenericJsonPath(jsonPath).jqueryPath());
        return this;
    }

    public FullCompositionJson forJsonPath(String[] path) {
        if (GenericJsonPath.isTerminalValue(Arrays.asList(path), path.length - 1)) isJsonDataBlock = false;
        this.jsonPath = Optional.of(new JsonbSelect(Arrays.asList(path)).field());
        return this;
    }

    public boolean isJsonDataBlock() {
        return isJsonDataBlock;
    }
}
