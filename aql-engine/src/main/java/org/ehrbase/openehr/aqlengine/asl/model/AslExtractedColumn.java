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
package org.ehrbase.openehr.aqlengine.asl.model;

import static org.ehrbase.jooq.pg.Tables.AUDIT_DETAILS;
import static org.ehrbase.jooq.pg.Tables.COMP_VERSION;
import static org.ehrbase.jooq.pg.Tables.EHR_;
import static org.ehrbase.jooq.pg.tables.CompData.COMP_DATA;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.ehrbase.jooq.pg.tables.Ehr;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsWrapper;
import org.ehrbase.openehr.dbformat.AncestorStructureRmType;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPathUtil;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.jooq.Field;

public enum AslExtractedColumn {
    NAME_VALUE(
            AqlObjectPathUtil.NAME_VALUE,
            COMP_DATA.ENTITY_NAME,
            String.class,
            false,
            Stream.concat(Arrays.stream(StructureRmType.values()), Arrays.stream(AncestorStructureRmType.values()))
                    .map(Enum::name)
                    .toArray(String[]::new)),
    VO_ID(
            AqlObjectPath.parse("uid/value"),
            List.of(COMP_DATA.VO_ID, COMP_VERSION.SYS_VERSION),
            String.class,
            true,
            StructureRmType.COMPOSITION.name(),
            StructureRmType.EHR_STATUS.name(),
            RmConstants.ORIGINAL_VERSION),
    ROOT_CONCEPT(
            // same path as ARCHETYPE_NODE_ID (alternative for Compositions)
            AqlObjectPathUtil.ARCHETYPE_NODE_ID,
            COMP_VERSION.ROOT_CONCEPT,
            String.class,
            true,
            StructureRmType.COMPOSITION.name()),
    ARCHETYPE_NODE_ID(
            AqlObjectPathUtil.ARCHETYPE_NODE_ID,
            List.of(COMP_DATA.RM_ENTITY, COMP_DATA.ENTITY_CONCEPT),
            String.class,
            false,
            Stream.concat(Arrays.stream(StructureRmType.values()), Arrays.stream(AncestorStructureRmType.values()))
                    // for Compositions ROOT_CONCEPT is used
                    .filter(v -> !v.equals(StructureRmType.COMPOSITION))
                    .map(Enum::name)
                    .toArray(String[]::new)),
    TEMPLATE_ID(
            AqlObjectPath.parse("archetype_details/template_id/value"),
            COMP_VERSION.TEMPLATE_ID,
            String.class,
            true,
            StructureRmType.COMPOSITION.name()),

    // EHR
    EHR_ID(AqlObjectPath.parse("ehr_id/value"), Ehr.EHR_.ID, UUID.class, false, RmConstants.EHR),
    EHR_SYSTEM_ID(
            AqlObjectPath.parse("system_id/value"), Collections.emptyList(), String.class, false, RmConstants.EHR),
    EHR_SYSTEM_ID_DV(AqlObjectPath.parse("system_id"), Collections.emptyList(), String.class, false, RmConstants.EHR),
    EHR_TIME_CREATED_DV(AqlObjectPath.parse("time_created"), EHR_.CREATION_DATE, String.class, false, RmConstants.EHR),
    EHR_TIME_CREATED(
            AqlObjectPath.parse("time_created/value"), EHR_.CREATION_DATE, String.class, false, RmConstants.EHR),

    // FOLDER
    FOLDER_ITEM_ID(
            AqlObjectPath.parse("items/id/value"), Collections.emptyList(), UUID.class, false, RmConstants.FOLDER),

    // ORIGINAL_VERSION
    OV_CONTRIBUTION_ID(
            AqlObjectPath.parse("contribution/id/value"),
            COMP_VERSION.CONTRIBUTION_ID,
            String.class,
            true,
            RmConstants.ORIGINAL_VERSION),
    OV_TIME_COMMITTED_DV(
            AqlObjectPath.parse("commit_audit/time_committed"),
            COMP_VERSION.SYS_PERIOD_LOWER,
            String.class,
            true,
            RmConstants.ORIGINAL_VERSION),
    OV_TIME_COMMITTED(
            AqlObjectPath.parse("commit_audit/time_committed/value"),
            COMP_VERSION.SYS_PERIOD_LOWER,
            String.class,
            true,
            RmConstants.ORIGINAL_VERSION),

