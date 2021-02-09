package org.ehrbase.rest.openehr.audit.old;

import org.ehrbase.rest.openehr.RestOpenehrOperation;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;

import java.io.Serializable;

/**
 * Audit dataset for openEHR/EHRbase operations.
 */
public class OpenehrAuditDataset implements Serializable {

    private RestOpenehrOperation operation;

    private EventOutcomeIndicator outcome;

    private String eventOutcomeDescription;

    private String sourceUserName;

    private String sourceUserId;

    private String destinationUserId;

    private String remoteAddress;

    public RestOpenehrOperation getOperation() {
        return operation;
    }

    public void setOperation(RestOpenehrOperation operation) {
        this.operation = operation;
    }

    public EventOutcomeIndicator getOutcome() {
        return outcome;
    }

    public void setOutcome(EventOutcomeIndicator outcome) {
        this.outcome = outcome;
    }

    public String getEventOutcomeDescription() {
        return eventOutcomeDescription;
    }

    public void setEventOutcomeDescription(String eventOutcomeDescription) {
        this.eventOutcomeDescription = eventOutcomeDescription;
    }

    public String getSourceUserName() {
        return sourceUserName;
    }

    public void setSourceUserName(String sourceUserName) {
        this.sourceUserName = sourceUserName;
    }

    public String getSourceUserId() {
        return sourceUserId;
    }

    public void setSourceUserId(String sourceUserId) {
        this.sourceUserId = sourceUserId;
    }

    public String getDestinationUserId() {
        return destinationUserId;
    }

    public void setDestinationUserId(String destinationUserId) {
        this.destinationUserId = destinationUserId;
    }


    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }
}
