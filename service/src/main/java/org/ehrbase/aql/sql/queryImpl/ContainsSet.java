/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
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

package org.ehrbase.aql.sql.queryImpl;

import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.impl.DSL;

import java.util.UUID;

import static org.ehrbase.jooq.pg.Tables.CONTAINMENT;
import static org.ehrbase.jooq.pg.Tables.ENTRY;

/**
 * Created by christian on 10/19/2016.
 */
public class ContainsSet {

    public static final String ENTRY_ROOT = "entry_root";

    private String containClause;
    private DSLContext context;
    private Select<?> select;

    public ContainsSet(String containsClause, DSLContext context) {
        this.containClause = containsClause;
        this.context = context;

        //jOOQ hack to support Postgres DISTINCT ON

        Field<?> distinctOnTemplate = DSL.field("DISTINCT ON({0}) {0}", ENTRY.TEMPLATE_ID.getDataType(), ENTRY.TEMPLATE_ID).as(ENTRY.TEMPLATE_ID);
        //(select root_json_key from jsonb_object_keys("ehr"."entry"."entry") root_json_key where root_json_key like '/composition%') as
//        Query jsonbRootKeySelect = DSL.query("select root_json_key from jsonb_object_keys(" + ENTRY.ENTRY_ + ") root_json_key where root_json_key like '/composition%')");
        Field<?> entryKey = DSL.field("(select root_json_key from jsonb_object_keys(" + ENTRY.ENTRY_ + ") root_json_key where root_json_key like '/composition%')").as(ENTRY_ROOT);
//        Field<?> entryKey = DSL.field("jsonb_object_keys("+ENTRY.ENTRY_+")").as(ENTRY_ROOT); //calculated field

        this.select = context
                .select(distinctOnTemplate, CONTAINMENT.COMP_ID, CONTAINMENT.LABEL, entryKey)
                .from(CONTAINMENT)
                .join(ENTRY)
                .on(CONTAINMENT.COMP_ID.eq(ENTRY.COMPOSITION_ID))
                .where(CONTAINMENT.COMP_ID.in(DSL.field(containClause)).and(DSL.field(ENTRY.ENTRY_ + "::text").ne("{}")));
    }

    public Result<?> getInSet() {
        if (StringUtils.isNotBlank(containClause)) {
            return select.fetch();
        } else {
            // If the AQL has no contains we need a dummy Record
            Result<Record4<String, UUID, Object, Object>> result = context.newResult(ENTRY.TEMPLATE_ID, CONTAINMENT.COMP_ID, CONTAINMENT.LABEL, DSL.field("entry_root"));
            result.add(context.newRecord(ENTRY.TEMPLATE_ID, CONTAINMENT.COMP_ID, CONTAINMENT.LABEL, DSL.field("entry_root")).values("*", null, "", ""));
            return result;
        }
    }

    public Select<?> getSelect() {
        //could not find the way to clone an existing select
        return context
                .selectDistinct(CONTAINMENT.COMP_ID)
                .from(CONTAINMENT)
                .join(ENTRY)
                .on(CONTAINMENT.COMP_ID.eq(ENTRY.COMPOSITION_ID))
                .where(CONTAINMENT.COMP_ID.in(DSL.field(containClause)));
    }
}
