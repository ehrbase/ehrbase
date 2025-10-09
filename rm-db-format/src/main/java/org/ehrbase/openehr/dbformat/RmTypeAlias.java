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

import java.util.List;
import java.util.Optional;

/**
 * For the database: Shorter aliases for RmObject types
 */
public record RmTypeAlias(String type, String alias, boolean structureAlias) {

    static final List<RmTypeAlias> values = List.of(
            structureAlias(StructureRmType.COMPOSITION),
            structureAlias(StructureRmType.FOLDER),
            structureAlias(StructureRmType.EHR_STATUS),
            structureAlias(StructureRmType.FEEDER_AUDIT),
            structureAlias(StructureRmType.FEEDER_AUDIT_DETAILS),
            structureAlias(StructureRmType.EVENT_CONTEXT),
            structureAlias(StructureRmType.SECTION),
            structureAlias(StructureRmType.GENERIC_ENTRY),
            structureAlias(StructureRmType.ADMIN_ENTRY),
            structureAlias(StructureRmType.OBSERVATION),
            structureAlias(StructureRmType.INSTRUCTION),
            structureAlias(StructureRmType.ACTION),
            structureAlias(StructureRmType.EVALUATION),
            structureAlias(StructureRmType.INSTRUCTION_DETAILS),
            structureAlias(StructureRmType.ACTIVITY),
            structureAlias(StructureRmType.HISTORY),
            structureAlias(StructureRmType.POINT_EVENT),
            structureAlias(StructureRmType.INTERVAL_EVENT),
            structureAlias(StructureRmType.ITEM_LIST),
            structureAlias(StructureRmType.ITEM_SINGLE),
            structureAlias(StructureRmType.ITEM_TABLE),
            structureAlias(StructureRmType.ITEM_TREE),
            structureAlias(StructureRmType.CLUSTER),
            structureAlias(StructureRmType.ELEMENT),
            alias("ARCHETYPED", "AR"),
            alias("ARCHETYPE_ID", "AX"),
            alias("ATTESTATION", "AT"),
            alias("AUDIT_DETAILS", "AD"),
            alias("CODE_PHRASE", "C"),
            alias("DV_BOOLEAN", "b"),
            alias("DV_CODED_TEXT", "c"),
            alias("DV_COUNT", "co"),
            alias("DV_DATE", "d"),
            alias("DV_DATE_TIME", "dt"),
            alias("DV_DURATION", "du"),
            alias("DV_EHR_URI", "eu"),
            alias("DV_IDENTIFIER", "id"),
            alias("DV_INTERVAL", "iv"),
            alias("DV_MULTIMEDIA", "mu"),
            alias("DV_ORDINAL", "o"),
            alias("DV_PARAGRAPH", "p"),
            alias("DV_PARSABLE", "pa"),
            alias("DV_PROPORTION", "pr"),
            alias("DV_QUANTITY", "q"),
            alias("DV_SCALE", "sc"),
            alias("DV_STATE", "st"),
            alias("DV_TEXT", "x"),
            alias("DV_TIME", "t"),
            alias("DV_URI", "u"),
            alias("GENERIC_ID", "GX"),
            alias("HIER_OBJECT_ID", "HX"),
            alias("INTERNET_ID", "IX"),
            alias("INTERVAL", "IV"),
            alias("ISM_TRANSITION", "IT"),
            alias("LINK", "LK"),
            alias("LOCATABLE_REF", "LR"),
            alias("OBJECT_REF", "OR"),
            alias("OBJECT_VERSION_ID", "OV"),
            alias("PARTICIPATION", "PA"),
            alias("PARTY_IDENTIFIED", "PI"),
            alias("PARTY_REF", "PF"),
            alias("PARTY_RELATED", "PR"),
            alias("PARTY_SELF", "PS"),
            alias("REFERENCE_RANGE", "RR"),
            alias("TEMPLATE_ID", "TP"),
            alias("TERMINOLOGY_ID", "T"),
            alias("TERM_MAPPING", "TM"),
            alias("UUID", "U"));

    private static RmTypeAlias structureAlias(StructureRmType sType) {
        return new RmTypeAlias(sType.name(), sType.getAlias(), true);
    }

    private static RmTypeAlias alias(String type, String alias) {
        return new RmTypeAlias(type, alias, false);
    }

