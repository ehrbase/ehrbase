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
package org.ehrbase.openehr.aqlengine.aql;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.collections4.MapUtils;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.openehr.aqlengine.asl.model.AslRmTypeAndConcept;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.condition.ComparisonOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.ExistsCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.LikeCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.LogicalOperatorCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.MatchesCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.NotCondition;
import org.ehrbase.openehr.sdk.aql.dto.condition.WhereCondition;
import org.ehrbase.openehr.sdk.aql.dto.containment.AbstractContainmentExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.Containment;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentNotOperator;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentSetOperator;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentVersionExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.AggregateFunction;
import org.ehrbase.openehr.sdk.aql.dto.operand.BooleanPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.ComparisonLeftOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.DoublePrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.operand.LikeOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.LongPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.MatchesOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.Operand;
import org.ehrbase.openehr.sdk.aql.dto.operand.PathPredicateOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.QueryParameter;
import org.ehrbase.openehr.sdk.aql.dto.operand.SingleRowFunction;
import org.ehrbase.openehr.sdk.aql.dto.operand.StringPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.TemporalPrimitive;
import org.ehrbase.openehr.sdk.aql.dto.operand.TerminologyFunction;
import org.ehrbase.openehr.sdk.aql.dto.orderby.OrderByExpression;
import org.ehrbase.openehr.sdk.aql.dto.path.AndOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPathUtil;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectClause;
import org.ehrbase.openehr.sdk.aql.dto.select.SelectExpression;
import org.ehrbase.openehr.sdk.aql.parser.AqlParseException;
import org.springframework.stereotype.Component;

/**
 * Replaces parameters in an AQL query
 */
@Component
public class AqlParameterPostProcessor implements AqlQueryParsingPostProcessor {

    @Override
    public void afterParseAql(final AqlQuery aqlQuery, final AqlQueryRequest request, final AqlQueryContext ctx) {
        replaceParameters(aqlQuery, request.parameters());
    }

    @Override
    public int getOrder() {
        return PARAMETER_REPLACEMENT_PRECEDENCE;
    }
    /**
     * Replaces all parameters in the <code>aqlQuery</code> with values from the <code>parameterMap</code>.
     * The replacement is performed in-place, modifying the source object.
     * Missing parameter values are set to NULL.
     *
     * @param aqlQuery the query to me modified
     * @param parameterMap a map of parameter values
     */
    public static void replaceParameters(AqlQuery aqlQuery, Map<String, Object> parameterMap) {
        if (MapUtils.isNotEmpty(parameterMap)) {
            // SELECT
            SelectParams.replaceParameters(aqlQuery.getSelect(), parameterMap);
            // FROM
            ContainmentParams.replaceParameters(aqlQuery.getFrom(), parameterMap);
            // WHERE
            WhereParams.replaceParameters(aqlQuery.getWhere(), parameterMap);
            // ORDER BY
            OrderByParams.replaceParameters(parameterMap, aqlQuery.getOrderBy());
        }
    }

    public static void replaceIdentifiedPathParameters(
            IdentifiedPath identifiedPath, Map<String, Object> parameterMap) {
        // revise root predicates in-place
        Optional.of(identifiedPath).map(IdentifiedPath::getRootPredicate).stream()
                .flatMap(List::stream)
                .map(AndOperatorPredicate::getOperands)
                .flatMap(List::stream)
                .forEach(co -> ObjectPathParams.replaceComparisonOperatorParameters(co, parameterMap));
        ObjectPathParams.replaceParameters(identifiedPath.getPath(), parameterMap)
                .ifPresent(identifiedPath::setPath);
    }

    /**
     * @param operand
     * @param parameterMap
     * @return a Stream of new primitive, if the operand needs to be replaced
     */
    private static Stream<Primitive> replaceOperandParameters(Operand operand, Map<String, Object> parameterMap) {

        return switch (operand) {
            case QueryParameter qp -> resolveParameters(qp, parameterMap);
            case IdentifiedPath path -> {
                replaceIdentifiedPathParameters(path, parameterMap);
                yield null;
            }
            case SingleRowFunction func -> {
                replaceFunctionParameters(func, parameterMap);
                yield null;
            }
            case TerminologyFunction __ -> null;
            case Primitive<?, ?> __ -> null;
        };
    }

