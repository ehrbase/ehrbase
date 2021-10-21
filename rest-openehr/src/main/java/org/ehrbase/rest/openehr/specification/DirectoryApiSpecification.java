package org.ehrbase.rest.openehr.specification;

import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.ehrbase.response.openehr.DirectoryResponseData;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "DIRECTORY")
@SuppressWarnings("java:S107")
public interface DirectoryApiSpecification {

    @Operation(
            summary = "Create directory",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#directory-directory-post")
    )
    ResponseEntity<DirectoryResponseData> createFolder(String openEhrVersion, String openEhrAuditDetails,
                                                       String contentType, String accept,
                                                       String prefer, UUID ehrId, Folder folder);

    @Operation(
            summary = "Update directory",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#directory-directory-put")
    )
    ResponseEntity<DirectoryResponseData> updateFolder(String openEhrVersion, String openEhrAuditDetails,
                                                       String contentType, String accept, String prefer,
                                                       ObjectVersionId folderId, UUID ehrId, Folder folder);


    @Operation(
            summary = "Delete directory",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#directory-directory-delete")
    )
    ResponseEntity<DirectoryResponseData> deleteFolder(String openEhrVersion, String openEhrAuditDetails, String accept,
                                                       ObjectVersionId folderId, String ehrIdString);

    @Operation(
            summary = "Get folder in directory version",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#directory-directory-get")
    )
    ResponseEntity<DirectoryResponseData> getFolder(String accept, UUID ehrId,
                                                    ObjectVersionId folderId, String path);

    @Operation(
            summary = "Get folder in directory version at time",
            externalDocs = @ExternalDocumentation(url = "https://specifications.openehr.org/releases/ITS-REST/latest/ehr.html#directory-directory-get-1")
    )
    ResponseEntity<DirectoryResponseData> getFolderVersionAtTime(String accept, UUID ehrId,
                                                                 Instant versionAtTime, String path);
}
