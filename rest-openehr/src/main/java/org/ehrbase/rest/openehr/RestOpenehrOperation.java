package org.ehrbase.rest.openehr;

public enum RestOpenehrOperation {

    // Composition

    CREATE_COMPOSITION("create-composition"),

    UPDATE_COMPOSITION("update-composition"),

    DELETE_COMPOSITION("delete-composition"),

    GET_COMPOSITION_BY_VERSION_ID("get-composition-by-version-id"),

    GET_COMPOSITION_AT_TIME("get-composition-at-time"),

    GET_VERSIONED_COMPOSITION("get-versioned-composition"),

    GET_VERSIONED_COMPOSITION_REVISION_HISTORY("get-versioned-composition-revision-history"),

    GET_VERSIONED_COMPOSITION_VERSION_BY_ID("get-versioned-composition-version-by-id"),

    GET_VERSIONED_COMPOSITION_VERSION_AT_TIME("get-versioned-composition-version-at-time");

    private final String code;

    RestOpenehrOperation(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
