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

import com.nedap.archie.rm.generic.PartyRelated;
import org.apache.commons.collections.map.PredicatedMap;
import org.apache.commons.collections4.functors.UniquePredicate;
import org.ehrbase.serialisation.CompositionSerializer;

import java.util.Map;

import static org.ehrbase.serialisation.CompositionSerializer.TAG_CLASS;

public class PartyRelatedAttributes implements I_SubjectAttributes {

    PartyRelated partyRelated;
    CompositionSerializer compositionSerializer;

    public PartyRelatedAttributes(PartyRelated partyRelated, CompositionSerializer compositionSerializer) {
        this.partyRelated = partyRelated;
        this.compositionSerializer = compositionSerializer;
    }

    public Map<String, Object> toMap(){
        Map<String, Object> valuemap = new PartyIdentifiedAttributes(partyRelated, compositionSerializer).toMap();
        valuemap.put("relationship", partyRelated.getRelationship());

        return valuemap;
    }
}
