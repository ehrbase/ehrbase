/*
 * Copyright (c) 2019 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.test_data.contribution;

import java.io.InputStream;

public enum ContributionTestDataCanonicalJson {
    ONE_ENTRY_COMPOSITION("contribution-one_entry-composition.json", "Contribution with rather minimal composition entry"),
    TWO_ENTRIES_COMPOSITION("contribution-two_entries-composition.json", "Contribution with two rather minimal composition entries");

    private final String filename;
    private final String description;

    ContributionTestDataCanonicalJson(String filename, String description) {
        this.filename = filename;
        this.description = description;
    }

    public InputStream getStream() {
        return getClass().getResourceAsStream("/contribution/canonical_json/" + filename);
    }
}