    public static String getNullableAlias(String type) {
        return switch (type) {
            case "COMPOSITION" -> "CO";
            case "FOLDER" -> "F";
            case "EHR_STATUS" -> "ES";
            case "EVENT_CONTEXT" -> "EC";
            case "SECTION" -> "SE";
            case "GENERIC_ENTRY" -> "GE";
            case "ADMIN_ENTRY" -> "AE";
            case "OBSERVATION" -> "OB";
            case "INSTRUCTION" -> "IN";
            case "ACTION" -> "AN";
            case "EVALUATION" -> "EV";
            case "INSTRUCTION_DETAILS" -> "ID";
            case "ACTIVITY" -> "AY";
            case "HISTORY" -> "HI";
            case "POINT_EVENT" -> "PE";
            case "INTERVAL_EVENT" -> "IE";
            case "FEEDER_AUDIT" -> "FA";
            case "FEEDER_AUDIT_DETAILS" -> "FD";
            case "ITEM_LIST" -> "IL";
            case "ITEM_SINGLE" -> "IS";
            case "ITEM_TABLE" -> "TA";
            case "ITEM_TREE" -> "TR";
            case "CLUSTER" -> "CL";
            case "ELEMENT" -> "E";
            case "ARCHETYPED" -> "AR";
            case "ARCHETYPE_ID" -> "AX";
            case "ATTESTATION" -> "AT";
            case "AUDIT_DETAILS" -> "AD";
            case "CODE_PHRASE" -> "C";
            case "DV_BOOLEAN" -> "b";
            case "DV_CODED_TEXT" -> "c";
            case "DV_COUNT" -> "co";
            case "DV_DATE" -> "d";
            case "DV_DATE_TIME" -> "dt";
            case "DV_DURATION" -> "du";
            case "DV_EHR_URI" -> "eu";
            case "DV_IDENTIFIER" -> "id";
            case "DV_INTERVAL" -> "iv";
            case "DV_MULTIMEDIA" -> "mu";
            case "DV_ORDINAL" -> "o";
            case "DV_PARAGRAPH" -> "p";
            case "DV_PARSABLE" -> "pa";
            case "DV_PROPORTION" -> "pr";
            case "DV_QUANTITY" -> "q";
            case "DV_SCALE" -> "sc";
            case "DV_STATE" -> "st";
            case "DV_TEXT" -> "x";
            case "DV_TIME" -> "t";
            case "DV_URI" -> "u";
            case "GENERIC_ID" -> "GX";
            case "HIER_OBJECT_ID" -> "HX";
            case "INTERNET_ID" -> "IX";
            case "INTERVAL" -> "IV";
            case "ISM_TRANSITION" -> "IT";
            case "LINK" -> "LK";
            case "LOCATABLE_REF" -> "LR";
            case "OBJECT_REF" -> "OR";
            case "OBJECT_VERSION_ID" -> "OV";
            case "PARTICIPATION" -> "PA";
            case "PARTY_IDENTIFIED" -> "PI";
            case "PARTY_REF" -> "PF";
            case "PARTY_RELATED" -> "PR";
            case "PARTY_SELF" -> "PS";
            case "REFERENCE_RANGE" -> "RR";
            case "TEMPLATE_ID" -> "TP";
            case "TERMINOLOGY_ID" -> "T";
            case "TERM_MAPPING" -> "TM";
            case "UUID" -> "U";
            case null, default -> null;
        };
    }

    public static String getAlias(String type) {
        String alias = getNullableAlias(type);
        if (alias == null) {
            throw new IllegalArgumentException("Missing alias for type " + type);
        }
        return alias;
    }

    public static Optional<String> optionalAlias(String type) {
        return Optional.ofNullable(getNullableAlias(type));
    }

    public static String getRmType(String alias) {
        return switch (alias) {
            case "CO" -> "COMPOSITION";
            case "F" -> "FOLDER";
            case "ES" -> "EHR_STATUS";
            case "EC" -> "EVENT_CONTEXT";
            case "SE" -> "SECTION";
            case "GE" -> "GENERIC_ENTRY";
            case "AE" -> "ADMIN_ENTRY";
            case "OB" -> "OBSERVATION";
            case "IN" -> "INSTRUCTION";
            case "AN" -> "ACTION";
            case "EV" -> "EVALUATION";
            case "ID" -> "INSTRUCTION_DETAILS";
            case "AY" -> "ACTIVITY";
            case "HI" -> "HISTORY";
            case "PE" -> "POINT_EVENT";
            case "IE" -> "INTERVAL_EVENT";
            case "FA" -> "FEEDER_AUDIT";
            case "FD" -> "FEEDER_AUDIT_DETAILS";
            case "IL" -> "ITEM_LIST";
            case "IS" -> "ITEM_SINGLE";
            case "TA" -> "ITEM_TABLE";
            case "TR" -> "ITEM_TREE";
            case "CL" -> "CLUSTER";
            case "E" -> "ELEMENT";
            case "AR" -> "ARCHETYPED";
            case "AX" -> "ARCHETYPE_ID";
            case "AT" -> "ATTESTATION";
            case "AD" -> "AUDIT_DETAILS";
            case "C" -> "CODE_PHRASE";
            case "b" -> "DV_BOOLEAN";
            case "c" -> "DV_CODED_TEXT";
            case "co" -> "DV_COUNT";
            case "d" -> "DV_DATE";
            case "dt" -> "DV_DATE_TIME";
            case "du" -> "DV_DURATION";
            case "eu" -> "DV_EHR_URI";
            case "id" -> "DV_IDENTIFIER";
            case "iv" -> "DV_INTERVAL";
            case "mu" -> "DV_MULTIMEDIA";
            case "o" -> "DV_ORDINAL";
            case "p" -> "DV_PARAGRAPH";
            case "pa" -> "DV_PARSABLE";
            case "pr" -> "DV_PROPORTION";
            case "q" -> "DV_QUANTITY";
            case "sc" -> "DV_SCALE";
            case "st" -> "DV_STATE";
            case "x" -> "DV_TEXT";
            case "t" -> "DV_TIME";
            case "u" -> "DV_URI";
            case "GX" -> "GENERIC_ID";
            case "HX" -> "HIER_OBJECT_ID";
            case "IX" -> "INTERNET_ID";
            case "IV" -> "INTERVAL";
            case "IT" -> "ISM_TRANSITION";
            case "LK" -> "LINK";
            case "LR" -> "LOCATABLE_REF";
            case "OR" -> "OBJECT_REF";
            case "OV" -> "OBJECT_VERSION_ID";
            case "PA" -> "PARTICIPATION";
            case "PI" -> "PARTY_IDENTIFIED";
            case "PF" -> "PARTY_REF";
            case "PR" -> "PARTY_RELATED";
            case "PS" -> "PARTY_SELF";
            case "RR" -> "REFERENCE_RANGE";
            case "TP" -> "TEMPLATE_ID";
            case "T" -> "TERMINOLOGY_ID";
            case "TM" -> "TERM_MAPPING";
            case "U" -> "UUID";
            default -> throw new IllegalArgumentException("Missing type for alias " + alias);
        };
    }
}
