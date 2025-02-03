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
package org.ehrbase.openehr.aqlengine;

import com.google.re2j.Pattern;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.ehrbase.api.dto.AqlQueryContext;
import org.ehrbase.api.dto.AqlQueryRequest;
import org.ehrbase.api.exception.AqlFeatureNotImplementedException;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.dto.containment.AbstractContainmentExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentSetOperator;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentSetOperatorSymbol;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath.PathNode;
import org.ehrbase.openehr.sdk.aql.util.AqlUtil;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.springframework.stereotype.Component;

/**
 * Replaces paths targeting EHR_STATUS, but starting at EHR. For more information {@see replaceEhrPaths}
 */
@Component
public class AqlEhrPathPostProcessor implements AqlQueryParsingPostProcessor {
    @Override
    public void afterParseAql(final AqlQuery aqlQuery, final AqlQueryRequest request, final AqlQueryContext ctx) {
        replaceEhrPaths(aqlQuery);
    }

    @Override
    public int getOrder() {
        return 200;
    }

    /**
     * Rephrases EHR.status to CONTAINS statements so that they can be handled regularly by the aql engine.
     * I.e. <code>SELECT e/ehr_status FROM EHR</code> is rewritten as <code>SELECT s FROM EHR e CONTAINS EHR_STATUS s</code>.
     * EHR/composition, EHR/directory, and EHR/folders are not supported because the path syntax implies that the objects are optional (vs. CONTAINS).
     */
    static void replaceEhrPaths(AqlQuery aqlQuery) {
        replaceEhrPath(aqlQuery, "ehr_status", RmConstants.EHR_STATUS, "s");
    }

    /**
     * Rephrases a path from EHR to EHR_STATUS as CONTAINS statement so that it can be handled regularly by the aql engine.
     * E.g. <code>SELECT e/status FROM EHR</code> is rewritten as <code>SELECT s FROM EHR e CONTAINS EHR_STATUS s</code>.
     */
    static void replaceEhrPath(AqlQuery aqlQuery, String ehrPath, String type, String aliasPrefix) {

        // gather paths that contain EHR/status.
        List<IdentifiedPath> ehrPaths = AqlQueryUtils.allIdentifiedPaths(aqlQuery)
                // EHR
                .filter(ip -> ip.getRoot() instanceof ContainmentClassExpression cce
                        && cce.getType().equals(RmConstants.EHR))
                // EHR.ehrPath...
                .filter(ip -> Optional.of(ip)
                        .map(IdentifiedPath::getPath)
                        .map(AqlObjectPath::getPathNodes)
                        .map(List::getFirst)
                        .map(PathNode::getAttribute)
                        .filter(ehrPath::equals)
                        .isPresent())
                .toList();

        if (ehrPaths.isEmpty()) {
            return;
        }

        if (ehrPaths.stream()
                        .map(IdentifiedPath::getRoot)
                        .map(AbstractContainmentExpression::getIdentifier)
                        .distinct()
                        .count()
                > 1) {
            throw new AqlFeatureNotImplementedException("Multiple EHR in FROM are not supported");
        }

        if (ehrPaths.stream().map(IdentifiedPath::getRootPredicate).anyMatch(CollectionUtils::isNotEmpty)) {
            throw new AqlFeatureNotImplementedException(
                    "Root predicates for EHR/%s are not supported".formatted(ehrPath));
        }

        if (ehrPaths.stream()
                        .map(IdentifiedPath::getPath)
                        .map(p -> p.getPathNodes().getFirst().getPredicateOrOperands())
                        .distinct()
                        .count()
                > 1) {
            // could result in multiple containments
            throw new AqlFeatureNotImplementedException(
                    "Specifying different predicates for EHR/%s is not supported".formatted(ehrPath));
        }
        // determine unused alias
        String alias = AqlUtil.streamContainments(aqlQuery.getFrom())
                .map(AbstractContainmentExpression::getIdentifier)
                .filter(Objects::nonNull)
                .filter(s -> s.matches(Pattern.quote(aliasPrefix) + "\\d*"))
                .map(s -> aliasPrefix.equals(s) ? 0 : Long.parseLong(s.substring(1)))
                .max(Comparator.naturalOrder())
                .map(i -> aliasPrefix + (i + 1))
                .orElse(aliasPrefix);

        // insert CONTAINS [type] (AND if needed)
        // what about "SELECT e[ehr_id=…]/status from EHR e"?
        ContainmentClassExpression ehrContainment =
                (ContainmentClassExpression) ehrPaths.getFirst().getRoot();

        ContainmentClassExpression ehrStatusContainment = new ContainmentClassExpression();
        ehrStatusContainment.setType(type);
        ehrStatusContainment.setIdentifier(alias);

        // copy first predicate (all all are the same)
        ehrPaths.stream()
                .findFirst()
                .map(IdentifiedPath::getPath)
                .map(p -> p.getPathNodes().getFirst().getPredicateOrOperands())
                .ifPresent(ehrStatusContainment::setPredicates);

        // add containment
        if (ehrContainment.getContains() == null) {
            ehrContainment.setContains(ehrStatusContainment);
        } else if (ehrContainment.getContains() instanceof ContainmentSetOperator cse
                && cse.getSymbol() == ContainmentSetOperatorSymbol.AND) {
            cse.setValues(Stream.concat(Stream.of(ehrStatusContainment), cse.getValues().stream())
                    .toList());
        } else {
            ContainmentSetOperator and = new ContainmentSetOperator();
            and.setSymbol(ContainmentSetOperatorSymbol.AND);
            and.setValues(List.of(ehrStatusContainment, ehrContainment.getContains()));
            ehrContainment.setContains(and);
        }

        // rewrite paths
        ehrPaths.forEach(ip -> {
            ip.setRoot(ehrStatusContainment);
            List<PathNode> pathNodes = ip.getPath().getPathNodes();
            ip.setPath(pathNodes.size() == 1 ? null : new AqlObjectPath(pathNodes.subList(1, pathNodes.size())));
        });
    }
}
