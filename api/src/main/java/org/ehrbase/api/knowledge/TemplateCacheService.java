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
package org.ehrbase.api.knowledge;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

public interface TemplateCacheService {

    String addOperationalTemplate(OPERATIONALTEMPLATE template);

    List<TemplateService.TemplateDetails> findAllTemplates();

    /**
     * retrieve an operational template document
     *
     * @param key the name of the operational template
     * @return String representation of an OPERATIONALTEMPLATE document or null
     */
    String retrieveOperationalTemplate(String key);

    WebTemplate getInternalTemplate(String templateId);

    /**
     * Deletes a given operational template physically from cache and from template storage and from cache.
     * Can only be executed if the template is no longer referenced by any Composition.
     *
     * @param templateUuid - Internalö id of template instance to delete
     */
    void deleteOperationalTemplate(UUID templateUuid);

    Optional<String> findTemplateIdByUuid(UUID uuid);

    Optional<UUID> findUuidByTemplateId(String templateId);

    String adminUpdateOperationalTemplate(String templateId, String content);

    int deleteAllOperationalTemplates();
}
