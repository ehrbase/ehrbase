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
package org.ehrbase.jooq.dbencoding.wrappers.json.writer.translator_db2raw;

import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;

/**
 * Created by christian on 4/26/2018.
 */
public class DomainStructure {

    public static final String OPEN_EHR_EHR = "openEHR-EHR-";
    String nodeIdentifier;

    public DomainStructure(String nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    /**
     * is it a composition structure element?
     *
     * @return
     */
    public boolean isArchetypeSlot() {
        return nodeIdentifier.contains(OPEN_EHR_EHR);
    }

    public String archetypeSlotType() {

        String type = "*UNDEF*";

        if (nodeIdentifier.equals(CompositionSerializer.TAG_EVENTS)) type = RmConstants.POINT_EVENT;
        else if (nodeIdentifier.equals(CompositionSerializer.TAG_ACTIVITIES)) type = "ACTIVITY";
        else if (nodeIdentifier.contains(OPEN_EHR_EHR))
            type = nodeIdentifier.substring(OPEN_EHR_EHR.length(), nodeIdentifier.indexOf("."));

        return type;
    }
}
