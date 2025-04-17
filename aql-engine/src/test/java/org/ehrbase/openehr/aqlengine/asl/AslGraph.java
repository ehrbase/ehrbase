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
package org.ehrbase.openehr.aqlengine.asl;

import static org.apache.commons.lang3.StringUtils.join;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.openehr.aqlengine.asl.meta.AslFieldOrigin;
import org.ehrbase.openehr.aqlengine.asl.meta.AslQueryOrigin;
import org.ehrbase.openehr.aqlengine.asl.meta.AslTypeOrigin;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslAndQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslDescendantCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFalseQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldJoinCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslFieldValueQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslNotNullQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslNotQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslOrQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslPathChildCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslQueryCondition;
import org.ehrbase.openehr.aqlengine.asl.model.condition.AslTrueQueryCondition;
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
import org.ehrbase.openehr.aqlengine.asl.model.query.AslDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslEncapsulatingQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslPathDataQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslStructureQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.AbstractContainmentExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentVersionExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;

public class AslGraph {

    public static String createAslGraph(AslRootQuery query) {
        return join(
                indented(0, "AslRootQuery"),
                selectGraph(1, query.getSelect()),
                indented(1, "FROM"),
                query.getChildren().stream()
                        .map(s -> sqToGraph(2, s.getLeft(), s.getRight()))
                        .collect(Collectors.joining()),
                section(1, query.getCondition(), Objects::nonNull, __ -> "WHERE", AslGraph::conditionToGraph),
                section(
                        1,
                        query.getGroupByFields(),
                        CollectionUtils::isNotEmpty,
                        __ -> "GROUP BY",
                        (l, fs) -> fs.stream()
                                .map(f -> indented(l, fieldToGraph(l, f)))
                                .collect(Collectors.joining())),
                section(
                        1,
                        query.getOrderByFields(),
                        CollectionUtils::isNotEmpty,
                        __ -> "ORDER BY",
                        (l, fs) -> fs.stream().map(f -> orderByToGraph(l, f)).collect(Collectors.joining())),
                section(1, query.getLimit(), Objects::nonNull, "LIMIT %d"::formatted, (l, v) -> ""),
                section(1, query.getOffset(), Objects::nonNull, "OFFSET %d"::formatted, (l, v) -> ""));
    }

    private static String selectGraph(int level, List<AslField> select) {
        return indented(level, "SELECT") + indented(level + 1, select.stream(), s -> fieldToGraph(level + 1, s));
    }

    private static <T> String sqToGraph(int level, AslQuery subquery, AslJoin join) {
        String fromStructure = section(
                level + 1,
                subquery,
                AslStructureQuery.class::isInstance,
                sq -> "FROM " + ((AslStructureQuery) sq).getType().name(),
                (l, sq) -> "");

        String fromEncapsulating = section(
                level + 2,
                subquery,
                AslEncapsulatingQuery.class::isInstance,
                __ -> "FROM",
                (l, sq) -> indented(
                        l,
                        ((AslEncapsulatingQuery) sq).getChildren().stream(),
                        c -> sqToGraph(l + 1, c.getLeft(), c.getRight())));
        String base = section(
                level + 1,
                subquery,
                AslDataQuery.class::isInstance,
                sq -> "BASE " + ((AslDataQuery) sq).getBase().getAlias(),
                (l, sq) -> "");

        String joinStr = Optional.ofNullable(join)
                .map(j -> indented(
                                level + 1,
                                j.getJoinType() + " " + j.getLeft().getAlias() + " -> "
                                        + j.getRight().getAlias())
                        + section(
                                level + 2,
                                j.getOn(),
                                CollectionUtils::isNotEmpty,
                                c -> "on",
                                AslGraph::conditionsToGraph))
                .orElse("");

        String queryComment =
                switch (subquery) {
                    case AslPathDataQuery pq -> pq.getPathNodes(pq.getDataField()).stream()
                            .map(p -> p.getAttribute() + p.getPredicateOrOperands())
                            .collect(Collectors.joining(".", " -- ", ""));
                    default -> "";
                };

        return origin(level, subquery)
                + indented(level, subquery.getAlias() + ": " + typeName(subquery) + queryComment)
                + selectGraph(level + 1, subquery.getSelect())
                + base
                + section(
                        level + 1, subquery.getCondition(), Objects::nonNull, c -> "WHERE", AslGraph::conditionToGraph)
                + fromStructure
                + fromEncapsulating
                + section(
                        level + 1,
                        subquery.getStructureConditions(),
                        CollectionUtils::isNotEmpty,
                        c -> "STRUCTURE CONDITIONS",
                        (l, cs) -> cs.stream()
                                .map(c -> conditionToGraph(level + 2, c))
                                .collect(Collectors.joining()))
                + joinStr;
    }

