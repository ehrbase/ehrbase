package org.ehrbase.rest.openehr.response;

public class ErrorBodyPayload {
    String errorType;
    String errorDescription;

    public ErrorBodyPayload(String errorType, String errorDescription) {
        this.errorType = errorType;
        this.errorDescription = errorDescription;
    }

    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\n");
        stringBuilder.append("\"error_type\":");
        stringBuilder.append("\"");
        stringBuilder.append(errorType);
        stringBuilder.append("\"");
        stringBuilder.append(",\n");
        stringBuilder.append("\"error_description\":");
        stringBuilder.append("\"");
        stringBuilder.append(errorDescription.replaceAll("\"",""));
        stringBuilder.append("\"");
        stringBuilder.append("\n");
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
