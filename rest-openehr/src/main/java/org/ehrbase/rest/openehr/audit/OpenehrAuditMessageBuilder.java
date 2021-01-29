package org.ehrbase.rest.openehr.audit;

import org.ehrbase.rest.openehr.RestOpenehrOperation;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventActionCode;
import org.openehealth.ipf.commons.audit.event.CustomAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.event.DelegatingAuditMessageBuilder;
import org.openehealth.ipf.commons.audit.types.EventType;
import org.openehealth.ipf.commons.audit.utils.AuditUtils;

/**
 * {@link org.openehealth.ipf.commons.audit.event.AuditMessageBuilder AuditMessageBuilder} used to build openEHR/EHRbase audit messages.
 */
public class OpenehrAuditMessageBuilder extends DelegatingAuditMessageBuilder<OpenehrAuditMessageBuilder, CustomAuditMessageBuilder> {

    private final AuditContext auditContext;

    public OpenehrAuditMessageBuilder(AuditContext auditContext, OpenehrAuditDataset auditDataset) {
        super(new CustomAuditMessageBuilder(
                auditDataset.getOutcome(),
                auditDataset.getEventOutcomeDescription(),
                eventActionCode(auditDataset.getOperation()),
                OpenehrEventIdCode.REST_OPERATION,
                EventType.of(auditDataset.getOperation().getCode(), "OpenEHR Event Type", auditDataset.getOperation().getCode()))); // TODO: Review
        this.auditContext = auditContext;
        delegate.setAuditSource(auditContext);
        setSourceParticipant(auditDataset);
        setDestinationParticipant(auditDataset);
    }

    private static EventActionCode eventActionCode(RestOpenehrOperation operation) {
        switch (operation) {
            case CREATE_COMPOSITION:
                return EventActionCode.Create;
            case GET_COMPOSITION_BY_VERSION_ID:
            case GET_COMPOSITION_AT_TIME:
            case GET_VERSIONED_COMPOSITION:
            case GET_VERSIONED_COMPOSITION_REVISION_HISTORY:
            case GET_VERSIONED_COMPOSITION_VERSION_BY_ID:
            case GET_VERSIONED_COMPOSITION_VERSION_AT_TIME:
                return EventActionCode.Read;
            case UPDATE_COMPOSITION:
                return EventActionCode.Update;
            case DELETE_COMPOSITION:
                return EventActionCode.Delete;
            default:
                return EventActionCode.Execute;
        }
    }

    protected OpenehrAuditMessageBuilder setSourceParticipant(OpenehrAuditDataset auditDataset) {
        delegate.addSourceActiveParticipant(auditDataset.getSourceUserId() != null ? auditDataset.getSourceUserId() : this.auditContext.getAuditValueIfMissing(),
                null,
                auditDataset.getSourceUserName(),
                AuditUtils.getHostFromUrl(auditDataset.getRemoteAddress()),
                true);
        return this;
    }

    protected OpenehrAuditMessageBuilder setDestinationParticipant(OpenehrAuditDataset auditDataset) {
        delegate.addDestinationActiveParticipant(auditDataset.getDestinationUserId() != null ? auditDataset.getDestinationUserId() : this.auditContext.getAuditValueIfMissing(),
                AuditUtils.getProcessId(),
                null,
                AuditUtils.getLocalIPAddress(),
                false);
        return this;
    }
}
