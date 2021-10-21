package org.ehrbase.rest.openehr.specification;

import com.nedap.archie.rm.composition.Composition;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.response.openehr.OriginalVersionResponseData;
import org.ehrbase.response.openehr.RevisionHistoryResponseData;
import org.ehrbase.response.openehr.VersionedObjectResponseData;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

@Tag(name = "VERSIONED_COMPOSITION")
@SuppressWarnings("java:S107")
public interface VersionedCompositionApiSpecification {

    @Operation(
            summary = "Get versioned composition",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-versioned_composition-get")
    )
    ResponseEntity<VersionedObjectResponseData<Composition>> retrieveVersionedCompositionByVersionedObjectUid(String accept,
                                                                                                              String ehrIdString,
                                                                                                              String versionedObjectUid);

    @Operation(
            summary = "Get versioned composition revision history",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-versioned_composition-get-1")
    )
    ResponseEntity<RevisionHistoryResponseData> retrieveVersionedCompositionRevisionHistoryByEhr(String accept,
                                                                                                 String ehrIdString,
                                                                                                 String versionedObjectUid);

    @Operation(
            summary = "Get versioned composition version by id",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#composition-versioned_composition-get-2")
    )
    ResponseEntity<OriginalVersionResponseData<Composition>> retrieveVersionOfCompositionByVersionUid(String accept,
                                                                                                      String ehrIdString,
                                                                                                      String versionedObjectUid,
                                                                                                      String versionUid);

    @Operation(
            summary = "Get versioned composition version at time",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#ehr_status-versioned_ehr_status-get-3")
    )
    ResponseEntity<OriginalVersionResponseData<Composition>> retrieveVersionOfCompositionByTime(String accept,
                                                                                                String ehrIdString,
                                                                                                String versionedObjectUid,
                                                                                                LocalDateTime versionAtTime);
}
