/*
 * Copyright (c) 2025 vitasystems GmbH.
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
package org.ehrbase.openehr.aqlengine.asl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.jooq.pg.Tables;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslDescendantCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslPathChildCondition;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslAggregatingField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslComplexExtractedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslConstantField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslFolderItemIdVirtualField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslOrderByField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslRmPathField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslSubqueryField;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslAuditDetailsJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslDelegatingJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslFolderItemJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoin;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslEncapsulatingQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslFilteringQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslPathDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRmObjectDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.jooq.TableField;
import org.springframework.stereotype.Component;

@Component
public class AslCleanupPostProcessor implements AslPostProcessor {

    @Override
    public void afterBuildAsl(
            final AslRootQuery aslRootQuery,
            final AqlQuery aqlQuery,
            final AqlQueryWrapper aqlQueryWrapper,
            final AqlQueryRequest aqlQueryRequest) {

        // TODO: Remove unnecessary path subquery nesting

        // Clean up select fields
        HashMap<AslQuery, Set<String>> usedFields = new HashMap<>();
        findUsedFields(aslRootQuery, usedFields);
        usedFields.forEach(AslCleanupPostProcessor::cleanupSelect);
    }

    private static void findUsedFields(AslQuery q, Map<AslQuery, Set<String>> usedFields) {
        switch (q) {
            case AslPathDataQuery pdq -> usedFields.compute(pdq.getBase(), (k, v) -> {
                Set<String> names = v != null ? v : new HashSet<>();
                names.add(AslStructureColumn.DATA.getFieldName());
                return names;
            });
            case AslRootQuery rq -> {
                Stream.of(
                                rq.getSelect().stream(),
                                rq.getChildren().stream()
                                        .map(Pair::getRight)
                                        .filter(Objects::nonNull)
                                        .map(AslJoin::getOn)
                                        .flatMap(List::stream)
                                        .flatMap(AslCleanupPostProcessor::streamJoinConditionFields),
                                AslUtils.streamConditionFields(rq.getCondition()),
                                rq.getStructureConditions().stream().flatMap(AslUtils::streamConditionFields),
                                rq.getOrderByFields().stream().map(AslOrderByField::field),
                                rq.getGroupByFields().stream(),
                                rq.getGroupByDvOrderedMagnitudeFields().stream())
                        .flatMap(s -> s)
                        .filter(Objects::nonNull)
                        .forEach(f -> usedFields.compute(f.getOwner(), (k, v) -> {
                            Set<String> names = v != null ? v : new HashSet<>();
                            getFieldNames(f).forEach(names::add);
                            return names;
                        }));
                rq.getChildren().stream().map(Pair::getLeft).forEach(cq -> findUsedFields(cq, usedFields));
            }
            case AslFilteringQuery fq -> usedFields.compute(fq.getSourceField().getOwner(), (k, v) -> {
                Set<String> names = v != null ? v : new HashSet<>();
                getFieldNames(fq.getSourceField()).forEach(names::add);
                return names;
            });
            case AslEncapsulatingQuery eq -> {
                Stream.of(
                                eq.getChildren().stream()
                                        .map(Pair::getRight)
                                        .filter(Objects::nonNull)
                                        .map(AslJoin::getOn)
                                        .flatMap(List::stream)
                                        .flatMap(AslCleanupPostProcessor::streamJoinConditionFields),
                                AslUtils.streamConditionFields(eq.getCondition()),
                                eq.getStructureConditions().stream().flatMap(AslUtils::streamConditionFields))
                        .flatMap(s -> s)
                        .filter(Objects::nonNull)
                        .forEach(f -> usedFields.compute(f.getOwner(), (k, v) -> {
                            Set<String> names = v != null ? v : new HashSet<>();
                            getFieldNames(f).forEach(names::add);
                            return names;
                        }));
                eq.getChildren().stream().map(Pair::getLeft).forEach(cq -> findUsedFields(cq, usedFields));
            }
            case AslStructureQuery sq -> usedFields.computeIfAbsent(sq, k -> new HashSet<>());
            case AslRmObjectDataQuery rodq -> throw new IllegalArgumentException("unexpected AslRmObjectDataQuery");
        }
    }

    private static Stream<AslField> streamJoinConditionFields(AslJoinCondition jc) {
        return switch (jc) {
            case AslPathFilterJoinCondition pfjc -> AslUtils.streamConditionFields(pfjc.getCondition());
            case AslAuditDetailsJoinCondition adjc -> Stream.of(
                    AslStructureColumn.AUDIT_ID.field().withOwner(adjc.getLeftOwner()),
                    new AslColumnField(UUID.class, Tables.AUDIT_DETAILS.ID.getName(), false)
                            .withOwner(adjc.getRightOwner()));
            case AslDelegatingJoinCondition adjc -> switch (adjc.getDelegate()) {
                case AslDescendantCondition djc -> Stream.of(
                        new AslColumnField(UUID.class, Tables.EHR_.ID.getName(), false).withOwner(djc.getLeftOwner()),
                        AslStructureColumn.EHR_ID.field().withOwner(djc.getLeftOwner()),
                        AslStructureColumn.EHR_ID.field().withOwner(djc.getRightOwner()),
                        AslStructureColumn.VO_ID.field().withOwner(djc.getLeftOwner()),
                        AslStructureColumn.VO_ID.field().withOwner(djc.getRightOwner()),
                        AslStructureColumn.NUM.field().withOwner(djc.getLeftOwner()),
                        AslStructureColumn.NUM_CAP.field().withOwner(djc.getLeftOwner()),
                        AslStructureColumn.NUM.field().withOwner(djc.getRightOwner()),
                        AslStructureColumn.EHR_FOLDER_IDX.field().withOwner(djc.getLeftOwner()),
                        AslStructureColumn.EHR_FOLDER_IDX.field().withOwner(djc.getRightOwner()));
                case AslFieldJoinCondition fjc -> Stream.of(fjc.getLeftField(), fjc.getRightField());
                case AslPathChildCondition pcjc -> Stream.of(
                        AslStructureColumn.EHR_ID.field().withOwner(pcjc.getLeftOwner()),
                        AslStructureColumn.EHR_ID.field().withOwner(pcjc.getRightOwner()),
                        AslStructureColumn.VO_ID.field().withOwner(pcjc.getLeftOwner()),
                        AslStructureColumn.VO_ID.field().withOwner(pcjc.getRightOwner()),
                        AslStructureColumn.NUM.field().withOwner(pcjc.getLeftOwner()),
                        AslStructureColumn.PARENT_NUM.field().withOwner(pcjc.getRightOwner()),
                        AslStructureColumn.EHR_FOLDER_IDX.field().withOwner(pcjc.getLeftOwner()),
                        AslStructureColumn.EHR_FOLDER_IDX.field().withOwner(pcjc.getRightOwner()));
            };
            case AslFolderItemJoinCondition fijc -> Stream.of(
                    AslStructureColumn.VO_ID.field().withOwner(fijc.getRightOwner()),
                    new AslFolderItemIdVirtualField().withOwner(fijc.getLeftOwner()));
        };
    }

    private static Stream<String> getFieldNames(final AslField field) {
        return switch (field) {
            case AslColumnField cf -> Stream.of(cf.getColumnName());
            case AslRmPathField pf -> Stream.of(pf.getSrcField().getColumnName());
            case AslConstantField __ -> Stream.empty();
            case AslAggregatingField af -> getFieldNames(af.getBaseField());
            case AslComplexExtractedColumnField ecf -> ecf.getExtractedColumn().getColumns().stream();
            case AslSubqueryField sqf -> Stream.concat(
                    AslUtils.getTargetType(sqf.getBaseQuery()).getPkeyFields().stream()
                            .map(TableField::getName),
                    Stream.of(AslStructureColumn.NUM.getFieldName(), AslStructureColumn.NUM_CAP.getFieldName()));
            case AslFolderItemIdVirtualField fidf -> Stream.of(fidf.getFieldName());
        };
    }

    private static void cleanupSelect(AslQuery q, Set<String> usedFieldNames) {
        if (q instanceof AslStructureQuery sq) {
            sq.getSelect()
                    .removeIf(f -> f instanceof AslColumnField cf && !usedFieldNames.contains(cf.getColumnName()));
        }
    }

    @Override
    public int getOrder() {
        return CLEAN_UP_PRECEDENCE;
    }
}
