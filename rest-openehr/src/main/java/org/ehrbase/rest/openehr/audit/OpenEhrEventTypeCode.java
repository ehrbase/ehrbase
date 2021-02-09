package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.types.EnumeratedCodedValue;
import org.openehealth.ipf.commons.audit.types.EventType;

/**
 * Audit Event Type Code for openEHR.
 */
public enum OpenEhrEventTypeCode implements EventType, EnumeratedCodedValue<EventType> {

    CREATE("249", "creation"),

    UPDATE("251", "modification"),

    DELETE("523", "deleted");

    private final EventType value;

    OpenEhrEventTypeCode(String code, String originalText) {
        this.value = EventType.of(code, "openehr", originalText);
    }

    @Override
    public EventType getValue() {
        return value;
    }
}
