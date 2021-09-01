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

    protected String itemType = null; //the actual data type as specified in the template
    protected String itemCategory = null; //the category of the object as per the WebTemplate (f.e. CLUSTER, ELEMENT ...)
    private Field<?> field; //the actual field

    public QualifiedAqlField(Field<?> field) {
        this.field = field;
    }

    public QualifiedAqlField(Field<?> field, String itemType, String itemCategory) {
        this.field = field;
        this.itemType = itemType;
        this.itemCategory = itemCategory;
    }

    public Field<?> getSQLField() {
        return field;
    }

    public void setField(Field<?> field) {
        this.field = field;
    }

    public QualifiedAqlField duplicate() {
        return new QualifiedAqlField(this.getSQLField(), this.itemType, this.itemCategory);
    }

    public boolean isQualified() {
        return itemType != null && itemCategory != null;
    }
}
