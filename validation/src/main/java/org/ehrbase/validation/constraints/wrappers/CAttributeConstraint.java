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

package org.ehrbase.validation.constraints.wrappers;

import org.ehrbase.validation.constraints.ElementConstraint;
import com.nedap.archie.rm.datavalues.DataValue;
import org.openehr.schemas.v1.ARCHETYPECONSTRAINT;
import org.openehr.schemas.v1.CATTRIBUTE;

/**
 * Created by christian on 7/15/2016.
 */
public class CAttributeConstraint extends ElementConstraint {


    private final String rmAttributeName;

    public CAttributeConstraint(String path, ARCHETYPECONSTRAINT constraint) {
        super(path, constraint);
        this.rmAttributeName = ((CATTRIBUTE) constraint).getRmAttributeName();
    }

    @Override
    public Boolean validate(DataValue value) {
        return false;
    }
}