    private static void replaceFunctionParameters(SingleRowFunction func, Map<String, Object> parameterMap) {
        Utils.reviseList(func.getOperandList(), o -> replaceOperandParameters(o, parameterMap));
    }

    private static Stream<Primitive> resolveParameters(QueryParameter param, Map<String, Object> parameterMap) {
        String paramName = param.getName();
        Object paramValue = parameterMap.get(paramName);

        return switch (paramValue) {
            case Collection<?> c -> c.stream().map(e -> toPrimitive(param.getName(), e));
            case null -> throw new AqlParseException("Missing parameter '%s'".formatted(paramName));
            default -> Stream.of(toPrimitive(param.getName(), paramValue));
        };
    }

    private static Primitive toPrimitive(String name, Object paramValue) {
        return switch (paramValue) {
            case null -> throw new AqlParseException("Missing parameter '%s'".formatted(name));
            case Integer i -> new LongPrimitive(i.longValue());
            case Long i -> new LongPrimitive(i);
            case Number nr -> new DoublePrimitive(nr.doubleValue());
            case String str -> Utils.stringToPrimitive(str);
            case Boolean b -> new BooleanPrimitive(b);
            default -> {
                throw new IllegalArgumentException("Type of parameter '%s' is not supported".formatted(name));
            }
        };
    }

    private record ModifiedElement<T>(int index, T node) {}

    private static final class OrderByParams {

        public static void replaceParameters(Map<String, Object> parameterMap, List<OrderByExpression> orderBy) {
            if (orderBy != null) {
                orderBy.stream()
                        .map(OrderByExpression::getStatement)
                        .forEach(path -> replaceIdentifiedPathParameters(path, parameterMap));
            }
        }
    }

    private static final class SelectParams {

        public static void replaceParameters(SelectClause selectClause, Map<String, Object> parameterMap) {
            selectClause.getStatement().stream()
                    .map(SelectExpression::getColumnExpression)
                    .forEach(ce -> {
                        switch (ce) {
                            case SingleRowFunction func -> replaceFunctionParameters(func, parameterMap);
                            case AggregateFunction aFunc ->
                                replaceIdentifiedPathParameters(aFunc.getIdentifiedPath(), parameterMap);
                            case IdentifiedPath identifiedPath ->
                                replaceIdentifiedPathParameters(identifiedPath, parameterMap);
                            case Primitive<?, ?> __ -> {
                                /* No parameters */
                            }
                            case TerminologyFunction __ -> {
                                /* No parameters */
                            }
                        }
                    });
        }
    }

    private static final class WhereParams {
        public static void replaceParameters(WhereCondition condition, Map<String, Object> parameterMap) {
            switch (condition) {
                case null -> {
                    /* NOOP */
                }
                case ComparisonOperatorCondition c -> {
                    replaceComparisonLeftOperandParameters(c.getStatement(), parameterMap);
                    ensureSingleElement(replaceOperandParameters(c.getValue(), parameterMap), c::setValue);
                }
                case NotCondition c -> replaceParameters(c.getConditionDto(), parameterMap);
                case MatchesCondition c ->
                    Utils.reviseList(c.getValues(), o -> replaceMatchesParameters(o, parameterMap));
                case LikeCondition c ->
                    replaceLikeOperandParameters(c.getValue(), parameterMap).ifPresent(c::setValue);
                case LogicalOperatorCondition c -> c.getValues().forEach(v -> replaceParameters(v, parameterMap));
                case ExistsCondition __ -> {
                    /* NOOP */
                }
                default -> throw new IllegalStateException("Unexpected value: " + condition);
            }
        }

        private static Optional<LikeOperand> replaceLikeOperandParameters(
                LikeOperand value, Map<String, Object> parameterMap) {
            if (value instanceof QueryParameter qp) {
                return Optional.of(qp.getName())
                        .map(parameterMap::get)
                        .map(Object::toString)
                        .<LikeOperand>map(Utils::stringToPrimitive)
                        .or(() -> Optional.of(new StringPrimitive(null)));
            } else {
                return Optional.empty();
            }
        }

        private static Stream<Primitive> replaceMatchesParameters(
                MatchesOperand operand, Map<String, Object> parameterMap) {
            if (operand instanceof QueryParameter qp) {
                return resolveParameters(qp, parameterMap);
            } else {
                return null;
            }
        }

