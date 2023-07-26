/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.jooq.dbencoding;

import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import java.util.HashMap;
import java.util.Map;
import org.ehrbase.jooq.dbencoding.wrappers.json.I_DvTypeAdapter;
import org.ehrbase.openehr.sdk.util.SnakeCase;

/**
 * format a RM object name as a DvText or DvCodedText
 */
public class NameAsDvText {

    private DvText aName;

    public NameAsDvText(DvText aName) {
        this.aName = aName;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> nameMap = new HashMap<>();
        if (aName instanceof DvCodedText) {
            nameMap.put("defining_code", ((DvCodedText) (aName)).getDefiningCode());
        }
        if (aName != null) {
            nameMap.put("value", aName.getValue());
        }
        nameMap.put(I_DvTypeAdapter.AT_TYPE, new SnakeCase(DvText.class.getSimpleName()).camelToUpperSnake());
        return nameMap;
    }
}
