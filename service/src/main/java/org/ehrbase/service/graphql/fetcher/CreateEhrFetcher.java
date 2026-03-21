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

import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.repository.EhrRepository;
import org.springframework.stereotype.Component;

/**
 * GraphQL mutation fetcher for creating EHRs.
 * Delegates to {@link EhrService#create(UUID, EhrStatus)}.
 */
@Component
public class CreateEhrFetcher implements DataFetcher<Map<String, Object>> {

    private final EhrService ehrService;
    private final EhrRepository ehrRepository;

    public CreateEhrFetcher(EhrService ehrService, EhrRepository ehrRepository) {
        this.ehrService = ehrService;
        this.ehrRepository = ehrRepository;
    }

    @Override
    public Map<String, Object> get(DataFetchingEnvironment env) {
        String subjectId = env.getArgument("subjectId");
        String subjectNamespace = env.getArgument("subjectNamespace");

        EhrStatus status = new EhrStatus();
        status.setArchetypeNodeId("openEHR-EHR-EHR_STATUS.generic.v1");
        status.setName(new DvText("EHR Status"));
        status.setQueryable(true);
        status.setModifiable(true);

        PartySelf subject = new PartySelf();
        if (subjectId != null) {
            subject.setExternalRef(new PartyRef(new HierObjectId(subjectId), subjectNamespace, "PERSON"));
        }
        status.setSubject(subject);

        UUID ehrId = ehrService.create(null, status);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", ehrId.toString());
        result.put("subjectId", subjectId);
        result.put("subjectNamespace", subjectNamespace);
        result.put("creationDate", ehrRepository.getCreationTime(ehrId).orElse(null));
        result.put("isModifiable", true);
        result.put("isQueryable", true);
        return result;
    }
}