    // AUDIT_DETAILS
    AD_SYSTEM_ID(
            AqlObjectPath.parse("system_id"), Collections.emptyList(), String.class, true, RmConstants.AUDIT_DETAILS),
    AD_DESCRIPTION_DV(
            AqlObjectPath.parse("description"),
            AUDIT_DETAILS.DESCRIPTION,
            String.class,
            true,
            RmConstants.AUDIT_DETAILS),
    AD_DESCRIPTION_VALUE(
            AqlObjectPath.parse("description/value"),
            AUDIT_DETAILS.DESCRIPTION,
            String.class,
            true,
            RmConstants.AUDIT_DETAILS),
    AD_CHANGE_TYPE_DV(
            AqlObjectPath.parse("change_type"),
            AUDIT_DETAILS.CHANGE_TYPE,
            String.class,
            true,
            RmConstants.AUDIT_DETAILS),
    AD_CHANGE_TYPE_VALUE(
            AqlObjectPath.parse("change_type/value"),
            AUDIT_DETAILS.CHANGE_TYPE,
            String.class,
            true,
            RmConstants.AUDIT_DETAILS),
    AD_CHANGE_TYPE_CODE_STRING(
            AqlObjectPath.parse("change_type/defining_code/code_string"),
            AUDIT_DETAILS.CHANGE_TYPE,
            String.class,
            true,
            RmConstants.AUDIT_DETAILS),
    AD_CHANGE_TYPE_PREFERRED_TERM(
            AqlObjectPath.parse("change_type/defining_code/preferred_term"),
            AUDIT_DETAILS.CHANGE_TYPE,
            String.class,
            true,
            RmConstants.AUDIT_DETAILS),
    AD_CHANGE_TYPE_TERMINOLOGY_ID_VALUE(
            AqlObjectPath.parse("change_type/defining_code/terminology_id/value"),
            Collections.emptyList(),
            String.class,
            true,
            RmConstants.AUDIT_DETAILS);

    private final AqlObjectPath path;
    private final List<String> columns;
    private final Class<?> columnType;
    private final Set<String> allowedRmTypes;
    private final boolean requiresVersionTable;

    <T> AslExtractedColumn(
            AqlObjectPath path,
            Field<?> column,
            Class<T> columnType,
            boolean requiresVersionTable,
            String... allowedRmTypes) {
        this(path, List.of(column), columnType, requiresVersionTable, allowedRmTypes);
    }

    <T> AslExtractedColumn(
            AqlObjectPath path,
            List<Field> columns,
            Class<T> columnType,
            boolean requiresVersionTable,
            String... allowedRmTypes) {
        this.path = Objects.requireNonNull(path).frozen();
        this.columnType = Objects.requireNonNull(columnType);
        this.columns = Optional.ofNullable(columns)
                .map(l -> l.stream().map(Field::getName).toList())
                .orElse(null);
        this.requiresVersionTable = requiresVersionTable;
        this.allowedRmTypes = Set.of(allowedRmTypes);
    }

    public AqlObjectPath getPath() {
        return path;
    }

    public Set<String> getAllowedRmTypes() {
        return allowedRmTypes;
    }

    public boolean requiresVersionTable() {
        return requiresVersionTable;
    }

    public static Optional<AslExtractedColumn> find(ContainsWrapper contains, AqlObjectPath toMatch) {
        return find(contains.getRmType(), toMatch);
    }

    public static Optional<AslExtractedColumn> find(String containmentType, AqlObjectPath toMatch) {
        return Arrays.stream(AslExtractedColumn.values())
                .filter(ep -> ep.matches(containmentType, toMatch))
                .findFirst();
    }

    public static Optional<AslExtractedColumn> find(String containmentType, AqlObjectPath toMatch, int skip) {
        List<PathNode> pathNodes = Optional.ofNullable(toMatch).map(AqlObjectPath::getPathNodes).stream()
                .flatMap(List::stream)
                .skip(skip)
                .toList();
        return find(containmentType, new AqlObjectPath(pathNodes));
    }

    public boolean matches(String containmentType, AqlObjectPath toMatch) {
        return allowedRmTypes.contains(containmentType) && Objects.equals(toMatch, path);
    }

    public Class<?> getColumnType() {
        return columnType;
    }

    public List<String> getColumns() {
        return columns;
    }
}
