/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.service;

import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.support.identification.TerminologyId;
import org.ehrbase.jooq.pg.udt.records.CodePhraseRecord;

public class PersistentCodePhrase {

    private CodePhrase rmCodePhrase;

    public PersistentCodePhrase(CodePhrase rmCodePhrase) {
        this.rmCodePhrase = rmCodePhrase;
    }

    public CodePhraseRecord encode() {
        if (rmCodePhrase == null) return null;
        return new CodePhraseRecord(rmCodePhrase.getTerminologyId().getValue(), rmCodePhrase.getCodeString());
    }

    public CodePhrase decode(CodePhraseRecord codePhraseRecord) {
        return new CodePhrase(
                new TerminologyId(codePhraseRecord.getTerminologyIdValue()), codePhraseRecord.getCodeString());
    }
}