        private static void replaceComparisonLeftOperandParameters(
                ComparisonLeftOperand statement, Map<String, Object> parameterMap) {
            switch (statement) {
                case SingleRowFunction func -> replaceFunctionParameters(func, parameterMap);
                case IdentifiedPath path -> replaceIdentifiedPathParameters(path, parameterMap);
                case TerminologyFunction __ -> {
                    /* cannot contain parameters */
                }
            }
        }
    }

    private static final class ContainmentParams {
        public static void replaceParameters(Containment containment, Map<String, Object> parameterMap) {
            switch (containment) {
                case null -> {
                    /*NOOP*/
                }
                case ContainmentSetOperator cso -> cso.getValues().forEach(c -> replaceParameters(c, parameterMap));
                case ContainmentNotOperator cno -> replaceParameters(cno.getContainmentExpression(), parameterMap);
                case ContainmentClassExpression cce -> replaceContainmentClassExpressionParameters(cce, parameterMap);
                case ContainmentVersionExpression cve ->
                    replaceContainmentVersionExpressionParameters(cve, parameterMap);
            }
        }

        private static void replaceContainmentClassExpressionParameters(
                ContainmentClassExpression cce, Map<String, Object> parameterMap) {
            streamComparisonOperatorPredicates(cce)
                    .forEach(op -> ObjectPathParams.replaceComparisonOperatorParameters(op, parameterMap));
            replaceParameters(cce.getContains(), parameterMap);
        }

        private static void replaceContainmentVersionExpressionParameters(
                ContainmentVersionExpression cve, Map<String, Object> parameterMap) {
            Optional.of(cve)
                    .map(ContainmentVersionExpression::getPredicate)
                    .ifPresent(op -> ObjectPathParams.replaceComparisonOperatorParameters(op, parameterMap));
            replaceParameters(cve.getContains(), parameterMap);
        }

        private static Stream<ComparisonOperatorPredicate> streamComparisonOperatorPredicates(
                ContainmentClassExpression cce) {
            return Optional.of(cce)
                    .filter(AbstractContainmentExpression::hasPredicates)
                    .map(AbstractContainmentExpression::getPredicates)
                    .stream()
                    .flatMap(List::stream)
                    .map(AndOperatorPredicate::getOperands)
                    .flatMap(Collection::stream);
        }
    }

    private static final class ObjectPathParams {

        /**
         * Replaces all parameters.
         * If parameters were replaced, the modified AqlObjectPath is returned.
         * The provided <code>path</code> object remains unchanged.
         *
         * @param path
         * @param parameterMap
         * @return
         */
        public static Optional<AqlObjectPath> replaceParameters(AqlObjectPath path, Map<String, Object> parameterMap) {
            if (path == null) {
                return Optional.empty();
            }
            return Utils.replaceChildParameters(
                            path.getPathNodes(), ObjectPathParams::replacePathNodeParameters, parameterMap)
                    .map(AqlObjectPath::new);
        }

        private static Optional<ComparisonOperatorPredicate> replaceComparisonOperatorPredicateParameters(
                ComparisonOperatorPredicate n, Map<String, Object> parameterMap) {
            Optional<AqlObjectPath> replacedPath = replaceParameters(n.getPath(), parameterMap);

            Optional<PathPredicateOperand> replacedValue =
                    switch (n.getValue()) {
                        case QueryParameter qp ->
                            Optional.of((PathPredicateOperand) ensureSingleElement(
                                    resolveParameters(qp, parameterMap), p -> validateParameterSyntax(n.getPath(), p)));
                        case Primitive __ -> Optional.empty();
                        case AqlObjectPath p ->
                            replaceParameters(p, parameterMap).map(PathPredicateOperand.class::cast);
                        default -> throw new IllegalStateException("Unexpected value: " + n.getValue());
                    };

            if (replacedPath.isEmpty() && replacedValue.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new ComparisonOperatorPredicate(
                    replacedPath.orElse(n.getPath()), n.getOperator(), replacedValue.orElse(n.getValue())));
        }

