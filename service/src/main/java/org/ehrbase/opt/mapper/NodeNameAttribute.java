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

package org.ehrbase.opt.mapper;

import org.openehr.schemas.v1.*;

/**
 * Created by christian on 3/16/2018.
 */
public class NodeNameAttribute {

    CATTRIBUTE cattribute;

    public NodeNameAttribute(CATTRIBUTE cattribute) {
        this.cattribute = cattribute;
    }

    public String staticName() {
        for (COBJECT cobject : cattribute.getChildrenArray()) {
            if (cobject instanceof CCOMPLEXOBJECT) {
                CCOMPLEXOBJECT ccomplexobject = (CCOMPLEXOBJECT) cobject;
                for (CATTRIBUTE cattribute1 : ccomplexobject.getAttributesArray()) {
                    if (cattribute1 instanceof CSINGLEATTRIBUTE) {
                        CSINGLEATTRIBUTE csingleattribute = (CSINGLEATTRIBUTE) cattribute1;
                        for (COBJECT cobject1 : csingleattribute.getChildrenArray()) {
                            if (cobject1 instanceof CPRIMITIVEOBJECT) {
                                CPRIMITIVEOBJECT cprimitiveobject = (CPRIMITIVEOBJECT) cobject1;
                                if (cprimitiveobject.getRmTypeName().equals(Constants.STRING)) {
                                    if (cprimitiveobject.getItem() != null && cprimitiveobject.getItem() instanceof CSTRING) {
                                        CSTRING cstring = (CSTRING) cprimitiveobject.getItem();
                                        return cstring.getListArray()[0].replaceAll("[^A-Za-z0-9 _]", "");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
