/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.plugin.extensionpoints;

import java.util.function.Function;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.pf4j.ExtensionPoint;

/**
 * Extension Point for Template handling.
 *
 * @author Stefan Spiska
 */
public interface TemplateExtensionPoint extends ExtensionPoint {

    /**
     * Intercept Template create
     *
     * @param input {@link OPERATIONALTEMPLATE} to be created
     * @param chain next Extension Point
     * @return templateId of the created template
     * @see <a href="I_EHR_COMPOSITION in openEHR Platform Service
     * Model">https://specifications.openehr.org/releases/SM/latest/openehr_platform.html#_i_ehr_composition_interface</a>
     */
    default String aroundCreation(OPERATIONALTEMPLATE input, Function<OPERATIONALTEMPLATE, String> chain) {
        return chain.apply(input);
    }
}
