package org.ehrbase.response.openehr;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.ehrbase.response.ehrscape.QueryDefinitionResultDto;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JacksonXmlRootElement
public class QueryDefinitionListResponseData {

    @JsonProperty
    private List<Map<String, String>> versions;

    public QueryDefinitionListResponseData(List<QueryDefinitionResultDto> definitionResultDtos) {
        this.versions = new ArrayList<>();

        for (QueryDefinitionResultDto queryDefinitionResultDto: definitionResultDtos) {
            Map<String, String> definition = new HashMap<>();
            definition.put("name", queryDefinitionResultDto.getQualifiedName());
            definition.put("version", queryDefinitionResultDto.getVersion());
            definition.put("saved", queryDefinitionResultDto.getSaved().format(DateTimeFormatter.ISO_DATE_TIME));
            definition.put("type", queryDefinitionResultDto.getType());
            versions.add(definition);
        }
    }

    public int size(){
        return versions.size();
    }

}
