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
package org.ehrbase.jooq.dbencoding.wrappers.json.writer.translator_db2raw;

public class CompositionRoot {
    String dbEncoded;

    public CompositionRoot(String dbEncoded) {
        this.dbEncoded = dbEncoded;
    }

    public String toString() {

        if (!dbEncoded.contains("/composition")) return "";

        int beginIndex = dbEncoded.indexOf("/composition");
        int endIndex = dbEncoded.indexOf("]", beginIndex) + 1;
        return dbEncoded.substring(beginIndex, endIndex).replace("\\u003d", "=").replace("\\u0027", "'");
    }
}
