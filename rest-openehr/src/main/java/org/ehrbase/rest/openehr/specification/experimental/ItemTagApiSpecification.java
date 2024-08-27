/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.rest.openehr.specification.experimental;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.UUID;
import org.ehrbase.api.dto.experimental.ItemTagDto;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Experimental REST interface specification for <a href="https://specifications.openehr.org/releases/RM/development/common.html#tags">Tags</a>
 */
@SuppressWarnings("java:S107")
public interface ItemTagApiSpecification {

    // @format:off

    // --- Common Documentation ---

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Operation(
            summary = "Create or Update tags",
            description = "Bulk creation/update of tags. Tags without IDs are created, those with IDs are updated.",
            requestBody = @RequestBody(
                    content = {
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = ItemTagDto.class)),
                                    examples = @ExampleObject("""
                                            [
                                              {
                                                "key": "tag::1",
                                                "value": "some textual value",
                                                "target_path": "/context/end_time[at0001]|value"
                                              }
                                            ]"""
                                    )
                            )
                    }
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "ItemTag ids in case Header Prefer is missing or contains return=minimal",
                            content = @Content(schema = @Schema(type = "uuid"))
                    ),
                    @ApiResponse(
                            responseCode = "200",
                            description = "ItemTag ids in case Header Prefer is missing or contains return=representation",
                            content = @Content(
                                    array = @ArraySchema(schema = @Schema(implementation = ItemTagDto.class))
                            )
                    )
            }
    )
    @interface OperationTagUpsert {
    }

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Operation(
            summary = "Get tags",
            description = "Returns all tags for or filters based on the given ids and/or keys.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "ItemTag for the composition",
                            content = @Content(
                                    array = @ArraySchema(schema = @Schema(implementation = ItemTagDto.class))
                            )
                    )
            }
    )
    @interface OperationTagsGet {
    }

    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Operation(
            summary = "Deletes tags",
            description = "Deletes all tags for matching the given uuid or ItemTag.id.",
            responses = {
                    @ApiResponse(responseCode = "204")
            }
    )
    @interface OperationTagsDelete {
    }

    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Parameter(
            description = "Preferred response type.",
            schema = @Schema(type = "string($prefer)", allowableValues = {"return=minimal", "return=representation"}),
            in = ParameterIn.HEADER
    )
    @interface ParameterPrefer {
    }

    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Parameter(
            description = "Identifier of owner object, such as EHR.",
            schema = @Schema(type = "string($uuid)"),
            required = true
    )
    @interface ParameterEhrId {
    }

    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Parameter(
            description = "Identifier of target, which may be a VERSIONED_OBJECT<T> or a VERSION<T>.",
            schema = @Schema(type = "string($uuid|$version)"),
            required = true
    )
    @interface ParameterVersionedObjectId {
    }

    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Parameter(
            description = "Filter for tag keys.",
            schema = @Schema(type = "string($key)")
    )
    @interface ParameterFilterKeys {
    }

    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Parameter(
            description = "Filter for tag Identifier.",
            schema = @Schema(type = "string($uuid)")
    )
    @interface ParameterFilterIds {
    }

    @Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Parameter(
            description = "List of ItemTag string($uuid) or entries to delete",
            schema = @Schema(oneOf = {UUID.class, ItemTagDto.class})
    )
    @interface ParameterDeleteIDs {
    }

    // --- EHR_STATUS ---

    @Tag(name = "EHR_STATUS")
    @OperationTagUpsert
    ResponseEntity<Object> upsertEhrStatusItemTags(
            String openehrVersion,
            String openehrAuditDetails,
            @ParameterPrefer String prefer,
            @ParameterEhrId String ehrIdString,
            @ParameterVersionedObjectId String versionedObjectUid,
            @RequestBody List<ItemTagDto> itemTags
    );

    @Tag(name = "EHR_STATUS")
    @OperationTagsGet
    ResponseEntity<List<ItemTagDto>> getEhrStatusItemTags(
            String openehrVersion,
            String openehrAuditDetails,
            @ParameterEhrId String ehrIdString,
            @ParameterVersionedObjectId String versionedObjectUid,
            @ParameterFilterIds List<String> ids,
            @ParameterFilterKeys List<String> keys
    );

    @Tag(name = "EHR_STATUS")
    @OperationTagsDelete
    ResponseEntity<Void> deleteEhrStatusItemTags(
            String openehrVersion,
            String openehrAuditDetails,
            @ParameterEhrId String ehrIdString,
            @ParameterVersionedObjectId String versionedObjectUid,
            @ParameterDeleteIDs List<Object> itemTagsOrUUIDs
    );

    // --- COMPOSITION ---

    @Tag(name = "ITEM_TAG")
    @OperationTagUpsert
    ResponseEntity<Object> upsertCompositionItemTags(
            String openehrVersion,
            String openehrAuditDetails,
            @ParameterPrefer String prefer,
            @ParameterEhrId String ehrIdString,
            @ParameterVersionedObjectId String versionedObjectUid,
            @RequestBody List<ItemTagDto> itemTags
    );

    @Tag(name = "ITEM_TAG")
    @OperationTagsGet
    ResponseEntity<List<ItemTagDto>> getCompositionItemTags(
            String openehrVersion,
            String openehrAuditDetails,
            @ParameterEhrId String ehrIdString,
            @ParameterVersionedObjectId String versionedObjectUid,
            @ParameterFilterIds List<String> ids,
            @ParameterFilterKeys List<String> keys
    );

    @Tag(name = "ITEM_TAG")
    @OperationTagsDelete
    ResponseEntity<Void> deleteCompositionItemTags(
            String openehrVersion,
            String openehrAuditDetails,
            @ParameterEhrId String ehrIdString,
            @ParameterVersionedObjectId String versionedObjectUid,
            @ParameterDeleteIDs List<Object> itemTagsOrUUIDs
    );

    // @format:on
}
