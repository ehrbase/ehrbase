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

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

public interface KnowledgeCacheService {

    String addOperationalTemplate(OPERATIONALTEMPLATE template);

    List<TemplateMetaData> listAllOperationalTemplates();

    Map<UUID, String> findAllTemplateIds();

    /**
     * retrieve an operational template document instance
     *
     * @param key the name of the operational template
     * @return an OPERATIONALTEMPLATE document instance or null
     * @see org.openehr.schemas.v1.OPERATIONALTEMPLATE
     */
    Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(String key);

    /**
     * retrieve a <b>cached</b> operational template document instance using its unique Id
     *
     * @param uuid the name of the operational template
     * @return an OPERATIONALTEMPLATE document instance or null
     * @see org.openehr.schemas.v1.OPERATIONALTEMPLATE
     */
    Optional<OPERATIONALTEMPLATE> retrieveOperationalTemplate(UUID uuid);

    /**
     * Deletes a given operational template physically from cache and from template storage and from cache. Should only
     * be executed if the template is no longer referenced by any Composition. Make sure you check for references before
     * deleting a template otherwise this causes inconsistencies and no longer deliverable Composition entries.
     *
     * @param template - The template instance to delete
     */
    void deleteOperationalTemplate(OPERATIONALTEMPLATE template);

    Optional<String> findTemplateIdByUuid(UUID uuid);

    Optional<UUID> findUuidByTemplateId(String templateId);

    String adminUpdateOperationalTemplate(InputStream content);

    int deleteAllOperationalTemplates();
}
