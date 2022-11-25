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
package org.ehrbase.aql.sql.binding;

/**
 * String container with a qualifier
 * <p>
 * the qualifier is used to identify a query part strategy (SQL or JSON)
 * </p>
 * Created by christian on 5/20/2016.
 */
public interface I_TaggedStringBuilder {

    StringBuilder append(String string);

    void replaceLast(String previous, String newString);

    int lastIndexOf(String string);

    int indexOf(String string);

    void replace(String previous, String newString);

    void replace(Integer start, Integer end, String replacement);

    void insert(Integer offset, String toInsert);

    String toString();

    int length();

    TagField getTagField();

    void setTagField(TagField tagField);

    boolean startWith(String tag);

    enum TagField {
        JSQUERY,
        SQLQUERY
    }
}
