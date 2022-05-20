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
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.TermMapping;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.util.ArrayList;
import java.util.List;

/**
 * since parsing of array of UDTs seems to fail with jOOQ 3.12, we are encoding term_mappings as
 * an array for TEXT with the following format:
 * match|purpose_value|purpose_terminology_id|purpose_code_string|target_terminology_id|target_code_string.
 * For example:
 * =|Erfasst|local|irgendein Purpose|SNOMED-CT|345356676789
 * Methods encodeAsString and decode deal with the formatting
 */
public class PersistentTermMapping {

    private TermMapping rmTermMapping;

    public PersistentTermMapping(TermMapping rmTermMapping) {
        this.rmTermMapping = rmTermMapping;
    }

    public PersistentTermMapping() {
        this.rmTermMapping = null;
    }

    public String encodeAsString() {
        if (rmTermMapping == null) return null;

        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(rmTermMapping.getMatch()).append("|");
        stringBuilder.append(rmTermMapping.getPurpose().getValue()).append("|");
        stringBuilder
                .append(rmTermMapping
                        .getPurpose()
                        .getDefiningCode()
                        .getTerminologyId()
                        .getValue())
                .append("|");
        stringBuilder
                .append(rmTermMapping.getPurpose().getDefiningCode().getCodeString())
                .append("|");
        stringBuilder
                .append(rmTermMapping.getTarget().getTerminologyId().getValue())
                .append("|");
        stringBuilder.append(rmTermMapping.getTarget().getCodeString());

        return stringBuilder.toString();
    }

    public TermMapping decode(String termMappingString) {

        String[] attributes = termMappingString.split("\\|");

        return new TermMapping(
                new CodePhrase(new TerminologyId(attributes[4]), attributes[5]),
                attributes[0].charAt(0),
                new DvCodedText(attributes[1], new CodePhrase(new TerminologyId(attributes[2]), attributes[3])));
    }

    public String[] termMappingRepresentation(List<TermMapping> termMappings) {
        List<String> dvCodedTextTermMappingRecords = new ArrayList<>();
        // prepare the term mappings array if any

        for (TermMapping termMapping : termMappings) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(new PersistentTermMapping(termMapping).encodeAsString());
            dvCodedTextTermMappingRecords.add(stringBuilder.toString());
        }

        return dvCodedTextTermMappingRecords.toArray(new String[] {});
    }
}
