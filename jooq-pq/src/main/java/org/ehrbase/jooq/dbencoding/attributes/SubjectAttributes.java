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

import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartyRelated;
import com.nedap.archie.rm.generic.PartySelf;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;

/**
 * populate the attributes for RM PartyProxy specialization
 */
public class SubjectAttributes {

    private final I_SubjectAttributes iSubjectAttributes;

    public SubjectAttributes(Object subject, CompositionSerializer compositionSerializer) {

        if (subject instanceof PartySelf)
            iSubjectAttributes = new PartySelfAttributes((PartySelf) subject, compositionSerializer);
        else if (subject instanceof PartyRelated)
            iSubjectAttributes = new PartyRelatedAttributes((PartyRelated) subject, compositionSerializer);
        else if (subject instanceof PartyIdentified)
            iSubjectAttributes = new PartyIdentifiedAttributes((PartyIdentified) subject, compositionSerializer);
        else throw new IllegalStateException("Could not handle subject of type:" + subject.getClass());
    }

    public Map<String, Object> toMap() {
        return iSubjectAttributes.toMap();
    }
}
