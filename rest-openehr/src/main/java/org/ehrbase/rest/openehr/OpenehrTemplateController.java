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

package org.ehrbase.rest.openehr;

import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.api.definitions.OperationalTemplateFormat;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.ehrbase.response.openehr.ErrorResponseData;
import org.ehrbase.response.openehr.ResponseData;
import org.ehrbase.response.openehr.TemplateResponseData;
import org.ehrbase.response.openehr.TemplatesResponseData;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.util.InternalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;

/**
 * Controller for /template resource as part of the Definitions sub-API of the openEHR REST API
 * <p>
 * TODO WIP state only implements endpoints from outer server side, everything else is a stub. Also with a lot of duplication at the moment, which should be reduced when implementing functionality.
 */
@Api(tags = {"Template"})
@RestController
@RequestMapping(path = "/rest/openehr/v1/definition/template", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrTemplateController extends BaseController {

    private final TemplateService templateService;

    @Autowired
    public OpenehrTemplateController(TemplateService templateService) {
        this.templateService = Objects.requireNonNull(templateService);
    }

    /*
        ADL 1.4
     */
    @PostMapping("/adl1.4")
    @ApiOperation(value = "Upload a new ADL 1.4 operational template (OPT).", response = TemplateResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Created successfully. Depending on the header Prefer either an empty body or a full representation body is returned.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),  // version string in location header optional
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }),
            @ApiResponse(code = 400, response = ErrorResponseData.class, message = "Bad request - unable to upload a template, because of invalid content."),
            // TODO: "This response is optional." how to proceed?
            @ApiResponse(code = 409, response = ErrorResponseData.class, message = "Version Conflict - template with given id and version already exists."),
            @ApiResponse(code = 406, response = ErrorResponseData.class, message = RESP_NOT_ACCEPTABLE_DESC),
            @ApiResponse(code = 415, response = ErrorResponseData.class, message = RESP_UNSUPPORTED_MEDIA_DESC)})
    @ResponseStatus(value = HttpStatus.CREATED)
    // overwrites default 200, fixes the wrong listing of 200 in swagger-ui (EHR-56)
    public ResponseEntity createTemplateClassic(@ApiParam(value = REQ_OPENEHR_VERSION) @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
                                                @ApiParam(value = REQ_OPENEHR_AUDIT) @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails, // TODO, see EHR-267
                                                @ApiParam(value = REQ_CONTENT_TYPE_BODY, required = true) @RequestHeader(value = CONTENT_TYPE) String contentType,
                                                @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false) String accept,
                                                @ApiParam(value = REQ_PREFER) @RequestHeader(value = PREFER, required = false) String prefer,
                                                @ApiParam(value = "The template to create", required = true) @RequestBody String template) {

        // TODO: only XML at the moment
        if (!MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_XML)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Only XML is supported at the moment");
        }

        String templateId = templateService.create(template);

        URI uri = URI.create(this.encodePath(getBaseEnvLinkURL() + "/rest/openehr/v1/definition/template/adl1.4/" + templateId));

        List<String> headerList = Arrays.asList(LOCATION, ETAG, LAST_MODIFIED);   // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so handled separately

        Optional<InternalResponse<ResponseData>> respData;   // variable to overload with more specific object if requested

        if (Optional.ofNullable(prefer).map(i -> i.equals(RETURN_REPRESENTATION)).orElse(false)) {      // null safe way to test prefer header
            respData = buildTemplateResponseData(templateId, accept, uri, headerList, () -> new TemplateResponseData());
        } else {    // "minimal" is default fallback
            respData = buildTemplateResponseData(templateId, accept, uri, headerList, () -> null);
        }

        // TODO remove 204?
        // returns 201 with body + headers, 204 only with headers or 500 error depending on what processing above yields
        return respData.map(i -> Optional.ofNullable(i.getResponseData()).map(j -> ResponseEntity.created(uri).headers(i.getHeaders()).body(j.get()))
                // when the body is empty
                .orElse(ResponseEntity.noContent().headers(i.getHeaders()).build()))
                // when no response could be created at all
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    // Note: based on latest-branch of 1.1.0 release of openEHR REST API, because this endpoint was changed significantly
    @GetMapping("/adl1.4")
    @ApiOperation(value = "List all available ADL 1.4 operational templates on the system.", response = TemplateResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok. Successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                    }),
            @ApiResponse(code = 406, response = ErrorResponseData.class, message = RESP_NOT_ACCEPTABLE_DESC)})
    public ResponseEntity getTemplatesClassic(@ApiParam(value = REQ_OPENEHR_VERSION) @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
                                              @ApiParam(value = REQ_OPENEHR_AUDIT) @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails, // TODO, see EHR-267
                                              @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false) String accept) {

        URI uri = URI.create(this.encodePath(getBaseEnvLinkURL() + "/rest/openehr/v1/definition/template/adl1.4"));

        List<String> headerList = Collections.emptyList();   // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so handled separately

        Optional<InternalResponse<ResponseData>> respData = buildTemplateResponseData("", accept, uri, headerList, () -> new TemplatesResponseData());

        // returns 200 with all templates OR error
        return respData.map(i -> ResponseEntity.ok().headers(i.getHeaders()).body(i.getResponseData().get()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    // Note: based on latest-branch of 1.1.0 release of openEHR REST API, because this endpoint was changed significantly
    @GetMapping("/adl1.4/{template_id}")
    @ApiOperation(value = "Gets a specified ADL 1.4 operational template.", response = TemplateResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok. Successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),  // only for single template
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class), // only for single template
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class) // only for single template
                    }),
            @ApiResponse(code = 404, response = ErrorResponseData.class, message = "Object Not Found - template with the specified id does not exist."),
            @ApiResponse(code = 406, response = ErrorResponseData.class, message = RESP_NOT_ACCEPTABLE_DESC)})
    public ResponseEntity getTemplateClassic(@ApiParam(value = REQ_OPENEHR_VERSION) @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
                                             @ApiParam(value = REQ_OPENEHR_AUDIT) @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails, // TODO, see EHR-267
                                             @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false) String accept,
                                             @ApiParam(value = "Template ID", required = true) @PathVariable(value = "template_id") String templateId) {

        URI uri = URI.create(this.encodePath(getBaseEnvLinkURL() + "/rest/openehr/v1/definition/template/adl1.4/" + templateId));

        List<String> headerList = Arrays.asList(LOCATION, ETAG, LAST_MODIFIED);   // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so handled separately

        Optional<InternalResponse<ResponseData>> respData = buildTemplateResponseData(templateId, accept, uri, headerList, () -> new TemplateResponseData());

        return respData.map(i -> ResponseEntity.ok().headers(i.getHeaders()).body(i.getResponseData().get()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /*
        ADL 2
     */
    @PostMapping("/adl2/{?version}")
    @ApiOperation(value = "Upload a new ADL 2 operational template.", response = TemplateResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Created successfully. Depending on the header Prefer either an empty body or a full representation body is returned.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),  // version string in location header optional
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class),
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class)
                    }),
            @ApiResponse(code = 400, response = ErrorResponseData.class, message = "Bad request - unable to upload a template, because of invalid content."),
            // TODO: "This response is optional." how to proceed?
            @ApiResponse(code = 409, response = ErrorResponseData.class, message = "Version Conflict - template with given id and version already exists."),
            @ApiResponse(code = 406, response = ErrorResponseData.class, message = RESP_NOT_ACCEPTABLE_DESC),
            @ApiResponse(code = 415, response = ErrorResponseData.class, message = RESP_UNSUPPORTED_MEDIA_DESC)})
    @ResponseStatus(value = HttpStatus.CREATED)
    // overwrites default 200, fixes the wrong listing of 200 in swagger-ui (EHR-56)
    public ResponseEntity<TemplateResponseData> createTemplateNew(@ApiParam(value = REQ_OPENEHR_VERSION) @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
                                                                  @ApiParam(value = REQ_OPENEHR_AUDIT) @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails, // TODO, see EHR-267
                                                                  @ApiParam(value = REQ_CONTENT_TYPE_BODY) @RequestHeader(value = CONTENT_TYPE, required = false) String contentType,
                                                                  @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false) String accept,
                                                                  @ApiParam(value = REQ_PREFER) @RequestHeader(value = PREFER, required = false) String prefer,
                                                                  @ApiParam(value = "a SEMVER version format, i.e. 1.0.1") @RequestParam(value = "version", required = false) String version,
                                                                  @ApiParam(value = "The template to create", required = true) @RequestBody String template) {

        // TODO implement handler - whole code below is only a stub yet

        TemplateResponseData data = new TemplateResponseData(); // empty for now

        URI url = URI.create("todo");
        // TODO - continuing stub but list of headers most likely the correct list of necessary ones
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setLocation(url);
        headers.setETag("\"something...\"");
        headers.setLastModified(1234565778);

        return Optional.ofNullable(data).map(i -> new ResponseEntity<>(i, headers, HttpStatus.CREATED))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    // TODO possible changes pending, keep an eye on: https://github.com/openEHR/specifications-ITS-REST/issues/85

    // Note: based on latest-branch of 1.1.0 release of openEHR REST API, because this endpoint was changed significantly
    // also, this endpoint combines what is listed as two endpoints: https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-adl-2-template-get
    @GetMapping("/adl2/{template_id}/{version_pattern}")
    @ApiOperation(value = "List all available ADL 2 operational templates on the system or a specific one. (Combined endpoint)", response = TemplateResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Ok. Successfully retrieved.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = LOCATION, description = RESP_LOCATION_DESC, response = URI.class),  // only for single template
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class), // only for single template
                            @ResponseHeader(name = LAST_MODIFIED, description = RESP_LAST_MODIFIED_DESC, response = long.class) // only for single template
                    }),
            @ApiResponse(code = 404, response = ErrorResponseData.class, message = "Object Not Found - template with the specified id does not exist."),
            @ApiResponse(code = 406, response = ErrorResponseData.class, message = RESP_NOT_ACCEPTABLE_DESC)})
    public ResponseEntity<TemplateResponseData> getTemplateNew(@ApiParam(value = REQ_OPENEHR_VERSION) @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
                                                               @ApiParam(value = REQ_OPENEHR_AUDIT) @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails, // TODO, see EHR-267
                                                               @ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false) String accept,
                                                               @ApiParam(value = "Template ID") @PathVariable(value = "template_id", required = false) String templateId,
                                                               @ApiParam(value = "SEMVER version pattern") @PathVariable(value = "version_pattern", required = false) String versionPattern) {

        // TODO implement handler - whole code below is only a stub yet

        // TODO how to do that with multiple templates?
        TemplateResponseData data = new TemplateResponseData(); // empty for now

        URI url = URI.create("todo");
        // TODO - continuing stub but list of headers most likely the correct list of necessary ones
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setLocation(url);
        headers.setETag("\"something...\"");
        headers.setLastModified(1234565778);

        return Optional.ofNullable(data).map(i -> new ResponseEntity<>(i, headers, HttpStatus.CREATED))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    /**
     * Builds response data for template endpoints. As specified there are two kinds of returns, with one or all templates.
     * Hence factory can work with TemplateResponseData or TemplatesResponseData.
     *
     * @param templateId ID of the template, can be empty ("") when all templates are requested
     * @param accept     Format the response should be delivered in, as given by request
     * @param uri        Location of resource
     * @param headerList List of headers to be set for response
     * @param factory    Works with TemplateResponseData or TemplatesResponseData
     * @param <T>        Type of response body
     * @return
     */
    private <T> Optional<InternalResponse<T>> buildTemplateResponseData(String templateId, String accept, URI uri, List<String> headerList, Supplier<T> factory) {
        // create either TemplateResponseData or null (means no body, only headers incl. link to resource), via lambda request
        T oneOrAllTemplates = factory.get();

        // do minimal scope steps
        // create and supplement headers with data depending on which headers are requested
        HttpHeaders respHeaders = new HttpHeaders();
        for (String header : headerList) {
            switch (header) {   // no default because everything else can be ignored
                case LOCATION:
                    respHeaders.setLocation(uri);
                    break;
                case ETAG:
                    respHeaders.setETag("\"" + templateId + "\"");
                    break;
                case LAST_MODIFIED:
                    // TODO should be VERSION.commit_audit.time_committed.value which is not implemented yet - mock for now
                    respHeaders.setLastModified(123124442);
                    break;
            }
        }

        // parse and set accepted format. with XML as fallback for empty header and error for non supported header
        MediaType mediaType = resolveContentType(accept, MediaType.APPLICATION_XML);
        OperationalTemplateFormat format;
        if (mediaType.equals(MediaType.APPLICATION_XML)) {
            format = OperationalTemplateFormat.XML;
        } else if (mediaType.equals(MediaType.APPLICATION_JSON)) {
            format = OperationalTemplateFormat.JSON;
        } else {
            throw new NotAcceptableException("Currently only xml (or emtpy for fallback) is allowed");
        }

        // is null when request wants only metadata returned, so skips provisioning of body if null
        if (oneOrAllTemplates != null) {
            if (oneOrAllTemplates.getClass().equals(TemplateResponseData.class)) {     // get only one template
                // when this "if" is true the following casting can be executed and data manipulated by reference (handled by temporary variable)
                TemplateResponseData objByReference = (TemplateResponseData) oneOrAllTemplates;

                // TODO very simple now, needs more sophisticated templateService
                String template = templateService.findOperationalTemplate(templateId, format);
                objByReference.set(template);

                // finally set last header // TODO only XML for now
                respHeaders.setContentType(MediaType.APPLICATION_XML);

            } else if (oneOrAllTemplates.getClass().equals(TemplatesResponseData.class)) {        // get all templates
                TemplatesResponseData objByReference = (TemplatesResponseData) oneOrAllTemplates;

                List<TemplateMetaDataDto> templates = templateService.getAllTemplates();
                objByReference.set(templates);
            } else
                throw new InternalServerException("Building template response data failed");    // i.e. wrong usage of buildTemplateResponseData()
        }

        return Optional.of(new InternalResponse<>(oneOrAllTemplates, respHeaders));
    }
}
