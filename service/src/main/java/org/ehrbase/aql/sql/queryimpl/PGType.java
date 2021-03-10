/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.aql.sql.queryimpl;

import com.nedap.archie.rm.datavalues.quantity.DvInterval;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.serialisation.dbencoding.wrappers.json.writer.translator_db2raw.GenericRmType;
import org.jooq.DataType;

import java.util.List;

import static org.jooq.impl.SQLDataType.*;

/**
 * Created by christian on 5/9/2018.
 */
public class PGType {

    List<String> segmentedPath;

    public PGType(List<String> segmentedPath) {
        this.segmentedPath = segmentedPath;
    }

    public DataType forRmType(String type) {
        String attribute = segmentedPath.get(segmentedPath.size() - 1);
        DataType pgtype = null;

        if (new GenericRmType(type).isSpecialized()){
            if (new GenericRmType(type).mainType().equals("DV_INTERVAL") &&
                    (StringUtils.endsWith(attribute, "lower_unbounded") || StringUtils.endsWith(attribute,"upper_unbounded")))
                pgtype = BOOLEAN;
            else
                type = new GenericRmType(type).specializedWith();
        }
        if (pgtype == null) {
            switch (type) {
                case "DV_QUANTITY":
                    if (StringUtils.endsWith(attribute, "magnitude"))
                        pgtype = REAL;
                    break;
                case "DV_PROPORTION":
                    if (StringUtils.endsWith(attribute, "numerator") || StringUtils.endsWith(attribute, "denominator"))
                        pgtype = REAL;
                    break;
                case "DV_COUNT":
                    if (StringUtils.endsWith(attribute, "magnitude"))
                        pgtype = BIGINT;
                    break;
                case "DV_ORDINAL":
                    if (StringUtils.endsWith(attribute, "value"))
                        pgtype = BIGINT;
                    break;
                case "DV_BOOLEAN":
                    if (StringUtils.endsWith(attribute, "value"))
                        pgtype = BOOLEAN;
                    break;
                default:
                    break;
            }
        }

        return pgtype;
    }
}
