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

import org.ehrbase.validation.constraints.ConstraintChecker;
import org.ehrbase.validation.constraints.OptConstraint;
import org.ehrbase.validation.constraints.OptConstraintMapper;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datastructures.ItemStructure;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.io.Serializable;

/**
 * Validator
 *
 * Validate a composition or an arbitrary item structure (e.g. Locatable) using
 * constraints defined in a 1.4 operational template.
 *
 * An optional boolean flag can be used to disable validation
 */
public class Validator implements Serializable {

    private boolean lenient = false;
    private OptConstraintMapper optConstraint;

    public Validator(OPERATIONALTEMPLATE operationaltemplate, boolean lenient) {
        this.lenient = lenient;
    }

    public Validator(OPERATIONALTEMPLATE operationaltemplate) throws IllegalArgumentException {
        optConstraint = new OptConstraint().map(operationaltemplate);
    }


    /**
     * Validate a composition
     * @param composition
     * @throws IllegalArgumentException
     */
    public void check(Composition composition) throws IllegalArgumentException {
        new ConstraintChecker(lenient, composition, optConstraint).validate();
    }

    /**
     * Validate an ItemStructure
     * @param itemStructure
     * @throws IllegalArgumentException
     */
    public void check(ItemStructure itemStructure) throws IllegalArgumentException {
        new ConstraintChecker(lenient, itemStructure, optConstraint).validate();
    }

    /**
     * set the lenient flag
     * if true, the validation is disabled.
     * @param lenient
     */
    public void setLenient(boolean lenient) {
        this.lenient = lenient;
    }
}
