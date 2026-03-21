package org.ehrbase.schemagen.enums;

public enum AdlVersion {
    ADL_1_4("1.4"),
    ADL_2_4("2.4");

    private final String version;

    AdlVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
