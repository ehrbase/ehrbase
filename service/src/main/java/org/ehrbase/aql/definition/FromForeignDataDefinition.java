/*
 * Copyright (c) 2016-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.definition;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the parameters of an element in a FROM clause
 * <p>
 * The FROM clause is generally associated to an EHR definition
 * </p>
 *
 * @author Christian Chevalley
 * @since 1.0
 */
public class FromForeignDataDefinition implements I_FromEntityDefinition {

    public enum FDType {
        PERSON,
        AGENT,
        ORGANISATION,
        GROUP
    }

    public static class NodePredicate {

        String field;
        String value;
        String identifier;
        String operator;

        public NodePredicate(String field, String value, String operator) {
            this.field = field;
            this.value = value;
            this.operator = operator;
        }

        public NodePredicate(String identifier) {
            this.identifier = identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getField() {
            return field;
        }

        public String getValue() {
            return value;
        }

        public String getOperator() {
            return operator;
        }

        public String toString() {
            return "ForeignData::" + getIdentifier() + "::" + getIdentifier() + "::" + getValue();
        }
    }

    private final List<NodePredicate> fromNodePredicates = new ArrayList<>();

    private String identifier;

    private final FDType fdType;

    public FromForeignDataDefinition(String type) {
        fdType = FDType.valueOf(type);
    }

    @Override
    public void add(String identifier, String value, String operator) {
        fromNodePredicates.add(new NodePredicate(identifier, value, operator));
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    public FDType getFdType() {
        return fdType;
    }

    /**
     * Assumes ehr_id/value has been given
     *
     * @return the string representation of FROM clause
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (NodePredicate predicate : fromNodePredicates) {
            builder.append(predicate).append(" ");
        }
        return builder.toString();
    }

    public List<NodePredicate> getFDPredicates() {
        return fromNodePredicates;
    }
}
