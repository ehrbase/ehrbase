package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.model.AuditMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class OpenEhrAuditStrategy<T extends OpenEhrAuditDataset> implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(OpenEhrAuditStrategy.class);

    private final AuditContext auditContext;

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

    }

    protected abstract AuditMessage[] getMessages(T auditDataset);
}
