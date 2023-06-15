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
package org.ehrbase.jooq.dbencoding;

import com.nedap.archie.rm.composition.*;
import com.nedap.archie.rm.datastructures.Event;
import com.nedap.archie.rm.datastructures.History;
import java.util.Map;

/** Used to insert Entry specific attributes in the encoded map. */
public class EntrySerialTree extends SerialTree {

    private final CompositionSerializer.WalkerOutputMode tagMode;

    public EntrySerialTree(Map<String, Object> map, CompositionSerializer.WalkerOutputMode tagMode) {
        super(map);
        this.tagMode = tagMode;
    }

    public Map<String, Object> insert(Composition composition, String attribute, Object addStructure) {
        Map<String, Object> retMap;
        switch (attribute) {
            case CompositionSerializer.TAG_COMPOSITION:
                retMap = super.insert(
                        composition,
                        new NodeEncoding(tagMode).tag(CompositionSerializer.TAG_COMPOSITION, composition, map),
                        addStructure);
                break;
            default:
                retMap = map;
        }

        return retMap;
    }

    public Map<String, Object> insert(ContentItem contentItem, String attribute, Object addStructure) {
        Map<String, Object> retMap;
        switch (attribute) {
            case CompositionSerializer.TAG_CONTENT:
                if (addStructure != null && !((Map) addStructure).containsKey(CompositionSerializer.TAG_NAME))
                    ((Map) addStructure)
                            .put(
                                    CompositionSerializer.TAG_NAME,
                                    new NameAsDvText(contentItem.getName())
                                            .toMap()); // this fixes the issue with SECTION name
                retMap = super.insert(
                        contentItem,
                        new NodeEncoding(tagMode).tag(CompositionSerializer.TAG_CONTENT, contentItem, map),
                        addStructure);
                break;
            default:
                retMap = map;
        }

        return retMap;
    }

    public Map<String, Object> insert(Observation observation, String attribute, Object addStructure) {
        Map<String, Object> retMap;
        switch (attribute) {
            case CompositionSerializer.TAG_PROTOCOL:
                retMap = super.insert(
                        observation,
                        new NodeEncoding(tagMode)
                                .tag(CompositionSerializer.TAG_PROTOCOL, observation.getProtocol(), map),
                        addStructure);
                break;
            case CompositionSerializer.TAG_DATA:
                retMap = super.insert(
                        observation,
                        new NodeEncoding(tagMode).tag(CompositionSerializer.TAG_DATA, observation.getData(), map),
                        addStructure);
                break;
            case CompositionSerializer.TAG_STATE:
                retMap = super.insert(
                        observation,
                        new NodeEncoding(tagMode).tag(CompositionSerializer.TAG_STATE, observation.getState(), map),
                        addStructure);
                break;
            default:
                retMap = map;
        }

        return retMap;
    }

    public Map<String, Object> insert(Evaluation evaluation, String attribute, Object addStructure) {
        Map<String, Object> retMap;
        switch (attribute) {
            case CompositionSerializer.TAG_PROTOCOL:
                retMap = super.insert(
                        evaluation,
                        new NodeEncoding(tagMode)
                                .tag(CompositionSerializer.TAG_PROTOCOL, evaluation.getProtocol(), map),
                        addStructure);
                break;
            case CompositionSerializer.TAG_DATA:
                retMap = super.insert(
                        evaluation,
                        new NodeEncoding(tagMode).tag(CompositionSerializer.TAG_DATA, evaluation.getData(), map),
                        addStructure);
                break;
            default:
                retMap = map;
        }

        return retMap;
    }

    public Map<String, Object> insert(Instruction instruction, String attribute, Object addStructure) {
        Map<String, Object> retMap;
        switch (attribute) {
            case CompositionSerializer.TAG_PROTOCOL:
                retMap = super.insert(
                        instruction,
                        new NodeEncoding(tagMode)
                                .tag(CompositionSerializer.TAG_PROTOCOL, instruction.getProtocol(), map),
                        addStructure);
                break;
            case CompositionSerializer.TAG_ACTIVITIES:
                retMap = super.insert(instruction, CompositionSerializer.TAG_ACTIVITIES, addStructure);
                break;
            default:
                retMap = map;
        }

        return retMap;
    }

    public Map<String, Object> insert(Activity activity, String attribute, Object addStructure) {
        Map<String, Object> retMap;
        switch (attribute) {
            case CompositionSerializer.TAG_ACTIVITIES:
                retMap = super.insert(
                        activity,
                        new NodeEncoding(tagMode).tag(CompositionSerializer.TAG_ACTIVITIES, activity, map),
                        addStructure);
                break;
            case CompositionSerializer.TAG_DESCRIPTION:
                retMap = super.insert(
                        activity,
                        new NodeEncoding(tagMode)
                                .tag(CompositionSerializer.TAG_DESCRIPTION, activity.getDescription(), map),
                        addStructure);
                break;

            default:
                retMap = map;
        }

        return retMap;
    }

    public Map<String, Object> insert(Action action, String attribute, Object addStructure) {
        Map<String, Object> retMap;
        switch (attribute) {
            case CompositionSerializer.TAG_PROTOCOL:
                retMap = super.insert(
                        action,
                        new NodeEncoding(tagMode).tag(CompositionSerializer.TAG_PROTOCOL, action.getProtocol(), map),
                        addStructure);
                break;
            case CompositionSerializer.TAG_DESCRIPTION:
                retMap = super.insert(
                        action,
                        new NodeEncoding(tagMode)
                                .tag(CompositionSerializer.TAG_DESCRIPTION, action.getDescription(), map),
                        addStructure);
                break;
            default:
                retMap = map;
        }

        return retMap;
    }

    public Map<String, Object> insert(History<?> history, String attribute, Object addStructure) {
        Map<String, Object> retMap;
        switch (attribute) {
            case CompositionSerializer.TAG_SUMMARY:
                retMap = super.insert(
                        history,
                        new NodeEncoding(tagMode).tag(CompositionSerializer.TAG_SUMMARY, history, map),
                        addStructure);
                break;
            case CompositionSerializer.TAG_EVENTS:
                retMap = super.insert(
                        history,
                        new NodeEncoding(tagMode).tag(CompositionSerializer.TAG_EVENTS, null, map),
                        addStructure);
                break;
            default:
                retMap = map;
        }

        return retMap;
    }

    public Map<String, Object> insert(Event<?> event, String attribute, Object addStructure) {
        Map<String, Object> retMap;
        switch (attribute) {
            case CompositionSerializer.TAG_DATA:
                retMap = super.insert(
                        event,
                        new NodeEncoding(tagMode).tag(CompositionSerializer.TAG_DATA, event.getData(), map),
                        addStructure);
                break;
            case CompositionSerializer.TAG_STATE:
                retMap = super.insert(
                        event,
                        new NodeEncoding(tagMode).tag(CompositionSerializer.TAG_STATE, event.getState(), map),
                        addStructure);
                break;
            default:
                retMap = map;
        }

        return retMap;
    }
}
