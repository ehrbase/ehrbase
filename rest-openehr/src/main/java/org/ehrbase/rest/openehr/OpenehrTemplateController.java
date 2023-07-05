/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.rest.openehr;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;

import com.nedap.archie.rm.composition.Composition;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.annotations.TenantAware;
import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.definitions.OperationalTemplateFormat;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.service.CompositionService;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.openehr.sdk.response.dto.ResponseData;
import org.ehrbase.openehr.sdk.response.dto.TemplateResponseData;
import org.ehrbase.openehr.sdk.response.dto.TemplatesResponseData;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionDto;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.TemplateMetaDataDto;
import org.ehrbase.rest.BaseController;
import org.ehrbase.rest.openehr.specification.TemplateApiSpecification;
import org.ehrbase.rest.util.InternalResponse;
import org.openehr.schemas.v1.TemplateDocument;
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
@TenantAware
@RestController
@RequestMapping(
        path = BaseController.API_CONTEXT_PATH_WITH_VERSION + "/definition/template",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrTemplateController extends BaseController implements TemplateApiSpecification {

    protected static final String ADL_1_4 = "adl1.4";
    private final TemplateService templateService;
    private final CompositionService compositionService;

    @Autowired
    public OpenehrTemplateController(TemplateService templateService, CompositionService compositionService) {
        this.templateService = Objects.requireNonNull(templateService);
        this.compositionService = Objects.requireNonNull(compositionService);
    }

    /*
       ADL 1.4
    */
    @Override
    @PostMapping(
            path = "/adl1.4",
            produces = {MediaType.APPLICATION_XML_VALUE})
    @ResponseStatus(value = HttpStatus.CREATED)
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_CREATE)
    public ResponseEntity createTemplateClassic(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false)
                    String openehrAuditDetails, // TODO, see EHR-267
            @RequestHeader(value = CONTENT_TYPE) String contentType,
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @RequestHeader(value = PREFER, required = false) String prefer,
            @RequestBody String template) {

        // TODO: only XML at the moment
        if (!MediaType.parseMediaType(contentType).isCompatibleWith(APPLICATION_XML)) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Only XML is supported at the moment");
        }

        TemplateDocument document;
        try {
            document =
                    TemplateDocument.Factory.parse(new ByteArrayInputStream(template.getBytes(StandardCharsets.UTF_8)));
        } catch (XmlException | IOException e) {
            throw new InvalidApiParameterException(e.getMessage());
        }

        String templateId = templateService.create(document.getTemplate());

        URI uri = createLocationUri(DEFINITION, TEMPLATE, ADL_1_4, templateId);

        List<String> headerList = Arrays.asList(
                LOCATION,
                ETAG,
                LAST_MODIFIED); // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so handled
        // separately

        Optional<InternalResponse<ResponseData>>
                respData; // variable to overload with more specific object if requested

        if (Optional.ofNullable(prefer)
                .map(i -> i.equals(RETURN_REPRESENTATION))
                .orElse(false)) { // null safe way to test prefer header
            respData = buildTemplateResponseData(templateId, accept, uri, headerList, () -> new TemplateResponseData());
        } else { // "minimal" is default fallback
            respData = buildTemplateResponseData(templateId, accept, uri, headerList, () -> null);
        }

        // TODO remove 204?
        // returns 201 with body + headers, 204 only with headers or 500 error depending on what processing above yields
        return respData.map(i -> Optional.ofNullable(i.getResponseData())
                        .map(j -> ResponseEntity.created(uri)
                                .headers(i.getHeaders())
                                .body(j.get()))
                        // when the body is empty
                        .orElse(ResponseEntity.noContent()
                                .headers(i.getHeaders())
                                .build()))
                // when no response could be created at all
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    // Note: based on latest-branch of 1.1.0 release of openEHR REST API, because this endpoint was changed
    // significantly
    @Override
    @GetMapping("/adl1.4")
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_READ)
    public ResponseEntity getTemplatesClassic(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false)
                    String openehrAuditDetails, // TODO, see EHR-267
            @RequestHeader(value = ACCEPT, required = false) String accept) {

        URI uri = createLocationUri(DEFINITION, TEMPLATE, ADL_1_4);

        List<String> headerList =
                Collections.emptyList(); // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so
        // handled
        // separately

        Optional<InternalResponse<ResponseData>> respData =
                buildTemplateResponseData("", accept, uri, headerList, () -> new TemplatesResponseData());

        // returns 200 with all templates OR error
        return respData.map(i -> ResponseEntity.ok()
                        .headers(i.getHeaders())
                        .body(i.getResponseData().get()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    // Note: based on latest-branch of 1.1.0 release of openEHR REST API, because this endpoint was changed
    // significantly
    @Override
    @GetMapping("/adl1.4/{template_id}")
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_READ)
    public ResponseEntity getTemplateClassic(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false)
                    String openehrAuditDetails, // TODO, see EHR-267
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "template_id") String templateId) {

        URI uri = createLocationUri(DEFINITION, ADL_1_4, templateId);

        List<String> headerList = Arrays.asList(
                LOCATION,
                ETAG,
                LAST_MODIFIED); // whatever is required by REST spec - CONTENT_TYPE only needed for 201, so handled
        // separately

        Optional<InternalResponse<ResponseData>> respData =
                buildTemplateResponseData(templateId, accept, uri, headerList, () -> new TemplateResponseData());

        return respData.map(i -> ResponseEntity.ok()
                        .headers(i.getHeaders())
                        .body(i.getResponseData().get()))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @GetMapping(path = "/adl1.4/{template_id}/example")
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_EXAMPLE)
    public ResponseEntity<String> getTemplateExample(
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "template_id") String templateId) {
        CompositionFormat format = extractCompositionFormat(accept);

        Composition composition = templateService.buildExample(templateId);

        HttpHeaders respHeaders = new HttpHeaders();
        if (format.equals(CompositionFormat.XML)) {
            respHeaders.setContentType(APPLICATION_XML);
        } else if (format.equals(CompositionFormat.JSON)) {
            respHeaders.setContentType(APPLICATION_JSON);
        }

        return ResponseEntity.ok()
                .headers(respHeaders)
                .body(compositionService
                        .serialize(new CompositionDto(composition, templateId, null, null), format)
                        .getValue());
    }

    /*
       ADL 2
       TODO WIP state only implements endpoints from outer server side, everything else is a stub. Also with a lot of duplication at the moment, which should be reduced when implementing functionality.
    */
    @Override
    @PostMapping("/adl2")
    @ResponseStatus(value = HttpStatus.CREATED)
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_CREATE)
    public ResponseEntity<TemplateResponseData> createTemplateNew(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false)
                    String openehrAuditDetails, // TODO, see EHR-267
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
        headers.setContentType(APPLICATION_JSON);
        headers.setLocation(url);
        headers.setETag("\"something...\"");
        headers.setLastModified(1234565778);

        return Optional.ofNullable(data)
                .map(i -> new ResponseEntity<>(i, headers, HttpStatus.CREATED))
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    // TODO possible changes pending, keep an eye on: https://github.com/openEHR/specifications-ITS-REST/issues/85

    // Note: based on latest-branch of 1.1.0 release of openEHR REST API, because this endpoint was changed
    // significantly
    // also, this endpoint combines what is listed as two endpoints:
    // https://specifications.openehr.org/releases/ITS-REST/latest/definitions.html#definitions-adl-2-template-get
    @Override
    @GetMapping("/adl2/{template_id}/{version_pattern}")
    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_TEMPLATE_READ)
    public ResponseEntity<TemplateResponseData> getTemplateNew(
            @RequestHeader(value = "openEHR-VERSION", required = false) String openehrVersion, // TODO, see EHR-267
            @RequestHeader(value = "openEHR-AUDIT_DETAILS", required = false)
                    String openehrAuditDetails, // TODO, see EHR-267
            @RequestHeader(value = ACCEPT, required = false) String accept,
            @PathVariable(value = "template_id", required = false) String templateId,
            @PathVariable(value = "version_pattern", required = false) String versionPattern) {

        // TODO implement handler - whole code below is only a stub yet

        // TODO how to do that with multiple templates?
        TemplateResponseData data = new TemplateResponseData(); // empty for now

        URI url = URI.create("todo");
        // TODO - continuing stub but list of headers most likely the correct list of necessary ones
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        headers.setLocation(url);
        headers.setETag("\"something...\"");
        headers.setLastModified(1234565778);

        return Optional.ofNullable(data)
                .map(i -> new ResponseEntity<>(i, headers, HttpStatus.CREATED))
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
    private <T> Optional<InternalResponse<T>> buildTemplateResponseData(
            String templateId, String accept, URI uri, List<String> headerList, Supplier<T> factory) {
        // create either TemplateResponseData or null (means no body, only headers incl. link to resource), via lambda
        // request
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
                    // TODO should be VERSION.commit_audit.time_committed.value which is not implemented yet - mock for
                    // now
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
            if (oneOrAllTemplates.getClass().equals(TemplateResponseData.class)) { // get only one template
                // when this "if" is true the following casting can be executed and data manipulated by reference
                // (handled by temporary variable)
                TemplateResponseData objByReference = (TemplateResponseData) oneOrAllTemplates;

                // TODO very simple now, needs more sophisticated templateService
                String template = templateService.findOperationalTemplate(templateId, format);
                objByReference.set(template);

                // finally set last header // TODO only XML for now
                respHeaders.setContentType(APPLICATION_XML);

            } else if (oneOrAllTemplates.getClass().equals(TemplatesResponseData.class)) { // get all templates
                TemplatesResponseData objByReference = (TemplatesResponseData) oneOrAllTemplates;

                List<TemplateMetaDataDto> templates = templateService.getAllTemplates();
                objByReference.set(templates);
            } else
                throw new InternalServerException(
                        "Building template response data failed"); // i.e. wrong usage of buildTemplateResponseData()
        }

        return Optional.of(new InternalResponse<>(oneOrAllTemplates, respHeaders));
    }
}
