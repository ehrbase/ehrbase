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

import static org.ehrbase.jooq.dbencoding.CompositionSerializer.TAG_GUIDELINE_ID;

import com.nedap.archie.rm.composition.CareEntry;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.ItemStack;
import org.ehrbase.jooq.dbencoding.RmObjectEncoding;

/** populate the attributes for RM CareEntry */
public abstract class CareEntryAttributes extends EntryAttributes {

    public CareEntryAttributes(
            CompositionSerializer compositionSerializer, ItemStack itemStack, Map<String, Object> map) {
        super(compositionSerializer, itemStack, map);
    }

    public Map<String, Object> toMap(CareEntry careEntry) {

        if (careEntry.getGuidelineId() != null) {

            map.put(TAG_GUIDELINE_ID, new RmObjectEncoding(careEntry.getGuidelineId()).toMap());
        }

        map = super.toMap(careEntry);

        return map;
    }
}
