package org.ehrbase.rest.openehr.specification;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.response.openehr.CompositionResponseData;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Tag(name = "COMPOSITION")
@SuppressWarnings("java:S107")
public interface CompositionApiSpecification {

    @Operation(
            summary = "Create composition",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-composition-post")
    )
    ResponseEntity createComposition(String openehrVersion, String openehrAuditDetails, String contentType,
                                     String accept, String prefer, String ehrIdString, String composition,
                                     HttpServletRequest request);

    @Operation(
            summary = "Update composition",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-composition-put")
    )
    ResponseEntity updateComposition(String openehrVersion, String openehrAuditDetails, String contentType,
                                     String accept, String prefer, String ifMatch, String ehrIdString,
                                     String versionedObjectUidString, String composition, HttpServletRequest request);

    @Operation(
            summary = "Delete composition",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-composition-delete")
    )
    ResponseEntity deleteComposition(String openehrVersion, String openehrAuditDetails, String ehrIdString,
                                     String precedingVersionUid, HttpServletRequest request);

    @Operation(
            summary = "Get composition by version id",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-composition-get")
    )
    ResponseEntity<CompositionResponseData> getCompositionByVersionId(String accept, String ehrIdString, String versionUid,
                                                                      LocalDateTime versionAtTime, HttpServletRequest request);

    @Operation(
            summary = "Get composition at time",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-composition-get-1")
    )
    ResponseEntity getCompositionByTime(String accept, String ehrIdString, String versionedObjectUid,
                                        LocalDateTime versionAtTime, HttpServletRequest request);
}
