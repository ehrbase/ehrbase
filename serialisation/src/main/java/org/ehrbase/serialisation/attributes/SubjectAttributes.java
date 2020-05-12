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
import com.nedap.archie.rm.generic.PartyRelated;
import com.nedap.archie.rm.generic.PartySelf;
import org.ehrbase.serialisation.CompositionSerializer;

import java.util.Map;

public class SubjectAttributes {

    private I_SubjectAttributes subjectAttributes;

    public SubjectAttributes(Object subject, CompositionSerializer compositionSerializer) {

        if (subject instanceof PartySelf)
            subjectAttributes = new PartySelfAttributes((PartySelf)subject, compositionSerializer);
        else if (subject instanceof PartyRelated)
            subjectAttributes = new PartyRelatedAttributes((PartyRelated)subject, compositionSerializer);
        else if (subject instanceof PartyIdentified)
            subjectAttributes = new PartyIdentifiedAttributes((PartyIdentified)subject, compositionSerializer);
        else
            throw new IllegalStateException("Could not handle subject of type:"+subject.getClass());
    }

    public Map<String, Object> toMap(){
        return subjectAttributes.toMap();
    }
}
