package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.codes.EventActionCode;

public class CompositionAuditDataset extends OpenEhrAuditDataset {

    private EventActionCode eventActionCode;

    private String ehrId;

    private String compositionUri;

    private String compositionTemplateId;

    public EventActionCode getEventActionCode() {
        return eventActionCode;
    }

    public void setEventActionCode(EventActionCode eventActionCode) {
        this.eventActionCode = eventActionCode;
    }

    public String getEhrId() {
        return ehrId;
    }

    public void setEhrId(String ehrId) {
        this.ehrId = ehrId;
    }

    public String getCompositionUri() {
        return compositionUri;
    }

    public void setCompositionUri(String compositionUri) {
        this.compositionUri = compositionUri;
    }

    public String getCompositionTemplateId() {
        return compositionTemplateId;
    }

    public void setCompositionTemplateId(String compositionTemplateId) {
        this.compositionTemplateId = compositionTemplateId;
    }

    boolean hasEhrId() {
        return ehrId != null;
    }

    boolean hasComposition() {
        return compositionUri != null;
    }
}