    private static <T> String section(
            int level, T t, Predicate<T> condition, Function<T, String> header, BiFunction<Integer, T, String> body) {
        if (!condition.test(t)) {
            return "";
        }
        Optional<String> heading =
                Optional.of(header.apply(t)).filter(StringUtils::isNotBlank).map(h -> indented(level, h));
        return heading.orElse("") + body.apply(level + (heading.isPresent() ? 1 : 0), t);
    }

    private static String typeName(AslQuery subquery) {
        String simpleName = subquery.getClass().getSimpleName();
        return StringUtils.removeStart(simpleName, "Asl");
    }

    private static String conditionToGraph(int level, AslQueryCondition condition) {
        return switch (condition) {
            case null -> "";
            case AslNotQueryCondition c -> indented(level, "NOT") + conditionToGraph(level + 1, c.getCondition());
            case AslFieldValueQueryCondition<?> c -> indented(
                    level, fieldToGraph(level, c.getField()) + " " + c.getOperator() + " " + c.getValues());
            case AslFalseQueryCondition __ -> indented(level, "false");
            case AslTrueQueryCondition __ -> indented(level, "true");
            case AslOrQueryCondition c -> indented(level, "OR")
                    + c.getOperands().stream()
                            .map(op -> conditionToGraph(level + 1, op))
                            .collect(Collectors.joining());
            case AslAndQueryCondition c -> indented(level, "AND")
                    + c.getOperands().stream()
                            .map(op -> conditionToGraph(level + 1, op))
                            .collect(Collectors.joining());
            case AslNotNullQueryCondition c -> indented(level, "NOT_NULL " + fieldToGraph(level + 1, c.getField()));
            case AslFieldJoinCondition c -> indented(
                    level,
                    "AslFieldJoinCondition %s.%s %s %s.%s"
                            .formatted(
                                    c.getLeftOwner().getAlias(),
                                    c.getLeftField().getAliasedName(),
                                    c.getOperator(),
                                    c.getRightOwner().getAlias(),
                                    c.getRightField().getAliasedName()));
            case AslDescendantCondition c -> indented(
                    level,
                    "DescendantCondition %s %s -> %s %s"
                            .formatted(
                                    c.getParentRelation(),
                                    c.getLeftOwner().getAlias(),
                                    c.getDescendantRelation(),
                                    c.getRightOwner().getAlias()));
            case AslPathChildCondition c -> indented(
                    level,
                    "PathChildCondition %s %s -> %s %s"
                            .formatted(
                                    c.getParentRelation(),
                                    c.getLeftOwner().getAlias(),
                                    c.getChildRelation(),
                                    c.getRightOwner().getAlias()));
        };
    }

    private static String conditionsToGraph(int level, List<AslJoinCondition> joinConditions) {
        return joinConditions.stream()
                .map(jc -> switch (jc) {
                    case AslPathFilterJoinCondition c -> "PathFilterJoinCondition %s ->\n%s"
                            .formatted(c.getLeftOwner().getAlias(), conditionToGraph(level + 2, c.getCondition()));
                    case AslDelegatingJoinCondition c -> "DelegatingJoinCondition %s ->\n%s"
                            .formatted(c.getLeftOwner().getAlias(), conditionToGraph(level + 2, c.getDelegate()));
                    case AslAuditDetailsJoinCondition c -> "AuditDetailsJoinCondition %s -> %s"
                            .formatted(
                                    c.getLeftOwner().getAlias(),
                                    c.getRightOwner().getAlias());
                    case AslFolderItemJoinCondition
                    c -> "FolderItemJoinCondition FOLDER -> %s [%s.vo_id in %s.data.items[].id.value]"
                            .formatted(
                                    c.descendantRelation(),
                                    c.getRightOwner().getAlias(),
                                    c.getLeftOwner().getAlias());
                })
                .map(s -> indented(level, s))
                .collect(Collectors.joining());
    }

    private static String orderByToGraph(int level, AslOrderByField sortOrderPair) {
        String field = indented(level, field(level, sortOrderPair.field()) + " " + sortOrderPair.direction());
        return origin(sortOrderPair.field())
                .map(origin -> indented(level, origin) + field)
                .orElse(field);
    }

    private static String fieldToGraph(int level, AslField aslField) {
        String field = field(level, aslField);
        return origin(aslField)
                .map(origin -> origin + "\n" + indention(level) + field)
                .orElse(field);
    }

