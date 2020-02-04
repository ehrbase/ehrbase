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

package org.ehrbase.validation.constraints.wrappers;

import org.openehr.schemas.v1.ARCHETYPECONSTRAINT;
import org.openehr.schemas.v1.CATTRIBUTE;
import org.openehr.schemas.v1.COBJECT;

import java.util.Map;

/**
 * Created by christian on 7/23/2016.
 */
public class CArchetypeConstraint extends CConstraint implements I_CArchetypeConstraintValidate {

    public CArchetypeConstraint(Map<String, Map<String, String>> localTerminologyLookup) {
        super(localTerminologyLookup);
    }

    @Override
    public void validate(String path, Object aValue, ARCHETYPECONSTRAINT archetypeconstraint) throws IllegalArgumentException {
        if (archetypeconstraint instanceof COBJECT)
            new CObject(localTerminologyLookup).validate(path, aValue, archetypeconstraint);
        else if (archetypeconstraint instanceof CATTRIBUTE)
            new CAttribute(localTerminologyLookup).validate(path, aValue, archetypeconstraint);
        else
            throw new IllegalArgumentException("INTERNAL: could not resolve archetypeconstraint type:" + archetypeconstraint);
    }
}
