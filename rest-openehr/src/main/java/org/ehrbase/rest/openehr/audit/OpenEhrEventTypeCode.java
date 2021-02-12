package org.ehrbase.rest.openehr.audit;

import org.openehealth.ipf.commons.audit.codes.EventActionCode;
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

    public static OpenEhrEventTypeCode resolve(EventActionCode eventActionCode) {
        switch (eventActionCode) {
            case Create:
                return CREATE;
            case Update:
                return UPDATE;
            case Delete:
                return DELETE;
            default:
                return null;
        }
    }

    @Override
    public EventType getValue() {
        return value;
    }
}
