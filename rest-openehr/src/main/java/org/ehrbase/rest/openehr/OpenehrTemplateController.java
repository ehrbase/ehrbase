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

import com.nedap.archie.rm.composition.Composition;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.ehrbase.api.definitions.OperationalTemplateFormat;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.response.ehrscape.CompositionDto;
import org.ehrbase.response.ehrscape.CompositionFormat;
import org.ehrbase.response.ehrscape.StructuredString;
import org.ehrbase.response.ehrscape.TemplateMetaDataDto;
import org.ehrbase.response.openehr.ResponseData;
import org.ehrbase.response.openehr.TemplateResponseData;
import org.ehrbase.response.openehr.TemplatesResponseData;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.TemplateApiSpecification;
import org.ehrbase.rest.util.InternalResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for /template resource as part of the Definitions sub-API of the openEHR REST API
 */
@RestController
@RequestMapping(path = "${openehr-api.context-path:/rest/openehr}/v1/definition/template", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrTemplateController extends BaseController implements TemplateApiSpecification {

    private final TemplateService templateService;
  private final CompositionService compositionService;

  @Autowired
  public OpenehrTemplateController(
      TemplateService templateService, CompositionService compositionService) {
        this.templateService = Objects.requireNonNull(templateService);
    this.compositionService = Objects.requireNonNull(compositionService);
    }

    /*
        ADL 1.4
     */
    @PostMapping("/adl1.4")
    @ResponseStatus(value = HttpStatus.CREATED)
    @Override
    public ResponseEntity createTemplateClassic(@RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
                                                @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails, // TODO, see EHR-267
                                                @RequestHeader(value = CONTENT_TYPE) String contentType,
                                                @RequestHeader(value = ACCEPT, required = false) String accept,
                                                @RequestHeader(value = PREFER, required = false) String prefer,
                                                @RequestBody String template) {

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
    @Override
    public ResponseEntity getTemplatesClassic(@RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
                                              @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails, // TODO, see EHR-267
                                              @RequestHeader(value = ACCEPT, required = false) String accept) {

        URI uri = URI.create(this.encodePath(getBaseEnvLinkURL() + "/rest/openehr/v1/definition/template/adl1.4"));

        List<String> headerList = Collections.emptyList();   // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so handled separately

        Optional<InternalResponse<ResponseData>> respData = buildTemplateResponseData("", accept, uri, headerList, () -> new TemplatesResponseData());

        // returns 200 with all templates OR error
        return respData.map(i -> ResponseEntity.ok().headers(i.getHeaders()).body(i.getResponseData().get()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    // Note: based on latest-branch of 1.1.0 release of openEHR REST API, because this endpoint was changed significantly
    @GetMapping("/adl1.4/{template_id}")
    @Override
    public ResponseEntity getTemplateClassic(@RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
                                             @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails, // TODO, see EHR-267
                                             @RequestHeader(value = ACCEPT, required = false) String accept,
                                             @PathVariable(value = "template_id") String templateId) {

        URI uri = URI.create(this.encodePath(getBaseEnvLinkURL() + "/rest/openehr/v1/definition/template/adl1.4/" + templateId));

        List<String> headerList = Arrays.asList(LOCATION, ETAG, LAST_MODIFIED);   // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so handled separately

        Optional<InternalResponse<ResponseData>> respData = buildTemplateResponseData(templateId, accept, uri, headerList, () -> new TemplateResponseData());

        return respData.map(i -> ResponseEntity.ok().headers(i.getHeaders()).body(i.getResponseData().get()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

  @GetMapping(path = "/adl1.4/{template_id}/example")
  public ResponseEntity<StructuredString> getTemplateExample(
      @RequestHeader(value = ACCEPT, required = false) String accept,
      @PathVariable(value = "template_id") String templateId) {
        CompositionFormat format = extractCompositionFormat(accept);

    Composition composition = templateService.buildExample(templateId);

        HttpHeaders respHeaders = new HttpHeaders();
          if (format.equals(CompositionFormat.XML)) {
            respHeaders.setContentType(MediaType.APPLICATION_XML);
        } else if (format.equals(CompositionFormat.JSON)) {
            respHeaders.setContentType(MediaType.APPLICATION_JSON);
        }

    ResponseEntity<StructuredString> body =
        ResponseEntity.ok()
            .headers(respHeaders)
            .body(
                compositionService.serialize(
                    new CompositionDto(composition, templateId, null, null), format));
    return body;
    }

    /*
        ADL 2
        TODO WIP state only implements endpoints from outer server side, everything else is a stub. Also with a lot of duplication at the moment, which should be reduced when implementing functionality.
     */
    @PostMapping("/adl2/{?version}")
    @ResponseStatus(value = HttpStatus.CREATED)
    @Override
    public ResponseEntity<TemplateResponseData> createTemplateNew(@RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
                                                                  @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails, // TODO, see EHR-267
                                                                  @RequestHeader(value = CONTENT_TYPE, required = false) String contentType,
                                                                  @RequestHeader(value = ACCEPT, required = false) String accept,
                                                                  @RequestHeader(value = PREFER, required = false) String prefer,
                                                                  @RequestParam(value = "version", required = false) String version,
                                                                  @RequestBody String template) {

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
    @Override
    public ResponseEntity<TemplateResponseData> getTemplateNew(@RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
                                                               @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false) String openehrAuditDetails, // TODO, see EHR-267
                                                               @RequestHeader(value = ACCEPT, required = false) String accept,
                                                               @PathVariable(value = "template_id", required = false) String templateId,
                                                               @PathVariable(value = "version_pattern", required = false) String versionPattern) {

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
            switch (header) {
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
                default:
                    // Ignore header
            }
        }

        // parse and set accepted format. with XML as fallback for empty header and error for non supported header
        MediaType mediaType = resolveContentType(accept, MediaType.APPLICATION_XML);
        OperationalTemplateFormat format;
        if (mediaType.isCompatibleWith(MediaType.APPLICATION_XML)) {
            format = OperationalTemplateFormat.XML;
        } else if (mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
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
