/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Hannover Medical School.
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

package org.ehrbase.test_data.operationaltemplate;

import java.io.InputStream;
import java.util.Arrays;

public enum OperationalTemplateTestData {

    IDCR_IMMUNISATION_SUMMARY("IDCR Immunisation summary", "IDCR - Immunisation summary.v0.opt", "IDCR - Immunisation summary.v0"),
    RIPPLE_CONFORMANCE_TEST("RIPPLE Conformance Test", "RIPPLE-Conformance Test.opt", "RIPPLE - Conformance Test template"),
    IDCR_LABORATORY_TEST("IDCR  Laboratory Test Report", "IDCR - Laboratory Test Report.v0.opt", "IDCR - Laboratory Test Report.v0"),
    IDCR_PROBLEM_LIST("IDCR  Problem List.v1", "IDCR - Problem List.v1.opt", "IDCR - Problem List.v1"),
    BLOOD_PRESSURE_SIMPLE("Very simple blood_pressure template", "ehrbase_blood_pressure_simple.de.v0.opt", "ehrbase_blood_pressure_simple.de.v0"),
    MULTI_OCCURRENCE("Template with multiple occurrence", "ehrbase_multi_occurrence.de.opt", "ehrbase_multi_occurrence.de.v1"),
    ALL_TYPES("Template with all types", "Test_all_types.opt", "test_all_types.en.v1");

    private final String filename;
    private final String templateId;
    private final String description;

    OperationalTemplateTestData(String description, String filename, String templateId) {
        this.filename = filename;
        this.description = description;
        this.templateId = templateId;
    }


    public InputStream getStream() {
        return getClass().getResourceAsStream("/operationaltemplate/" + filename);
    }

    public static OperationalTemplateTestData findByTemplateId(String templateId) {
        return Arrays.stream(OperationalTemplateTestData.values())
                .filter(o -> o.getTemplateId().equals(templateId))
                .findAny()
                .orElse(null);
    }

    public String getTemplateId() {
        return templateId;
    }
}
