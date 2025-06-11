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
package org.ehrbase.openehr.aqlengine.asl.model.query;

import java.util.List;
import org.ehrbase.openehr.aqlengine.asl.meta.AslQueryOrigin;

public abstract sealed class AslDataQuery extends AslQuery permits AslRmObjectDataQuery, AslPathDataQuery {

    private AslQuery base;
    private final AslQuery baseProvider;

    protected AslDataQuery(String alias, AslQueryOrigin origin, AslQuery base, AslQuery baseProvider) {
        super(alias, origin, List.of());
        this.base = base;
        this.baseProvider = baseProvider;
    }

    public AslQuery getBase() {
        return base;
    }

    public void setBase(AslStructureQuery base) {
        this.base = base;
    }

    public AslQuery getBaseProvider() {
        return baseProvider;
    }
}
