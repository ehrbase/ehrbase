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

import com.nedap.archie.rm.support.identification.ObjectVersionId;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.UUID;
import org.ehrbase.api.service.CompositionService;
import org.springframework.stereotype.Component;

/**
 * GraphQL mutation fetcher for deleting compositions.
 */
@Component
public class DeleteCompositionFetcher implements DataFetcher<Boolean> {

    private final CompositionService compositionService;

    public DeleteCompositionFetcher(CompositionService compositionService) {
        this.compositionService = compositionService;
    }

    @Override
    public Boolean get(DataFetchingEnvironment env) {
        UUID compositionId = UUID.fromString(env.getArgument("compositionId"));
        int version = env.getArgument("version");

        UUID ehrId = compositionService
                .getEhrIdForComposition(compositionId)
                .orElseThrow(() -> new IllegalArgumentException("Composition not found: " + compositionId));

        String systemId = "local.ehrbase.org";
        ObjectVersionId targetObjId = new ObjectVersionId(compositionId + "::" + systemId + "::" + version);

        compositionService.delete(ehrId, targetObjId);
        return true;
    }
}
