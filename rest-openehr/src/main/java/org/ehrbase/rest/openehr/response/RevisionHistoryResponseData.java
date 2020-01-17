/*
 * Copyright (c) 2020 Jake Smolka (Hannover Medical School).
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

package org.ehrbase.rest.openehr.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.generic.RevisionHistoryItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JacksonXmlRootElement(localName = "revision_history")
public class RevisionHistoryResponseData {

    @JsonProperty   // FIXME VERSIONED_OBJECT_POC: this layer needs to be ignored in output JSON, i.e. outer '{ "revisionHistory": ... }' needs to be removed from output
    private List<Map<String, Object>> revisionHistory;

    public RevisionHistoryResponseData(RevisionHistory rh) {
        this.revisionHistory = new ArrayList<>();

        for (RevisionHistoryItem item : rh.getItems()) {

            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("version_id", item.getVersionId());
            itemMap.put("audit", item.getAudits());

            this.revisionHistory.add(itemMap);
        }
    }

    @JsonProperty()
    public List<Map<String, Object>> getRevisionHistory() {
        return revisionHistory;
    }

    public void setRevisionHistory(List<Map<String, Object>> revisionHistory) {
        this.revisionHistory = revisionHistory;
    }
}
