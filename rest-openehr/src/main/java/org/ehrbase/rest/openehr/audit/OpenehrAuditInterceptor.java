package org.ehrbase.rest.openehr.audit;

import org.ehrbase.rest.openehr.RestOpenehrOperation;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.EventOutcomeIndicator;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interceptor that registers DICOM audit messages using the audit module from Open eHealth Integration Platform.
 */
@Component
public class OpenehrAuditInterceptor implements HandlerInterceptor {

    private static final String AUDIT_DATASET = "OpenEhrAuditDataset";

    private final AuditContext auditContext;

    public OpenehrAuditInterceptor(AuditContext auditContext) {
        this.auditContext = auditContext;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        OpenehrAuditDataset auditDataset = new OpenehrAuditDataset();
        auditDataset.setOperation(RestOpenehrOperation.CREATE_COMPOSITION); // TODO: Resolve operation dynamically
        auditDataset.setSourceUserId(request.getRequestURI());
        auditDataset.setDestinationUserId(request.getRequestURL().toString());
        auditDataset.setRemoteAddress(request.getRemoteAddr());
        request.setAttribute(AUDIT_DATASET, auditDataset);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        OpenehrAuditDataset auditDataset = (OpenehrAuditDataset) request.getAttribute(AUDIT_DATASET);

        HttpStatus status = HttpStatus.valueOf(response.getStatus());
        if (!status.isError()) {
            auditDataset.setOutcome(EventOutcomeIndicator.Success);
        } else {
            if (status.is4xxClientError()) {
                auditDataset.setOutcome(EventOutcomeIndicator.SeriousFailure);
            } else {
                auditDataset.setOutcome(EventOutcomeIndicator.MajorFailure);
            }
            Throwable exception = (Throwable) request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE);
            if (exception != null) {
                auditDataset.setEventOutcomeDescription(exception.getMessage());
            }
        }

        auditContext.audit(new OpenehrAuditMessageBuilder(auditContext, auditDataset).getMessage());
    }
}
