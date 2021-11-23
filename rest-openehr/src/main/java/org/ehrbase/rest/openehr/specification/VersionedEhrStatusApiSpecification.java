package org.ehrbase.rest.openehr.specification;

import com.nedap.archie.rm.ehr.EhrStatus;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.response.openehr.OriginalVersionResponseData;
import org.ehrbase.response.openehr.RevisionHistoryResponseData;
import org.ehrbase.response.openehr.VersionedObjectResponseData;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

@Tag(name = "VERSIONED_EHR_STATUS")
@SuppressWarnings("java:S107")
public interface VersionedEhrStatusApiSpecification {

    @Operation(
            summary = "Get versioned EHR_STATUS",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-versioned_ehr_status-get")
    )
    ResponseEntity<VersionedObjectResponseData<EhrStatus>> retrieveVersionedEhrStatusByEhr(String ehrIdString, String accept);

    @Operation(
            summary = "Get versioned EHR_STATUS revision history",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-versioned_ehr_status-get-1")
    )
    ResponseEntity<RevisionHistoryResponseData> retrieveVersionedEhrStatusRevisionHistoryByEhr(String accept, String ehrIdString);

    @Operation(
            summary = "Get versioned EHR_STATUS version by time",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-versioned_ehr_status-get-2")
    )
    ResponseEntity<OriginalVersionResponseData<EhrStatus>> retrieveVersionOfEhrStatusByTime(String accept,
                                                                                            String ehrIdString,
                                                                                            LocalDateTime versionAtTime);

    @Operation(
            summary = "Get versioned EHR_STATUS version by id",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-versioned_ehr_status-get-3")
    )
    ResponseEntity<OriginalVersionResponseData<EhrStatus>> retrieveVersionOfEhrStatusByVersionUid(String accept,
                                                                                                  String ehrIdString,
                                                                                                  String versionUid);
}
