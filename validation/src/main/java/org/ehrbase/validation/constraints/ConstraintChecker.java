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


import org.ehrbase.validation.Cardinality;
import org.ehrbase.validation.Message;
import org.ehrbase.validation.constraints.hardwired.CHistory;
import org.ehrbase.validation.constraints.util.LocatableHelper;
import org.ehrbase.validation.constraints.wrappers.CArchetypeConstraint;
import org.ehrbase.validation.constraints.wrappers.ValidationException;
import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datastructures.Element;
import com.nedap.archie.rm.datastructures.History;
import com.nedap.archie.rm.datastructures.ItemStructure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Map;

/**
 * Mostly here to avoid cyclic dependencies
 * Created by christian on 8/16/2016.
 */
public class ConstraintChecker {

    private boolean lenient;
    private ConstraintMapper constraintMapper;
    private Locatable locatable;
    private Cardinality cardinality;

    private static Logger log = LogManager.getLogger(ConstraintChecker.class);

    public ConstraintChecker(Boolean lenient, Composition composition, ConstraintMapper constraintMapper){
        this.lenient = lenient;
        this.locatable = composition;
        this.constraintMapper = constraintMapper;
        cardinality = new Cardinality(constraintMapper, locatable, lenient);
    }

    public ConstraintChecker(Boolean lenient, ItemStructure structure, ConstraintMapper constraintMapper){
        this.lenient = lenient;
        this.locatable = structure;
        this.constraintMapper = constraintMapper;
        cardinality = new Cardinality(constraintMapper, locatable, lenient);
    }

    private void validateElement(String path, Element referenceElement) throws IllegalArgumentException {

        if (lenient) return;

        ConstraintMapper.ConstraintItem constraint = constraintMapper.getConstraintItem(LocatableHelper.siblingPath(path));
        if (constraint == null){
            String tentativePath = LocatableHelper.simplifyPath(path);
            Object tentativeElement = locatable.itemAtPath(tentativePath);
            if (tentativeElement == null)
                log.debug("No constraint matching element (node could not be identified):"+tentativePath);
            else {
                //we should have an ElementWrapper here...
                if (tentativeElement instanceof Element) {
                    constraint = constraintMapper.getConstraintItem(tentativePath);
                    if (constraint == null)
                        log.debug("No constraint matching element:" + tentativeElement);
                    else {
                        if (constraint instanceof OptConstraintMapper.OptConstraintItem) {

                            new CArchetypeConstraint(constraintMapper.getLocalTerminologyLookup()).validate(constraint.getPath(), referenceElement, ((OptConstraintMapper.OptConstraintItem) constraint).getConstraint());
                        }
                    }
                }
                else
                    log.debug("identified node is not an Element..."+tentativeElement);
            }

        }
        else {
            if (constraint instanceof OptConstraintMapper.OptConstraintItem) {
                new CArchetypeConstraint(constraintMapper.getLocalTerminologyLookup()).validate(constraint.getPath(), referenceElement, ((OptConstraintMapper.OptConstraintItem) constraint).getConstraint());
            }
        }

    }

    private void validateItem(String path, Object item) throws IllegalArgumentException {
        if (lenient || item == null) return;

        if (item instanceof History)
            new CHistory(constraintMapper).validate(LocatableHelper.simplifyPath(path), item);
        else if (item instanceof Element)
            validateElement(path, (Element) item);
        else
            throw new IllegalStateException("Unhandled specific data type:"+item);
    }

    private String validateElements() throws IllegalArgumentException {
        if (lenient) return "";

        if (constraintMapper == null) return "";

        StringBuilder validationException = new StringBuilder();

        Iterator<Map.Entry<String, ConstraintMapper.ConstraintItem>> iterator = constraintMapper.getElementConstraintIterator();
        int count = 0;
        while (iterator.hasNext()) {
            count++;
            //check Cardinality
            Map.Entry<String, ConstraintMapper.ConstraintItem> watch = iterator.next();
            String path = watch.getKey();

            for (Object pathItem : locatable.itemsAtPath(path))
                if (pathItem instanceof Locatable) {
                    Locatable item = (Locatable) pathItem;

                    //if null, it has not be assigned potentially (example, unassigned protocol)
                    ConstraintMapper.CardinalityItem cardinalityItem = constraintMapper.getCardinalityList().get(path);

                    //get the cardinality if specified
                    if (cardinalityItem != null)
                        cardinality.check(item, path, cardinalityItem);

                    //validate this element
                    try {
                        if (item instanceof Element && !isNilElement((Element) item))
                            validateItem(path, item);
                    } catch (Exception e) {
                        validationException.append(new Message().encode(path, e.getMessage(), "")).append("\n");
                    }
                }
        }

        log.debug("Validated "+count+" elements");
        return validationException.toString();
    }

    public void validate() throws IllegalArgumentException {
        StringBuilder exceptions = new StringBuilder();
        exceptions.append(validateElements());
        exceptions.append(cardinality.validate());

        if (exceptions.length() > 0)
            ValidationException.raise("", exceptions.toString(),"");
        else
            log.debug("Locatable successfully validated");
    }

    private boolean isNilElement(Element element){
        return (element.getNullFlavour() == null && element.getValue() == null);
    }
}
