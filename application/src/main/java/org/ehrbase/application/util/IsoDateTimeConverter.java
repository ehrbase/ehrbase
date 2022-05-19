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
package org.ehrbase.application.util;

import java.time.Instant;
import java.time.ZonedDateTime;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * IsoDateTimeConverter for parsing input ISO 6801 Date strings into a ZonedDateTime that contains the DateTime value
 * parsed into UTC time.
 */
@Component
public class IsoDateTimeConverter implements Converter<String, Instant> {

    @Override
    public Instant convert(final String source) {
        if (source.isEmpty()) {
            return null;
        }

        ZonedDateTime zdt = ZonedDateTime.parse(source);
        return zdt.toInstant();
    }
}
