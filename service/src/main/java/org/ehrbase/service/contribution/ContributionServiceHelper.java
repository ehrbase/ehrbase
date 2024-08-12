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
package org.ehrbase.service.contribution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.ehr.EhrStatus;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;

/**
 * Helper class for processing of <code>CONTRIBUTION</code>s.
 */
public class ContributionServiceHelper {

    /**
     * Reads the given <code>content</code> JSON string as {@link ContributionCreateDto}
     *
     * @param content JSON <code>CONTRIBUTION</code>
     * @return {@link ContributionCreateDto} representation of the input <code>CONTRIBUTION</code> <code>String</code>
     */
    public static ContributionWrapper unmarshalContribution(String content) {
        try {
            ObjectNode root = (ObjectNode) CanonicalJson.MARSHAL_OM.readTree(content);
            // remove "_type", because Contribution is registered for "CONTRIBUTION" by archie
            root.remove("_type");

            // convert to DTO and wrap into holder
            ContributionCreateDto contributionCreateDto =
                    CanonicalJson.MARSHAL_OM.convertValue(root, ContributionCreateDto.class);
            ContributionWrapper contributionWrapper = new ContributionWrapper(contributionCreateDto);

            // register additional DTOs for RMObjects
            registerDtos(contributionWrapper, root);

            return contributionWrapper;
        } catch (RuntimeException | JsonProcessingException e) {
            throw new IllegalArgumentException("Error while processing given json input: " + e.getMessage(), e);
        }
    }

    /**
     * Register additional DTOs for the given <code>holder</code> {@link OriginalVersion#getData()} with an {@link EhrStatusDto}
     * representation of the input <code>jsonData</code>.
     *
     * @param contributionWrapper to register DTOs to
     * @param root                JSON node of the <code>CONTRIBUTION</code>
     */
    private static void registerDtos(ContributionWrapper contributionWrapper, ObjectNode root) {

        ArrayNode rawVersions = (ArrayNode) root.get("versions");
        List<OriginalVersion<? extends RMObject>> versions =
                contributionWrapper.getContributionCreateDto().getVersions();

        IntStream.range(0, versions.size()).forEach(idx -> {
            OriginalVersion<? extends RMObject> originalVersion = versions.get(idx);
            RMObject data = originalVersion.getData();

            if (data instanceof EhrStatus) {

                Optional.of(rawVersions.get(idx))
                        .map(node -> node.get("data"))
                        .map(JsonNode::deepCopy)
                        .ifPresent(node -> {
                            ((ObjectNode) node).remove("_type");
                            EhrStatusDto ehrStatusDto = CanonicalJson.MARSHAL_OM.convertValue(node, EhrStatusDto.class);
                            contributionWrapper.registerDtoForVersion(originalVersion, ehrStatusDto);
                        });
            }
        });
    }
}
