package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;

import java.io.Serializable;

public class OpenEhrAuditDataset implements Serializable {

    private EventOutcomeIndicator eventOutcomeIndicator;

    private String eventOutcomeDescription;

    public EventOutcomeIndicator getEventOutcomeIndicator() {
        return eventOutcomeIndicator;
    }

    public void setEventOutcomeIndicator(EventOutcomeIndicator eventOutcomeIndicator) {
        this.eventOutcomeIndicator = eventOutcomeIndicator;
    }

    public String getEventOutcomeDescription() {
        return eventOutcomeDescription;
    }

    public void setEventOutcomeDescription(String eventOutcomeDescription) {
        this.eventOutcomeDescription = eventOutcomeDescription;
    }
}
