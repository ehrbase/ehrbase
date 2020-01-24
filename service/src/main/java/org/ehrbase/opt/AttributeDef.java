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

import org.apache.commons.lang3.StringUtils;
import org.ehrbase.ehr.encode.wrappers.SnakeCase;
import org.ehrbase.opt.mapper.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by christian on 4/25/2018.
 */
public class AttributeDef {

    String identifier;

    public AttributeDef(String identifier) {
        this.identifier = identifier;
    }

    public Map<String, String> naming() {

        Map<String, String> synonyms = new HashMap<>();

        synonyms.put(Constants.ATTRIBUTE, attributeIdentifier());
        synonyms.put(Constants.NAME, attributeName());
        synonyms.put(Constants.ID, attributeEhrScapeID());

        return synonyms;
    }

    public String attributeIdentifier() {
        return new SnakeCase(identifier).camelToSnake();
    }

    public String attributeName() {
        return StringUtils.capitalize(identifier);
    }

    public String attributeEhrScapeID() {
        return new NodeId(attributeIdentifier()).ehrscape();
    }

}
