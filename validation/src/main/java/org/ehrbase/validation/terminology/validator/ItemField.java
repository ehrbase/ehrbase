/*
 * Copyright (c) 2020 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
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
package org.ehrbase.validation.terminology.validator;

import com.nedap.archie.rm.archetyped.Pathable;
import org.apache.commons.lang3.StringUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

/**
 * Simple utility class to materialize an object in a Pathable using a getter. This is used f.e. to
 * retrieve an attribute ('getTime').
 * @param <T> The expected class for the materialize object: RMObject, Pathable
 */
public class ItemField<T> {

    private Pathable pathable;

    public ItemField(Pathable pathable) {
        this.pathable = pathable;
    }

    public T objectForField(Field field) throws IllegalArgumentException, InternalError {
        String getterName = "get"+ StringUtils.capitalize(field.getName());
        MethodHandle methodHandle;
        try {
            methodHandle = MethodHandles.lookup().findVirtual(pathable.getClass(), getterName, MethodType.methodType(field.getType()));
        }
        catch (NoSuchMethodException | IllegalAccessException e){
            throw new InternalError("Internal error:"+e.getMessage());
        }
        try {
            Object object = methodHandle.invoke(pathable);
            return (T) object;
        }
        catch (Throwable throwable){
            throw new InternalError("Internal:"+throwable.getMessage());
        }
    }
}
