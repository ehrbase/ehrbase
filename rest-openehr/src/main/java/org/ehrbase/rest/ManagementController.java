package org.ehrbase.rest;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.ehrbase.api.authorization.EhrbaseAuthorization;
import org.ehrbase.api.authorization.EhrbasePermission;
import org.ehrbase.api.service.ManagementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * API endpoint to get or set on the fly LogLevel.
 */
@Tag(name = "Management", description = "Get or Set on the fly LogLevel")
@RestController
@RequestMapping(
        path = "/management",
        produces = {MediaType.APPLICATION_XML_VALUE, MediaType.APPLICATION_JSON_VALUE})
public class ManagementController extends BaseController {

    private final ManagementService managementService;

    public ManagementController(ManagementService managementService) {
        this.managementService = Objects.requireNonNull(managementService);
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_SYSTEM_STATUS)
    @GetMapping(path = "/log-level")
    @Operation(summary = "Get log level information on running EHRbase server instance")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description =
                                "EHRbase is available. Basic information on runtime and build is returned in body.",
                        headers = {
                            @Header(
                                    name = CONTENT_TYPE,
                                    description = RESP_CONTENT_TYPE_DESC,
                                    schema = @Schema(implementation = MediaType.class))
                        },
                        content = @Content(schema = @Schema(implementation = String.class)))
            })
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<String> getLogLevel() {

        return ResponseEntity.ok(managementService.getLogLevel());
    }

    @EhrbaseAuthorization(permission = EhrbasePermission.EHRBASE_SYSTEM_STATUS)
    @PostMapping(path = "/log-level/{logLevel}")
    @Operation(summary = "Get log level information on running EHRbase server instance")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description =
                                    "EHRbase is available. Basic information on runtime and build is returned in body.",
                            headers = {
                                    @Header(
                                            name = CONTENT_TYPE,
                                            description = RESP_CONTENT_TYPE_DESC,
                                            schema = @Schema(implementation = MediaType.class))
                            },
                            content = @Content(schema = @Schema(implementation = String.class)))
            })
    @ResponseStatus(value = HttpStatus.OK)
    public ResponseEntity<String> setLogLevel(@NotNull @PathVariable String logLevel) {

        return ResponseEntity.ok(managementService.setLogLevel(logLevel));
    }
}
