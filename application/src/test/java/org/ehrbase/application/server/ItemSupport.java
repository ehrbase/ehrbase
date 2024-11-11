/*
 * Copyright (c) 2024 vitasystems GmbH.
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
package org.ehrbase.application.server;

import org.hibernate.validator.constraints.UUID;

public class ItemSupport {
    static String ITEM_ID = "_ITEM_ID_";
    static String ITEM_NS = "_ITEM_NS_";

    static String ITEM_TEMPLATE =
            """
    {
      "id": {
        "_type": "HIER_OBJECT_ID",
        "value": "_ITEM_ID_"
      },
      "namespace": "ITEM_NS",
      "type": "VERSIONED_COMPOSITION"
    }
	""";

    public static String create(UUID itemUUID, String namespace) {
        return ITEM_TEMPLATE.replace(ITEM_ID, itemUUID.toString()).replace(ITEM_NS, namespace);
    }

    public static String create(String itemUUID, String namespace) {
        return ITEM_TEMPLATE.replace(ITEM_ID, itemUUID).replace(ITEM_NS, namespace);
    }
}
