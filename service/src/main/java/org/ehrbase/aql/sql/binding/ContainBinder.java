/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.aql.sql.binding;

import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.containment.Containment;
import org.ehrbase.aql.containment.ContainmentSet;
import org.ehrbase.aql.containment.Predicates;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SelectQuery;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;

import static org.ehrbase.jooq.pg.Tables.CONTAINMENT;

/**
 * Binds the SELECT, FROM, WHERE clauses to SQL expression
 * Binds nested sets containments to a SQL expression
 * <p>
 * Build up the queries to map variables to path (e.g. COMPOSITION c1 [openEHR-EHR-COMPOSITION.referral.v1])
 * Created by christian on 4/19/2016.
 */
public class ContainBinder {

    private static final String CONTAIN_SUBSELECT_TEMPLATE =
            "SELECT DISTINCT comp_id " +
                    "FROM ehr.containment " +
                    "WHERE label ~";

    private static final Character SINGLE_QUOTE = 0x27;
    private static final String RIGHT_WILDCARD = ".*";
    public static final String LEFT_WILDCARD = "*.";
    public static final String INNER_WILDCARD = ".*.";


    private List<ContainmentSet> nestedSets;

    private boolean useSimpleCompositionContainment = false; //true if check only if ehr contains any composition


    public ContainBinder(List<ContainmentSet> containmentSets) {
        this.nestedSets = containmentSets;
        //do some clean up (remove children pointing to empty parents...
//        int index = nestedSets.size() - 1;
        for (int index = nestedSets.size() - 1; index >= 0; index--) {
            ContainmentSet containmentSet = nestedSets.get(index);
            if (containmentSet != null && containmentSet.getParentSet() != null && containmentSet.getParentSet().isEmpty()) {
                ContainmentSet adopter = containmentSet.getParentSet();

                while (adopter != null && adopter.isEmpty()) {
                    nestedSets.remove(adopter);
                    adopter = adopter.getParentSet();
                }
                if (adopter.getContainmentList() != null) {
                    containmentSet.setParentSet(adopter);
                }

            }
        }
    }


    public String bind() {

        List<Predicates> predicates = new ArrayList<>();
        for (ContainmentSet containmentSet : nestedSets) {
            predicates.add(new PredicatesBinder().bind(containmentSet));
        }

        //factor containment
        factor4LTree(predicates);

        //assemble an SQL statement
        String query = inlineSqlQuery(predicates);

        return query;
    }

    public SelectQuery bind(DSLContext context) {

        List<Predicates> predicates = new ArrayList<>();
        for (ContainmentSet containmentSet : nestedSets) {
            predicates.add(new PredicatesBinder().bind(containmentSet));
        }

        //factor containment
        factor4LTree(predicates);

        //assemble an SQL statement
        SelectQuery query = jooqQuery(context, predicates);

        return query;
    }

    private String assembleSetOp(List<String> pendingAtomics, List<Predicates.Details> details, String opExpression, StringBuilder query) {
        for (Predicates.Details definition : details) {
            if (definition != null && !definition.isVoid()) {
                if (!pendingAtomics.isEmpty()) {
                    query.append(pendingAtomics.get(0));
                    pendingAtomics.remove(0);
                }
//                query.append(opExpression+ DEFAULT_CONTAIN_QUERY_TEMPLATE + SINGLE_QUOTE + definition.expression + SINGLE_QUOTE);
                query.append(opExpression + CONTAIN_SUBSELECT_TEMPLATE + SINGLE_QUOTE + definition.getExpression() + SINGLE_QUOTE);
            } else if (pendingAtomics.size() > 1) {
                //it is an intersect with the pending atomics...
                query.append(pendingAtomics.get(0));
                query.append(opExpression + pendingAtomics.get(1));
                pendingAtomics.remove(0);
                pendingAtomics.remove(0);
            }
            pendingAtomics.add(query.toString());
            query = new StringBuilder();
        }

        return query.toString();
    }

    private SelectQuery singularSelect(DSLContext context, Predicates.Details definition) {
        SelectQuery selectQuery = context.selectQuery();

        if (definition.getExpression().equals("COMPOSITION%")) {
            //check existence of a a composition for an EHR
//            Condition condition = DSL.condition(DSL.exists(context.select(DSL.value(1, Integer.class)).from(COMPOSITION).where()))
            useSimpleCompositionContainment = true;
            return null;
        } else {
            Condition condition = DSL.condition("label ~" + SINGLE_QUOTE + definition.getExpression() + SINGLE_QUOTE);
            selectQuery.addConditions(condition);
            selectQuery.addFrom(CONTAINMENT);
            selectQuery.addDistinctOn(CONTAINMENT.COMP_ID);
            selectQuery.addSelect(CONTAINMENT.COMP_ID);
            return selectQuery;
        }
    }

