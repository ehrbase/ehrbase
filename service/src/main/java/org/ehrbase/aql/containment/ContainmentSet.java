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

import java.util.List;
import org.apache.commons.collections4.set.ListOrderedSet;

/**
 * Define the set of containments for a CONTAINS clause
 * <p>
 * Containment sets are associated with Set (boolean) operators and their relation with an enclosing set
 * (inclusion). This structure define the set operations required at the query layer implementation. For
 * example, in an SQL context, this will define set operation like: INTERSECT, UNION, EXCEPT.
 * </p>
 * Created by christian on 4/12/2016.
 */
public class ContainmentSet {

    private int serial; // for debugging purpose only
    private Containment enclosing;
    private ContainmentSet parentSet;
    private ListOrderedSet<Object> containmentList = new ListOrderedSet<>();

    public ContainmentSet(int serial, Containment enclosing) {
        this.serial = serial;
        this.enclosing = enclosing;
    }

    public void add(Containment containment) {
        containmentList.add(containment);
    }

    public void addAll(List<Containment> containments) {
        containmentList.addAll(containments);
    }

    public void add(String operator) {
        containmentList.add(new ContainOperator(operator));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(serial + "|");

        if (!containmentList.isEmpty()) {
            boolean comma = false;
            for (Object item : containmentList) {
                if (comma) sb.append(",");

                comma = true;

                if (item instanceof Containment) sb.append(item);
                else if (item instanceof ContainOperator) {
                    sb.append(((ContainOperator) item).getOperator());
                } else if (item instanceof String) {
                    sb.append(item);
                } else sb.append("-- Unhandled Item Type --");
            }
        } else sb.append("--EMPTY SET--");

        if (parentSet != null) sb.append("<<<IN PARENT#" + parentSet.serial);
        else sb.append("<<< ROOT");
        return sb.toString();
    }

    public int size() {
        return containmentList.size();
    }

    public boolean isEmpty() {
        return (containmentList.isEmpty() && enclosing.getSymbol() == null && enclosing.getClassName() == null);
    }

    public void setParentSet(ContainmentSet parentSet) {
        this.parentSet = parentSet;
    }

    public ListOrderedSet<Object> getContainmentList() {
        return containmentList;
    }

    public ContainmentSet getParentSet() {
        return parentSet;
    }

    public Containment getEnclosing() {
        return enclosing;
    }
}
