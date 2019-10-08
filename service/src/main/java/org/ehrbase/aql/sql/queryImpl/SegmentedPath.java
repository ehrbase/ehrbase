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

import java.util.List;

/**
 * Created by christian on 5/9/2018.
 */
public class SegmentedPath {

    List<String> segmentedPath;

    public SegmentedPath(List<String> segmentedPath) {
        this.segmentedPath = segmentedPath;
    }

    public String reduce() {

        StringBuffer stringBuffer = new StringBuffer();

        for (String segment : segmentedPath) {

            if (segment.startsWith("/composition"))
                continue;
            if (segment.startsWith("/value"))
                continue;
            if (!segment.contains("[") && !segment.contains("]"))
                continue;
            if (!segment.startsWith("/"))
                continue;
            stringBuffer.append(segment);
        }

        return stringBuffer.toString();

    }
}
