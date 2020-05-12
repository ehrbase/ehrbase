/*
 * Copyright (c) 2020 Christian Chevalley (Hannover Medical School).
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
package org.ehrbase.serialisation.attributes;

import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartySelf;
import org.ehrbase.serialisation.CompositionSerializer;

import java.util.Map;

import static org.ehrbase.serialisation.CompositionSerializer.TAG_CLASS;

public class PartyIdentifiedAttributes implements I_SubjectAttributes {

    PartyIdentified partyIdentified;
    CompositionSerializer compositionSerializer;

    public PartyIdentifiedAttributes(PartyIdentified partyIdentified, CompositionSerializer compositionSerializer) {
        this.partyIdentified = partyIdentified;
        this.compositionSerializer = compositionSerializer;
    }

    public Map<String, Object> toMap(){
        Map<String, Object> valuemap = compositionSerializer.newPathMap();

        valuemap.put(TAG_CLASS, partyIdentified.getClass().getSimpleName());

        valuemap.put("external_ref", partyIdentified.getExternalRef());
        valuemap.put("name", partyIdentified.getName());
        if (!partyIdentified.getIdentifiers().isEmpty())
            valuemap.put("identifiers", partyIdentified.getIdentifiers());

        return valuemap;
    }
}
