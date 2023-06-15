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
package org.ehrbase.aql.sql.queryimpl;

import static org.jooq.impl.SQLDataType.*;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.jooq.dbencoding.wrappers.json.writer.translator_db2raw.GenericRmType;
import org.jooq.DataType;

/**
 * Created by christian on 5/9/2018.
 */
public class PGType {

    public static final String MAGNITUDE = "magnitude";
    public static final String VALUE = "value";
    public static final String VALUE_VALUE = "/value,value";
    public static final String COMMA_VALUE = ",value";
    public static final String NUMERATOR = "numerator";
    public static final String DENOMINATOR = "denominator";
    List<String> segmentedPath;
    private final IQueryImpl.Clause clause;

    public PGType(List<String> segmentedPath, IQueryImpl.Clause clause) {
        this.segmentedPath = segmentedPath;
        this.clause = clause;
    }

    public DataType forRmType(String type) {
        String attribute = segmentedPath.get(segmentedPath.size() - 1);
        String actualType = type;
        DataType pgtype = null;

        if (new GenericRmType(type).isSpecialized()) {
            if (new GenericRmType(type).mainType().equals("DV_INTERVAL")
                    && (StringUtils.endsWith(attribute, "lower_unbounded")
                            || StringUtils.endsWith(attribute, "upper_unbounded"))) pgtype = BOOLEAN;
            else actualType = new GenericRmType(type).specializedWith();
        }

        if (pgtype == null) {
            pgtype = resolvePgTypeFromRmType(actualType, attribute);
        }

        // smart guess(?)
        if (pgtype == null && attribute.endsWith(MAGNITUDE)) // this may happen when we have a choice...
        pgtype = NUMERIC;

        return pgtype;
    }

    DataType resolvePgTypeFromRmType(String type, String attribute) {

        DataType pgtype = null;

        switch (type) {
            case "DV_QUANTITY":
                if (StringUtils.endsWith(attribute, MAGNITUDE)) pgtype = NUMERIC;
                break;
            case "DV_PROPORTION":
                if (StringUtils.endsWith(attribute, NUMERATOR) || StringUtils.endsWith(attribute, DENOMINATOR))
                    pgtype = NUMERIC;
                break;
            case "DV_COUNT":
                if (StringUtils.endsWith(attribute, MAGNITUDE)) pgtype = BIGINT;
                break;
            case "DV_ORDINAL":
                if (StringUtils.endsWith(attribute, VALUE_VALUE)) pgtype = BIGINT;
                break;
            case "DV_BOOLEAN":
                if (StringUtils.endsWith(attribute, COMMA_VALUE)) pgtype = BOOLEAN;
                break;
            case "DV_DURATION":
                // we cast to pg interval only in WHERE clause
                // interval type is handled by jOOQ with a built-in type and
                // wrongly formatted when rendering as it looses the ISO_8601 formatting (YearToSecond jOOQ class)
                if (StringUtils.endsWith(attribute, COMMA_VALUE)) {
                    if (clause.equals(IQueryImpl.Clause.WHERE)) pgtype = INTERVAL;
                    else pgtype = VARCHAR;
                }
                break;
            default:
                break;
        }

        if (pgtype == null && attribute.endsWith(MAGNITUDE)) // this may happen when we have a choice...
        pgtype = NUMERIC;

        return pgtype;
    }
}
