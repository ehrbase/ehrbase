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
package org.ehrbase.aql.containment;

/**
 * used to fulfill undefined symbol in AQL. F.e. whenever an expression such as:
 *  CONTAINS CLUSTER [openEHR-EHR-CLUSTER.location.v1] is passed.
 */
public class AnonymousSymbol {

    private int fieldId;

    public AnonymousSymbol() {
        this.fieldId = 0;
    }

    public AnonymousSymbol(int fieldId) {
        this.fieldId = fieldId;
    }

    public String generate(String prefix) {
        return prefix + "_" + (++fieldId);
    }
}
