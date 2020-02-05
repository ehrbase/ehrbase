/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
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
package org.ehrbase.terminology.openehr.implementation;

import org.ehrbase.terminology.openehr.TerminologyResourceException;
import org.ehrbase.terminology.openehr.TerminologyInterface;

import java.util.HashMap;
import java.util.Map;

public class LocalizedTerminologies {

    private Map<String, TerminologyInterface> terminologies = new HashMap<>();
    private AttributeCodesetMapping codesetMapping;

    public LocalizedTerminologies() throws TerminologyResourceException {
        terminologies.put("en", new SimpleTerminologyInterface("en"));
        terminologies.put("ja", new SimpleTerminologyInterface("ja"));
        terminologies.put("pt", new SimpleTerminologyInterface("pt"));

        codesetMapping = AttributeCodesetMapping.getInstance();
    }

    public TerminologyInterface locale(String language){
        if (!terminologies.containsKey(language))
            return getDefault();

        return terminologies.get(language);
    }

    public TerminologyInterface getDefault(){
        return terminologies.get("en");
    }

    public AttributeCodesetMapping codesetMapping(){
        return codesetMapping;
    }
}
