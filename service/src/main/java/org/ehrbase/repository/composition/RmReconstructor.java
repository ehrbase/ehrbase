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
package org.ehrbase.repository.composition;

import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.archetyped.TemplateId;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvBoolean;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvIdentifier;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.DvURI;
import com.nedap.archie.rm.datavalues.encapsulated.DvMultimedia;
import com.nedap.archie.rm.datavalues.encapsulated.DvParsable;
import com.nedap.archie.rm.datavalues.quantity.DvCount;
import com.nedap.archie.rm.datavalues.quantity.DvOrdinal;
import com.nedap.archie.rm.datavalues.quantity.DvProportion;
import com.nedap.archie.rm.datavalues.quantity.DvQuantity;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDate;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDuration;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvTime;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.support.identification.ArchetypeID;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;

/**
 * Reconstructs an Archie RM Composition from normalized table column values.
 * Inverse of {@link RmTreeWalker#extract}.
 *
 * <p>Walks the WebTemplate tree and for each ELEMENT, looks up the column values
 * and constructs the appropriate DV type.
 */
public final class RmReconstructor {

    private static final Set<String> STRUCTURE_NODES = Set.of(
            "COMPOSITION",
            "SECTION",
            "OBSERVATION",
            "EVALUATION",
            "INSTRUCTION",
            "ACTION",
            "ADMIN_ENTRY",
            "ACTIVITY",
            "HISTORY",
            "EVENT",
            "POINT_EVENT",
            "INTERVAL_EVENT",
            "ITEM_TREE",
            "ITEM_LIST",
            "ITEM_SINGLE",
            "ITEM_TABLE");

    private RmReconstructor() {}

    /**
     * Reconstructs a Composition from column values and metadata.
     *
     * @param mainRow      column values from the main template table
     * @param childRows    column values from child tables, keyed by child table name
     * @param webTemplate  the WebTemplate for structural guidance
     * @param meta         composition metadata from ehr_system.composition
     * @return reconstructed Composition RM object
     */
    public static Composition reconstruct(
            Map<String, Object> mainRow,
            Map<String, List<Map<String, Object>>> childRows,
            WebTemplate webTemplate,
            CompositionMetadata meta) {

        Composition composition = new Composition();

        // Set composition-level metadata
        composition.setArchetypeNodeId(meta.archetypeId());
        composition.setName(new DvText(meta.templateName()));

        // Set archetype details
        Archetyped archetypeDetails = new Archetyped();
        archetypeDetails.setArchetypeId(new ArchetypeID(meta.archetypeId()));
        TemplateId templateId = new TemplateId();
        templateId.setValue(webTemplate.getTemplateId());
        archetypeDetails.setTemplateId(templateId);
        composition.setArchetypeDetails(archetypeDetails);

        // Language and territory
        if (meta.language() != null) {
            composition.setLanguage(new CodePhrase(new TerminologyId("ISO_639-1"), meta.language()));
        }
        if (meta.territory() != null) {
            composition.setTerritory(new CodePhrase(new TerminologyId("ISO_3166-1"), meta.territory()));
        }

        // Category
        if (meta.categoryCode() != null) {
            composition.setCategory(new DvCodedText(
                    meta.categoryCode(), new CodePhrase(new TerminologyId("openehr"), meta.categoryCode())));
        }

        // Composer
        PartyIdentified composer = new PartyIdentified();
        composer.setName(meta.composerName());
        composition.setComposer(composer);

        // TODO: Walk WebTemplate tree to reconstruct content structure
        // This requires building the full RM tree (Sections, Observations, Elements, etc.)
        // For now, this creates the composition shell with metadata.
        // Full content reconstruction will be implemented when specific templates are tested.

        return composition;
    }

