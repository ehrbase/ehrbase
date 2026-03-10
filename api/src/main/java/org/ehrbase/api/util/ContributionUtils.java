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
package org.ehrbase.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;

/**
 * Helper class for processing of <code>CONTRIBUTION</code>s.
 */
public class ContributionUtils {

    private ContributionUtils() {
        /* NOOP */
    }

    /**
     * Reads the given <code>content</code> JSON string as {@link ContributionCreateDto}
     *
     * @param content JSON <code>CONTRIBUTION</code>
     * @return {@link ContributionCreateDto} representation of the input <code>CONTRIBUTION</code> <code>String</code>
     */
    public static ContributionCreateDto unmarshalContribution(String content) {
        try {
            return unmarshalContribution(CanonicalJson.MARSHAL_OM.readTree(content));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException | JsonProcessingException e) {
            throw new IllegalArgumentException("Error while processing given json input: " + e.getMessage(), e);
        }
    }

    /**
     * Reads the given <code>root</code> JSON ObjectNode as {@link ContributionCreateDto}
     *
     * @param root JSON <code>CONTRIBUTION</code>
     * @return {@link ContributionCreateDto} representation of the input <code>CONTRIBUTION</code> <code>String</code>
     */
    public static ContributionCreateDto unmarshalContribution(JsonNode root) {
        try {
            // remove "_type", because Contribution is registered for "CONTRIBUTION" by archie
            ((ObjectNode) root).remove("_type");

            // convert to DTO and wrap into holder
            return CanonicalJson.MARSHAL_OM.convertValue(root, ContributionCreateDto.class);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Error while processing given json input: " + e.getMessage(), e);
        }
    }
}
