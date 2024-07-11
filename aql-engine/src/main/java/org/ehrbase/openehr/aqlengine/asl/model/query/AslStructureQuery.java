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
package org.ehrbase.openehr.aqlengine.asl.model.query;

import static org.ehrbase.jooq.pg.Tables.COMP_DATA;
import static org.ehrbase.jooq.pg.Tables.COMP_VERSION;
import static org.ehrbase.jooq.pg.Tables.EHR_;
import static org.ehrbase.jooq.pg.Tables.EHR_FOLDER_DATA;
import static org.ehrbase.jooq.pg.Tables.EHR_FOLDER_VERSION;
import static org.ehrbase.jooq.pg.Tables.EHR_STATUS_DATA;
import static org.ehrbase.jooq.pg.Tables.EHR_STATUS_VERSION;

import com.nedap.archie.rm.archetyped.Locatable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.jooq.pg.Tables;
import org.ehrbase.openehr.aqlengine.asl.AslUtils;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldValueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition.AslConditionOperator;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField.FieldSource;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.dbformat.RmAttributeAlias;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.dbformat.StructureRoot;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.jooq.Table;
import org.jooq.TableField;

/**
 *<pre>
 * select
 *       "sCOMPOSITIONsq"."vo_id" as "sCOMPOSITIONc0_vo_id",
 *       "sCOMPOSITIONsq"."ehr_id" as "sCOMPOSITIONc0_ehr_id",
 *       "sCOMPOSITIONsq"."entity_idx" as "sCOMPOSITIONc0_entity_idx",
 *       "sCOMPOSITIONsq"."entity_idx_cap" as "sCOMPOSITIONc0_entity_idx_cap",
 *       "sCOMPOSITIONsq"."entity_idx_len" as "sCOMPOSITIONc0_entity_idx_len",
 *       "sCOMPOSITIONsq"."entity_concept" as "sCOMPOSITIONc0_entity_concept",
 *       "sCOMPOSITIONsq"."entity_name" as "sCOMPOSITIONc0_entity_name",
 *       "sCOMPOSITIONsq"."rm_entity" as "sCOMPOSITIONc0_rm_entity"
 *     from "ehr"."comp" as "sCOMPOSITIONsq"
 *     where (
 *       (and other-predicates)
 *     )
 *     </pre>
 */
public final class AslStructureQuery extends AslQuery {

    public static final String ENTITY_ATTRIBUTE = "entity_attribute";

    public boolean isRequiresVersionTableJoin() {
        return requiresVersionTableJoin;
    }

    public boolean isRepresentsOriginalVersionExpression() {
        return representsOriginalVersionExpression;
    }

    public void setRepresentsOriginalVersionExpression(boolean representsOriginalVersionExpression) {
        this.representsOriginalVersionExpression = representsOriginalVersionExpression;
    }

    public enum AslSourceRelation {
        EHR(StructureRoot.EHR, null, EHR_),
        EHR_STATUS(StructureRoot.EHR_STATUS, EHR_STATUS_VERSION, EHR_STATUS_DATA),
        COMPOSITION(StructureRoot.COMPOSITION, COMP_VERSION, COMP_DATA),
        FOLDER(StructureRoot.FOLDER, EHR_FOLDER_VERSION, EHR_FOLDER_DATA),
        AUDIT_DETAILS(null, null, Tables.AUDIT_DETAILS);

        private static final Map<StructureRoot, AslSourceRelation> BY_STRUCTURE_ROOT =
                new EnumMap<>(StructureRoot.class);

        private final StructureRoot structureRoot;
        private final Table<?> versionTable;
        private final Table<?> dataTable;

        private final List<TableField<?, ?>> pkeyFields;

        AslSourceRelation(StructureRoot structureRoot, Table<?> versionTable, Table<?> dataTable) {
            this.structureRoot = structureRoot;
            this.versionTable = versionTable;
            this.dataTable = dataTable;
            this.pkeyFields = List.of(ObjectUtils.firstNonNull(versionTable, dataTable)
                    .getPrimaryKey()
                    .getFieldsArray());
        }

