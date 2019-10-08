/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) Ripple Foundation CIC Ltd, UK, 2017
 * Author: Christian Chevalley
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

package org.ehrbase.opt.mapper;

import org.openehr.bmm.core.BmmClass;

import java.util.Map;

/**
 * Created by christian on 2/14/2018.
 */
public class MandatoryAttributes {


    private String rmTypeName;


    public MandatoryAttributes(String rmTypeName) {
        this.rmTypeName = rmTypeName;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> mandatoryAttributes;

        BmmClass bmmClass = BmmModelHelper.getBmmClassForName(rmTypeName);

        mandatoryAttributes = BmmModelHelper.getRequiredAttributes(bmmClass);
        mandatoryAttributes.remove(Constants.NAME);
        mandatoryAttributes.remove("archetype_node_id");
        mandatoryAttributes.remove("archetype_details");
        mandatoryAttributes.remove(Constants.DATA);
        mandatoryAttributes.remove(Constants.DESCRIPTION);

        return mandatoryAttributes;
    }

}
