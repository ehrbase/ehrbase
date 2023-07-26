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
package org.ehrbase.jooq.dbencoding.wrappers.json;

import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;

/**
 * Created by christian on 10/6/2016.
 */
public interface I_DvTypeAdapter {

    enum AdapterType {
        PG_JSONB,
        RAW_JSON,
        _DBJSON2RAWJSON,
        DBJSON2RAWJSON
    }

    String matchNodePredicate =
            "/(content|protocol|events|data|description|instruction|items|activities|activity|composition|entry|evaluation|observation|action|at)\\[([(0-9)|(A-Z)|(a-z)|\\-|_|\\.]*)\\]";

    String TAG_CLASS_RAW_JSON = "_type";

    String NAME = "name";
    String AT_TYPE = "_type";
    String AT_CLASS = AT_TYPE;
    String ARCHETYPE_NODE_ID = "archetype_node_id";
    String ITEMS = "items";
    String EVENTS = "events";
    String VALUE = "value";
    String ELEMENT = RmConstants.ELEMENT;
}
