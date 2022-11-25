/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.ehr.knowledge;

import java.io.InputStream;

public enum TemplateTestData {
    CLINICAL_CONTENT_VALIDATION("clinical_content_validation.opt"),
    IMMUNISATION_SUMMARY("IDCR - Immunisation summary.v0.opt"),
    NON_UNIQUE_AQL_PATH("non_unique_aql_paths.opt"),
    ANAMNESE("Anamnese.opt");

    private final String filename;

    TemplateTestData(String filename) {
        this.filename = filename;
    }

    public InputStream getStream() {
        return getClass().getResourceAsStream("/knowledge/" + filename);
    }
}
