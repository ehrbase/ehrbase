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

import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;

public class ArrayClosure {

    JsonWriter writer;
    private String parentItemsArchetypeNodeId;
    private String parentItemsType;

    ArrayClosure(JsonWriter writer, String parentItemsArchetypeNodeId, String parentItemsType) {
        this.writer = writer;
        this.parentItemsArchetypeNodeId = parentItemsArchetypeNodeId;
        this.parentItemsType = parentItemsType;
    }

    /**
     * close an item array
     */
    private void close() throws IOException {
        if (parentItemsArchetypeNodeId != null)
            writer.name(I_DvTypeAdapter.ARCHETYPE_NODE_ID).value(parentItemsArchetypeNodeId);
        if (parentItemsType != null) writer.name(I_DvTypeAdapter.AT_CLASS).value(parentItemsType);
    }

    public void start() throws IOException {
        close();
    }
}
