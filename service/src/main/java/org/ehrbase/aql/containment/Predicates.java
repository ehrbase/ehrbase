/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Container defining list of predicates with their associated operator (if any).
 * Created by christian on 4/23/2016.
 */
public class Predicates {

    private ContainmentSet containmentSet;

    public static class Details {
        private String expression;
        private Containment containedIn;
        private ContainmentSet inSet;

        public Details(String expression, ContainmentSet inSet, Containment enclosing) {
            this.expression = expression;
            this.containedIn = enclosing;
            this.inSet = inSet;
        }

        public boolean isVoid() {
            return (getExpression() == null || getExpression().length() == 0);
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        public Containment getContainedIn() {
            return containedIn;
        }

        public void setContainedIn(Containment containedIn) {
            this.containedIn = containedIn;
        }

        public ContainmentSet getInSet() {
            return inSet;
        }

        public void setInSet(ContainmentSet inSet) {
            this.inSet = inSet;
        }
    }

    public Predicates(ContainmentSet containmentSet) {
        this.containmentSet = containmentSet;
    }

    private final List<Details> intersectPredicates = new ArrayList<>();
    private final List<Details> exceptPredicates = new ArrayList<>();
    private final List<Details> unionPredicates = new ArrayList<>();
    private final List<Details> atomicPredicates = new ArrayList<>();

    public ContainmentSet getContainmentSet() {
        return containmentSet;
    }

    public List<Details> getIntersectPredicates() {
        return intersectPredicates;
    }

    public List<Details> getExceptPredicates() {
        return exceptPredicates;
    }

    public List<Details> getUnionPredicates() {
        return unionPredicates;
    }

    public List<Details> getAtomicPredicates() {
        return atomicPredicates;
    }
}
