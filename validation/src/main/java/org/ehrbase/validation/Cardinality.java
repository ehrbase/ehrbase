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

package org.ehrbase.validation;

import org.ehrbase.validation.constraints.ConstraintMapper;
import org.ehrbase.validation.constraints.ConstraintOccurrences;
import org.ehrbase.validation.constraints.NodeCounter;
import org.ehrbase.validation.constraints.util.LocatableHelper;
import org.ehrbase.validation.constraints.wrappers.IntervalComparator;
import org.ehrbase.validation.constraints.wrappers.ValidationException;
import com.nedap.archie.rm.archetyped.Locatable;

import java.util.List;
import java.util.Map;

public class Cardinality {

    private ConstraintMapper constraintMapper;
    private Locatable locatable;
    private boolean lenient;

    public Cardinality(ConstraintMapper constraintMapper, Locatable locatable, Boolean lenient) {
        this.constraintMapper = constraintMapper;
        this.locatable = locatable;
        this.lenient = lenient;
    }

    /**
     * check the cardinality
     * @param structure a locatable (e.g. node)
     * @param path the path of this node
     * @param cardinalityItem the cardinality constraints
     */
    public void check(Locatable structure, String path, ConstraintMapper.CardinalityItem cardinalityItem){
        Object locatable = structure.itemAtPath(path);

        NodeCounter counter =  new NodeCounter();
        counter.count(locatable);
        Integer childOccurrence = counter.getCount();

        try {
            IntervalComparator.isWithinBoundaries(childOccurrence, cardinalityItem.getCardinality());
        } catch (Exception e){
            //check if this is optional (occurence)
            //TODO: check for a transitive optional existence in the path
            if (childOccurrence == 0 && !(cardinalityItem.getExistence().isOptional())){
                //check if a transitive optionality is specified

                if (!isTransitivelyOptional(path))
                    ValidationException.raise(path, "Cardinality not matched, expected:" + IntervalComparator.toString(cardinalityItem.getCardinality().asInterval()) + ", actual:" + childOccurrence, "CAR01");
            }
            else if (childOccurrence > 0 || !(cardinalityItem.getExistence().isOptional())) {
                ValidationException.raise(path, "Cardinality not matched, expected:" + IntervalComparator.toString(cardinalityItem.getCardinality().asInterval()) + ", actual:" + childOccurrence, "CAR01");
            }
        }
    }

    /**
     * check if the parent container is *really* optional, that is its lower cardinality is 0 and it is not used...
     * NB. There are many discussions about this cardinality problem, presumably fixed with ADL2 (?)
     * @param path
     * @return
     */
    public boolean isTransitivelyOptional(String path) {

//        return false;

        //traverse upward the path and check if a parent node is optional
        List<String> pathSegments = LocatableHelper.dividePathIntoSegments(path);

        for (int i = pathSegments.size() - 1; i >= 0; i--){
            String checkPath = "/"+String.join("/", pathSegments.subList(0, i));
            if (constraintMapper.getOccurrencesMap().containsKey(checkPath)){
                ConstraintOccurrences occurrences = constraintMapper.getOccurrencesMap().get(checkPath);
                if (occurrences.isOptional()) {
                    //check if this optional node contains any datavalue element in its children
                    NodeCounter counter =  new NodeCounter();
                    counter.count(locatable.itemsAtPath(checkPath));
                    Integer elementCount = counter.getCount();
                    if (elementCount > 0)
                        return false;
                    else
                        return true;
                }
            }
        }

        // we have tried all parents node
        return true;
    }

    /**
     * Validate the cardinality of an item, that is whether the item is within the cardinality boundaries
     * @return a string containing the validation errors if any
     */
    public String validate() {

        StringBuffer exceptions = new StringBuffer();

        if (lenient) return "";

        if (constraintMapper == null) return "";

        int valcount = 0;

        for (Map.Entry<String, ConstraintMapper.CardinalityItem> entry: constraintMapper.getCardinalityList().entrySet()){
            valcount++;
            //get the corresponding node
            List<Object> item = locatable.itemsAtPath(entry.getKey());
//
//            NodeCounter counter =  new NodeCounter();
//            counter.count(item);
            Integer childOccurrence = item.size();

            try {
                IntervalComparator.isWithinBoundaries(childOccurrence, entry.getValue().getCardinality());
            } catch (Exception e){
                //check if this is optional (occurence)
                if (!isTransitivelyOptional(entry.getKey())) {
                    if (childOccurrence == 0 && !(entry.getValue().getExistence().isOptional())) {
                        exceptions.append(new Message().encode(entry.getKey(), "Cardinality not matched, expected:" + IntervalComparator.toString(entry.getValue().getCardinality().asInterval()) + ", actual:" + childOccurrence, "CAR01"));
                    } else if (childOccurrence > 0 || !(entry.getValue().getExistence().isOptional())) {
                        exceptions.append(new Message().encode(entry.getKey(), "Cardinality not matched, expected:" + IntervalComparator.toString(entry.getValue().getCardinality().asInterval()) + ", actual:" + childOccurrence, "CAR01"));
                    }
                }
            }
        }

//        log.debug("Validated "+valcount+" cardinality constraints");
        return exceptions.toString();
    }
}
