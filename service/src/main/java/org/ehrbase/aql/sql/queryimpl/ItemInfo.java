/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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

import java.io.Serializable;

public class ItemInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String itemType;
    private final String itemCategory;

    public ItemInfo(String itemType, String itemCategory) {
        this.itemType = itemType;
        this.itemCategory = itemCategory;
    }

    public String getItemType() {
        return itemType;
    }

    public String getItemCategory() {
        return itemCategory;
    }
}
