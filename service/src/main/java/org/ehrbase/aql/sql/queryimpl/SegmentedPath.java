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

import java.util.List;

/**
 * Created by christian on 5/9/2018.
 */
public class SegmentedPath {

    List<String> segmentedPathExpression;

    public SegmentedPath(List<String> segmentedPathExpression) {
        this.segmentedPathExpression = segmentedPathExpression;
    }

    public String reduce() {

        StringBuilder stringBuilder = new StringBuilder();

        for (String segment : segmentedPathExpression) {

            if (segment.startsWith("/feeder_audit")) {
                stringBuilder.append("/feeder_audit"); // sub-field are undecidable including type in other_details
                break;
            }

            if (segment.startsWith("/composition")
                    || segment.startsWith("/value")
                    || (!segment.contains("[") && !segment.contains("]"))
                    || !segment.startsWith("/")) continue;

            stringBuilder.append(segment);
        }

        return stringBuilder.toString();
    }
}
