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

package org.ehrbase.validation.constraints;

import org.ehrbase.validation.constraints.wrappers.IntervalComparator;
import org.ehrbase.validation.constraints.wrappers.ValidationException;
import com.nedap.archie.base.MultiplicityInterval;
import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.composition.Composition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.*;

/**
 * Created by christian on 8/9/2016.
 */
public abstract class ConstraintMapper implements Serializable {

    Logger logger = LogManager.getLogger(ConstraintMapper.class);

    protected Map<String, Map<String, String>> localTerminologyLookup;
    private boolean lenient;

    public class OccurrenceItem implements Serializable {
        private ConstraintOccurrences constraintOccurrences;
        private MultiplicityInterval existence;

        OccurrenceItem(ConstraintOccurrences constraintOccurrences, MultiplicityInterval existence) {
            this.constraintOccurrences = constraintOccurrences;
            this.existence = existence;
        }

        public ConstraintOccurrences getConstraintOccurrences() {
            return constraintOccurrences;
        }

        public MultiplicityInterval getExistence() {
            return existence;
        }
    }

    public class CardinalityItem implements Serializable {
        private ConstraintOccurrences existence;
        private ConstraintOccurrences cardinality;

        CardinalityItem(ConstraintOccurrences existence, ConstraintOccurrences cardinality) {
            this.existence = existence;
            this.cardinality = cardinality;
        }

        public ConstraintOccurrences getExistence() {
            return existence;
        }

        public ConstraintOccurrences getCardinality() {
            return cardinality;
        }
    }

    Map<String, OccurrenceItem> watchList = new HashMap<>(); //required nodes
    Set<String> validNodeList = new HashSet<>(); //valid nodes
    Map<String, CardinalityItem> cardinalityList = new HashMap<>(); //valid nodes
    Map<String, ConstraintOccurrences> occurrencesMap = new HashMap<>(); //transitive list of occurrences

    public Map<String, CardinalityItem> getCardinalityList() {
        return cardinalityList;
    }

    public abstract class ConstraintItem implements Serializable {
        private String path;

        ConstraintItem(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }
    }

    Map<String, ConstraintItem> elementConstraintMap = new HashMap<>();

    public ConstraintItem getConstraintItem(String key) {
        return elementConstraintMap.get(key);
    }

    public Map<String, Map<String, String>> getLocalTerminologyLookup() {
        return localTerminologyLookup;
    }

    public boolean isValidNode(String path) {
        return validNodeList.contains(path);
    }

    public void updateWatchList(String path) {
        //retrieve path in watch list and remove it
        watchList.remove(path);
    }

    public void _validateCardinality(Composition composition) throws IllegalArgumentException {
        for (Map.Entry<String, CardinalityItem> entry : cardinalityList.entrySet()) {
            //get the corresponding node
            Object locatable = composition.itemAtPath(entry.getKey());

            int childOccurrence;

            //TODO: traverse the locatable to find out what is actually existing (e.g. dirtyBit set!)
            if (locatable instanceof List) {
                childOccurrence = ((List) locatable).size();
            } else
                childOccurrence = 1;
            try {
                IntervalComparator.isWithinBoundaries(childOccurrence, entry.getValue().getCardinality());
            } catch (Exception e) {
                //check if this is optional (occurence)
                if (!(entry.getValue().getExistence().isOptional())) {
                    ValidationException.raise(entry.getKey(), "Cardinality not matched, expected:" + IntervalComparator.toString(entry.getValue().getCardinality().asInterval()) + ", actual:" + childOccurrence, "CAR01");
                }
            }
        }
    }

    public void validateWatchList(Composition composition) {

        for (Map.Entry<String, OccurrenceItem> watch : watchList.entrySet()) {
            String path = watch.getKey();

            Locatable item = (Locatable) composition.itemAtPath(path);

            if (item == null) {
                ValidationException.raise(path, "Mandatory element not found", "ELM01");
            }
//            if (item instanceof ElementWrapper){
//
//            }
        }
    }

    Iterator<Map.Entry<String, ConstraintItem>> getElementConstraintIterator() {
        return elementConstraintMap.entrySet().iterator();
    }


    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }

    public Map<String, ConstraintOccurrences> getOccurrencesMap() {
        return occurrencesMap;
    }
}
