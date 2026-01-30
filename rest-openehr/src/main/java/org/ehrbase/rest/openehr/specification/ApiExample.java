/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
package org.ehrbase.rest.openehr.specification;

/**
 * API Example objects
 */
public class ApiExample {

    private ApiExample() {}

    static final String EHR_STATUS_JSON =
            """
        {
            "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
            "name": {
                "value": "EHR status"
            },
            "uid": {
                "_type": "OBJECT_VERSION_ID",
                "value": "9e3eb79b-1caa-4ab9-8cd4-d374b7c42bb4::local.ehrbase.org::1"
            },
            "subject": {
                "_type": "PARTY_SELF"
            },
            "is_queryable": true,
            "is_modifiable": true
        }
        """;
}
