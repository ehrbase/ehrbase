/*
 *  Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 *  This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and  limitations under the License.
 *
 */

package org.ehrbase.aql.sql.queryimpl;

import org.jooq.Field;

public class QualifiedAqlField {

    private boolean containsJqueryPath = false; //true if at leas one AQL path is contained in expression
    protected boolean jsonDataBlock = false; //true if the field reference a json structure
    protected String itemType = null; //the actual data type as specified in the template
    protected String itemCategory = null; //the category of the object as per the WebTemplate (f.e. CLUSTER, ELEMENT ...)
    private Field<?> field; //the actual field
    private String jsonbItemPath; //this is the actual label on a jsonb column by default
    private String optionalPath;

    public QualifiedAqlField(Field<?> field) {
        this.field = field;
    }

    public QualifiedAqlField(Field<?> field, String itemType, String itemCategory, boolean jsonDataBlock, boolean containsJqueryPath, String jsonbItemPath) {
        this.field = field;
        this.itemType = itemType;
        this.itemCategory = itemCategory;
        this.jsonDataBlock = jsonDataBlock;
        this.containsJqueryPath = containsJqueryPath;
        this.jsonbItemPath = jsonbItemPath;
    }

    public void setContainsJqueryPath(boolean containsJqueryPath) {
        this.containsJqueryPath = containsJqueryPath;
    }

    public void setJsonDataBlock(boolean jsonDataBlock) {
        this.jsonDataBlock = jsonDataBlock;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }

    public boolean isContainsJqueryPath() {
        return containsJqueryPath;
    }

    public boolean isJsonDataBlock() {
        return jsonDataBlock;
    }

    public String getItemType() {
        return itemType;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public Field<?> getSQLField() {
        return field;
    }

    public void setJsonbItemPath(String jsonbItemPath) {
        this.jsonbItemPath = jsonbItemPath;
    }


    public String getJsonbItemPath() {
        return jsonbItemPath;
    }


    public String getOptionalPath() {
        return optionalPath;
    }

    public void setOptionalPath(String optionalPath) {
        this.optionalPath = optionalPath;
    }

    public void setField(Field<?> field) {
        this.field = field;
    }
}
