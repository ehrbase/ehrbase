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

import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlBeans;
import org.apache.xmlbeans.impl.values.XmlAnyTypeImpl;
import org.openehr.schemas.v1.ARCHETYPECONSTRAINT;

import javax.xml.namespace.QName;

/**
 * Created by christian on 7/23/2016.
 */
public interface I_CArchetypeConstraintValidate {

    static String getXmlType(ARCHETYPECONSTRAINT archetypeconstraint) {
        QName qName = new QName("http://www.w3.org/2001/XMLSchema-instance", "type", "xsi");
        XmlAnyTypeImpl attribute = (XmlAnyTypeImpl) archetypeconstraint.selectAttribute(qName);
        String attributeValue = attribute.getStringValue();
        if (attributeValue.contains(":"))
            return attributeValue.split(":")[1];
        else
            return attribute.getStringValue();
    }

    static SchemaType findSchemaType(String name) {
        String ns = "http://schemas.openehr.org/v1";
        QName qName = new QName(ns, name);
        return XmlBeans.getContextTypeLoader().findType(qName);
    }

    void validate(String path, Object aValue, ARCHETYPECONSTRAINT archetypeconstraint) throws IllegalArgumentException;
}
