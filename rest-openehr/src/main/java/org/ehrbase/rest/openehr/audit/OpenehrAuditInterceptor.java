package org.ehrbase.rest.openehr.audit;

import org.ehrbase.rest.openehr.RestOpenehrOperation;
import org.ehrbase.rest.openehr.audit.old.OpenehrAuditDataset;
import org.ehrbase.rest.openehr.audit.old.OpenehrAuditMessageBuilder;
import org.ehrbase.rest.openehr.controller.BaseController;
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

/**
 * Interceptor that registers DICOM audit messages using the audit module from IPF.
 */
public class OpenehrAuditInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(OpenehrAuditInterceptor.class);

    private static final String AUDIT_DATASET = "OpenEhrAuditDataset";

    private final AuditContext auditContext;

    public OpenehrAuditInterceptor(AuditContext auditContext) {
        this.auditContext = auditContext;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        LOG.debug("Intercepting incoming request...");

        OpenehrAuditDataset auditDataset = new OpenehrAuditDataset();
        auditDataset.setSourceUserId(request.getRequestURI());
        if (request.getUserPrincipal() != null) {
            auditDataset.setSourceUserName(request.getUserPrincipal().getName());
        }
        auditDataset.setDestinationUserId(request.getRequestURL().toString());
        auditDataset.setRemoteAddress(request.getRemoteAddr());
        request.setAttribute(AUDIT_DATASET, auditDataset);
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        RestOpenehrOperation operation = (RestOpenehrOperation) request.getAttribute(BaseController.REST_OPERATION);

        OpenehrAuditDataset auditDataset = (OpenehrAuditDataset) request.getAttribute(AUDIT_DATASET);
        auditDataset.setOperation(operation);

        HttpStatus status = HttpStatus.valueOf(response.getStatus());
        if (status.isError()) {
            if (status.is4xxClientError()) {
                auditDataset.setOutcome(EventOutcomeIndicator.SeriousFailure);
            } else {
                auditDataset.setOutcome(EventOutcomeIndicator.MajorFailure);
            }
            Throwable exception = (Throwable) request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE);
            if (exception != null) {
                auditDataset.setEventOutcomeDescription(exception.getMessage());
            }
        } else {
            auditDataset.setOutcome(EventOutcomeIndicator.Success);
        }

        AuditMessage message = new OpenehrAuditMessageBuilder(auditContext, auditDataset)
                .getMessage();
        auditContext.audit(message);
        LOG.debug("AuditMessage sent to audit repository: {}", message);
    }
}
