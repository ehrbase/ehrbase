/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Jake Smolka (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.api.service;

import org.ehrbase.api.definitions.CompositionFormat;
import org.ehrbase.api.definitions.OperationalTemplateFormat;
import org.ehrbase.api.definitions.StructuredString;
import org.ehrbase.api.dto.TemplateMetaDataDto;
import org.ehrbase.api.dto.WebTemplate;

import java.util.List;

public interface TemplateService extends BaseService {
    List<TemplateMetaDataDto> getAllTemplates();

    StructuredString buildExample(String templateId, CompositionFormat format);

    WebTemplate findTemplate(String templateId);

    /**
     * Finds and returns the given operational template as string represented in requested format.
     * @param templateId - Unique name of operational template
     * @param format - As enum value from {@link OperationalTemplateFormat}
     * @return
     * @throws RuntimeException When the template couldn't be found, the format isn't support or in case of another error.
     */
    String findOperationalTemplate(String templateId, OperationalTemplateFormat format) throws RuntimeException;

    String create(String content);
}
