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
public class FromEhrDefinition implements I_FromEntityDefinition {

    public static class EhrPredicate {
        String field;
        String value;
        String identifier;
        String operator;

        public EhrPredicate(String field, String value, String operator) {
            this.field = field;
            this.value = value;
            this.operator = operator;
        }

        public EhrPredicate(String identifier) {
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
            return "EHR::" + getIdentifier() + "::" + getIdentifier() + "::" + getValue();
        }

        public boolean isVoid() {
            return field == null && value == null && operator == null;
        }
    }

    private boolean isEHR = false;

    private String identifier;

    private final List<EhrPredicate> fromEhrPredicates = new ArrayList<>();

    @Override
    public void add(String identifier, String value, String operator) {
        fromEhrPredicates.add(new EhrPredicate(identifier, value, operator));
    }

    public void setIsEHR(boolean isEHR) {
        this.isEHR = isEHR;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public boolean isEHR() {
        return isEHR;
    }

    public String getIdentifier() {
        return identifier;
    }

    /**
     * Assumes ehr_id/value has been given.
     *
     * @return the string representation of FROM clause
     */
    public String toString() {

        StringBuilder builder = new StringBuilder();

        for (EhrPredicate predicate : fromEhrPredicates) {
            builder.append(predicate).append(" ");
        }
        return builder.toString();
    }

    public List<EhrPredicate> getEhrPredicates() {
        return fromEhrPredicates;
    }
}
