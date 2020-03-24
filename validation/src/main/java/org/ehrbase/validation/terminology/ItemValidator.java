/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
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
package org.ehrbase.validation.terminology;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.datavalues.quantity.DvOrdered;
import org.ehrbase.terminology.openehr.TerminologyInterface;
import org.ehrbase.terminology.openehr.implementation.AttributeCodesetMapping;
import org.ehrbase.validation.terminology.validator.I_TerminologyCheck;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;

public class ItemValidator {

    private Map<String, ValidationHandler> validationRegistryList;

    ItemValidator() {
        validationRegistryList = new HashMap<>();
   }

   public ItemValidator add(I_TerminologyCheck validator) throws NoSuchMethodException, IllegalAccessException, InternalError {
      Class rmClass = validator.rmClass();
      if (rmClass == null){
          throw new IllegalStateException("Internal error:"+validator.getClass()+" does not define a matching RM class! (hint: RM_CLASS must be defined in validator class)");
      }
       MethodHandle methodHandle = MethodHandles.lookup().findStatic(validator.getClass(), "check",
               MethodType.methodType(void.class, new Class[]{TerminologyInterface.class, AttributeCodesetMapping.class, String.class, rmClass, String.class}));

       validationRegistryList.put(rmClass.getCanonicalName(), new ValidationHandler(rmClass, methodHandle));

       return this;
   }

   boolean isValidatedRmObjectType(RMObject rmObject){
        if (!validationRegistryList.containsKey(rmObject.getClass().getCanonicalName())) {
            try {
                if (rmObject.getClass().equals(rmObject.getClass().asSubclass(DvOrdered.class))) {
                    return true;
                }
            }
            catch (Exception e){
                return false;
            }
        }
        return true;
   }

    boolean isValidatedRmObjectType(Class aRmObjectClass){
        if (!validationRegistryList.containsKey(aRmObjectClass.getCanonicalName())){
            try {
                aRmObjectClass.asSubclass(DvOrdered.class);
                return true;
            }
            catch (Exception e){
                return false;
            }
        }
        return true;
    }

   private ValidationHandler matchValidator(RMObject rmObject){
        String rmClassName = rmObject.getClass().getCanonicalName();

        return validationRegistryList.get(rmClassName);
   }

    private ValidationHandler matchValidator(Class rmClass){

        return validationRegistryList.get(rmClass.getCanonicalName());
    }

   public void validate(TerminologyInterface terminologyInterface, AttributeCodesetMapping codesetMapping, String fieldName, RMObject rmObject, String language) throws IllegalArgumentException, InternalError {
        if (rmObject == null)
            return;

        ValidationHandler validationHandler = matchValidator(rmObject);

        if (validationHandler == null){
            //check if this rmObject class is a subclass of DvOrdered
            try {
                if (rmObject.getClass().equals(rmObject.getClass().asSubclass(DvOrdered.class))) {
                    validationHandler = matchValidator(DvOrdered.class);
                }
            }
            catch (Exception e){
                return;
            }
        }

       try {
           //invoke validation
           MethodHandle methodHandle = validationHandler.check();
           methodHandle.invoke(terminologyInterface, codesetMapping, fieldName, rmObject, language);
       } catch (Throwable throwable){
           if (throwable instanceof IllegalArgumentException)
               throw new IllegalArgumentException(throwable.getMessage());
           else
                throw new IllegalStateException(throwable.getMessage());
       }

   }
}
