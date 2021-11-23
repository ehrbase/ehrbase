package org.ehrbase.rest.openehr.specification;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "CONTRIBUTION")
@SuppressWarnings("java:S107")
public interface ContributionApiSpecification {

    @Operation(
            summary = "Create contribution",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#contribution-contribution-post")
    )
    ResponseEntity createContribution(String openehrVersion, String openehrAuditDetails, String contentType, String accept,
                                      String prefer, String ehrIdString, String contribution);

    @Operation(
            summary = "Get contribution by id",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#contribution-contribution-get")
    )
    ResponseEntity getContribution(String openehrVersion, String openehrAuditDetails, String accept,
                                   String ehrIdString, String contributionUidString);
}
