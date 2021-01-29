package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.types.EnumeratedCodedValue;
import org.openehealth.ipf.commons.audit.types.EventId;

/**
 * Audit Event Type Code for openEHR/EHRbase.
 */
public enum OpenehrEventIdCode implements EventId, EnumeratedCodedValue<EventId> {

    REST_OPERATION("rest", "REST Operation");

    private final EventId value;

    OpenehrEventIdCode(String code, String displayName) {
        this.value = EventId.of(code, "OpenEHR Event ID", displayName);
    }

    @Override
    public EventId getValue() {
        return value;
    }
}