    private static String field(int level, AslField field) {
        String providerAlias = (field.getInternalProvider() != null)
                ? (field.getInternalProvider().getAlias() + ".")
                : "";
        return switch (field) {
            case AslColumnField f -> providerAlias
                    + f.getAliasedName()
                    + Optional.of(f)
                            .map(AslColumnField::getExtractedColumn)
                            .map(e -> " -- " + e.getPath().render())
                            .orElse("");
            case AslComplexExtractedColumnField f -> providerAlias
                    + f.getOwner().getAlias() + "_"
                    + f.getExtractedColumn().name().toLowerCase()
                    + Optional.of(f)
                            .map(AslComplexExtractedColumnField::getExtractedColumn)
                            .map(e -> " -- COMPLEX " + e.name() + " "
                                    + e.getPath().render())
                            .orElse("");
            case AslAggregatingField f -> "%s(%s%s)"
                    .formatted(
                            f.getFunction(),
                            f.isDistinct() ? "DISTINCT " : "",
                            Optional.of(f)
                                    .map(AslAggregatingField::getBaseField)
                                    .map(bf -> fieldToGraph(level, bf.withOrigin((AslFieldOrigin) null)))
                                    .orElse("*"));
            case AslSubqueryField f -> sqToGraph(level + 1, f.getBaseQuery(), null)
                    + (f.getFilterConditions().isEmpty()
                            ? ""
                            : indented(level + 1, "Filter:")
                                    + f.getFilterConditions().stream()
                                            .map(c -> conditionToGraph(level + 2, c))
                                            .collect(Collectors.joining("\n", "", "")));
            case AslConstantField f -> "CONSTANT (%s): %s".formatted(f.getType().getSimpleName(), f.getValue());
            case AslFolderItemIdVirtualField f -> providerAlias + f.aliasedName() + " -- FOLDER.items";
            case AslRmPathField f -> providerAlias
                    + f.getSrcField().getAliasedName()
                    + f.getPathInJson().stream()
                            .map(PathNode::getAttribute)
                            .collect(Collectors.joining(" -> ", " -> ", ""));
        };
    }

    private static Optional<String> origin(AslField field) {
        return Optional.ofNullable(field.getOrigin()).map(origin -> "-- " + identifiedPath(origin.path()));
    }

    private static String origin(int level, AslQuery aslQuery) {
        AslQueryOrigin queryOrigin = aslQuery.getOrigin();
        if (queryOrigin == null || queryOrigin.typeOrigins().isEmpty()) {
            return indented(level == 2 ? 2 : 0, "");
        }
        return queryOrigin.typeOrigins().stream()
                .map(origin -> {
                    String type =
                            switch (origin) {
                                case AslTypeOrigin.AslRmTypeOrigin rmTypeOrigin -> rmTypeOrigin.getRmType();
                                case AslTypeOrigin.AslVersionTypeOrigin versionTypeOrigin -> versionTypeOrigin
                                                .getRmType()
                                        + " "
                                        + versionTypeOrigin.getRmTypeOrigin().getRmType();
                            };
                    return indented(
                                    level == 2 ? 2 : 1,
                                    "-- " + type + " "
                                            + Optional.ofNullable(origin.getAlias())
                                                    .orElse(""))
                            + origin.getFieldPaths().stream()
                                    .map(identifiedPath -> indented(level, "-- " + identifiedPath(identifiedPath)))
                                    .collect(Collectors.joining("", "", ""));
                })
                .collect(Collectors.joining("\n", "", ""))
                .replaceAll("\n+", "\n");
    }

    private static <T> String indented(int level, Stream<T> entries, Function<T, String> toString) {
        String prefix = indention(level);
        return entries.map(toString).collect(Collectors.joining("\n" + prefix, prefix, "\n"));
    }

    private static <T> String indented(int level, String str) {
        return indention(level) + str + "\n";
    }

    private static <T> String indention(int level) {
        return StringUtils.repeat("  ", level);
    }

    private static String identifiedPath(IdentifiedPath identifiedPath) {
        AbstractContainmentExpression root = identifiedPath.getRoot();
        String type =
                switch (root) {
                    case ContainmentClassExpression containmentClassExpression:
                        yield containmentClassExpression.getType();
                    case ContainmentVersionExpression containmentVersionExpression:
                        yield containmentVersionExpression
                                .getVersionPredicateType()
                                .name();
                };
        String predicates = root.hasPredicates()
                ? andOperatorPredicate(
                        Optional.ofNullable(identifiedPath.getRootPredicate()).orElseGet(root::getPredicates))
                : "";
        String identifier = root.getIdentifier();
        String pathPart = Optional.ofNullable(identifiedPath.getPath())
                .map(AqlObjectPath::render)
                .orElse("");
        return Optional.ofNullable(identifier).map(it -> type + " " + it).orElse(type) + predicates + "/" + pathPart;
    }

    private static String andOperatorPredicate(Collection<AndOperatorPredicate> predicates) {
        return predicates.stream()
                .flatMap(predicate -> predicate.getOperands().stream().map(operand -> {
                    String path = operand.getPath().render();
                    String value =
                            ((Primitive<?, ?>) operand.getValue()).getValue().toString();
                    if (path.equals("archetype_node_id")
                            && operand.getOperator() == ComparisonOperatorPredicate.PredicateComparisonOperator.EQ) {
                        return value;
                    }
                    return path + operand.getOperator().getSymbol() + value;
                }))
                .collect(Collectors.joining(",", "[", "]"));
    }
}
