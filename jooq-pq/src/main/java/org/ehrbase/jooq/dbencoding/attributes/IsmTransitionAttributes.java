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

import static org.ehrbase.jooq.dbencoding.CompositionSerializer.TAG_CLASS;

import com.nedap.archie.rm.composition.IsmTransition;
import com.nedap.archie.rm.datavalues.DvText;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.PathMap;
import org.ehrbase.jooq.dbencoding.SimpleClassName;

public class IsmTransitionAttributes {

    private final IsmTransition ismTransition;

    public IsmTransitionAttributes(IsmTransition ismTransition) {
        this.ismTransition = ismTransition;
    }

    /**
     * encode the attributes lower snake case to comply with UML conventions and make is queryable
     * @return
     */
    public Map<String, Object> toMap() {
        Map<String, Object> valuemap = PathMap.getInstance();

        if (ismTransition == null) return null;

        if (ismTransition.getReason() != null) {
            List<Map<String, Object>> reasons = new ArrayList<>();
            for (DvText reason : ismTransition.getReason()) {
                valuemap.put(TAG_CLASS, new SimpleClassName(reason).toString());
                valuemap.put("value", reason.getValue());
                reasons.add(valuemap);
            }
            valuemap.put("reason", reasons);
        }
        if (ismTransition.getCareflowStep() != null) {
            valuemap.put("careflow_step", ismTransition.getCareflowStep());
        }
        if (ismTransition.getCurrentState() != null) {
            valuemap.put("current_state", ismTransition.getCurrentState());
        }
        if (ismTransition.getTransition() != null) {
            valuemap.put("transition", ismTransition.getTransition());
        }
        return valuemap;
    }
}
