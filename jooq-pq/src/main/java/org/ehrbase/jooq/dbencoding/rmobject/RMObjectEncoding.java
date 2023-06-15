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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.EncodeUtilArchie;
import org.ehrbase.jooq.dbencoding.rawjson.LightRawJsonEncoder;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;

/** utility class to deal with specific RM object DB encode/decode (f.e. FeederAudit) */
public abstract class RMObjectEncoding {

    protected String toDB(Map<String, Object> objectMap) {
        GsonBuilder builder = EncodeUtilArchie.getGsonBuilderInstance();
        Gson gson = builder.setPrettyPrinting().create();
        return gson.toJson(objectMap);
    }

    protected RMObject fromDB(String rmClassName, String dbJonRepresentation) {
        Class clazz = ArchieRMInfoLookup.getInstance().getClass(rmClassName);
        return fromDB(clazz, dbJonRepresentation);
    }

    protected RMObject fromDB(Class clazz, String dbJonRepresentation) {
        JsonElement interpreted = new LightRawJsonEncoder(dbJonRepresentation).encodeContentAsJson(null);
        return new CanonicalJson().unmarshal(interpreted.toString(), clazz);
    }
}
