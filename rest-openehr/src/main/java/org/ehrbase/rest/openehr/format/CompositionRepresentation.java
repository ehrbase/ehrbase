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
package org.ehrbase.rest.openehr.format;

import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.springframework.http.MediaType;

/**
 * Defines all supported combinations of {@link MediaType}s with its corresponding {@link CompositionFormat}s. To
 * select an available {@link CompositionRepresentation} use the
 * {@link #selectFromMediaTypeWithFormat(MediaType, CompositionFormat)} function.
 */
public enum CompositionRepresentation {

    /**
     * An <code>XML</code> representation of a composition
     */
    XML(MediaType.APPLICATION_XML, CompositionFormat.XML),

    /**
     * A canonical <code>JSON</code> representation of a composition
     */
    JSON(MediaType.APPLICATION_JSON, CompositionFormat.JSON),

    /**
     * A structured <code>JSON</code> (<code>structSDT</code>) representation of a composition
     */
    // FlatFormat.STRUCTURED application/openehr.structSDT+json
    JSON_STRUCTURED(OpenEHRMediaType.APPLICATION_WT_STRUCTURED_SCHEMA_JSON, CompositionFormat.STRUCTURED),

    /**
     * A flat <code>JSON</code> (<code>simSDT</code>) representation of a composition
     */
    // FlatFormat.SIM_SDT application/openehr.simSDT+json
    JSON_FLAT(OpenEHRMediaType.APPLICATION_WT_FLAT_SCHEMA_JSON, CompositionFormat.FLAT);

    /**
     * The actual format of the composition
     */
    public final CompositionFormat format;

    /**
     * The used <code>Content-Type</code> of the composition
     */
    public final MediaType mediaType;

    CompositionRepresentation(MediaType mediaType, CompositionFormat compositionFormat) {
        this.mediaType = mediaType;
        this.format = compositionFormat;
    }

    /**
     * Selects the supported {@link CompositionRepresentation} from the given {@link MediaType} in combination with the
     * provided {@link CompositionFormat}.
     *
     * @param mediaType of the serialized composition
     *
     * @param format of the composition use for serialization and/or deserialization
     *
     * @return {@link CompositionRepresentation} of a composition to use
     * @throws InvalidApiParameterException when the given <code>format</code> is not supported at all.
     * @throws NotAcceptableException when content type or composition format is not supported or the input is invalid.
     */
    public static CompositionRepresentation selectFromMediaTypeWithFormat(
            MediaType mediaType, CompositionFormat format) {

        if (format != null) {
            validateSupportedCompositionFormat(format);
        }

        if (mediaType.isCompatibleWith(MediaType.APPLICATION_XML)) {
            return selectXMLRepresentation(mediaType, format);
        } else if (mediaType.isCompatibleWith(OpenEHRMediaType.APPLICATION_WT_FLAT_SCHEMA_JSON)) {
            return CompositionRepresentation.JSON_FLAT;
        } else if (mediaType.isCompatibleWith(OpenEHRMediaType.APPLICATION_WT_STRUCTURED_SCHEMA_JSON)) {
            return CompositionRepresentation.JSON_STRUCTURED;
        } else if (mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            return selectJSONRepresentation(mediaType, format);
        } else {
            throw new NotAcceptableException("Only compositions in XML or JSON are supported at the moment");
        }
    }

    private static void validateSupportedCompositionFormat(CompositionFormat format) {
        switch (format) {
            case FLAT, STRUCTURED, XML, JSON -> {
                /* supported format */
            }
            case RAW, EXPANDED, ECISFLAT -> throw new InvalidApiParameterException(
                    String.format("Format %s not supported", format));
        }
    }

    private static CompositionRepresentation selectXMLRepresentation(MediaType mediaType, CompositionFormat format) {
        // extract the appropriate composition format and fallback to standard XML if needed
        if (format == null || format == CompositionFormat.XML) {
            return CompositionRepresentation.XML;
        } else {
            throw new NotAcceptableException(
                    "Only composition format [XML] is supported at the moment for type [%s]".formatted(mediaType));
        }
    }

    private static CompositionRepresentation selectJSONRepresentation(MediaType mediaType, CompositionFormat format) {
        // extract the appropriate composition format and fallback to standard JSON if needed
        return switch (format) {
            case null -> CompositionRepresentation.JSON;
            case CompositionFormat.JSON -> CompositionRepresentation.JSON;
            case CompositionFormat.FLAT -> CompositionRepresentation.JSON_FLAT;
            case CompositionFormat.STRUCTURED -> CompositionRepresentation.JSON_STRUCTURED;
            default -> throw new NotAcceptableException(
                    "Only compositions formats [JSON, FLAT, STRUCTURED] are supported at the moment for [%s]"
                            .formatted(mediaType));
        };
    }
}
