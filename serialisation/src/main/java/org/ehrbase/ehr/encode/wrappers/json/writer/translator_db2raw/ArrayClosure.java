/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.ehr.encode.wrappers.json.writer.translator_db2raw;

import com.google.gson.stream.JsonWriter;
import com.nedap.archie.rm.datastructures.Cluster;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import org.ehrbase.ehr.encode.wrappers.json.I_DvTypeAdapter;

import java.io.IOException;

public class ArrayClosure {

    JsonWriter writer;
    String parentItemsArchetypeNodeId = null;
    String parentItemsType = null;
    String parentItemsName = null;

    public ArrayClosure(JsonWriter writer, String parentItemsArchetypeNodeId, String parentItemsType, String parentItemsName) {
        this.writer = writer;
        this.parentItemsArchetypeNodeId = parentItemsArchetypeNodeId;
        this.parentItemsType = parentItemsType;
        this.parentItemsName = parentItemsName;
    }

    /**
     * close an item array
     */
    public void close() throws IOException {
        if (parentItemsArchetypeNodeId != null)
            writer.name(I_DvTypeAdapter.ARCHETYPE_NODE_ID).value(parentItemsArchetypeNodeId);
        if (parentItemsType != null) {
            writer.name(I_DvTypeAdapter.AT_CLASS).value(parentItemsType);
            if(parentItemsType.equals(ArchieRMInfoLookup.getInstance().getTypeInfo(Cluster.class).getRmName()))
                writer.name(I_DvTypeAdapter.NAME).beginObject().name(I_DvTypeAdapter.VALUE).value(parentItemsName).endObject();
        }
    }

    public void start() throws IOException {
        close();
    }
}

