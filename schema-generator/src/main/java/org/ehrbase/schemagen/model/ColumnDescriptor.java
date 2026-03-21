package org.ehrbase.schemagen.model;

public record ColumnDescriptor(
        String name,
        String pgType,
        boolean nullable,
        String defaultValue,
        String comment) {

    public ColumnDescriptor(String name, String pgType) {
        this(name, pgType, true, null, null);
    }

    public ColumnDescriptor(String name, String pgType, boolean nullable) {
        this(name, pgType, nullable, null, null);
    }
}
