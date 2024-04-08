/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.openehr.dbformat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nedap.archie.rm.RMObject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class VersionedObjectDataStructureTest {

    @Test
    void testStructureRmTypeAlias() {
        // duplicate aliases?
        Arrays.stream(StructureRmType.values()).collect(Collectors.toMap(v -> v.getAlias(), v -> v));
    }

    @ParameterizedTest
    @EnumSource(
            value = StructureRmType.class,
            mode = Mode.EXCLUDE,
            names = {"EVENT_CONTEXT", "FEEDER_AUDIT", "FEEDER_AUDIT_DETAILS", "INSTRUCTION_DETAILS"})
    void testFeederAuditPreservedForElementOnly(StructureRmType type) throws JsonProcessingException {
        String feederAudit =
                """
                {
                    "_type": "FEEDER_AUDIT",
                    "originating_system_item_ids": [
                        {
                            "_type": "DV_IDENTIFIER",
                            "id": "system_id",
                            "type": "id"
                        }
                    ]
                }
                """;
        String toParse =
                """
                {
                    "_type": "%s",
                    "name": {
                        "_type": "DV_TEXT",
                        "value": "name"
                    },
                    "feeder_audit": %s,
                    "archetype_node_id": "at0005"
                }
                """
                        .formatted(type, feederAudit);

        List<StructureNode> roots = VersionedObjectDataStructure.createDataStructure(
                CanonicalJson.MARSHAL_OM.readValue(toParse, RMObject.class));

        assertEquals(2, roots.size());
        assertThat(roots.get(1).getJsonNode().toString()).isEqualToIgnoringWhitespace(feederAudit);
        ObjectNode jsonNode = roots.get(0).getJsonNode();
        assertEquals(type == StructureRmType.ELEMENT, jsonNode.has("feeder_audit"));
        if (type == StructureRmType.ELEMENT) {
            assertEquals(roots.get(1).getJsonNode(), jsonNode.get("feeder_audit"));
        }
    }
}
