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
import javax.annotation.Nonnull;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;

/**
 * ValidationService
 * <p>
 * performs a composition validation based on the constraints defined in a 1.4 operational template
 * The service is a wrapper of Validator and use a cache to optimize validation since the
 * construction of the constraints is somewhat resource intensive. The validation constraints are
 * maintained into a standard java cache. The service uses KnowledgeCache to retrieve operational
 * templates.
 */
public interface ValidationService {

    /**
     * Initially check if the <code>composition</code> is valid for further processing.
     *
     * @param composition to validate
     * @throws IllegalArgumentException in case the given <code>composition</code> is invalid.
     */
    void check(Composition composition);

    /**
     * Initially check if <code>ehrStatus</code> is valid for further processing.
     *
     * @param ehrStatus to validate
     * @throws IllegalArgumentException in case the given <code>ehrStatus</code> is invalid.
     */
    void check(@Nonnull EhrStatusDto ehrStatus);

    /**
     * Initially check if <code>contribution</code> is valid for further processing.
     *
     * @param contribution to validate
     * @throws IllegalArgumentException in case the given <code>contribution</code> is invalid.
     */
    void check(ContributionCreateDto contribution);
}
