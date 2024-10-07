/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.test.fixtures;

import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.archetyped.FeederAudit;
import com.nedap.archie.rm.datastructures.ItemStructure;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.UIDBasedId;
import org.ehrbase.api.dto.EhrStatusDto;

public class EhrStatusDtoFixture {

    private static final DvText NAME = new DvText("EHR_STATUS");
    private static final String ARCHETYPE_NODE_ID = "openEHR-EHR-EHR_STATUS.generic.v1";
    private static final PartySelf SUBJECT = new PartySelf();

    public static EhrStatusDto ehrStatusDto(UIDBasedId uid) {
        return ehrStatus(uid, ARCHETYPE_NODE_ID, NAME, null, null, SUBJECT, true, true, null);
    }

    public static EhrStatusDto ehrStatusDto(DvText name) {
        return ehrStatus(null, ARCHETYPE_NODE_ID, name, null, null, SUBJECT, true, true, null);
    }

    public static EhrStatusDto ehrStatusDto(Archetyped archetyped) {
        return ehrStatus(null, ARCHETYPE_NODE_ID, NAME, archetyped, null, SUBJECT, true, true, null);
    }

    public static EhrStatusDto ehrStatusDto(FeederAudit feederAudit) {
        return ehrStatus(null, ARCHETYPE_NODE_ID, NAME, null, feederAudit, SUBJECT, true, true, null);
    }

    public static EhrStatusDto ehrStatusDto(PartySelf subject) {
        return ehrStatus(null, ARCHETYPE_NODE_ID, NAME, null, null, subject, true, true, null);
    }

    public static EhrStatusDto ehrStatusDto(Boolean isQueryable, Boolean isModifiable) {
        return ehrStatus(null, ARCHETYPE_NODE_ID, NAME, null, null, SUBJECT, isQueryable, isModifiable, null);
    }

    public static EhrStatusDto ehrStatusDto(ItemStructure itemStructure) {
        return ehrStatus(null, ARCHETYPE_NODE_ID, NAME, null, null, SUBJECT, true, true, itemStructure);
    }

    private static EhrStatusDto ehrStatus(
            UIDBasedId uid,
            String archetypeNodeId,
            DvText name,
            Archetyped archetypeDetails,
            FeederAudit feederAudit,
            PartySelf subject,
            Boolean isQueryable,
            Boolean isModifiable,
            ItemStructure otherDetails) {
        return new EhrStatusDto(
                uid,
                archetypeNodeId,
                name,
                archetypeDetails,
                feederAudit,
                subject,
                isQueryable,
                isModifiable,
                otherDetails);
    }
}
