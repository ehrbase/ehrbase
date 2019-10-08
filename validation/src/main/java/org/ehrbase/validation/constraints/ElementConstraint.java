/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openehr.schemas.v1.ARCHETYPECONSTRAINT;

/**
 * Created by christian on 7/15/2016.
 */
public abstract class ElementConstraint implements I_ElementConstraint {

    Logger logger = LogManager.getLogger(ElementConstraint.class);
    protected ARCHETYPECONSTRAINT constraint;
    protected String path;

    public ElementConstraint(String path, ARCHETYPECONSTRAINT constraint) {
        this.path = path;
        this.constraint = constraint;
    }

}
