package org.ehrbase.response.openehr;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.ehrbase.response.ehrscape.QueryDefinitionResultDto;

import java.time.format.DateTimeFormatter;

@JacksonXmlRootElement
public class QueryDefinitionResponseData {

    //the initial query without substitution (!)
    @JsonProperty(value = "q")
    private String query;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "type")
    private String type;

    @JsonProperty(value = "version")
    private String version;

    @JsonProperty(value = "saved")
    private String saved;

    public QueryDefinitionResponseData(QueryDefinitionResultDto definitionResultDto) {
        this.query = definitionResultDto.getQueryText();
        this.name = definitionResultDto.getQualifiedName();
        this.version = definitionResultDto.getVersion();
        this.saved = definitionResultDto.getSaved().format(DateTimeFormatter.ISO_DATE_TIME);
        this.type = definitionResultDto.getType();
    }

    public String getQuery() {
        return query;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getSaved() {
        return saved;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
