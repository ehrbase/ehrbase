/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

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

package org.ehrbase.aql.containment;

import org.ehrbase.aql.definition.FromEhrDefinition;
import org.ehrbase.aql.definition.FromForeignDataDefinition;
import org.ehrbase.aql.sql.queryImpl.JsonbEntryQuery;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Map identifiers in an AQL expression with their container and query strategy.
 * <p>
 * The query strategy specifies whether an identifier is associated to an ARCHETYPED structure
 * or is bound to "static" data (such as a composition composer)
 * </p>
 * Created by christian on 4/12/2016.
 */
public class IdentifierMapper {

    public class Mapper {
        private Class queryStrategy; //specifies the constructor to use depending on the identifier
        private Object container;

        public Mapper(Object container) {
            this.container = container;
        }

        public Class getQueryStrategy() {
            return queryStrategy;
        }

        public Object getContainer() {
            return container;
        }

        public void setQueryStrategy(Class queryStrategy) {
            this.queryStrategy = queryStrategy;
        }

        public void setContainer(Object container) {
            this.container = container;
        }
    }

    private Map<String, Mapper> mapper = new HashMap<>();

    public void add(Object definition) {
        if (definition instanceof Containment) {
            Containment containment = (Containment) definition;
            if (mapper.containsKey(containment.getSymbol()))
                throw new IllegalArgumentException("Symbol already exists:" + containment.getSymbol());
            Mapper def = new Mapper(containment);
            mapper.put(containment.getSymbol(), def);
        } else if (definition instanceof FromEhrDefinition.EhrPredicate) {
            FromEhrDefinition.EhrPredicate ehrPredicate = (FromEhrDefinition.EhrPredicate) definition;
            if (mapper.containsKey(ehrPredicate.getField()))
                throw new IllegalArgumentException("Symbol already exists:" + ehrPredicate.getField());
            Mapper def = new Mapper(ehrPredicate);
            mapper.put(ehrPredicate.getIdentifier(), def);
        } else if (definition instanceof FromForeignDataDefinition.NodePredicate) {
            FromForeignDataDefinition.NodePredicate nodePredicate = (FromForeignDataDefinition.NodePredicate) definition;
            if (mapper.containsKey(nodePredicate.getField()))
                throw new IllegalArgumentException("Symbol already exists:" + nodePredicate.getField());
            Mapper def = new Mapper(nodePredicate);
            mapper.put(nodePredicate.getIdentifier(), def);
        }
    }

    private Mapper get(String symbol) {
        Mapper mapped = mapper.get(symbol);
        return mapped;
    }

    public Object getContainer(String symbol) {
        Mapper mapped = mapper.get(symbol);
        if (mapped == null)
            return null;
        return mapped.getContainer();
    }

    public FromEhrDefinition.EhrPredicate getEhrContainer() {
        for (Map.Entry<String, Mapper> containment : mapper.entrySet()) {
            if (containment.getValue().getContainer() instanceof FromEhrDefinition.EhrPredicate && !((FromEhrDefinition.EhrPredicate) containment.getValue().getContainer()).isVoid())
                return (FromEhrDefinition.EhrPredicate) containment.getValue().getContainer();
        }
        return null;
    }

    public boolean hasPathExpression() {
        for (Map.Entry<String, Mapper> containment : mapper.entrySet()) {
            if (containment.getValue().getQueryStrategy() != null && containment.getValue().getQueryStrategy().equals(JsonbEntryQuery.class))
                return true;
        }
        return false;

    }


    public boolean hasEhrContainer() {
        for (Map.Entry<String, Mapper> containment : mapper.entrySet()) {
            if (containment.getValue().getContainer() instanceof FromEhrDefinition.EhrPredicate)
                return true;
        }
        return false;
    }

    public Class getQueryStrategy(String symbol) {
        Mapper mapped = mapper.get(symbol);
        if (mapped == null)
            return null;
        return mapped.getQueryStrategy();
    }

    public String getPath(String symbol) {
        Mapper definition = mapper.get(symbol);
        if (definition == null)
            throw new IllegalArgumentException("Could not resolve identifier:" + symbol);

        Object containment = definition.getContainer();
        if (containment instanceof Containment) {
            if (containment != null) {
                return ((Containment) containment).getPath();
            }
        }
        return null;
    }

    public void setPath(String symbol, String path) {
        Mapper definition = mapper.get(symbol);
        Object containment = definition.getContainer();
        if (containment instanceof Containment) {
            if (containment != null) {
                ((Containment) containment).setPath(path);
            }
        }
    }

    public void setQueryStrategy(String symbol, Class queryImplementation) {
        Mapper definition = mapper.get(symbol);
        definition.setQueryStrategy(queryImplementation);
    }

    public String getArchetypeId(String symbol) {
        Mapper definition = mapper.get(symbol);
        Object containment = definition.getContainer();
        if (containment instanceof Containment) {

            if (containment != null) {
                return ((Containment) containment).getArchetypeId();
            }
        }
        return null;
    }

    public String getClassName(String symbol) {
        Mapper definition = mapper.get(symbol);
        if (definition == null)
            throw new IllegalArgumentException("Identifier is not defined in FROM/CONTAIN clause:" + symbol);

        Object containment = definition.getContainer();
        if (containment instanceof Containment) {
            if (containment != null) {
                return ((Containment) containment).getClassName();
            }
        } else if (containment instanceof FromEhrDefinition.EhrPredicate)
            return "EHR";

        return null;
    }

    public Set<String> identifiers() {
        return mapper.keySet();
    }

    public String dump() {

        StringBuffer sb = new StringBuffer();

        for (Map.Entry<String, Mapper> objectEntry : mapper.entrySet()) {
            sb.append(objectEntry.getKey() + "::" + objectEntry.getValue().toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