    /**
     * Reconstructs a DataValue from column values based on the RM type.
     * Used by the tree walker during full content reconstruction.
     *
     * @param rmType   the RM type name (e.g., "DV_QUANTITY")
     * @param colBase  the base column name
     * @param row      the column values map
     * @return reconstructed DataValue, or null if all columns are null
     */
    public static com.nedap.archie.rm.datavalues.DataValue reconstructDvValue(
            String rmType, String colBase, Map<String, Object> row) {

        return switch (rmType) {
            case "DV_BOOLEAN" -> {
                Boolean val = getAs(row, colBase, Boolean.class);
                yield val != null ? new DvBoolean(val) : null;
            }

            case "DV_QUANTITY" -> {
                Double magnitude = getAs(row, colBase + "_magnitude", Double.class);
                String units = getAs(row, colBase + "_units", String.class);
                Integer precision = getAs(row, colBase + "_precision", Integer.class);
                yield magnitude != null ? new DvQuantity(units, magnitude, precision != null ? precision : 0) : null;
            }

            case "DV_COUNT" -> {
                Long val = getAs(row, colBase, Long.class);
                yield val != null ? new DvCount(val) : null;
            }

            case "DV_CODED_TEXT" -> {
                String value = getAs(row, colBase + "_value", String.class);
                String code = getAs(row, colBase + "_code", String.class);
                String terminology = getAs(row, colBase + "_terminology", String.class);
                if (value == null) yield null;
                if (code != null && terminology != null) {
                    yield new DvCodedText(value, new CodePhrase(new TerminologyId(terminology), code));
                }
                yield new DvCodedText(value, new CodePhrase(new TerminologyId("local"), code != null ? code : ""));
            }

            case "DV_TEXT" -> {
                String val = getAs(row, colBase, String.class);
                yield val != null ? new DvText(val) : null;
            }

            case "DV_DATE_TIME" -> {
                String val = getAs(row, colBase, String.class);
                yield val != null ? new DvDateTime(val) : null;
            }

            case "DV_DATE" -> {
                String val = getAs(row, colBase, String.class);
                yield val != null ? new DvDate(val) : null;
            }

            case "DV_TIME" -> {
                String val = getAs(row, colBase, String.class);
                yield val != null ? new DvTime(val) : null;
            }

            case "DV_DURATION" -> {
                String val = getAs(row, colBase, String.class);
                yield val != null ? new DvDuration(val) : null;
            }

            case "DV_PROPORTION" -> {
                Double numerator = getAs(row, colBase + "_numerator", Double.class);
                Double denominator = getAs(row, colBase + "_denominator", Double.class);
                Integer type = getAs(row, colBase + "_type", Integer.class);
                yield numerator != null
                        ? new DvProportion(numerator, denominator != null ? denominator : 1.0, type != null ? type : 0)
                        : null;
            }

            case "DV_ORDINAL" -> {
                Long ordinalValue = getAs(row, colBase + "_value", Long.class);
                String symbolValue = getAs(row, colBase + "_symbol_value", String.class);
                String symbolCode = getAs(row, colBase + "_symbol_code", String.class);
                if (ordinalValue == null) yield null;
                DvCodedText symbol = new DvCodedText(
                        symbolValue != null ? symbolValue : "",
                        new CodePhrase(new TerminologyId("local"), symbolCode != null ? symbolCode : ""));
                yield new DvOrdinal(ordinalValue, symbol);
            }

            case "DV_IDENTIFIER" -> {
                String id = getAs(row, colBase + "_id", String.class);
                if (id == null) yield null;
                DvIdentifier dv = new DvIdentifier();
                dv.setId(id);
                dv.setIssuer(getAs(row, colBase + "_issuer", String.class));
                dv.setAssigner(getAs(row, colBase + "_assigner", String.class));
                dv.setType(getAs(row, colBase + "_type", String.class));
                yield dv;
            }

            case "DV_URI", "DV_EHR_URI" -> {
                String val = getAs(row, colBase, String.class);
                yield val != null ? new DvURI(URI.create(val)) : null;
            }

            case "DV_MULTIMEDIA" -> {
                String uri = getAs(row, colBase + "_uri", String.class);
                String mediaType = getAs(row, colBase + "_media_type", String.class);
                if (uri == null) yield null;
                DvMultimedia dv = new DvMultimedia();
                dv.setUri(new DvURI(URI.create(uri)));
                if (mediaType != null) {
                    dv.setMediaType(new CodePhrase(new TerminologyId("IANA_media-types"), mediaType));
                }
                yield dv;
            }

            case "DV_PARSABLE" -> {
                String val = getAs(row, colBase + "_value", String.class);
                String formalism = getAs(row, colBase + "_formalism", String.class);
                yield val != null ? new DvParsable(val, formalism != null ? formalism : "") : null;
            }

            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private static <T> T getAs(Map<String, Object> row, String key, Class<T> type) {
        Object val = row.get(key);
        if (val == null) return null;
        if (type.isInstance(val)) return (T) val;
        // Handle numeric type conversions
        if (val instanceof Number num) {
            if (type == Double.class) return (T) Double.valueOf(num.doubleValue());
            if (type == Long.class) return (T) Long.valueOf(num.longValue());
            if (type == Integer.class) return (T) Integer.valueOf(num.intValue());
        }
        if (type == String.class) return (T) val.toString();
        return null;
    }
}
