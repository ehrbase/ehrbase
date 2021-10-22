package org.ehrbase.rest.openehr.specification;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.response.openehr.QueryResponseData;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

@Tag(name = "Query")
@SuppressWarnings("java:S107")
public interface QueryApiSpecification {

    @Operation(
            summary = "Execute ad-hoc (non-stored) AQL query",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/query.html#query-execute-query-get")
    )
    ResponseEntity<QueryResponseData> getAdhocQuery(String accept, String query, Integer offset, Integer fetch,
                                                    Map<String, Object> queryParameters, HttpServletRequest request);

    @Operation(
            summary = "Execute ad-hoc (non-stored) AQL query",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/query.html#query-execute-query-post")
    )
    ResponseEntity<QueryResponseData> postAdhocQuery(String accept, String contentType, String query, HttpServletRequest request);

    @Operation(
            summary = "Execute stored query",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/query.html#query-execute-query-get-1")
    )
    ResponseEntity<QueryResponseData> getStoredQuery(String accept, String qualifiedQueryName, Optional<String> version,
                                                     Integer offset, Integer fetch, Map<String, Object> queryParameter,
                                                     HttpServletRequest request);

    @Operation(
            summary = "Execute stored query",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/query.html#query-execute-query-post-1")
    )
    ResponseEntity<QueryResponseData> postStoredQuery(String accept, String contentType, String ifNoneMatch,
                                                      String qualifiedQueryName, Optional<String> version,
                                                      String parameterBody, HttpServletRequest request);
}
