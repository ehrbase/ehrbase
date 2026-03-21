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
package org.ehrbase.service.graphql.fetcher;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.springframework.stereotype.Component;

/**
 * GraphQL mutation fetcher for updating compositions.
 * Uses version for optimistic locking.
 */
@Component
public class UpdateCompositionFetcher implements DataFetcher<Map<String, Object>> {

    private final CompositionService compositionService;

    public UpdateCompositionFetcher(CompositionService compositionService) {
        this.compositionService = compositionService;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Map<String, Object> get(DataFetchingEnvironment env) {
        UUID compositionId = UUID.fromString(env.getArgument("compositionId"));
        int version = env.getArgument("version");
        Object data = env.getArgument("data");

        UUID ehrId = compositionService
                .getEhrIdForComposition(compositionId)
                .orElseThrow(() -> new IllegalArgumentException("Composition not found: " + compositionId));

        String systemId = "local.ehrbase.org";
        ObjectVersionId targetObjId = new ObjectVersionId(compositionId + "::" + systemId + "::" + version);

        String jsonContent = data instanceof String s ? s : data.toString();
        Composition composition = new CanonicalJson().unmarshal(jsonContent, Composition.class);

        UUID resultId =
                compositionService.update(ehrId, targetObjId, composition).orElseThrow();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("compositionId", resultId.toString());
        result.put("ehrId", ehrId.toString());
        result.put(
                "templateId",
                composition.getArchetypeDetails() != null
                                && composition.getArchetypeDetails().getTemplateId() != null
                        ? composition.getArchetypeDetails().getTemplateId().getValue()
                        : "");
        result.put("version", version + 1);
        result.put("committedAt", OffsetDateTime.now().toString());
        return result;
    }
}
