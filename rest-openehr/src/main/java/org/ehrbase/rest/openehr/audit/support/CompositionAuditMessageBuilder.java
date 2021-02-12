package org.ehrbase.rest.openehr.audit.support;

import org.ehrbase.rest.openehr.audit.CompositionAuditDataset;
import org.ehrbase.rest.openehr.audit.OpenEhrEventIdCode;
import org.ehrbase.rest.openehr.audit.OpenEhrEventTypeCode;
import org.openehealth.ipf.commons.audit.AuditContext;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectDataLifeCycle;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectIdTypeCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectTypeCode;
import org.openehealth.ipf.commons.audit.codes.ParticipantObjectTypeCodeRole;

@SuppressWarnings("UnusedReturnValue")
public class CompositionAuditMessageBuilder extends OpenEhrAuditMessageBuilder {

    public CompositionAuditMessageBuilder(AuditContext auditContext, CompositionAuditDataset auditDataset) {
        super(auditContext, auditDataset, auditDataset.getEventActionCode(), OpenEhrEventIdCode.COMPOSITION,
                OpenEhrEventTypeCode.resolve(auditDataset.getEventActionCode()));
    }

    public CompositionAuditMessageBuilder addComposition(CompositionAuditDataset auditDataset) {
        delegate.addParticipantObjectIdentification(
                ParticipantObjectIdTypeCode.URI,
                auditDataset.getCompositionTemplateId(),
                null,
                null,
                auditDataset.getCompositionUri(),
                ParticipantObjectTypeCode.System,
                null,
                resolveCompositionObjectDataLifeCycle(auditDataset),
                null);
        return this;
    }

    public CompositionAuditMessageBuilder addPatient(CompositionAuditDataset auditDataset) {
        delegate.addParticipantObjectIdentification(
                ParticipantObjectIdTypeCode.PatientNumber,
                null,
                null,
                null,
                auditDataset.getEhrId() ,
                ParticipantObjectTypeCode.Person,
                ParticipantObjectTypeCodeRole.Patient,
                null,
                null);
        return this;
    }

    private ParticipantObjectDataLifeCycle resolveCompositionObjectDataLifeCycle(CompositionAuditDataset auditDataset) {
        switch (auditDataset.getEventActionCode()) {
            case Create:
                return ParticipantObjectDataLifeCycle.Origination;
            case Update:
                return ParticipantObjectDataLifeCycle.Amendment;
            case Read:
                return ParticipantObjectDataLifeCycle.Disclosure;
            default:
                return null;
        }
    }
}
