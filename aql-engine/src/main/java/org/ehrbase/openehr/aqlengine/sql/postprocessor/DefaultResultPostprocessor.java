/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.openehr.aqlengine.sql.postprocessor;

import com.nedap.archie.rm.RMObject;
import org.ehrbase.openehr.dbformat.DbToRmFormat;
import org.jooq.JSONB;
import org.jooq.Record2;

/**
 * Handles JSONB and primitive result columns.
 * JSONB will be passed to {@link DbToRmFormat}. Everything else will not be altered.
 */
public class DefaultResultPostprocessor implements AqlSqlResultPostprocessor {
    @Override
    public Object postProcessColumn(Object columnValue) {

        return switch (columnValue) {
            case null -> null;
            case Record2[] rec -> DbToRmFormat.reconstructFromDbFormat(RMObject.class, rec);
            case JSONB jsonb -> DbToRmFormat.reconstructFromDbFormat(RMObject.class, jsonb.data());
            default -> columnValue;
        };
    }
}
