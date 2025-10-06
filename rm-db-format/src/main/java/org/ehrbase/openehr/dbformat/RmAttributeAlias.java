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
            alias("archetype_node_id", 'A'),
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
            alias("lower", 'l'),
            alias("lower_included", "li"),
            alias("lower_unbounded", "lu"),
            alias("magnitude", 'm'),
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
        return switch (attribute) {
        case "activities" -> "a";
        case "content" -> "c";
        case "context" -> "x";
        case "data" -> "d";
        case "description" -> "t";
        case "events" -> "e";
        case "instruction_details" -> "n";
        case "item" -> "j";
        case "items" -> "i";
        case "other_context" -> "o";
        case "protocol" -> "p";
        case "rows" -> "r";
        case "state" -> "s";
        case "summary" -> "y";
        case "wf_details" -> "w";
        case "feeder_audit" -> "f";
        case "accuracy" -> "ay";
        case "accuracy_is_percent" -> "ayp";
        case "action_archetype_id" -> "aa";
        case "activity_id" -> "ac";
        case "alternate_text" -> "at";
        case "archetype_details" -> "ad";
        case "archetype_id" -> "aX";
        case "archetype_node_id" -> "A";
        case "assigner" -> "as";
        case "attestations" -> "att";
        case "attested_view" -> "atv";
        case "careflow_step" -> "cf";
        case "category" -> "ca";
        case "change_type" -> "ct";
        case "charset" -> "ch";
        case "code_string" -> "cd";
        case "committer" -> "co";
        case "commit_audit" -> "cau";
        case "composer" -> "cp";
        case "compression_algorithm" -> "calg";
        case "contribution" -> "con";
        case "current_state" -> "cu";
        case "defining_code" -> "df";
        case "denominator" -> "de";
        case "details" -> "dt";
        case "domain_concept" -> "dc";
        case "duration" -> "du";
        case "encoding" -> "ec";
        case "end_time" -> "et";
        case "expiry_time" -> "ex";
        case "external_ref" -> "er";
        case "feeder_system_audit" -> "fs";
        case "feeder_system_item_ids" -> "fX";
        case "folders" -> "fo";
        case "formalism" -> "fm";
        case "formatting" -> "fr";
        case "function" -> "fu";
        case "guideline_id" -> "gX";
        case "health_care_facility" -> "hc";
        case "hyperlink" -> "hy";
        case "id" -> "X";
        case "identifiers" -> "Xs";
        case "instruction_id" -> "iX";
        case "integrity_check" -> "ic";
        case "integrity_check_algorithm" -> "ica";
        case "interval" -> "in";
        case "ism_transition" -> "it";
        case "issuer" -> "is";
        case "is_modifiable" -> "im";
        case "is_pending" -> "ip";
        case "is_queryable" -> "iq";
        case "is_terminal" -> "il";
        case "language" -> "la";
        case "lifecycle_state" -> "ls";
        case "links" -> "lk";
        case "location" -> "lc";
        case "lower" -> "l";
        case "lower_included" -> "li";
        case "lower_unbounded" -> "lu";
        case "magnitude" -> "m";
        case "magnitude_status" -> "ms";
        case "mappings" -> "mp";
        case "match" -> "ma";
        case "math_function" -> "mf";
        case "meaning" -> "me";
        case "media_type" -> "mt";
        case "mode" -> "mo";
        case "name" -> "N";
        case "namespace" -> "ns";
        case "narrative" -> "nv";
        case "normal_range" -> "nr";
        case "normal_status" -> "nt";
        case "null_flavour" -> "nf";
        case "null_reason" -> "nl";
        case "numerator" -> "nu";
        case "origin" -> "og";
        case "original_content" -> "oc";
        case "originating_system_audit" -> "oa";
        case "originating_system_item_ids" -> "os";
        case "other_details" -> "od";
        case "other_input_version_uids" -> "oX";
        case "other_participations" -> "op";
        case "other_reference_ranges" -> "or";
        case "participations" -> "pp";
        case "path" -> "pa";
        case "performer" -> "pf";
        case "period" -> "pe";
        case "preceding_version_uid" -> "pX";
        case "precision" -> "pc";
        case "preferred_term" -> "pt";
        case "proof" -> "prf";
        case "property" -> "pr";
        case "provider" -> "pv";
        case "purpose" -> "pu";
        case "qualified_rm_entity" -> "qr";
        case "range" -> "ra";
        case "reason" -> "re";
        case "relationship" -> "rs";
        case "rm_entity" -> "rm";
        case "rm_name" -> "rn";
        case "rm_originator" -> "ro";
        case "rm_version" -> "rv";
        case "sample_count" -> "sn";
        case "scheme" -> "sc";
        case "setting" -> "se";
        case "signature" -> "sig";
        case "size" -> "si";
        case "specialisation" -> "sp";
        case "start_time" -> "st";
        case "subject" -> "su";
        case "symbol" -> "sy";
        case "system_id" -> "sX";
        case "target" -> "ta";
        case "template_id" -> "tm";
        case "terminology_id" -> "te";
        case "territory" -> "ty";
        case "thumbnail" -> "th";
        case "time" -> "ti";
        case "time_asserted" -> "ts";
        case "time_committed" -> "tc";
        case "timing" -> "tg";
        case "transition" -> "tr";
        case "type" -> "tp";
        case "uid" -> "U";
        case "units" -> "un";
        case "units_display_name" -> "ud";
        case "units_system" -> "us";
        case "upper" -> "u";
        case "upper_included" -> "ui";
        case "upper_unbounded" -> "uu";
        case "uri" -> "ur";
        case "value" -> "V";
        case "version_id" -> "vX";
        case "wf_definition" -> "wd";
        case "width" -> "wi";
        case "workflow_id" -> "wX";
        case "_index" -> "I";
        case "_magnitude" -> "M";
        case TYPE_ATTRIBUTE -> "T";
        default -> throw new IllegalArgumentException("Missing alias for attribute " + attribute);
        };
    }

    public static String[] rmToJsonPathParts(String rmPath) {
        return Arrays.stream(rmPath.split("/")).map(RmAttributeAlias::getAlias).toArray(String[]::new);
    }

    public static String getAttribute(String alias) {
        return switch (alias) {
            case "a" -> "activities";
            case "c" -> "content";
            case "x" -> "context";
            case "d" -> "data";
            case "t" -> "description";
            case "e" -> "events";
            case "n" -> "instruction_details";
            case "j" -> "item";
            case "i" -> "items";
            case "o" -> "other_context";
            case "p" -> "protocol";
            case "r" -> "rows";
            case "s" -> "state";
            case "y" -> "summary";
            case "w" -> "wf_details";
            case "f" -> "feeder_audit";
            case "ay" -> "accuracy";
            case "ayp" -> "accuracy_is_percent";
            case "aa" -> "action_archetype_id";
            case "ac" -> "activity_id";
            case "at" -> "alternate_text";
            case "ad" -> "archetype_details";
            case "aX" -> "archetype_id";
            case "A" -> "archetype_node_id";
            case "as" -> "assigner";
            case "att" -> "attestations";
            case "atv" -> "attested_view";
            case "cf" -> "careflow_step";
            case "ca" -> "category";
            case "ct" -> "change_type";
            case "ch" -> "charset";
            case "cd" -> "code_string";
            case "co" -> "committer";
            case "cau" -> "commit_audit";
            case "cp" -> "composer";
            case "calg" -> "compression_algorithm";
            case "con" -> "contribution";
            case "cu" -> "current_state";
            case "df" -> "defining_code";
            case "de" -> "denominator";
            case "dt" -> "details";
            case "dc" -> "domain_concept";
            case "du" -> "duration";
            case "ec" -> "encoding";
            case "et" -> "end_time";
            case "ex" -> "expiry_time";
            case "er" -> "external_ref";
            case "fs" -> "feeder_system_audit";
            case "fX" -> "feeder_system_item_ids";
            case "fo" -> "folders";
            case "fm" -> "formalism";
            case "fr" -> "formatting";
            case "fu" -> "function";
            case "gX" -> "guideline_id";
            case "hc" -> "health_care_facility";
            case "hy" -> "hyperlink";
            case "X" -> "id";
            case "Xs" -> "identifiers";
            case "iX" -> "instruction_id";
            case "ic" -> "integrity_check";
            case "ica" -> "integrity_check_algorithm";
            case "in" -> "interval";
            case "it" -> "ism_transition";
            case "is" -> "issuer";
            case "im" -> "is_modifiable";
            case "ip" -> "is_pending";
            case "iq" -> "is_queryable";
            case "il" -> "is_terminal";
            case "la" -> "language";
            case "ls" -> "lifecycle_state";
            case "lk" -> "links";
            case "lc" -> "location";
            case "l" -> "lower";
            case "li" -> "lower_included";
            case "lu" -> "lower_unbounded";
            case "m" -> "magnitude";
            case "ms" -> "magnitude_status";
            case "mp" -> "mappings";
            case "ma" -> "match";
            case "mf" -> "math_function";
            case "me" -> "meaning";
            case "mt" -> "media_type";
            case "mo" -> "mode";
            case "N" -> "name";
            case "ns" -> "namespace";
            case "nv" -> "narrative";
            case "nr" -> "normal_range";
            case "nt" -> "normal_status";
            case "nf" -> "null_flavour";
            case "nl" -> "null_reason";
            case "nu" -> "numerator";
            case "og" -> "origin";
            case "oc" -> "original_content";
            case "oa" -> "originating_system_audit";
            case "os" -> "originating_system_item_ids";
            case "od" -> "other_details";
            case "oX" -> "other_input_version_uids";
            case "op" -> "other_participations";
            case "or" -> "other_reference_ranges";
            case "pp" -> "participations";
            case "pa" -> "path";
            case "pf" -> "performer";
            case "pe" -> "period";
            case "pX" -> "preceding_version_uid";
            case "pc" -> "precision";
            case "pt" -> "preferred_term";
            case "prf" -> "proof";
            case "pr" -> "property";
            case "pv" -> "provider";
            case "pu" -> "purpose";
            case "qr" -> "qualified_rm_entity";
            case "ra" -> "range";
            case "re" -> "reason";
            case "rs" -> "relationship";
            case "rm" -> "rm_entity";
            case "rn" -> "rm_name";
            case "ro" -> "rm_originator";
            case "rv" -> "rm_version";
            case "sn" -> "sample_count";
            case "sc" -> "scheme";
            case "se" -> "setting";
            case "sig" -> "signature";
            case "si" -> "size";
            case "sp" -> "specialisation";
            case "st" -> "start_time";
            case "su" -> "subject";
            case "sy" -> "symbol";
            case "sX" -> "system_id";
            case "ta" -> "target";
            case "tm" -> "template_id";
            case "te" -> "terminology_id";
            case "ty" -> "territory";
            case "th" -> "thumbnail";
            case "ti" -> "time";
            case "ts" -> "time_asserted";
            case "tc" -> "time_committed";
            case "tg" -> "timing";
            case "tr" -> "transition";
            case "tp" -> "type";
            case "U" -> "uid";
            case "un" -> "units";
            case "ud" -> "units_display_name";
            case "us" -> "units_system";
            case "u" -> "upper";
            case "ui" -> "upper_included";
            case "uu" -> "upper_unbounded";
            case "ur" -> "uri";
            case "V" -> "value";
            case "vX" -> "version_id";
            case "wd" -> "wf_definition";
            case "wi" -> "width";
            case "wX" -> "workflow_id";
            case "I" -> "_index";
            case "M" -> "_magnitude";
            case "T" -> TYPE_ATTRIBUTE;
            default -> throw new IllegalArgumentException("Missing attribute for alias " + alias);
        };
    }

    /**
     * For aliases consisting of only one character
     * @param ch
     * @return
     */
    public static String aliasByAliasChar(char ch) {
        return switch (ch) {
            case 'a' -> "a";
            case 'c' -> "c";
            case 'x' -> "x";
            case 'd' -> "d";
            case 't' -> "t";
            case 'e' -> "e";
            case 'n' -> "n";
            case 'j' -> "j";
            case 'i' -> "i";
            case 'o' -> "o";
            case 'p' -> "p";
            case 'r' -> "r";
            case 's' -> "s";
            case 'y' -> "y";
            case 'w' -> "w";
            case 'f' -> "f";
            case 'A' -> "A";
            case 'X' -> "X";
            case 'l' -> "l";
            case 'm' -> "m";
            case 'N' -> "N";
            case 'U' -> "U";
            case 'u' -> "u";
            case 'V' -> "V";
            case 'I' -> "I";
            case 'M' -> "M";
            case 'T' -> "T";
                default -> throw new IllegalArgumentException("Unknown alias %s".formatted(ch));
            };
    }
}
