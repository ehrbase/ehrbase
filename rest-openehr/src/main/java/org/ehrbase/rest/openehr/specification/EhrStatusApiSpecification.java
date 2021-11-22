package org.ehrbase.rest.openehr.specification;

import com.nedap.archie.rm.ehr.EhrStatus;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.response.openehr.EhrStatusResponseData;
import org.springframework.http.ResponseEntity;

@Tag(name = "EHR_STATUS")
@SuppressWarnings("java:S107")
public interface EhrStatusApiSpecification {

    @Operation(
            summary = "Get EHR_STATUS version by time",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-ehr_status-get")
    )
    ResponseEntity<EhrStatusResponseData> retrieveEhrStatusByTime(String accept, String ehrIdString, String versionAtTime);

    @Operation(
            summary = "Get EHR_STATUS by version id",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-ehr_status-get-1")
    )
    ResponseEntity<EhrStatusResponseData> retrieveEhrStatusById(String accept, String ehrIdString, String versionUid);

    @Operation(
            summary = "Update EHR_STATUS",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-ehr_status-put")
    )
    ResponseEntity<EhrStatusResponseData> updateEhrStatus(String accept, String contentType, String prefer,
                                                          String ifMatch, String ehrIdString, EhrStatus ehrStatus);
}
