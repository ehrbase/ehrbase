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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * For the database: Shorter aliases for RmObject types
 */
public record RmType(String type, String alias, boolean structureAlias) {

    static final List<RmType> values = List.of(
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

    private static Map<String, String> type2alias;

    private static Map<String, String> alias2type;

    static {
        type2alias = values.stream().collect(Collectors.toMap(RmType::type, RmType::alias));
        alias2type = values.stream().collect(Collectors.toMap(RmType::alias, RmType::type));
    }

    private static RmType structureAlias(StructureRmType sType) {
        return new RmType(sType.name(), sType.getAlias(), true);
    }

    private static RmType alias(String type, String alias) {
        return new RmType(type, alias, false);
    }

    public static String getAlias(String type) {
        String alias = type2alias.get(type);
        if (alias == null) {
            throw new IllegalArgumentException("Missing alias for type " + type);
        }
        return alias;
    }

    public static Optional<String> optionalAlias(String type) {
        return Optional.ofNullable(type).map(type2alias::get);
    }

    public static String getRmType(String alias) {
        String type = alias2type.get(alias);
        if (type == null) {
            throw new IllegalArgumentException("Missing type for alias " + alias);
        }
        return type;
    }
}
