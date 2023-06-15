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
package org.ehrbase.jooq.dbencoding.attributes;

import com.nedap.archie.rm.composition.InstructionDetails;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.PathMap;

public class InstructionDetailsAttributes {

    private final InstructionDetails instructionDetails;

    public InstructionDetailsAttributes(InstructionDetails instructionDetails) {
        this.instructionDetails = instructionDetails;
    }

    /**
     * encode the attributes lower snake case to comply with UML conventions and make is queryable
     * @return
     */
    public Map<String, Object> toMap() {
        Map<String, Object> valuemap = PathMap.getInstance();

        if (instructionDetails == null) return null;

        if (instructionDetails.getActivityId() != null) {
            valuemap.put("activity_id", instructionDetails.getActivityId());
        }
        if (instructionDetails.getInstructionId() != null) {
            valuemap.put("instruction_id", instructionDetails.getInstructionId());
        }
        if (instructionDetails.getWfDetails() != null) {
            valuemap.put("wf_details", instructionDetails.getWfDetails());
        }
        return valuemap;
    }
}
