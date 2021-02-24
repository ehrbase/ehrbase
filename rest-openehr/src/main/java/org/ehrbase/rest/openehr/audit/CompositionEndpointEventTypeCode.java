/*
 * Copyright (c) 2021 Vitasystems GmbH.
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
package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.types.EnumeratedCodedValue;
import org.openehealth.ipf.commons.audit.types.EventType;
import org.springframework.http.HttpMethod;

/**
 * Audit Event Type Code for openEHR.
 */
public enum CompositionEndpointEventTypeCode implements EventType, EnumeratedCodedValue<EventType> {

    CREATE("249", "creation"),

    UPDATE("251", "modification"),

    DELETE("523", "deleted");

    private final EventType value;

    CompositionEndpointEventTypeCode(String code, String originalText) {
        this.value = EventType.of(code, "openehr", originalText);
    }

    public static CompositionEndpointEventTypeCode resolve(HttpMethod method) {
        switch (method) {
            case POST:
                return CREATE;
            case PUT:
                return UPDATE;
            case DELETE:
                return DELETE;
            default:
                return null;
        }
    }

    @Override
    public EventType getValue() {
        return value;
    }
}
