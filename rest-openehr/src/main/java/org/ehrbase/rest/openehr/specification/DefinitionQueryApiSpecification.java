package org.ehrbase.rest.openehr.specification;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.response.openehr.QueryDefinitionListResponseData;
import org.ehrbase.response.openehr.QueryDefinitionResponseData;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

@Tag(name = "STORED_QUERY")
@SuppressWarnings("java:S107")
public interface DefinitionQueryApiSpecification {

    @Operation(
            summary = "List stored queries",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-stored-query-get")
    )
    ResponseEntity<QueryDefinitionListResponseData> getStoredQueryList(String accept, String qualifiedQueryName);

    @Operation(
            summary = "Store a query",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-stored-query-put")
    )
    ResponseEntity<QueryDefinitionResponseData> putStoreQuery(String accept,
                                                              String qualifiedQueryName,
                                                              Optional<String> version,
                                                              String type,
                                                              String queryPayload);

    @Operation(
            summary = "Get stored query and info/metadata",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-stored-query-get-1")
    )
    ResponseEntity<QueryDefinitionResponseData> getStoredQueryVersion(String accept,
                                                                      String qualifiedQueryName,
                                                                      Optional<String> version);

    @Operation(summary = "Delete a query", hidden = true)
    ResponseEntity<QueryDefinitionResponseData> deleteStoredQuery(String accept,
                                                                  String qualifiedQueryName,
                                                                  String version);
}
