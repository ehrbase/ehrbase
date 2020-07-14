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

package org.ehrbase.opt;

import org.ehrbase.serialisation.util.SnakeCase;

/**
 * Created by christian on 3/8/2018.
 */
public class NodeId {

    String name;

    public NodeId(String name) {
        this.name = name;
    }

    /**
     * transform the name into an ehrscape pseudo id
     *
     * @return
     */
    public String ehrscape() {
        String ehrscapeId = name.replaceAll("/| |-", "_").toLowerCase();

        ehrscapeId = ehrscapeId.replaceAll("([_])\\1{1,}", "$1");

        return new SnakeCase(ehrscapeId).camelToSnake();
    }
}
