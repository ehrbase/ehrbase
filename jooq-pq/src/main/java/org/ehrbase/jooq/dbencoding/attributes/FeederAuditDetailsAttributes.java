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
package org.ehrbase.jooq.dbencoding.attributes;

import com.nedap.archie.rm.archetyped.FeederAuditDetails;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.PathMap;
import org.ehrbase.jooq.dbencoding.RmObjectEncoding;
import org.ehrbase.jooq.dbencoding.rawjson.LightRawJsonEncoder;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.openehr.sdk.util.SnakeCase;

public class FeederAuditDetailsAttributes {

    private final FeederAuditDetails feederAuditDetails;

    public FeederAuditDetailsAttributes(FeederAuditDetails feederAuditDetails) {
        this.feederAuditDetails = feederAuditDetails;
    }

    /**
     * encode the attributes lower snake case to comply with UML conventions and make is queryable
     * @return
     */
    public Map<String, Object> toMap() {
        Map<String, Object> valuemap = PathMap.getInstance();

        if (feederAuditDetails == null) return null;

        valuemap.put(
                I_DvTypeAdapter.AT_TYPE, new SnakeCase(FeederAuditDetails.class.getSimpleName()).camelToUpperSnake());

        if (feederAuditDetails.getLocation() != null) {
            valuemap.put("location", new RmObjectEncoding(feederAuditDetails.getLocation()).toMap());
        }
        if (feederAuditDetails.getProvider() != null) {
            valuemap.put("provider", new RmObjectEncoding(feederAuditDetails.getProvider()).toMap());
        }
        if (feederAuditDetails.getSubject() != null) {
            valuemap.put("subject", new RmObjectEncoding(feederAuditDetails.getSubject()).toMap());
        }
        if (feederAuditDetails.getSystemId() != null) {
            valuemap.put("system_id", feederAuditDetails.getSystemId());
        }
        if (feederAuditDetails.getTime() != null) {
            valuemap.put("time", new RmObjectEncoding(feederAuditDetails.getTime()).toMap());
        }
        if (feederAuditDetails.getVersionId() != null) {
            valuemap.put("version_id", feederAuditDetails.getVersionId());
        }
        if (feederAuditDetails.getOtherDetails() != null) {
            String dbEncoded = new CompositionSerializer().dbEncode(feederAuditDetails.getOtherDetails());
            Map<String, Object> asMap = new LightRawJsonEncoder(dbEncoded).encodeOtherDetailsAsMap();
            String nodeId = asMap.get("/archetype_node_id").toString();
            // make sure node id is wrapped in [ and ] and throw errors if invalid input
            if (!nodeId.startsWith("[")) {
                if (nodeId.endsWith("]")) throw new IllegalArgumentException("Invalid archetype node id");
                nodeId = "[" + nodeId + "]";
            } else if (!nodeId.endsWith("]")) {
                throw new IllegalArgumentException("Invalid archetype node id");
            }
            valuemap.put("other_details" + nodeId, asMap);
        }
        return valuemap;
    }
}