        public StructureRoot getStructureRoot() {
            return structureRoot;
        }

        public Table<?> getVersionTable() {
            return versionTable;
        }

        public Table<?> getDataTable() {
            return dataTable;
        }

        public List<TableField<?, ?>> getPkeyFields() {
            return pkeyFields;
        }

        static {
            for (AslSourceRelation value : values()) {
                if (value.structureRoot != null) {
                    BY_STRUCTURE_ROOT.put(value.structureRoot, value);
                }
            }
        }

        public static AslSourceRelation get(StructureRoot structureRoot) {
            return BY_STRUCTURE_ROOT.get(structureRoot);
        }
    }

    private static final Set<String> NON_LOCATABLE_STRUCTURE_RM_TYPES = Arrays.stream(StructureRmType.values())
            .filter(StructureRmType::isStructureEntry)
            .filter(s -> !Locatable.class.isAssignableFrom(s.type))
            .map(StructureRmType::getAlias)
            .collect(Collectors.toSet());

    private final Map<IdentifiedPath, AslPathFilterJoinCondition> joinConditionsForFiltering = new HashMap<>();
    private final AslSourceRelation type;
    private final Collection<String> rmTypes;
    private final List<AslField> fields = new ArrayList<>();
    private final String alias;
    private final boolean requiresVersionTableJoin;
    private boolean representsOriginalVersionExpression = false;

    public AslStructureQuery(
            String alias,
            AslSourceRelation type,
            List<AslField> fields,
            Collection<String> rmTypes,
            Collection<String> rmTypesConstraint,
            String attribute,
            boolean requiresVersionTableJoin) {
        super(alias, new ArrayList<>());
        this.type = type;
        this.rmTypes = List.copyOf(rmTypes);
        this.requiresVersionTableJoin = requiresVersionTableJoin;
        fields.forEach(this::addField);
        this.alias = alias;
        if (type != AslSourceRelation.EHR && type != AslSourceRelation.AUDIT_DETAILS) {
            if (!rmTypes.isEmpty()) {
                List<String> aliasedRmTypes = rmTypes.stream()
                        .map(StructureRmType::getAliasOrTypeName)
                        .toList();
                if (NON_LOCATABLE_STRUCTURE_RM_TYPES.containsAll(aliasedRmTypes)) {
                    this.structureConditions.add(new AslFieldValueQueryCondition(
                            AslUtils.findFieldForOwner(AslStructureColumn.ENTITY_CONCEPT, this.getSelect(), this),
                            AslConditionOperator.IS_NULL,
                            List.of()));
                }
            }
            if (!rmTypesConstraint.isEmpty()) {
                List<String> aliasedRmTypes = rmTypesConstraint.stream()
                        .map(StructureRmType::getAliasOrTypeName)
                        .toList();
                this.structureConditions.add(new AslFieldValueQueryCondition(
                        AslUtils.findFieldForOwner(AslStructureColumn.RM_ENTITY, this.getSelect(), this),
                        AslConditionOperator.IN,
                        aliasedRmTypes));
            }
            if (StringUtils.isNotBlank(attribute)) {
                this.structureConditions.add(new AslFieldValueQueryCondition(
                        new AslColumnField(String.class, ENTITY_ATTRIBUTE, FieldSource.withOwner(this), false),
                        AslConditionOperator.EQ,
                        List.of(RmAttributeAlias.getAlias(attribute))));
            }
        }
    }

    public Collection<String> getRmTypes() {
        return rmTypes;
    }

    private void addField(AslField aslField) {
        fields.add(aslField.withOwner(this));
    }

    @Override
    public Map<IdentifiedPath, List<AslPathFilterJoinCondition>> joinConditionsForFiltering() {
        return joinConditionsForFiltering.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> List.of(e.getValue())));
    }

    public void addJoinConditionForFiltering(IdentifiedPath ip, AslQueryCondition condition) {
        this.joinConditionsForFiltering.put(ip, new AslPathFilterJoinCondition(this, condition));
    }

    @Override
    public List<AslField> getSelect() {
        return fields;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    public AslSourceRelation getType() {
        return type;
    }
}
