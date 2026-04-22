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
package org.ehrbase.api.service;

import com.nedap.archie.rm.composition.Composition;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.UUID;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.jspecify.annotations.Nullable;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

public interface TemplateService {

    record TemplateDetails(
            UUID id, String templateId, OffsetDateTime creationTime, String concept, String archetypeId) {}

    Collection<TemplateDetails> findAllTemplates();

    WebTemplate getInternalTemplate(String templateId);

    WebTemplate findWebTemplate(String templateId);

    /**
     * Finds and returns the given operational template as string represented in requested format.
     *
     * @param templateId - Unique name of operational template
     * @return
     * @throws ObjectNotFoundException When the template couldn't be found
     */
    String findOperationalTemplate(String templateId);

    String create(OPERATIONALTEMPLATE content);

    @Nullable
    String findTemplateIdByUuid(UUID uuid);

    @Nullable
    UUID findUuidByTemplateId(String templateId);

    /**
     * Deletes a given template from storage physically. The template is no longer available. If you
     * try to delete a template that is used in at least one Composition Entry or in one history entry
     * the deletion will be rejected.
     *
     * @param templateId - Template id to delete, e.g. "IDCR Allergies List.v0"
     */
    void adminDeleteTemplate(String templateId);

    /**
     * Replaces a given template in the storage and updates the cache with the new template content.
     * Will be rejected if the template has referencing Compositions.
     *
     * @param template - New content to overwrite the template with
     * @return - New template id
     */
    String adminUpdateTemplate(OPERATIONALTEMPLATE template);

    /**
     * Deletes all templates from target template storage and returns the number of deleted templates.
     * If any template is referenced by at least one Composition the deletion will be rejected and no
     * template will be removed.
     *
     * @return - Number of deleted templates
     */
    int adminDeleteAllTemplates();

    Composition buildExample(String templateId);

    static OPERATIONALTEMPLATE buildOperationalTemplate(String content) throws XmlException {
        return org.openehr.schemas.v1.TemplateDocument.Factory.parse(content).getTemplate();
    }

    static OPERATIONALTEMPLATE buildOperationalTemplate(InputStream in) throws XmlException {
        org.openehr.schemas.v1.TemplateDocument document;
        try (in) {
            document = org.openehr.schemas.v1.TemplateDocument.Factory.parse(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return document.getTemplate();
    }
}
