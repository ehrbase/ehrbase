/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.jooq.dbencoding.rmobject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nedap.archie.rm.archetyped.Link;
import java.util.Arrays;
import java.util.List;
import org.ehrbase.jooq.dbencoding.EncodeUtilArchie;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.ArchieObjectMapperProvider;

/**
 * Encode/decode a Links object as a json structure. Should be used to support FeederAudit at DB
 * level (f.e. Composition Entry)
 */
public class LinksEncoding extends RMObjectEncoding {

    public String toDB(List<Link> linkList) {
        GsonBuilder builder = EncodeUtilArchie.getGsonBuilderInstance();
        Gson gson = builder.setPrettyPrinting().create();
        return gson.toJson(linkList);
    }

    public List<Link> fromDB(String dbJonRepresentation) {
        List<Link> list;
        try {
            list = Arrays.asList(
                    ArchieObjectMapperProvider.getObjectMapper().readValue(dbJonRepresentation, Link[].class));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException();
        }

        return list;
    }
}
