package org.ehrbase.rest.openehr.audit;

import org.ehrbase.rest.openehr.RestOpenehrOperation;
import org.openehealth.ipf.commons.audit.types.EnumeratedCodedValue;
import org.openehealth.ipf.commons.audit.types.EventType;

/**
 * Audit Event Type Code for openEHR/EHRbase.
 */
public enum OpenehrEventTypeCode implements EventType, EnumeratedCodedValue<EventType> {

    CREATE_COMPOSITION("create-composition", "Create composition"),

    UPDATE_COMPOSITION("update-composition", "Update composition"),

    DELETE_COMPOSITION("delete-composition", "Delete composition"),

    GET_COMPOSITION_BY_VERSION_ID("get-composition-by-version-id", "Get composition by version id"),

    GET_COMPOSITION_AT_TIME("get-composition-at-time", "Get composition at time"),

    GET_VERSIONED_COMPOSITION("get-versioned-composition", "Get versioned composition"),

    GET_VERSIONED_COMPOSITION_REVISION_HISTORY("get-versioned-composition-revision-history", "Get versioned composition revision history"),

    GET_VERSIONED_COMPOSITION_VERSION_BY_ID("get-versioned-composition-version-by-id", "Get versioned composition version by id"),

    GET_VERSIONED_COMPOSITION_VERSION_AT_TIME("get-versioned-composition-version-at-time", "Get versioned composition version at time");


    private final EventType value;

    OpenehrEventTypeCode(String code, String displayName) {
        this.value = EventType.of(code, "OpenEHR Event Type", displayName);
    }

    public static OpenehrEventTypeCode resolve(RestOpenehrOperation operation) {
        for (OpenehrEventTypeCode eventType : values()) {
            if (eventType.getCode().equals(operation.getCode())) {
                return eventType;
            }
        }
        return null;
    }

    @Override
    public EventType getValue() {
        return value;
    }
}
