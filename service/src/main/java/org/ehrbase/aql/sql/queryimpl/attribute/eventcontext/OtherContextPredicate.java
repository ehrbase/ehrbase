/*
 * Copyright (c) 2020-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.queryimpl.attribute.eventcontext;

import org.ehrbase.openehr.sdk.aql.webtemplatepath.AqlPath;

/**
 * @author Christian Chevalley
 * @author Renaud Subiger
 * @since 1.0
 */
public class OtherContextPredicate {

    private final String path;

    public OtherContextPredicate(String path) {
        this.path = path;
    }

    public String adjustForQuery() {
        var aqlPath = AqlPath.parse(path);
        var aqlNodes = aqlPath.getNodes();

        for (int i = 0; i < aqlNodes.size(); i++) {
            if ("other_context".equals(aqlNodes.get(i).getName())) {
                aqlPath = aqlPath.replaceNode(i, aqlNodes.get(i).withAtCode(null));
            }
        }

        // Removes initial "/" if not present in original path
        var result = aqlPath.format(true);
        return path.startsWith("/") ? result : result.substring(1);
    }
}
