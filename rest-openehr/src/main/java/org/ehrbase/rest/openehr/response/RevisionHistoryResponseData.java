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
