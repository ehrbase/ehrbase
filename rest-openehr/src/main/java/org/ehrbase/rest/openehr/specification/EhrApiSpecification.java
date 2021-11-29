package org.ehrbase.rest.openehr.specification;

import com.nedap.archie.rm.ehr.EhrStatus;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.response.openehr.EhrResponseData;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;

@Tag(name = "EHR")
@SuppressWarnings("java:S107")
public interface EhrApiSpecification {

    @Operation(
            summary = "Create EHR",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr-ehr-post")
    )
    ResponseEntity createEhr(String openehrVersion,
                             String openehrAuditDetails,
                             String contentType,
                             String accept,
                             String prefer,
                             EhrStatus ehrStatus,
                             HttpServletRequest request);

    @Operation(
            summary = "Create EHR with id",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr-ehr-put")
    )
    ResponseEntity<EhrResponseData> createEhrWithId(String openehrVersion,
                                                    String openehrAuditDetails,
                                                    String accept,
                                                    String prefer,
                                                    String ehrIdString,
                                                    EhrStatus ehrStatus,
                                                    HttpServletRequest request);

    @Operation(
            summary = "Get EHR summary by id",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr-ehr-get")
    )
    ResponseEntity<EhrResponseData> retrieveEhrById(String accept, String ehrIdString, HttpServletRequest request);

    @Operation(
            summary = "Get EHR summary by subject id",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr-ehr-get-1")
    )
    ResponseEntity<EhrResponseData> retrieveEhrBySubject(String accept,
                                                         String subjectId,
                                                         String subjectNamespace,
                                                         HttpServletRequest request);
}
