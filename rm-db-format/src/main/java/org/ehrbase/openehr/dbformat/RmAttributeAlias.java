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
package org.ehrbase.openehr.dbformat;

import static org.ehrbase.openehr.dbformat.DbToRmFormat.TYPE_ATTRIBUTE;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * For the database: Shorter aliases for attributes of RmObjects
 */
public record RmAttributeAlias(String attribute, String alias) {

    public static final List<RmAttributeAlias> VALUES = List.of(
            // INSTRUCTION
            alias("activities", 'a'),
            // COMPOSITION
            alias("content", 'c'),
            alias("context", 'x'),
            // ADMIN_ENTRY, EVALUATION, OBSERVATION, EVENT, INTERVAL_EVENT, POINT_EVENT
            alias("data", 'd'),
            // ACTION, ACTIVITY
            alias("description", 't'),
            // HISTORY
            alias("events", 'e'),
            // ACTION
            alias("instruction_details", 'n'),
            // ITEM_SINGLE
            alias("item", 'j'),
            // SECTION, CLUSTER, ITEM_LIST, ITEM_TREE
            alias("items", 'i'),
            // EVENT_CONTEXT
            alias("other_context", 'o'),
            // ACTION, CARE_ENTRY, EVALUATION, INSTRUCTION, OBSERVATION
            alias("protocol", 'p'),
            // ITEM_TABLE
            alias("rows", 'r'),
            // OBSERVATION, EVENT, INTERVAL_EVENT, POINT_EVENT
            alias("state", 's'),
            // HISTORY
            alias("summary", 'y'),
            // INSTRUCTION_DETAILS
            alias("wf_details", 'w'),
            // FEEDER_AUDIT
            alias("feeder_audit", 'f'),
            alias("accuracy", "ay"),
            alias("accuracy_is_percent", "ayp"),
            alias("action_archetype_id", "aa"),
            alias("activity_id", "ac"),
            alias("alternate_text", "at"),
            alias("archetype_details", "ad"),
            alias("archetype_id", "aX"),
            alias("archetype_node_id", "A"),
            alias("assigner", "as"),
            alias("attestations", "att"),
            alias("attested_view", "atv"),
            alias("careflow_step", "cf"),
            alias("category", "ca"),
            alias("change_type", "ct"),
            alias("charset", "ch"),
            alias("code_string", "cd"),
            alias("committer", "co"),
            alias("commit_audit", "cau"),
            alias("composer", "cp"),
            alias("compression_algorithm", "calg"),
            alias("contribution", "con"),
            alias("current_state", "cu"),
            alias("defining_code", "df"),
            alias("denominator", "de"),
            alias("details", "dt"),
            alias("domain_concept", "dc"),
            alias("duration", "du"),
            alias("encoding", "ec"),
            alias("end_time", "et"),
            alias("expiry_time", "ex"),
            alias("external_ref", "er"),
            alias("feeder_system_audit", "fs"),
            alias("feeder_system_item_ids", "fX"),
            alias("folders", "fo"),
            alias("formalism", "fm"),
            alias("formatting", "fr"),
            alias("function", "fu"),
            alias("guideline_id", "gX"),
            alias("health_care_facility", "hc"),
            alias("hyperlink", "hy"),
            alias("id", "X"),
            alias("identifiers", "Xs"),
            alias("instruction_id", "iX"),
            alias("integrity_check", "ic"),
            alias("integrity_check_algorithm", "ica"),
            alias("interval", "in"),
            alias("ism_transition", "it"),
            alias("issuer", "is"),
            alias("is_modifiable", "im"),
            alias("is_pending", "ip"),
            alias("is_queryable", "iq"),
            alias("is_terminal", "il"),
            alias("language", "la"),
            alias("lifecycle_state", "ls"),
            alias("links", "lk"),
            alias("location", "lc"),
            alias("lower", "l"),
            alias("lower_included", "li"),
            alias("lower_unbounded", "lu"),
            alias("magnitude", "m"),
            alias("magnitude_status", "ms"),
            alias("mappings", "mp"),
            alias("match", "ma"),
            alias("math_function", "mf"),
            alias("meaning", "me"),
            alias("media_type", "mt"),
            alias("mode", "mo"),
            alias("name", "N"),
            alias("namespace", "ns"),
            alias("narrative", "nv"),
            alias("normal_range", "nr"),
            alias("normal_status", "nt"),
            alias("null_flavour", "nf"),
            alias("null_reason", "nl"),
            alias("numerator", "nu"),
            alias("origin", "og"),
            alias("original_content", "oc"),
            alias("originating_system_audit", "oa"),
            alias("originating_system_item_ids", "os"),
            alias("other_details", "od"),
            alias("other_input_version_uids", "oX"),
            alias("other_participations", "op"),
            alias("other_reference_ranges", "or"),
            alias("participations", "pp"),
            alias("path", "pa"),
            alias("performer", "pf"),
            alias("period", "pe"),
            alias("preceding_version_uid", "pX"),
            alias("precision", "pc"),
            alias("preferred_term", "pt"),
            alias("proof", "prf"),
            alias("property", "pr"),
            alias("provider", "pv"),
            alias("purpose", "pu"),
            alias("qualified_rm_entity", "qr"),
            alias("range", "ra"),
            alias("reason", "re"),
            alias("relationship", "rs"),
            alias("rm_entity", "rm"),
            alias("rm_name", "rn"),
            alias("rm_originator", "ro"),
            alias("rm_version", "rv"),
            alias("sample_count", "sn"),
            alias("scheme", "sc"),
            alias("setting", "se"),
            alias("signature", "sig"),
            alias("size", "si"),
            alias("specialisation", "sp"),
            alias("start_time", "st"),
            alias("subject", "su"),
            alias("symbol", "sy"),
            alias("system_id", "sX"),
            alias("target", "ta"),
            alias("template_id", "tm"),
            alias("terminology_id", "te"),
            alias("territory", "ty"),
            alias("thumbnail", "th"),
            alias("time", "ti"),
            alias("time_asserted", "ts"),
            alias("time_committed", "tc"),
            alias("timing", "tg"),
            alias("transition", "tr"),
            alias("type", "tp"),
            alias("uid", "U"),
            alias("units", "un"),
            alias("units_display_name", "ud"),
            alias("units_system", "us"),
            alias("upper", "u"),
            alias("upper_included", "ui"),
            alias("upper_unbounded", "uu"),
            alias("uri", "ur"),
            alias("value", "V"),
            alias("version_id", "vX"),
            alias("wf_definition", "wd"),
            alias("width", "wi"),
            alias("workflow_id", "wX"),
            alias("_index", "I"),
            alias("_magnitude", "M"),
            alias(TYPE_ATTRIBUTE, "T"));

