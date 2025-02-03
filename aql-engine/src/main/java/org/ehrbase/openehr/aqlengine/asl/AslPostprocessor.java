/*
 * Copyright (c) 2025 vitasystems GmbH.
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
package org.ehrbase.openehr.aqlengine.asl;

import javax.annotation.Nonnull;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.springframework.core.Ordered;

/**
 * Used for modifications of the AslRootQuery before it is passed to the next layer.
 * All Spring beans implementing this interface will be picked up by AqlQueryService.
 * <p>
 * Specifying the Order is required since inplace modifications may affect each other.
 * Having multiple beans of this type with the same order value may produce inconsistent results.
 */
public interface AslPostprocessor extends Ordered {
    /**
     * Invoked after the building the AslRootQuery and before building the DB query.
     * This method can be used to modify the AslRootQuery inplace.
     * Modifications of the given AqlQuery or AqlQueryWrapper will not have any effect on the generated DB query.
     *
     * @param aslRootQuery    the AslRootQuery built based on aqlQuery and aqlQueryWrapper
     * @param aqlQuery        the parsed and post-processed AqlQuery
     * @param aqlQueryWrapper the AqlQueryWrapper created from aqlQuery
     * @param aqlQueryRequest the AqlQueryRequest provided to the service
     */
    @Nonnull
    void afterBuildAsl(
            AslRootQuery aslRootQuery,
            AqlQuery aqlQuery,
            AqlQueryWrapper aqlQueryWrapper,
            AqlQueryRequest aqlQueryRequest);
}