    private SelectQuery assembleSetOp(DSLContext context, List<SelectQuery> pendingAtomics, List<Predicates.Details> details, String opExpression, SelectQuery query) {
        for (Predicates.Details definition : details) {
            if (definition != null && !definition.isVoid()) {
                if (!pendingAtomics.isEmpty()) {
                    query.addFrom(pendingAtomics.get(0));
                    pendingAtomics.remove(0);
                }
                SelectQuery secondary = singularSelect(context, definition);
                switch (opExpression) {
                    case PredicatesBinder.INTERSECT_EXPRESSION:
                        query.intersect(secondary);
                        break;
                    case PredicatesBinder.UNION_EXPRESSION:
                        query.unionAll(secondary);
                        break;
                    case PredicatesBinder.EXCEPT_EXPRESSION:
                        query.exceptAll(secondary);
                        break;

                }
            } else if (pendingAtomics.size() > 1) {
                //it is an intersect with the pending atomics...
                SelectQuery initial = pendingAtomics.get(0);
                SelectQuery secondary = pendingAtomics.get(1);
                switch (opExpression) {
                    case PredicatesBinder.INTERSECT_EXPRESSION:
                        initial.intersect(secondary);
                        break;
                    case PredicatesBinder.UNION_EXPRESSION:
                        initial.unionAll(secondary);
                        break;
                    case PredicatesBinder.EXCEPT_EXPRESSION:
                        initial.exceptAll(secondary);
                        break;

                }
                pendingAtomics.remove(0);
                pendingAtomics.remove(0);
            }
            pendingAtomics.add(query);
        }

        return query;
    }

    private String inlineSqlQuery(List<Predicates> predicatesList) {
        StringBuilder query = new StringBuilder();
        List<String> pendingAtomics = new ArrayList<>();
        for (Predicates predicates : predicatesList) {
            if (predicates == null)
                continue;
            //start with the atomic predicate
            if (!predicates.getAtomicPredicates().isEmpty()) {
                for (Predicates.Details definition : predicates.getAtomicPredicates()) {
                    query.append(CONTAIN_SUBSELECT_TEMPLATE + SINGLE_QUOTE + definition.getExpression() + SINGLE_QUOTE);
                    pendingAtomics.add(query.toString());
                    query = new StringBuilder();
                }
            }
            if (!predicates.getIntersectPredicates().isEmpty()) {
                assembleSetOp(pendingAtomics, predicates.getIntersectPredicates(), PredicatesBinder.INTERSECT_EXPRESSION, query);
                query = new StringBuilder();
            }
            if (!predicates.getUnionPredicates().isEmpty()) {
                assembleSetOp(pendingAtomics, predicates.getUnionPredicates(), PredicatesBinder.UNION_EXPRESSION, query);
                query = new StringBuilder();
            }
            if (!predicates.getExceptPredicates().isEmpty()) {
                assembleSetOp(pendingAtomics, predicates.getExceptPredicates(), PredicatesBinder.EXCEPT_EXPRESSION, query);
                query = new StringBuilder();
            }
        }
        if (!pendingAtomics.isEmpty()) {
            query.append(pendingAtomics.get(0));
            return query.toString();
        }
        return "";
    }

    private SelectQuery<?> jooqQuery(DSLContext context, List<Predicates> predicatesList) {
        List<SelectQuery> pendingAtomics = new ArrayList<>();
        SelectQuery<?> selectQuery = null;
        for (Predicates predicates : predicatesList) {
            //start with the atomic predicate
            if (predicates == null)
                continue;
            if (!predicates.getAtomicPredicates().isEmpty()) {
                for (Predicates.Details definition : predicates.getAtomicPredicates()) {
                    selectQuery = singularSelect(context, definition);
                    pendingAtomics.add(selectQuery);
                    selectQuery = context.selectQuery();
                }
            }
            if (!predicates.getIntersectPredicates().isEmpty()) {
                assembleSetOp(context, pendingAtomics, predicates.getIntersectPredicates(), PredicatesBinder.INTERSECT_EXPRESSION, selectQuery);
                selectQuery = context.selectQuery();
            }
            if (!predicates.getUnionPredicates().isEmpty()) {
                assembleSetOp(context, pendingAtomics, predicates.getUnionPredicates(), PredicatesBinder.UNION_EXPRESSION, selectQuery);
                selectQuery = context.selectQuery();
            }
            if (!predicates.getExceptPredicates().isEmpty()) {
                assembleSetOp(context, pendingAtomics, predicates.getExceptPredicates(), PredicatesBinder.EXCEPT_EXPRESSION, selectQuery);
                selectQuery = context.selectQuery();
            }
        }
        if (!pendingAtomics.isEmpty()) {
            if (pendingAtomics.get(0) != null)
                selectQuery.addFrom(pendingAtomics.get(0));
            else
                return null;
            return selectQuery;
        }
        return null;
    }


    private void factorEnclosing(List<Predicates.Details> predicatesDetails) {
        for (Predicates.Details details : predicatesDetails) {
            //traverse the enclosing containment to prefix the expression
            Containment enclosure = details.getContainedIn();
            while (enclosure != null) {
                if (StringUtils.isNotEmpty(enclosure.getArchetypeId())) {
                    String labelized = labelize(enclosure.getArchetypeId());
                    details.setExpression(labelized + ((labelized.length() > 0) ? INNER_WILDCARD : LEFT_WILDCARD) + details.getExpression());
                } else
                    details.setExpression(LEFT_WILDCARD + details.getExpression());
                enclosure = enclosure.getEnclosingContainment();
            }
        }
    }

    /**
     * Factor containments as an ltree expression
     *
     * @param predicates
     */
    private void factor4LTree(List<Predicates> predicates) {

        for (Predicates predicatesDetail : predicates) {
            if (predicatesDetail != null) {
                factorEnclosing(predicatesDetail.getAtomicPredicates());
                factorEnclosing(predicatesDetail.getUnionPredicates());
                factorEnclosing(predicatesDetail.getIntersectPredicates());
                factorEnclosing(predicatesDetail.getExceptPredicates());
            }
        }

    }

    public static String labelize(String archetypeId) {
        return archetypeId.replaceAll("\\-", "_").replaceAll("\\.", "_");
    }


    public boolean isUseSimpleCompositionContainment() {
        return useSimpleCompositionContainment;
    }
}
