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

import com.nedap.archie.rm.generic.PartyIdentified;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.PathMap;

/**
 * populate the attributes for RM PartyIdentified
 */
public class PartyIdentifiedAttributes implements I_SubjectAttributes {

    PartyIdentified partyIdentified;
    CompositionSerializer compositionSerializer;

    public PartyIdentifiedAttributes(PartyIdentified partyIdentified, CompositionSerializer compositionSerializer) {
        this.partyIdentified = partyIdentified;
        this.compositionSerializer = compositionSerializer;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> valuemap = PathMap.getInstance();

        valuemap.put(TAG_CLASS, partyIdentified.getClass().getSimpleName());

        valuemap.put("external_ref", partyIdentified.getExternalRef());
        valuemap.put("name", partyIdentified.getName());
        if (!partyIdentified.getIdentifiers().isEmpty()) valuemap.put("identifiers", partyIdentified.getIdentifiers());

        return valuemap;
    }
}
