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

import java.util.HashSet;
import java.util.Set;

public abstract class ContainsCheck {
    String label;
    String checkExpression;
    Set<String> templateIds = new HashSet<>(); // list of template_ids satisfying the proposition

    public ContainsCheck(String label) {
        this.label = label;
    }

    public void addTemplateId(String templateId) {
        templateIds.add(templateId);
    }

    public abstract String getSymbol();

    public Set<String> addAllTemplateIds(Set<String> templates) {
        this.templateIds.addAll(templates);
        return templateIds;
    }

    public Set<String> getTemplateIds() {
        return templateIds;
    }
}
