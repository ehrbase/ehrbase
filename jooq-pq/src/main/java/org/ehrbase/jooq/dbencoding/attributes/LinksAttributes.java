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

import com.nedap.archie.rm.archetyped.Link;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.PathMap;
import org.ehrbase.jooq.dbencoding.SimpleClassName;

/**
 * populate the attributes for RM Links
 */
public class LinksAttributes {

    private final List<Link> linkList;

    public LinksAttributes(List<Link> linkList) {
        this.linkList = linkList;
    }

    public List<Map<String, Object>> toMap() {
        List<Map<String, Object>> links = new ArrayList<>();
        for (Link link : linkList) {
            Map<String, Object> valuemap = PathMap.getInstance();
            valuemap.put(TAG_CLASS, new SimpleClassName(link).toString());
            valuemap.put("meaning", link.getMeaning());
            valuemap.put("type", link.getType());
            valuemap.put("target", link.getTarget());
            links.add(valuemap);
        }

        return links;
    }
}