    private static Map<String, String> attribute2alias;

    private static Map<String, String> alias2attribute;

    static {
        attribute2alias =
                VALUES.stream().collect(Collectors.toMap(RmAttributeAlias::attribute, RmAttributeAlias::alias));
        alias2attribute =
                VALUES.stream().collect(Collectors.toMap(RmAttributeAlias::alias, RmAttributeAlias::attribute));
    }

    /**
     * Entries that are usually used for StructureNode.entityIdx
     * @param attribute
     * @param alias
     */
    private static RmAttributeAlias alias(String attribute, char alias) {
        return alias(attribute, Character.toString(alias));
    }

    /**
     * Entries that are not or rarely used for StructureNode.entityIdx
     * @param attribute
     * @param alias
     */
    private static RmAttributeAlias alias(String attribute, String alias) {
        return new RmAttributeAlias(attribute, alias);
    }

    public static String getAlias(String attribute) {
        String alias = attribute2alias.get(attribute);
        if (alias == null) {
            throw new IllegalArgumentException("Missing alias for attribute " + attribute);
        }
        return alias;
    }

    public static String[] rmToJsonPathParts(String rmPath) {
        return Arrays.stream(rmPath.split("/")).map(RmAttributeAlias::getAlias).toArray(String[]::new);
    }

    public static String getAttribute(String alias) {
        String attribute = alias2attribute.get(alias);
        if (attribute == null) {
            throw new IllegalArgumentException("Missing attribute for alias " + alias);
        }
        return attribute;
    }

    public static boolean isAlias(String toCheck) {
        return alias2attribute.keySet().contains(toCheck);
    }
}
