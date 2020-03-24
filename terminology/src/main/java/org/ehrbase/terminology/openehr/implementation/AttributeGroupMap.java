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
package org.ehrbase.terminology.openehr.implementation;

import java.util.Map;

public class AttributeGroupMap {

    String attribute; /** well known attribute as found in the RM model **/
    ContainerType container; /** codeset or openehr group (f.e. "null flavours") **/
    Map<String, String> idMap; /** maps an attribute id to a given group/codeset depending on the language **/

    public AttributeGroupMap(String attribute, String container, Map<String, String> idMap) {
        this.attribute = attribute;
        this.container = ContainerType.valueOf(container.toUpperCase());
        this.idMap = idMap;
    }

    public String getAttribute() {
        return attribute;
    }

    /**
     * specifies whether the codes belong to a group in a codeset or a plain codeset
     * @return
     */
    public ContainerType getContainer() {
        return container;
    }

    public Map<String, String> getIdMap() {
        return idMap;
    }
}
