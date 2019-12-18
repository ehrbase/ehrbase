/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

import com.nedap.archie.rm.composition.Composition;

import java.util.UUID;

/**
 * ValidationService
 *
 * performs a composition validation based on the constraints defined in a 1.4 operational template
 * The service is a wrapper of Validator and use a cache to optimize validation since the construction
 * of the constraints is somewhat resource intensive.
 * The validation constraints are maintained into a standard java cache. The service uses KnowledgeCache
 * to retrieve operational templates.
 *
 */
public interface ValidationService {
    /**
     * check a composition based on the operation template constraints
     * @param templateUUID the template UUID
     * @param composition the RM composition
     * @throws Exception if the validation fails or the template cannot be resolved
     */
    void check(UUID templateUUID, Composition composition) throws Throwable;

    /**
     * check a composition based on the operation template constraints
     * @param templateID the template Id (String)
     * @param composition the RM composition
     * @throws Exception if the validation fails or the template cannot be resolved
     */
    void check(String templateID, Composition composition) throws Exception;

    /**
     * Remove all mappings in the cache
     */
    void invalidate();
}
