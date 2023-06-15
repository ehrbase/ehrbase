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

import static org.ehrbase.jooq.dbencoding.CompositionSerializer.*;

import com.nedap.archie.rm.datastructures.Event;
import com.nedap.archie.rm.datastructures.IntervalEvent;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.CompositionSerializer;
import org.ehrbase.jooq.dbencoding.ItemStack;

/**
 * populate the attributes for RM Event, in particular timing information
 */
public class EventAttributes extends LocatableAttributes {

    public EventAttributes(CompositionSerializer compositionSerializer, ItemStack itemStack, Map<String, Object> map) {
        super(compositionSerializer, itemStack, map);
    }

    public Map<String, Object> toMap(Event<?> event) {

        if (event instanceof IntervalEvent) {
            IntervalEvent<?> intervalEvent = (IntervalEvent<?>) event;
            if (intervalEvent.getWidth() != null) map.put(TAG_WIDTH, intervalEvent.getWidth());
            if (intervalEvent.getMathFunction() != null) map.put(TAG_MATH_FUNCTION, intervalEvent.getMathFunction());

            if (intervalEvent.getSampleCount() != null) {
                map.put("sample_count", intervalEvent.getSampleCount());
            }
        }

        if (event.getTime() != null) {
            map = toMap(TAG_TIME, event.getTime(), event.getName());
        }

        map = super.toMap(event);

        return map;
    }
}
