/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gson.JsonElement;
import com.nedap.archie.rm.composition.Composition;
import org.apache.commons.io.IOUtils;
import org.ehrbase.jooq.dbencoding.rawjson.LightRawJsonEncoder;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.serialisation.xmlencoding.CanonicalXML;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LinkedTreeMapAdapterTest {

    @Test
    void testFixWrongDbEncoding() throws Exception {

        String db_encoded =
                IOUtils.resourceToString("/sample_data/db_serialization_with_wrong_loc_att_enc.json", UTF_8);
        Assertions.assertNotNull(db_encoded);

        JsonElement converted = new LightRawJsonEncoder(db_encoded).encodeContentAsJson(null);

        // see if this can be interpreted by Archie
        Composition object = new CanonicalJson().unmarshal(converted.toString(), Composition.class);

        Assertions.assertNotNull(object);

        String interpreted = new CanonicalXML().marshal(object);

        Assertions.assertNotNull(interpreted);
    }
}
