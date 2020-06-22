/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
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

import org.ehrbase.aql.containment.ContainOperator;
import org.ehrbase.aql.containment.Containment;
import org.ehrbase.aql.containment.ContainmentSet;
import org.ehrbase.aql.containment.Predicates;
import org.ehrbase.aql.containment.Predicates.Details;
import org.apache.commons.collections4.set.ListOrderedSet;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

class PredicatesBinder {
    private Predicates predicates;
    static final String EXCEPT_EXPRESSION = " EXCEPT ";
    static final String INTERSECT_EXPRESSION = " INTERSECT ";
    static final String UNION_EXPRESSION = " UNION ";

    PredicatesBinder() {
    }

    private Object lookAhead(ListOrderedSet<Object> containmentList, int cursor) {
        if (cursor + 1 >= containmentList.size())
            return null;
        return containmentList.get(cursor + 1);
    }

    private boolean isOperator(Object object) {
        return object instanceof ContainOperator;
    }

    private boolean isContainment(Object object) {
        return object instanceof Containment;
    }

    private boolean isLastItem(ListOrderedSet<Object> containmentList, int cursor) {
        return cursor + 1 >= containmentList.size();
    }

    private int resolveUnallocated(Deque<ContainOperator> operatorStack) {

        //check what's in stack
        if (!predicates.getAtomicPredicates().isEmpty() && !operatorStack.isEmpty()) {
            ContainOperator operator = operatorStack.pop();
            int cursor = predicates.getAtomicPredicates().size() - 1;
            switch (operator.getOperator()) {
                case "AND":
                    Details details = predicates.getAtomicPredicates().get(cursor);
//                        Predicates.Details details = predicates.new Details(item, null);
                    predicates.getIntersectPredicates().add(details);
                    predicates.getAtomicPredicates().remove(cursor);
                    break;
                case "OR":
                    details = predicates.getAtomicPredicates().get(cursor);
//                        Predicates.Details details = predicates.new Details(item, null);
                    predicates.getUnionPredicates().add(details);
                    predicates.getAtomicPredicates().remove(cursor);
                    break;
                case "XOR":
//                        item = predicates.atomicPredicates.get(indexLast).expression;
                    details = predicates.getAtomicPredicates().get(cursor);
                    predicates.getExceptPredicates().add(details);
                    predicates.getAtomicPredicates().remove(cursor);
                    break;

            }
        }

        return predicates.getAtomicPredicates().size();
    }

    private StringBuilder buildBooleanExpression(StringBuilder containedPredicateLtree, String operator, List<Details> predicatesList, ContainmentSet inSet, Containment enclosing) {
        if (predicates.getAtomicPredicates().size() == 2) {
            containedPredicateLtree.append(predicates.getAtomicPredicates().get(0));
            containedPredicateLtree.append(operator);
            containedPredicateLtree.append(predicates.getAtomicPredicates().get(1));
            predicates.getAtomicPredicates().remove(1);
            predicates.getAtomicPredicates().remove(0);
            predicates.getAtomicPredicates().add(new Details(containedPredicateLtree.toString(), inSet, enclosing));
        } else if (predicates.getAtomicPredicates().size() == 1) {
            containedPredicateLtree.append(predicates.getAtomicPredicates().get(0));
            containedPredicateLtree.append(operator);
            predicates.getAtomicPredicates().remove(0);
            predicates.getAtomicPredicates().add(new Details(containedPredicateLtree.toString(), inSet, enclosing));
        } else if (predicates.getAtomicPredicates().isEmpty()) {
//            containedPredicateLtree.append(operator);
            predicatesList.add(new Details(containedPredicateLtree.toString(), inSet, enclosing));
        }

        return containedPredicateLtree;
    }

    Predicates bind(ContainmentSet containmentSet) {
        if (containmentSet == null)
            return null;
        this.predicates = new Predicates(containmentSet);
        Deque<ContainOperator> operatorStack = new ArrayDeque<>();

        StringBuilder containedPredicateLtree = new StringBuilder();
        for (int i = 0; i < containmentSet.getContainmentList().size(); i++) {
            Object item = containmentSet.getContainmentList().get(i);
            if (isContainment(item)) {
                Containment containmentDefinition = ((Containment) item);
                String archetypeId = containmentDefinition.getArchetypeId();
                if (archetypeId.length() != 0) {
                    containedPredicateLtree.append(ContainBinder.labelize(archetypeId));
                } else { //use the classname
                    containedPredicateLtree.append(containmentDefinition.getClassName() + "%");
                }
                if (!isLastItem(containmentSet.getContainmentList(), i) && !isOperator(lookAhead(containmentSet.getContainmentList(), i))) {
                    containedPredicateLtree.append(ContainBinder.INNER_WILDCARD);
                }
//                else if (isLastItem(containmentSet.getContainmentList(), i) && i == 0) {//single item
//                    containedPredicateLtree.append(RIGHT_WILDCARD);
//                }
            } else if (isOperator(item)) {
                ContainOperator operator = (ContainOperator) item;
                switch (operator.getOperator()) {
                    case "OR":
//                        containedPredicateLtree.append("|");
//                        break;
                        if (containedPredicateLtree.length() == 0) { //promote the atomicPredicates to intersectPredicates, it is a AND on two nested groups
                            containedPredicateLtree = buildBooleanExpression(containedPredicateLtree, EXCEPT_EXPRESSION, predicates.getUnionPredicates(), containmentSet.getParentSet(), containmentSet.getEnclosing());
                        } else {
                            predicates.getUnionPredicates().add(new Details(containedPredicateLtree.toString(), containmentSet.getParentSet(), containmentSet.getEnclosing()));
                            operatorStack.push(operator);
                        }
                        containedPredicateLtree = new StringBuilder();
                        break;
                    case "XOR":
                        if (containedPredicateLtree.length() == 0) { //promote the atomicPredicates to intersectPredicates, it is a AND on two nested groups
                            containedPredicateLtree = buildBooleanExpression(containedPredicateLtree, EXCEPT_EXPRESSION, predicates.getExceptPredicates(), containmentSet.getParentSet(), containmentSet.getEnclosing());
                        } else {
                            predicates.getExceptPredicates().add(new Details(containedPredicateLtree.toString(), containmentSet.getParentSet(), containmentSet.getEnclosing()));
                            operatorStack.push(operator);
                        }
                        containedPredicateLtree = new StringBuilder();
                        break;
                    case "AND":
                        if (containedPredicateLtree.length() == 0) { //promote the atomicPredicates to intersectPredicates, it is a AND on two nested groups
                            containedPredicateLtree = buildBooleanExpression(containedPredicateLtree, INTERSECT_EXPRESSION, predicates.getIntersectPredicates(), containmentSet.getParentSet(), containmentSet.getEnclosing());
                        } else {
                            predicates.getAtomicPredicates().add(new Details(containedPredicateLtree.toString(), containmentSet.getParentSet(), containmentSet.getEnclosing()));
                            operatorStack.push(operator);
                        }

                        containedPredicateLtree = new StringBuilder(); //reset the string buffer
//                            logger.warn("Operator is not supported:"+operator);
                        break;
                }
            } else {
                //unhandled object
            }
            int size = resolveUnallocated(operatorStack);
//            while (size != 0)
//                size = resolveUnallocated(operatorStack);
        }
//            containedClause.append(containedPredicateLtree);
        if (containedPredicateLtree.length() > 0)
            predicates.getAtomicPredicates().add(new Details(containedPredicateLtree.toString(), containmentSet.getParentSet(), containmentSet.getEnclosing()));

        return predicates;

    }
}