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
import org.ehrbase.aql.sql.queryImpl.I_QueryImpl;
import org.ehrbase.aql.sql.queryImpl.attribute.FieldResolutionContext;
import org.ehrbase.aql.sql.queryImpl.attribute.GenericJsonPath;
import org.ehrbase.aql.sql.queryImpl.attribute.I_RMObjectAttribute;
import org.ehrbase.aql.sql.queryImpl.attribute.JoinSetup;
import org.jooq.Configuration;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import java.util.Optional;
import java.util.UUID;

import static org.ehrbase.aql.sql.queryImpl.AqlRoutines.*;
import static org.ehrbase.jooq.pg.Routines.jsEhr;
import static org.ehrbase.jooq.pg.Tables.EHR_;

public class FullEhrJson extends EhrAttribute {

    protected TableField tableField = EHR_.ID;
    protected Optional<String> jsonPath = Optional.empty();

    public FullEhrJson(FieldResolutionContext fieldContext, JoinSetup joinSetup) {
        super(fieldContext, joinSetup);
    }

    @Override
    public Field<?> sqlField() {
        fieldContext.setJsonDatablock(true);
        fieldContext.setRmType("EHR");

        //to retrieve DB dialect
        Configuration configuration = fieldContext.getContext().configuration();

        //query the json representation of EHR
        Field jsonFullEhr;

        if (jsonPath.isPresent()) {
            //deals with queries on arrays if any
            String path = jsonPath.get();
            String[] suffix = null;
            String[] prefix = null;
            if ((path.startsWith("'{compositions") && path.length() > "'{compositions}'".length())||
                (path.startsWith("'{contributions") && path.length() > "'{contributions}'".length())||
                (path.startsWith("'{folders") && path.length() > "'{folders}'".length())) {
                    String[] tokens = path.split(",", 2);

                    if (tokens.length != 2)
                        throw new IllegalArgumentException("Could not interpret:"+path);

                    prefix = jsonpathParameters(tokens[0]);
                    suffix = jsonpathParameters(tokens[1]);

                    if (isValuePath(suffix))
                        fieldContext.setJsonDatablock(false);
            }

            if (prefix != null) {
                  jsonFullEhr =
                        DSL.field(
                                jsonpathItemAsText(configuration,
                                        jsonArraySplitElements( configuration,
                                                jsonpathItem( configuration,
                                                        jsEhr(
                                                                DSL.field(I_JoinBinder.ehrRecordTable.getName().concat(".").concat(tableField.getName())).cast(UUID.class),
                                                                DSL.val(fieldContext.getServerNodeId())
                                                        ).cast(JSONB.class)
                                                        ,
                                                        prefix)
                                        )
                                        , suffix)
                        );

                if (fieldContext.getClause().equals(I_QueryImpl.Clause.WHERE))
                    jsonFullEhr = DSL.field(DSL.select(jsonFullEhr));
            }
            else
                jsonFullEhr = DSL.field(
                        jsonpathItem( configuration,
                            jsEhr(
                                DSL.field(I_JoinBinder.ehrRecordTable.getName().concat(".").concat(tableField.getName())).cast(UUID.class),
                                DSL.val(fieldContext.getServerNodeId())
                            ).cast(JSONB.class),
                            jsonpathParameters(jsonPath.get())
                        )
                );
        }
        else
            jsonFullEhr = DSL.field(
                    jsEhr(
                            DSL.field(I_JoinBinder.ehrRecordTable.getName().concat(".").concat(tableField.getName())).cast(UUID.class),
                            DSL.val(fieldContext.getServerNodeId())
                    ).cast(String.class)
            );

        if (fieldContext.isWithAlias())
            return aliased(DSL.field(jsonFullEhr));
        else
            return defaultAliased(jsonFullEhr);
    }

    @Override
    public I_RMObjectAttribute forTableField(TableField tableField) {
        this.tableField = tableField;
        return this;
    }

    public FullEhrJson forJsonPath(String jsonPath){
        if (jsonPath == null || jsonPath.isEmpty()) {
            this.jsonPath = Optional.empty();
            return this;
        }
        this.jsonPath = Optional.of(new GenericJsonPath(jsonPath).jqueryPath().replaceAll("/name,0,value", "name,value"));
        return this;
    }

    private boolean isValuePath(String[] pathItems){
        return pathItems[pathItems.length - 1].equals("'value'");
    }
}