        /**
         * if ARCHETYPE_NODE_ID: check syntax
         * @param path
         * @param p
         */
        private static void validateParameterSyntax(AqlObjectPath path, Primitive p) {
            if (AqlObjectPathUtil.ARCHETYPE_NODE_ID.equals(path)) {
                if (p instanceof StringPrimitive sp) {
                    try {
                        AslRmTypeAndConcept.fromArchetypeNodeId(sp.getValue());
                    } catch (IllegalArgumentException e) {
                        throw new AqlParseException(
                                "Invalid parameter for %s".formatted(AqlObjectPathUtil.ARCHETYPE_NODE_ID));
                    }
                } else {
                    throw new AqlParseException(
                            "Invalid parameter type for %s".formatted(AqlObjectPathUtil.ARCHETYPE_NODE_ID));
                }
            }
        }

        private static Optional<AndOperatorPredicate> replaceAndOperatorPredicateParameters(
                AndOperatorPredicate and, Map<String, Object> parameterMap) {
            return Utils.replaceChildParameters(
                            and.getOperands(),
                            ObjectPathParams::replaceComparisonOperatorPredicateParameters,
                            parameterMap)
                    .map(AndOperatorPredicate::new);
        }

        private static Optional<AqlObjectPath.PathNode> replacePathNodeParameters(
                AqlObjectPath.PathNode node, Map<String, Object> parameterMap) {
            return Utils.replaceChildParameters(
                            node.getPredicateOrOperands(),
                            ObjectPathParams::replaceAndOperatorPredicateParameters,
                            parameterMap)
                    .map(l -> new AqlObjectPath.PathNode(node.getAttribute(), l));
        }

        /**
         * {@link ComparisonOperatorPredicate}s are used in
         * <ul>
         *     <li><code>ContainmentClassExpression.predicates.predicateOrOperands.operands</code>/li>
         *     <li><code>ContainmentVersionExpression.predicate</code></li>
         *     <li><code>IdentifiedPath.rootPredicate.predicateOrOperands.operands</code></li>
         *     <li><code>IdentifiedPath.path</code> (via AqlObjectPath)</li>
         *     <li><code>AqlObjectPath.pathNodes.predicateOrOperands.operands</code></li>
         *     <li>code>ComparisonOperatorPredicate.path</code> (recursively via AqlObjectPath)</li>
         *     <li>code>ComparisonOperatorPredicate.value</code> (recursively via PathPredicateOperand implementation AqlObjectPath)</li>
         * </ul>
         *
         * @param op
         * @param parameterMap
         */
        public static void replaceComparisonOperatorParameters(
                ComparisonOperatorPredicate op, Map<String, Object> parameterMap) {
            Optional<AqlObjectPath> newPath = replaceParameters(op.getPath(), parameterMap);

            Object newValue =
                    switch (op.getValue()) {
                        case null ->
                            throw new NullPointerException(
                                    "Missing value for path " + op.getPath().render());
                        case QueryParameter qp ->
                            ensureSingleElement(
                                    resolveParameters(qp, parameterMap), p -> validateParameterSyntax(op.getPath(), p));
                        case Primitive<?, ?> __ -> null;
                        case AqlObjectPath path -> replaceParameters(path, parameterMap);
                        default ->
                            throw new IllegalArgumentException("Unexpected type of value: "
                                    + op.getValue().getClass().getSimpleName());
                    };

            newPath.ifPresent(op::setPath);
            if (newValue != null) {
                op.setValue((PathPredicateOperand<?>) newValue);
            }
        }
    }

    static final class TemporalPrimitivePattern {

        static final Pattern TEMPORAL_PATTERN;

        static {
            // see AqlLexer.g4
            String DIGIT = "[0-9]";
            // Year in ISO8601:2004 is 4 digits with 0-filling as needed
            String YEAR = DIGIT + DIGIT + DIGIT + DIGIT;
            // month in year
            String MONTH = nonCapturing(or("[0][1-9]", "[1][0-2]"));
            // day in month
            String DAY = nonCapturing(or("[0][1-9]", "[12][0-9]", "[3][0-1]"));
            // hour in 24 hour clock
            String HOUR = nonCapturing(or("[01][0-9]", "[2][0-3]"));
            // minutes
            String MINUTE = "[0-5][0-9]";
            String SECOND = MINUTE;

            String DATE_SHORT = YEAR + MONTH + DAY;
            String DATE_LONG = YEAR + '-' + MONTH + '-' + DAY;
            String TIME_SHORT = HOUR + MINUTE + SECOND;
            String TIME_LONG = HOUR + ':' + MINUTE + ':' + SECOND;
            String FRACTIONAL_SECONDS = "\\." + nonCapturing(DIGIT) + "{1,9}";
            // hour offset, e.g. `+09:30`, or else literal `Z` indicating +0000.
            String TIMEZONE = or("Z", nonCapturing("[-+]", HOUR, optional("[:]?" + MINUTE)));

            TEMPORAL_PATTERN = Pattern.compile(or(
                    // extended datetime
                    DATE_LONG + optional("T", TIME_LONG, optional(FRACTIONAL_SECONDS), optional(TIMEZONE)),
                    // compact datetime
                    DATE_SHORT + optional("T", TIME_SHORT, optional(FRACTIONAL_SECONDS), optional(TIMEZONE)),
                    // compact & extended time
                    nonCapturing(or(TIME_SHORT, TIME_LONG)) + optional(FRACTIONAL_SECONDS) + optional(TIMEZONE)));
        }

