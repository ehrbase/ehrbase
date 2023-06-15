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

/**
 * Created by christian on 4/26/2018.
 */
public class ArchetypeNodeId {

    JsonWriter writer;
    String archetypeNodeId;

    public ArchetypeNodeId(JsonWriter writer, String nodeIdentifier) {
        this.writer = writer;
        this.archetypeNodeId = nodeIdentifier;
    }

    public void write() throws IOException {
        if (archetypeNodeId != null && !archetypeNodeId.isEmpty()) {
            writer.name(I_DvTypeAdapter.ARCHETYPE_NODE_ID).value(archetypeNodeId);

            if (new DomainStructure(archetypeNodeId).isArchetypeSlot())
                writer.name(I_DvTypeAdapter.AT_TYPE).value(new DomainStructure(archetypeNodeId).archetypeSlotType());
        }
    }
}
