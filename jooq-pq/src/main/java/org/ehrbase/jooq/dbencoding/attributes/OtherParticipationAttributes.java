/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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

import com.nedap.archie.rm.generic.Participation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.PathMap;
import org.ehrbase.jooq.dbencoding.SimpleClassName;

/**
 * populate the attributes for RM OtherParticipations in CareEntry
 */
public class OtherParticipationAttributes {

    private final List<Participation> participationList;
    private final CompositionSerializer compositionSerializer;

    public OtherParticipationAttributes(
            List<Participation> participationList, CompositionSerializer compositionSerializer) {
        this.participationList = participationList;
        this.compositionSerializer = compositionSerializer;
    }

    public List<Map<String, Object>> toMap() {
        List<Map<String, Object>> participations = new ArrayList<>();
        for (Participation participation : participationList) {
            Map<String, Object> valuemap = PathMap.getInstance();
            valuemap.put(TAG_CLASS, new SimpleClassName(participation).toString());
            valuemap.put("function", participation.getFunction());
            valuemap.put("mode", participation.getMode());
            valuemap.put("time", participation.getTime());
            valuemap.put(
                    "performer", new SubjectAttributes(participation.getPerformer(), compositionSerializer).toMap());
            participations.add(valuemap);
        }

        return participations;
    }
}