        private TemporalPrimitivePattern() {}

        private static String nonCapturing(String... content) {
            return Arrays.stream(content).collect(Collectors.joining("", "(?:", ")"));
        }

        private static String or(String... content) {
            return String.join("|", content);
        }

        private static String optional(String... content) {
            return nonCapturing(content) + "?";
        }

        public static boolean matches(String input) {
            return TEMPORAL_PATTERN.matcher(input).matches();
        }
    }

    static final class Utils {

        public static StringPrimitive stringToPrimitive(String str) {
            if (TemporalPrimitivePattern.matches(str)) {
                return TemporalPrimitive.fromString(str);
            } else {
                return new StringPrimitive(str);
            }
        }

        /**
         * For each entry of the list the <code>replacementFunc</code> is called.
         * It if returns new entries, the old one is replaced.
         *
         * @param list
         * @param replacementFunc
         * @param <T>
         */
        public static <T> void reviseList(List<T> list, Function<T, Stream<? extends T>> replacementFunc) {
            if (list.isEmpty()) {
                return;
            }
            final ListIterator<T> li = list.listIterator();
            while (li.hasNext()) {
                Stream<? extends T> replacementsStream = replacementFunc.apply(li.next());
                if (replacementsStream != null) {
                    li.remove();
                    replacementsStream.forEach(li::add);
                }
            }
            if (list.isEmpty()) {
                throw new AqlParseException("Parameter replacement resulted in empty operand list");
            }
        }

        /**
         * Generic function to hierarchically replace all parameters of an object.
         * If parameters were replaced, a new list with modified objects is returned.
         * The provided <code>children</code> remain unchanged.
         *
         * @param children
         * @param childReplacementFunc returns an Optional with a modified child, if applicable
         * @param parameterMap
         * @return
         */
        public static <C> Optional<List<C>> replaceChildParameters(
                List<C> children,
                BiFunction<C, Map<String, Object>, Optional<C>> childReplacementFunc,
                Map<String, Object> parameterMap) {

            ModifiedElement<C>[] modifiedElements = IntStream.range(0, children.size())
                    .mapToObj(i -> childReplacementFunc
                            .apply(children.get(i), parameterMap)
                            .map(m -> new ModifiedElement<C>(i, m)))
                    .flatMap(Optional::stream)
                    .toArray(ModifiedElement[]::new);

            if (modifiedElements.length == 0) {
                return Optional.empty();
            }

            C[] newChildren = (C[]) children.toArray();
            for (ModifiedElement<C> modifiedNode : modifiedElements) {
                newChildren[modifiedNode.index()] = modifiedNode.node();
            }
            return Optional.of(List.of(newChildren));
        }
    }

    /**
     * Makes sure that if singleElementStream is not null, it contains exactly one element.
     * The element is presented to the elementConsumer and then returned.
     *
     * @param singleElementStream
     * @param elementConsumer
     * @return
     * @param <T>
     */
    private static <T> T ensureSingleElement(Stream<T> singleElementStream, Consumer<T> elementConsumer) {
        if (singleElementStream == null) {
            return null;
        }
        Iterator<T> it = singleElementStream.iterator();
        if (it.hasNext()) {
            T first = it.next();
            if (it.hasNext()) {
                throw new AqlParseException("One of the parameters does not support multiple values");
            }
            elementConsumer.accept(first);
            return first;
        } else {
            throw new AqlParseException("Empty parameter replacement results");
        }
    }
}
