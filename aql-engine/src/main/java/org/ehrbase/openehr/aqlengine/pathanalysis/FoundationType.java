/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.openehr.aqlengine.pathanalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * https://specifications.openehr.org/releases/BASE/latest/foundation_types.html
 * <p>
 * Note that the types names are based on the Archie type model and are not fully aligned with the specification.
 */
public enum FoundationType {
    BOOLEAN(FoundationTypeCategory.BOOLEAN),
    /**
     * Java-Alias for "Octet".
     * Only as multiple-valued for byte arrays
     */
    BYTE(FoundationTypeCategory.BYTEA),
    DOUBLE(FoundationTypeCategory.NUMERIC),
    INTEGER(FoundationTypeCategory.NUMERIC),
    /**
     * Java-Alias for "Integer64"
     */
    LONG(FoundationTypeCategory.NUMERIC),
    STRING(FoundationTypeCategory.TEXT),
    URI(FoundationTypeCategory.TEXT),

    /*
     * <pre>
     * Openehr:
     * Temporal
     * - Iso8601_type
     * -- Iso8601_date
     * -- Iso8601_time
     * -- Iso8601_date_time
     * -- Iso8601_duration
     *
     * DV_DATE_TIME: inherit DV_TEMPORAL, Iso8601_date_time
     * DV_DATE: inherit DV_TEMPORAL, Iso8601_date
     * DV_TIME: inherit DV_TEMPORAL, Iso8601_time
     * DV_DURATION: inherit DV_AMOUNT, Iso8601_duration
     *
     * As opposed to the specification, the Archie temporal classes do not extend/implement a subtype of Temporal.
     *
     * Also the value property of these classes is not of type String:
     * - DvDateTime.value: TemporalAccessor
     * - DvDate.value: Temporal
     * - DvTime.value: TemporalAccessor
     * - DvDuration.value: TemporalAmount
     *
     * It may be feasible to treat those temporal types as subtypes of STRING.
     *
     * </pre>
     */
    TEMPORAL(FoundationTypeCategory.TEXT),
    TEMPORAL_ACCESSOR(FoundationTypeCategory.TEXT),
    TEMPORAL_AMOUNT(FoundationTypeCategory.TEXT),
    /**
     * Java-Alias for "Character",
     * Only used for TERM_MAPPING.match
     */
    CHAR(FoundationTypeCategory.TEXT),
    /**
     * Java-Alias for "Any": not a foundation type, used as generic placeholder in INTERVAL<T>
     */
    OBJECT(FoundationTypeCategory.ANY);

    private static final Map<String, FoundationType> BY_TYPE_NAME = new HashMap<>();

    static {
        for (FoundationType value : values()) {
            BY_TYPE_NAME.put(value.name(), value);
        }
    }

    public final FoundationTypeCategory category;

    FoundationType(FoundationTypeCategory category) {
        this.category = category;
    }

    public static Optional<FoundationType> byTypeName(String typeName) {
        return Optional.ofNullable(BY_TYPE_NAME.get(typeName));
    }
}
