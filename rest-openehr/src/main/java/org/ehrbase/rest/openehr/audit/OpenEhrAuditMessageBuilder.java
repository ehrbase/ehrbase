package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.event.CustomAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.event.DelegatingAuditMessageBuilder;

public class OpenEhrAuditMessageBuilder extends DelegatingAuditMessageBuilder<OpenEhrAuditMessageBuilder, CustomAuditMessageBuilder> {

    public OpenEhrAuditMessageBuilder(AuditContext auditContext, OpenEhrAuditDataset auditDataset) {
        super(new CustomAuditMessageBuilder(auditDataset.getEventOutcomeIndicator(),
                auditDataset.getEventOutcomeDescription(), null, null, null));

        setSourceActiveParticipant(auditDataset);
        setDestinationActiveParticipant(auditDataset);
        delegate.setAuditSource(auditContext);
    }

    protected OpenEhrAuditMessageBuilder setSourceActiveParticipant(OpenEhrAuditDataset auditDataset) {
        delegate.addSourceActiveParticipant("", null, null, "", true);
        return this;
    }

    protected OpenEhrAuditMessageBuilder setDestinationActiveParticipant(OpenEhrAuditDataset auditDataset) {
        delegate.addDestinationActiveParticipant("", null, null, "", false);
        return this;
    }
}
