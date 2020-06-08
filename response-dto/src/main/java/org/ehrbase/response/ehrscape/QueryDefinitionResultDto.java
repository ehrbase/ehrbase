package org.ehrbase.response.ehrscape;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.ZonedDateTime;

public class QueryDefinitionResultDto {
    private String qualifiedName;
    private String version;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private ZonedDateTime saved;
    private String queryText;
    private String type;

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ZonedDateTime getSaved() {
        return saved;
    }

    public void setSaved(ZonedDateTime saved) {
        this.saved = saved;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
