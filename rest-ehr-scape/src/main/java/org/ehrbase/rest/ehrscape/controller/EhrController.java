/*
 * Copyright (c) 2019 Stefan Spiska (Vitasystems GmbH) and Jake Smolka (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.rest.ehrscape.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.exception.DuplicateObjectException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.response.ehrscape.EhrStatusDto;
import org.ehrbase.rest.ehrscape.controller.OperationNotesResourcesReader.ApiNotes;
import org.ehrbase.rest.ehrscape.responsedata.Action;
import org.ehrbase.rest.ehrscape.responsedata.EhrResponseData;
import org.ehrbase.rest.ehrscape.responsedata.Meta;
import org.ehrbase.rest.ehrscape.responsedata.RestHref;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Controller for /ehr resource of EhrScape REST API */
@RestController
@RequestMapping(
    path = "/rest/ecis/v1/ehr",
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class EhrController extends BaseController {

  private static final String MODIFIABLE = "modifiable";
  private static final String QUERYABLE = "queryable";

  final EhrService ehrService;

  @Autowired
  public EhrController(EhrService ehrService) {

    this.ehrService = Objects.requireNonNull(ehrService);
  }

  @PostMapping()
  @ApiOperation(value = "Create a EHR for subject", response = EhrResponseData.class)
  @ApiResponses(
      value = {
        @ApiResponse(
            code = 201,
            message =
                "Successfully created - response body will contain the newly created EHR's ID."),
        @ApiResponse(
            code = 400,
            message =
                "Bad request - no subject ID specified, no subject namespace specified, EHR for the specified subject ID and namespace already exists."),
        @ApiResponse(code = 401, message = "Unauthenticated - could not authenticate the user."),
        @ApiResponse(
            code = 403,
            message =
                "Forbidden - the user does not have the required permissions to execute this request.")
      })
  @ResponseStatus(
      value =
          HttpStatus
              .CREATED) // overwrites default 200, fixes the wrong listing of 200 in swagger-ui
                        // (EHR-56)
  public ResponseEntity<EhrResponseData> createEhr(
      @ApiParam(value = "Id of a subject in a remote namespace")
          @RequestParam(value = "subjectId", required = false)
          String subjectId,
      @ApiParam(value = "Remote namespace of the subject")
          @RequestParam(value = "subjectNamespace", required = false)
          String subjectNamespace,
      @ApiParam(value = "The external ID of the committer user.")
          @RequestParam(value = "committerId", required = false)
          String committerId,
      @ApiParam(
              value =
                  "The name of the committer user. If omitted, the current session's logged in user's name will be used.")
          @RequestParam(value = "committerName", required = false)
          String committerName,
      @ApiParam(value = "Sets the response type")
          @RequestHeader(value = "Content-Type", required = false)
          String contentType,
      @ApiParam(value = "otherDetails") @RequestBody(required = false) String content)
      throws Exception {

    // subjectId and subjectNamespace are not required by EhrScape spec but without those parameters
    // a 400 error shall be returned
    if ((subjectId == null) || (subjectNamespace == null)) {
      throw new InvalidApiParameterException("subjectId or subjectNamespace missing");
    } else if ((subjectId.isEmpty()) || (subjectNamespace.isEmpty())) {
      throw new InvalidApiParameterException("subjectId or subjectNamespace emtpy");
    }
    EhrStatus ehrStatus = extractEhrStatus(content);
    PartySelf partySelf =
        new PartySelf(new PartyRef(new HierObjectId(subjectId), subjectNamespace, null));
    ehrStatus.setSubject(partySelf);
    UUID ehrId = ehrService.create(ehrStatus, null);

    // TODO: use config file or alike to set the basic api path
    URI url = URI.create(getBaseEnvLinkURL() + "/rest/ecis/v1/ehr/" + ehrId.toString());
    return Optional.ofNullable(ehrId)
        .flatMap(i -> buildEhrResponseData(i, Action.CREATE, contentType))
        .map(ResponseEntity.created(url)::body)
        .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
  }

  @GetMapping()
  @ApiOperation(value = "Find EHR by subject")
  @ApiResponses(
      value = {
        @ApiResponse(
            code = 200,
            message = "Success - response body will contain information about the EHR."),
        @ApiResponse(
            code = 204,
            message = "No content - no EHR for the specified subject ID and namespace exists."),
        @ApiResponse(
            code = 400,
            message = "Bad request - no subject ID specified, no subject namespace specified."),
        @ApiResponse(code = 401, message = "Unauthenticated - could not authenticate the user."),
        @ApiResponse(
            code = 403,
            message =
                "Forbidden - the user does not have the required permissions to execute this request.")
      })
  public ResponseEntity<EhrResponseData> getEhr(
      @ApiParam(required = true, value = "Id of a subject in a remote namespace")
          @RequestParam(value = "subjectId")
          String subjectId,
      @ApiParam(required = true, value = "Remote namespace of the subject")
          @RequestParam(value = "subjectNamespace")
          String subjectNamespace,
      @ApiParam(value = "Sets the response type")
          @RequestHeader(value = "Content-Type", required = false)
          String contentType) {

    Optional<UUID> ehrId = ehrService.findBySubject(subjectId, subjectNamespace);
    return ehrId
        .flatMap(i -> buildEhrResponseData(i, Action.RETRIEVE, contentType))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }

  @GetMapping(path = "/{uuid}")
  @ApiOperation(value = "Returns information about the specified EHR")
  @ApiResponses(
      value = {
        @ApiResponse(
            code = 200,
            message = "Success - response body will contain information about the EHR."),
        @ApiResponse(code = 401, message = "Unauthenticated - could not authenticate the user."),
        @ApiResponse(
            code = 403,
            message =
                "Forbidden - the user does not have the required permissions to execute this request."),
        @ApiResponse(code = 404, message = "Not found - EHR does not exist.")
      })
  public ResponseEntity<EhrResponseData> getEhr(
      @ApiParam(value = "EHR ID", required = true) @PathVariable("uuid") UUID ehrId,
      @ApiParam(value = "Sets the response type")
          @RequestHeader(value = "Content-Type", required = false)
          String contentType) {

    return Optional.ofNullable(ehrId)
        .flatMap(i -> buildEhrResponseData(i, Action.RETRIEVE, contentType))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping(path = "/{uuid}/status")
  @ApiOperation(value = "Update status of the specified EHR")
  @ApiNotes("ehrPutEhrUuidStatus.md")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Success - EHR_STATUS updated."),
        @ApiResponse(
            code = 201,
            message = "(not valid, ignore. documentation produces this entry automatically."),
        @ApiResponse(code = 401, message = "Unauthenticated - could not authenticate the user."),
        @ApiResponse(
            code = 403,
            message =
                "Forbidden - the user does not have the required permissions to execute this request."),
        @ApiResponse(code = 404, message = "Not found - specified EHR was not found.")
      })
  public ResponseEntity<EhrResponseData> updateStatus(
      @ApiParam(value = "EHR ID", required = true) @PathVariable("uuid") UUID ehrId,
      @ApiParam(value = "EHR status.", required = true) @RequestBody() String ehrStatus,
      @ApiParam(value = "Sets the response type")
          @RequestHeader(value = "Content-Type", required = false)
          String contentType) {

    ehrService.updateStatus(ehrId, extractEhrStatus(ehrStatus), null);
    return Optional.ofNullable(ehrId)
        .flatMap(i -> buildEhrResponseData(i, Action.UPDATE, contentType))
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }

  private EhrStatus extractEhrStatus(
      @RequestBody @ApiParam(value = "EHR status.", required = true) String content) {
    EhrStatus ehrStatus = new EhrStatus();

    if (StringUtils.isNotBlank(content)) {
      Gson json = new GsonBuilder().create();
      Map<String, Object> atributes = json.fromJson(content, Map.class);

      if (atributes.containsKey(MODIFIABLE)) {
        ehrStatus.setModifiable((Boolean) atributes.get(MODIFIABLE));
      }

      if (atributes.containsKey(QUERYABLE))
        ehrStatus.setQueryable((Boolean) atributes.get(QUERYABLE));
    }
    return ehrStatus;
  }

  private Optional<EhrResponseData> buildEhrResponseData(
      UUID ehrId, Action create, String contentType) {
    // check for valid format header to produce content accordingly
    CompositionFormat format;
    if (contentType == null) {
      // assign default if no header was set
      format = CompositionFormat.RAW;
    } else {
      // if header was set process it
      MediaType mediaType = MediaType.parseMediaType(contentType);

      if (mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
        format = CompositionFormat.RAW;
      } else if (mediaType.isCompatibleWith(MediaType.APPLICATION_XML)) {
        format = CompositionFormat.XML;
      } else {
        throw new InvalidApiParameterException("Wrong Content-Type header in request");
      }
    }

    // Optional<EhrStatusDto> ehrStatus = ehrService.getEhrStatus(ehrId, CompositionFormat.FLAT);
    // // older, keep until rework of formatting
    Optional<EhrStatusDto> ehrStatus = ehrService.getEhrStatusEhrScape(ehrId, format);
    if (!ehrStatus.isPresent()) {
      return Optional.empty();
    }
    EhrResponseData responseData = new EhrResponseData();
    responseData.setAction(create);
    responseData.setEhrId(ehrId);
    responseData.setEhrStatus(ehrStatus.get());
    RestHref url = new RestHref();
    // TODO: use config file or alike to set the basic api path
    url.setUrl(getBaseEnvLinkURL() + "/rest/ecis/v1/ehr/" + responseData.getEhrId());
    Meta meta = new Meta();
    meta.setHref(url);
    responseData.setMeta(meta);
    return Optional.of(responseData);
  }

  /**
   * Specific handler that overwrites inherited handling to comply with EhrScape API specification.
   * In the case 'GET /ehr' a not found object shall invoke NO_CONTENT instead of NOT_FOUND.
   */
  @Override
  @ExceptionHandler(ObjectNotFoundException.class)
  public ResponseEntity<Map<String, String>> restErrorHandler(ObjectNotFoundException e) {
    return createErrorResponse(e.getMessage(), HttpStatus.NO_CONTENT); // 204
  }

  /**
   * Specific handler that overwrites inherited handling to comply with EhrScape API specification.
   * In the case 'POST /ehr' a duplicate object shall invoke BAD_REQUEST instead of
   * ALREADY_REPORTED.
   */
  @Override
  @ExceptionHandler(DuplicateObjectException.class)
  public ResponseEntity<Map<String, String>> restErrorHandler(DuplicateObjectException e) {
    return createErrorResponse(e.getMessage(), HttpStatus.BAD_REQUEST); // 400
  }
}
