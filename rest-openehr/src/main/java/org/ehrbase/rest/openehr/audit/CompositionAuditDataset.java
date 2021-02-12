package org.ehrbase.rest.openehr.audit;

public class CompositionAuditDataset extends OpenEhrAuditDataset {

    private String compositionUri;

    private String compositionTemplateId;

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
}
