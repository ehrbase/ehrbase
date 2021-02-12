package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;

import java.io.Serializable;

public abstract class OpenEhrAuditDataset implements Serializable {

    private EventOutcomeIndicator eventOutcomeIndicator;

    private String eventOutcomeDescription;

    private String sourceUserId;

    private String sourceAddress;

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

    public String getSourceUserId() {
        return sourceUserId;
    }

    public void setSourceUserId(String sourceUserId) {
        this.sourceUserId = sourceUserId;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }
}
