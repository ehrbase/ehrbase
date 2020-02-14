/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
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

package org.ehrbase.serialisation;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.composition.Composition;
import org.ehrbase.ehr.encode.rawjson.LightRawJsonEncoder;

import java.io.IOException;
import java.util.Map;

public class RawJson implements RMDataFormat {

    private Map<String, String> ltreeMap;

    @Override
    public String marshal(RMObject rmObject) {
        try {
            CompositionSerializer compositionSerializer = new CompositionSerializer();
            String encode = compositionSerializer.dbEncode(rmObject);
            ltreeMap = compositionSerializer.getLtreeMap();
            return encode;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    public <T extends RMObject> T unmarshal(String value, Class<T> clazz) {

        String converted;
        if (clazz.equals(Composition.class)) {
            converted = new LightRawJsonEncoder(value).encodeCompositionAsString();
        } else {
            converted = new LightRawJsonEncoder(value).encodeContentAsString(null);
        }
        try {
            return JacksonUtil.getObjectMapper().readValue(converted, clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public Map<String, String> getLtreeMap() {
        return ltreeMap;
    }
}
