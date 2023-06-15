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

import com.google.gson.internal.LinkedTreeMap;
import java.util.ArrayList;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;

public class Siblings {

    ArrayList<LinkedTreeMap> arrayList;

    public Siblings(ArrayList<LinkedTreeMap> arrayList) {
        this.arrayList = arrayList;
    }

    public String archetypeNodeId() {
        for (LinkedTreeMap linkedTreeMap : arrayList) {
            if (linkedTreeMap.get(I_DvTypeAdapter.ARCHETYPE_NODE_ID) != null) {
                return (String) linkedTreeMap.get(I_DvTypeAdapter.ARCHETYPE_NODE_ID);
            }
        }
        return null;
    }

    public String type() {
        for (LinkedTreeMap linkedTreeMap : arrayList) {
            if (linkedTreeMap.get(I_DvTypeAdapter.AT_TYPE) != null) {
                return (String) linkedTreeMap.get(I_DvTypeAdapter.AT_TYPE);
            } else if (linkedTreeMap.get(CompositionSerializer.TAG_CLASS) != null) {
                return (String) linkedTreeMap.get(CompositionSerializer.TAG_CLASS);
            }
        }
        return null;
    }
}
