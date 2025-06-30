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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.openehr.aqlengine.asl.model.AslStructureColumn;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslAggregatingField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslComplexExtractedColumnField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslConstantField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslFolderItemIdVirtualField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslOrderByField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslRmPathField;
import org.ehrbase.openehr.aqlengine.asl.model.field.AslSubqueryField;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslDelegatingJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslFolderItemJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoin;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.join.AslPathFilterJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslDataQuery;
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

    private static final class UsedFields {
        Map<AslQuery, Set<String>> usedFields = new HashMap<>();

        public void forEachQuery(BiConsumer<AslQuery, Set<String>> queryConsumer) {
            usedFields.forEach(queryConsumer);
        }

        public void addFieldNames(AslQuery base, AslField field) {
            usedFields.compute(base, (k, v) -> {
                Set<String> result = v != null ? v : new HashSet<>();
                streamFieldNames(field).forEach(result::add);
                return result;
            });
        }

        public void addFieldNames(AslQuery base, Collection<String> names) {
            usedFields.compute(base, (k, v) -> {
                Set<String> result = v != null ? v : new HashSet<>();
                result.addAll(names);
                return result;
            });
        }

        public void addFieldName(AslQuery base, String name) {
            usedFields.compute(base, (k, v) -> {
                Set<String> result = v != null ? v : new HashSet<>();
                result.add(name);
                return result;
            });
        }
    }

    @Override
    public void afterBuildAsl(
            final AslRootQuery aslRootQuery,
            final AqlQuery aqlQuery,
            final AqlQueryWrapper aqlQueryWrapper,
            final AqlQueryRequest aqlQueryRequest) {

        // TODO: Remove unnecessary path subquery nesting

        // Clean up select fields
        UsedFields usedFields = new UsedFields();
        findUsedFields(aslRootQuery, usedFields);
        usedFields.forEachQuery(AslCleanupPostProcessor::cleanupSelect);
    }

    private static void findUsedFields(AslQuery q, UsedFields usedFields) {
        switch (q) {
            case AslPathDataQuery pdq -> usedFields.addFieldName(pdq.getBase(), AslStructureColumn.DATA.getFieldName());
            case AslRootQuery rq -> {
                concatStreams(
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
                        //                        .filter(Objects::nonNull)
                        .forEach(f -> usedFields.addFieldNames(determineOwner(f), f));

                rq.getChildren().forEach(cq -> findUsedFields(cq.getLeft(), usedFields));
            }
            case AslEncapsulatingQuery eq -> {
                concatStreams(
                                eq.getChildren().stream()
                                        .map(Pair::getRight)
                                        .filter(Objects::nonNull)
                                        .map(AslJoin::getOn)
                                        .flatMap(List::stream)
                                        .flatMap(AslCleanupPostProcessor::streamJoinConditionFields),
                                AslUtils.streamConditionFields(eq.getCondition()),
                                eq.getStructureConditions().stream().flatMap(AslUtils::streamConditionFields))
                        //                        .filter(Objects::nonNull)
                        .forEach(f -> usedFields.addFieldNames(determineOwner(f), f));
                eq.getChildren().stream().map(Pair::getLeft).forEach(cq -> findUsedFields(cq, usedFields));
            }
            case AslFilteringQuery fq -> usedFields.addFieldNames(
                    fq.getSourceField().getOwner(), fq.getSourceField());
            case AslStructureQuery sq -> {
                usedFields.addFieldNames(
                        sq,
                        List.of(
                                // Default column to keep since an empty SELECT is translated to SELECT * by JOOQ
                                AslStructureColumn.VO_ID.getFieldName(), "id"));
                sq.joinConditionsForFiltering().values().stream()
                        .flatMap(Collection::stream)
                        .flatMap(AslCleanupPostProcessor::streamJoinConditionFields)
                        .forEach(f -> usedFields.addFieldNames(determineOwner(f), f));
            }
            case AslRmObjectDataQuery rodq -> throw new IllegalArgumentException("unexpected AslRmObjectDataQuery");
        }
    }

    @SafeVarargs
    private static <T> Stream<T> concatStreams(Stream<T>... streams) {
        return Arrays.stream(streams).flatMap(s -> s);
    }

    private static AslQuery determineOwner(AslField f) {
        return switch (f) {
            case null -> null;
            case AslSubqueryField sf -> ((AslDataQuery) sf.getBaseQuery()).getBase();
            case AslAggregatingField af -> determineOwner(af.getBaseField());
            default -> f.getOwner();
        };
    }

    /**
     * see ConditionUtils::buildJoinCondition
     * @param joinCondition
     * @return Stream of used AslFields
     */
    private static Stream<AslField> streamJoinConditionFields(AslJoinCondition joinCondition) {
        return switch (joinCondition) {
            case AslPathFilterJoinCondition pfjc -> AslUtils.streamConditionFields(pfjc.getCondition());
            case AslDelegatingJoinCondition adjc -> switch (adjc.getDelegate()) {
                case AslFieldJoinCondition fjc -> Stream.of(fjc.getLeftField(), fjc.getRightField());
            };
            case AslFolderItemJoinCondition fijc -> Stream.of(
                    AslStructureColumn.VO_ID.fieldWithOwner(fijc.getRightOwner()),
                    new AslFolderItemIdVirtualField(AslField.FieldSource.withOwner(fijc.getLeftOwner())));
        };
    }

    private static Stream<String> streamFieldNames(final AslField field) {
        return switch (field) {
            case AslColumnField cf -> Stream.of(cf.getColumnName());
            case AslRmPathField pf -> Stream.of(pf.getSrcField().getColumnName());
            case AslConstantField<?> __ -> Stream.empty();
            case AslAggregatingField af -> streamFieldNames(af.getBaseField());
            case AslComplexExtractedColumnField ecf -> ecf.getExtractedColumn().getColumns().stream();
            case AslSubqueryField sqf -> concatStreams(
                    AslUtils.getTargetType(((AslDataQuery) sqf.getBaseQuery()).getBase()).getPkeyFields().stream()
                            .map(TableField::getName),
                    sqf.getFilterConditions().stream()
                            .flatMap(AslUtils::streamConditionFields)
                            .flatMap(AslCleanupPostProcessor::streamFieldNames),
                    Stream.of(AslStructureColumn.NUM.getFieldName()),
                    Stream.of(AslStructureColumn.NUM_CAP.getFieldName()));
            case AslFolderItemIdVirtualField f -> Stream.of(f.getFieldName());
            case null -> Stream.empty();
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
