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

import com.fasterxml.jackson.annotation.JsonValue;
import com.nedap.archie.rm.generic.RevisionHistory;
import com.nedap.archie.rm.generic.RevisionHistoryItem;

import java.util.ArrayList;
import java.util.List;

public class RevisionHistoryResponseData {

    private RevisionHistory revisionHistory;

    public RevisionHistoryResponseData(RevisionHistory rh) {
        List<RevisionHistoryItem> items = new ArrayList<>(rh.getItems());
        this.revisionHistory = new RevisionHistory(items);
    }

    public RevisionHistory getRevisionHistory() {
        return revisionHistory;
    }

    @JsonValue
    public List<RevisionHistoryItem> getRevisionHistoryItems() {
        return revisionHistory.getItems();
    }

    public void setRevisionHistory(RevisionHistory revisionHistory) {
        this.revisionHistory = revisionHistory;
    }
}
