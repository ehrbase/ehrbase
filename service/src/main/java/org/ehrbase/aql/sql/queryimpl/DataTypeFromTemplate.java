/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
import org.ehrbase.service.IntrospectService;
import org.jooq.DataType;

public class DataTypeFromTemplate {

    private final IntrospectService introspectCache;
    private String itemType;
    private String itemCategory;
    private DataType identifiedType = null;
    private final boolean ignoreUnresolvedIntrospect;
    private final IQueryImpl.Clause clause;

    public DataTypeFromTemplate(
            IntrospectService introspectCache, boolean ignoreUnresolvedIntrospect, IQueryImpl.Clause clause) {
        this.introspectCache = introspectCache;
        this.ignoreUnresolvedIntrospect = ignoreUnresolvedIntrospect;
        this.clause = clause;
    }

    public void evaluate(String templateId, List<String> referenceItemPathArray) {
        // type casting from introspected data value type
        try {
            if (introspectCache == null) throw new IllegalArgumentException("MetaDataCache is not initialized");
            String reducedItemPathArray = new SegmentedPath(referenceItemPathArray).reduce();
            if (reducedItemPathArray != null && !reducedItemPathArray.isEmpty()) {
                ItemInfo info = introspectCache.getInfo(templateId, reducedItemPathArray);

                itemType = info.getItemType();
                itemCategory = info.getItemCategory();
                if (itemType != null) {
                    identifiedType = new PGType(referenceItemPathArray, clause).forRmType(itemType);
                } else {
                    identifiedType = new PGType(referenceItemPathArray, clause).forRmType("UNKNOWN");
                }
            }
        } catch (Exception e) {
            if (!ignoreUnresolvedIntrospect)
                throw new IllegalArgumentException(
                        "Unresolved type, missing template?(" + templateId + "), reason:" + e);
        }
    }

    public String getItemType() {
        return itemType;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public DataType getIdentifiedType() {
        return identifiedType;
    }
}
