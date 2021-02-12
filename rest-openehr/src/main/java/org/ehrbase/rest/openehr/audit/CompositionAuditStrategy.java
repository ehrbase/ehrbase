package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.model.AuditMessage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CompositionAuditStrategy extends OpenEhrAuditStrategy<CompositionAuditDataset> {

    public CompositionAuditStrategy(AuditContext auditContext) {
        super(auditContext);
    }

    @Override
    protected CompositionAuditDataset createAuditDataset() {
        return new CompositionAuditDataset();
    }

    @Override
    protected void enrichAuditDataset(CompositionAuditDataset auditDataset, HttpServletRequest request, HttpServletResponse response) {
        super.enrichAuditDataset(auditDataset, request, response);
    }

    @Override
    protected AuditMessage[] getMessages(CompositionAuditDataset auditDataset) {
        return new AuditMessage[0];
    }
}
