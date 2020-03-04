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

package org.ehrbase.aql.sql.queryImpl;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Created by christian on 5/9/2018.
 */
public class PGType {

    List<String> segmentedPath;

    public PGType(List<String> segmentedPath) {
        this.segmentedPath = segmentedPath;
    }

    public String forRmType(String type) {
        String attribute = segmentedPath.get(segmentedPath.size() - 1);
        String pgtype = null;

        switch (type) {
            case "DV_QUANTITY":
                if (StringUtils.endsWith(attribute, "magnitude"))
                    pgtype = "real";
                break;
            case "DV_PROPORTION":
                if (StringUtils.endsWith(attribute, "numerator"))
                    pgtype = "real";
                else if (StringUtils.endsWith(attribute, "denominator"))
                    pgtype = "real";
                break;
            case "DV_COUNT":
                if (StringUtils.endsWith(attribute, "magnitude"))
                    pgtype = "int8";
                break;
            case "DV_ORDINAL":
                if (StringUtils.endsWith(attribute, "value"))
                    pgtype = "int8";
                break;
        }

        return pgtype;
    }
}
