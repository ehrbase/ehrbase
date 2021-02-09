package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.types.EnumeratedCodedValue;
import org.openehealth.ipf.commons.audit.types.EventId;

/**
 * Audit Event ID Code for openEHR.
 */
public enum OpenEhrEventIdCode implements EventId, EnumeratedCodedValue<EventId> {

    COMPOSITION("composition"),

    QUERY("query");

    private final EventId value;

    OpenEhrEventIdCode(String code) {
        this.value = EventId.of(code, "http://www.openehr.org/api/v1/ehr", code);
    }

    @Override
    public EventId getValue() {
        return value;
    }
}
