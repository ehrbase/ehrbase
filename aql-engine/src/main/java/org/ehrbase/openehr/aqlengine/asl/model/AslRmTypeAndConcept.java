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
package org.ehrbase.openehr.aqlengine.asl.model;

import org.ehrbase.openehr.dbformat.RmType;

/**
 * archetypeNodeId maps to rm entity and entity concept columns
 *
 * @param aliasedRmType
 * @param concept
 */
public record AslRmTypeAndConcept(String aliasedRmType, String concept) {

    public static final String ARCHETYPE_PREFIX = "openEHR-EHR-";

    public static AslRmTypeAndConcept fromArchetypeNodeId(String archetypeNodeId) {
        if (archetypeNodeId == null) {
            return null;
        }

        if (archetypeNodeId.startsWith(ARCHETYPE_PREFIX)) {
            int pos = archetypeNodeId.indexOf('.', ARCHETYPE_PREFIX.length());
            if (pos < 0) {
                throw new IllegalArgumentException("Archetype id is not valid: " + archetypeNodeId);
            }
            String alias = RmType.optionalAlias(archetypeNodeId.substring(ARCHETYPE_PREFIX.length(), pos))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Archetype id for unsupported/unknown RM type: " + archetypeNodeId));
            String concept = archetypeNodeId.substring(pos);
            return new AslRmTypeAndConcept(alias, concept);

        } else if (archetypeNodeId.startsWith("at") || archetypeNodeId.startsWith("id")) {
            // at or id code
            return new AslRmTypeAndConcept(null, archetypeNodeId);
        } else {
            throw new IllegalArgumentException("Invalid archetype_node_id: %s".formatted(archetypeNodeId));
        }
    }

    /**
     * Removes the fixed prefix from archetype ids (openEHR-EHR-{RM-type}),
     * but leaves the '.', which hints the missing prefix
     * @param archetypeNodeId
     * @return
     */
    public static String toEntityConcept(String archetypeNodeId) {
        if (archetypeNodeId == null) {
            return null;
        }
        return fromArchetypeNodeId(archetypeNodeId).concept;
    }
}
