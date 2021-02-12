package org.ehrbase.rest.openehr.audit.support;

import org.ehrbase.rest.openehr.audit.OpenEhrAuditDataset;
import org.ehrbase.rest.openehr.audit.OpenEhrEventIdCode;
import org.ehrbase.rest.openehr.audit.OpenEhrEventTypeCode;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventActionCode;
import org.openehealth.ipf.commons.audit.event.CustomAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.event.DelegatingAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.utils.AuditUtils;

@SuppressWarnings("UnusedReturnValue")
public abstract class OpenEhrAuditMessageBuilder extends DelegatingAuditMessageBuilder<OpenEhrAuditMessageBuilder, CustomAuditMessageBuilder> {

    private final AuditContext auditContext;

    protected OpenEhrAuditMessageBuilder(AuditContext auditContext, OpenEhrAuditDataset auditDataset, EventActionCode eventActionCode,
                                         OpenEhrEventIdCode eventId, OpenEhrEventTypeCode eventTypeCode) {

        super(new CustomAuditMessageBuilder(auditDataset.getEventOutcomeIndicator(),
                auditDataset.getEventOutcomeDescription(),
                eventActionCode,
                eventId,
                eventTypeCode));
        this.auditContext = auditContext;
        delegate.setAuditSource(auditContext);
        setSourceActiveParticipant(auditDataset);
        setDestinationActiveParticipant();
    }

    protected OpenEhrAuditMessageBuilder setSourceActiveParticipant(OpenEhrAuditDataset auditDataset) {
        delegate.addSourceActiveParticipant(auditDataset.getSourceUserId() != null ? auditDataset.getSourceUserId() : auditContext.getAuditValueIfMissing(),
                null,
                null,
                auditDataset.getSourceAddress(),
                true);
        return this;
    }

    protected OpenEhrAuditMessageBuilder setDestinationActiveParticipant() {
        delegate.addDestinationActiveParticipant(auditContext.getAuditSourceId(),
                null,
                null,
                AuditUtils.getLocalIPAddress(),
                false);
        return this;
    }
}
