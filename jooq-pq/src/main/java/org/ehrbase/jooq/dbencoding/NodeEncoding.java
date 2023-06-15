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
package org.ehrbase.jooq.dbencoding;

import com.nedap.archie.rm.archetyped.Locatable;
import org.apache.commons.collections.map.MultiValueMap;

public class NodeEncoding {

    private final CompositionSerializer.WalkerOutputMode tagMode;

    public NodeEncoding(CompositionSerializer.WalkerOutputMode tagMode) {
        this.tagMode = tagMode;
    }

    public String tag(String prefix, Locatable node, Object container) {
        switch (tagMode) {
            case PATH:
                if (node == null) return prefix;
                else {
                    String path = prefix + "[" + node.getArchetypeNodeId() + "]";
                    if (!container.getClass().equals(MultiValueMap.class)
                            && !(path.startsWith(CompositionSerializer.TAG_DESCRIPTION))
                            && !(path.startsWith(CompositionSerializer.TAG_COMPOSITION))
                            && (path.contains("[openEHR-")
                                    || path.contains(CompositionSerializer.TAG_ACTIVITIES)
                                    || path.contains(CompositionSerializer.TAG_ITEMS)
                                    || path.contains(CompositionSerializer.TAG_EVENTS))) {

                        // expand name in key
                        String name = node.getName().getValue();

                        if (name != null) {
                            path = path.substring(0, path.lastIndexOf("]")) + " and name/value='" + name + "']";
                        }
                    }

                    return path;
                }

            case NAMED:
            case EXPANDED:
            case RAW:
                if (prefix.equals(CompositionSerializer.TAG_ORIGIN)
                        || prefix.equals(CompositionSerializer.TAG_TIME)
                        || prefix.equals(CompositionSerializer.TAG_TIMING)
                        || (prefix.equals(CompositionSerializer.TAG_EVENTS) && node == null))
                    return "[" + prefix.substring(1) + "]";
                else if (node == null)
                    return "!!!INVALID NAMED for " + prefix + " !!!"; // comes from encodeNodeAttribute...
                else {
                    /* ISSUE, the name can be a translation hence any query in the JSON structure will be impossible!
                    F.e. String name = node.nodeName();
                    */
                    return node.getArchetypeNodeId();
                }

            default:
                return "*INVALID MODE*";
        }
    }
}
