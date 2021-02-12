package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.openehealth.ipf.commons.audit.model.AuditMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class OpenEhrAuditStrategy<T extends OpenEhrAuditDataset> implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(OpenEhrAuditStrategy.class);

    protected final AuditContext auditContext;

    protected OpenEhrAuditStrategy(AuditContext auditContext) {
        this.auditContext = auditContext;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {

        T auditDataset = createAuditDataset();
        enrichAuditDataset(auditDataset, request, response);

        AuditMessage[] messages = getMessages(auditDataset);
        auditContext.audit(messages);

        LOG.debug("Messages sent to the audit repository: {}", (Object[]) messages);
    }

    protected abstract T createAuditDataset();

    protected void enrichAuditDataset(T auditDataset, HttpServletRequest request, HttpServletResponse response) {
        if (request.getUserPrincipal() != null) {
            auditDataset.setSourceUserId(request.getUserPrincipal().getName());
        }
        auditDataset.setSourceAddress(request.getRemoteAddr());

        HttpStatus status = HttpStatus.valueOf(response.getStatus());
        EventOutcomeIndicator eventOutcomeIndicator;
        String eventOutcomeDescription = status.value() + " " + status.getReasonPhrase();

        if (status.isError()) {
            if (status == HttpStatus.NOT_FOUND) {
                eventOutcomeIndicator = EventOutcomeIndicator.MinorFailure;
            } else if (status.is4xxClientError()) {
                eventOutcomeIndicator = EventOutcomeIndicator.SeriousFailure;
            } else {
                eventOutcomeIndicator = EventOutcomeIndicator.MajorFailure;
            }

            Throwable exception = (Throwable) request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE);
            if (exception != null) {
                eventOutcomeDescription += ": " + exception.getMessage();
            }
        } else {
            eventOutcomeIndicator = EventOutcomeIndicator.Success;
        }

        auditDataset.setEventOutcomeIndicator(eventOutcomeIndicator);
        auditDataset.setEventOutcomeDescription(eventOutcomeDescription);
    }

    protected abstract AuditMessage[] getMessages(T auditDataset);
}
