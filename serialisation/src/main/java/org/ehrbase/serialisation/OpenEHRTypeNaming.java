/*
 * Modifications copyright (C) 2019 Vitasystems GmbH and Hannover Medical School.

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Nedap Healthcare
 * This file is part of Project Archie
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
package org.ehrbase.serialisation;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.nedap.archie.base.OpenEHRBase;
import com.nedap.archie.rminfo.ArchieAOMInfoLookup;
import com.nedap.archie.rminfo.ArchieRMInfoLookup;
import com.nedap.archie.rminfo.ModelInfoLookup;
import com.nedap.archie.rminfo.RMTypeInfo;

import java.io.IOException;

/**
 * Class that handles naming of Archie RM and AOM objects for use in Jackson.
 * <p>
 * The AOM class CComplexObject will get the type name "C_COMPLEX_OBJECT"
 * The RM class DvDateTime will get the type name "DV_DATE_TIME"
 */
public class OpenEHRTypeNaming extends ClassNameIdResolver {

    private ModelInfoLookup rmInfoLookup = ArchieRMInfoLookup.getInstance();
    private ModelInfoLookup aomInfoLookup = ArchieAOMInfoLookup.getInstance();

    protected OpenEHRTypeNaming() {
        super(TypeFactory.defaultInstance().constructType(OpenEHRBase.class), TypeFactory.defaultInstance());
    }

    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.NAME;
    }

    @Override
    public String idFromValue(Object value) {

        RMTypeInfo typeInfo = rmInfoLookup.getTypeInfo(value.getClass());
        if (typeInfo == null) {
            typeInfo = aomInfoLookup.getTypeInfo(value.getClass());
        }
        if (typeInfo != null) {
            //this case is faster and should always work. If for some reason it does not, the case below works fine instead.
            return typeInfo.getRmName();
        } else {
            return rmInfoLookup.getNamingStrategy().getTypeName(value.getClass());
        }
// This should work in all cases for openEHR-classes and this should not be used for other classes
// Additional code for making this work on non-ehr-types:
//        } else {
//            return super.idFromValue(value);
//        }
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {
        return _typeFromId(id, context);
    }

    @Override
    protected JavaType _typeFromId(String typeName, DatabindContext ctxt) throws IOException {
        Class result = rmInfoLookup.getClass(typeName);
        if (result == null) {
            //AOM class?
            result = aomInfoLookup.getClass(typeName);
        }
        if (result != null) {
            TypeFactory typeFactory = (ctxt == null) ? _typeFactory : ctxt.getTypeFactory();
            return typeFactory.constructSpecializedType(_baseType, result);
        }
        return super._typeFromId(typeName, ctxt);
    }
}
