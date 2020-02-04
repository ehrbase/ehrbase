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

import org.ehrbase.validation.constraints.ConstraintOccurrences;
import org.ehrbase.validation.constraints.ElementConstraint;
import com.nedap.archie.rm.datavalues.DataValue;
import org.openehr.schemas.v1.ARCHETYPECONSTRAINT;
import org.openehr.schemas.v1.COBJECT;

/**
 * Validate a generic object constraint
 *
 * Created by christian on 7/15/2016.
 */
public class CObjectConstraint extends ElementConstraint {

    private final String rmTypeName;
    private final ConstraintOccurrences constraintOccurrences;

    public CObjectConstraint(String path, ARCHETYPECONSTRAINT constraint) {
        super(path, constraint);
        this.rmTypeName = ((COBJECT) constraint).getRmTypeName();
        this.constraintOccurrences = new ConstraintOccurrences(((COBJECT) constraint).getOccurrences());
    }

    @Override
    public Boolean validate(DataValue value) throws IllegalArgumentException {

        //check occurences
        //element is not set but at least one occurence is required
//        if (!wrapper.dirtyBitSet() && constraintOccurrences.lower > 0)
//            return false;

        //TODO: check multiple occurences boundary
        //
//        if (constraint instanceof CPRIMITIVE){
//
//        }
//        else if (constraint instanceof CDOMAINTYPE){
//            String rmTypeName = ((CDOMAINTYPE) constraint).getRmTypeName();
//
//            Class contraintTypeClass = Class.forName(constraint.schemaType().getFullJavaName());
//
//            String domainType = Utils.snakeToCamel(rmTypeName);
//            if (Utils.isConstraintImplemented(domainType)){
//                Class constraintImpl = VBeanUtil.findConstraintClass(domainType);
//                //invoke the static validator using attributes
//                Method validator = constraintImpl.getDeclaredMethod("validate", DataValue.class, contraintTypeClass);
//                return (Boolean)validator.invoke(null, value, constraint);
//            }
//
//        }
//        else if (constraint instanceof CCOMPLEXOBJECT){
//            String domainType = Utils.snakeToCamel(((CCOMPLEXOBJECT) constraint).getRmTypeName());
//            //get the corresponding class validation method
//            if (VBeanUtil.isConstraintImplemented(domainType)){
//                Class constraintImpl = VBeanUtil.findConstraintClass(domainType);
//                //invoke the static validator using attributes
//                Method validator = constraintImpl.getDeclaredMethod("validate", DataValue.class, CATTRIBUTE[].class);
//                return (Boolean)validator.invoke(null, value, ((CCOMPLEXOBJECT)constraint).getAttributesArray());
//            }
//
//        }

        return true;
    }

//    private Class findImplementingConstraintClass(String rmTypeName) throws ClassNotFoundException {
////        String constraintTypeName = ("C_"+rmTypeName).replaceAll("_", "");
//        Class contraintTypeClass = Class.forName(constraint.schemaType().getFullJavaImplName().split(".impl")[0]+"."+constraintTypeName);
//        return contraintTypeClass;
//    }

//    private Class findImplementingConstraintClass(String rmTypeName) throws ClassNotFoundException {
//        String constraintTypeName = ("C_"+rmTypeName).replaceAll("_", "");
//        Class contraintTypeClass = Class.forName(constraint.schemaType().getFullJavaImplName().split(".impl")[0]+"."+constraintTypeName);
//        return contraintTypeClass;
//    }
}
