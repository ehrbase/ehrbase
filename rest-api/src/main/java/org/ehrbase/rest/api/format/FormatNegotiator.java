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
package org.ehrbase.rest.api.format;

import org.ehrbase.rest.api.media.EhrMediaType;
import org.springframework.http.MediaType;

/**
 * Resolves composition format from Content-Type, Accept header, and {@code ?format=} query parameter.
 * Supports cross-format: accept in any format, return in any other.
 */
public final class FormatNegotiator {

    private FormatNegotiator() {}

    /**
     * Formats supported for composition serialization.
     */
    public enum Format {
        CANONICAL_JSON("JSON", MediaType.APPLICATION_JSON),
        CANONICAL_XML("XML", MediaType.APPLICATION_XML),
        FLAT("FLAT", EhrMediaType.APPLICATION_WT_FLAT),
        STRUCTURED("STRUCTURED", EhrMediaType.APPLICATION_WT_STRUCTURED);

        private final String paramValue;
        private final MediaType mediaType;

        Format(String paramValue, MediaType mediaType) {
            this.paramValue = paramValue;
            this.mediaType = mediaType;
        }

        public String paramValue() {
            return paramValue;
        }

        public MediaType mediaType() {
            return mediaType;
        }
    }

    /**
     * Resolves input format from Content-Type header and optional {@code ?format=} parameter.
     */
    public static Format resolveInput(String contentType, String formatParam) {
        if (formatParam != null && !formatParam.isBlank()) {
            return fromParam(formatParam);
        }
        if (contentType == null) {
            return Format.CANONICAL_JSON;
        }
        return fromMediaType(contentType);
    }

    /**
     * Resolves output format from Accept header and optional {@code ?format=} parameter.
     */
    public static Format resolveOutput(String acceptHeader, String formatParam) {
        if (formatParam != null && !formatParam.isBlank()) {
            return fromParam(formatParam);
        }
        if (acceptHeader == null || acceptHeader.contains("*/*")) {
            return Format.CANONICAL_JSON;
        }
        return fromMediaType(acceptHeader);
    }

    private static Format fromParam(String param) {
        return switch (param.toUpperCase()) {
            case "JSON" -> Format.CANONICAL_JSON;
            case "XML" -> Format.CANONICAL_XML;
            case "FLAT" -> Format.FLAT;
            case "STRUCTURED" -> Format.STRUCTURED;
            default ->
                throw new org.ehrbase.api.exception.NotAcceptableException(
                        "Unsupported format: %s. Supported: JSON, XML, FLAT, STRUCTURED".formatted(param));
        };
    }

    private static Format fromMediaType(String mediaType) {
        String lower = mediaType.toLowerCase();
        if (lower.contains("xml")) return Format.CANONICAL_XML;
        if (lower.contains("flat")) return Format.FLAT;
        if (lower.contains("structured")) return Format.STRUCTURED;
        return Format.CANONICAL_JSON;
    }
}
