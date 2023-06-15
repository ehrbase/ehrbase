/*
 * Copyright (c) 2020 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.containment;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;
import org.ehrbase.openehr.sdk.webtemplate.parser.NodeId;
import org.ehrbase.service.KnowledgeCacheService;

/**
 * Handles and evaluation contain proposition:
 * Simple contain chain (e.g. CONTAINS [...] CONTAINS [...]...) are evaluated for existence of paths within defined templates
 * Complex contain (expressed as logical expressions of Simple Contains) are evaluated as set operation (intersection, union. disjunction)
 */
public class ContainPropositions {

    private Map<String, ContainsCheck> propositionsEvalMap = new LinkedHashMap<>(); // ordered contain evaluation map
    /**
     * containers (containment definitions) with path values completed
     */
    private final IdentifierMapper identifierMapper;
    /**
     * set of templates for which the CONTAIN expression is true
     */
    private Set<String> templates = new HashSet<>();

    public ContainPropositions(IdentifierMapper identifierMapper) {
        this.identifierMapper = identifierMapper;
    }

    public void put(String key, ContainsCheck containsCheck) {
        propositionsEvalMap.put(key, containsCheck);
    }

    public ContainsCheck get(String key) {
        return propositionsEvalMap.get(key);
    }

    /**
     * evaluate the set of proposition IN-ORDER.
     * The results are given as:
     * - a list of templates for which the propositions are satisfied
     * - paths in containers for each identified template
     * @param knowledgeCache
     */
    public void evaluate(KnowledgeCacheService knowledgeCache) {
        ContainsCheck lastCheck = null;
        // iterate in-order on the map of contains proposition and keep the last one (since it will contain the result)
        for (Map.Entry<String, ContainsCheck> entry : propositionsEvalMap.entrySet()) {
            if (entry.getValue() instanceof SimpleChainedCheck) {
                List<NodeId> jsonQuery = ((SimpleChainedCheck) entry.getValue()).jsonPathNodeFilterExpression();
                if (jsonQuery != null) { // case CONTAINS COMPOSITION c without any specified further containments
                    try {
                        // perform the json path query on all available templates
                        List<JsonPathQueryResult> results = new Templates(knowledgeCache).resolve(jsonQuery);
                        // reconciliate the containment with the identifier mapper entry
                        if (results != null && !results.isEmpty()) {
                            Containment containment = (Containment) identifierMapper.getContainer(
                                    entry.getValue().getSymbol());

                            for (JsonPathQueryResult jsonPathQueryResult : results) {
                                if (containment != null) { // it is null if the symbol is not specified
                                    containment.setPath(
                                            jsonPathQueryResult.getTemplateId(), jsonPathQueryResult.getAqlPath());
                                }
                                entry.getValue().addTemplateId(jsonPathQueryResult.getTemplateId());
                                if (new Containments(
                                                knowledgeCache,
                                                ((SimpleChainedCheck) entry.getValue()).getContainmentSet())
                                        .hasUnresolvedContainment(jsonPathQueryResult.getTemplateId())) {
                                    // resolve it
                                    new Containments(
                                                    knowledgeCache,
                                                    ((SimpleChainedCheck) entry.getValue()).getContainmentSet())
                                            .resolveContainers(jsonPathQueryResult.getTemplateId());
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Could not traverse cached templates:" + e);
                    }
                }
            } else {
                Set<String> resultSet = new HashSet<>();
                boolean expectOperator = false;
                String operator = null;

                for (Object token : ((ComplexContainsCheck) entry.getValue()).getTokens()) {
                    if (token instanceof ContainsCheck) {
                        if (expectOperator) throw new IllegalStateException("Invalid expression");
                        if (operator != null) {
                            if (resultSet.isEmpty()) resultSet = SetUtils.emptySet();

                            Set<String> combinedSet = ((ContainsCheck) token).getTemplateIds();
                            // apply operator
                            switch (operator) {
                                case "AND":
                                    resultSet = SetUtils.intersection(resultSet, combinedSet);
                                    break;
                                case "OR":
                                    resultSet = SetUtils.union(resultSet, combinedSet);
                                    break;
                                case "XOR":
                                    resultSet = SetUtils.disjunction(resultSet, combinedSet);
                                    break;
                                default:
                                    throw new IllegalArgumentException(
                                            "Unsupported contains combination operator:" + operator);
                            }
                        } else resultSet.addAll(((ContainsCheck) token).getTemplateIds());
                        expectOperator = true;
                    } else if (token instanceof ContainOperator) {
                        expectOperator = false;
                        switch (((ContainOperator) token).operator()) {
                            case AND:
                            case OR:
                            case XOR:
                                operator = ((ContainOperator) token).getOperator();
                                break;
                            default:
                                break;
                        }
                    } else if (token instanceof String && ((String) token).matches("\\(|\\)")) {
                        expectOperator = false;
                    }
                }
                // assign resultSet to proposition
                entry.getValue().addAllTemplateIds(resultSet);
            }
            lastCheck = entry.getValue();
        }
        // wrap up iteration
        if (lastCheck != null) templates.addAll(lastCheck.getTemplateIds());
    }

    /**
     * returns the list of templates for the expression
     * @return
     */
    public Set<String> resolvedTemplates() {
        return templates;
    }

    /**
     * true if this expression is for a simple COMPOSITION containment (e.g. no embedded CONTAINs)
     * @return
     */
    public boolean requiresTemplateWhereClause() {
        return identifierMapper.requiresTemplateWhereClause();
    }

    public boolean hasContains() {
        return propositionsEvalMap.size() > 0;
    }
}
