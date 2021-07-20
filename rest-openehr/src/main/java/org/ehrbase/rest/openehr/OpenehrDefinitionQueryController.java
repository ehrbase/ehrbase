package org.ehrbase.rest.openehr;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.swagger.annotations.*;
import org.ehrbase.api.service.QueryService;
import org.ehrbase.response.openehr.ErrorBodyPayload;
import org.ehrbase.response.openehr.QueryDefinitionListResponseData;
import org.ehrbase.response.openehr.QueryDefinitionResponseData;
import org.ehrbase.rest.BaseController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Api(tags = "Stored Query")
@RestController
@RequestMapping(path = "/rest/openehr/v1/definition/query", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class OpenehrDefinitionQueryController extends BaseController {

    final static Logger log = LoggerFactory.getLogger(OpenehrDefinitionQueryController.class);
    private QueryService queryService;

    @Autowired
    public OpenehrDefinitionQueryController(QueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService);
    }

    //----- DEFINITION: Manage Stored Query, From definition package https://openehr.github.io/specifications-ITS-REST/definitions.html#definitions-stored-query

    /**
     * Get a stored query
     * @param accept
     * @param qualifiedQueryName
     * @return
     */
    @RequestMapping(value = {"/{qualified_query_name}", ""}, method = RequestMethod.GET)
    @ApiOperation(value = "List stored queries", response = QueryDefinitionResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class)
                    })})
    public ResponseEntity<QueryDefinitionListResponseData>getStoredQueryList(@ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false) String accept,
                                                                             @ApiParam(value = "query name to be listed, example: org.openehr::compositions", required = false)
                                                               @PathVariable(value = "qualified_query_name", required = false) String qualifiedQueryName
                                                             ) {

        log.debug("getStoredQueryList invoked with the following input: " + qualifiedQueryName);

        QueryDefinitionListResponseData responseData = new QueryDefinitionListResponseData(queryService.retrieveStoredQueries(qualifiedQueryName));
        return ResponseEntity.ok(responseData);
    }

    @RequestMapping(value = {"/{qualified_query_name}/{version}"}, method = RequestMethod.GET)   //
    @ApiOperation(value = "Get stored query and info/metadata", response = QueryDefinitionResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class)
                    }),
            @ApiResponse(code = 400, message = "Invalid input, e.g. a request with missing required field q or invalid query syntax."),
            @ApiResponse(code = 404, message = "The specified query doesn't exist.")})
    public ResponseEntity<QueryDefinitionResponseData> getStoredQueryVersion(@ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false) String accept,
                                                                             @ApiParam(value = "query name to be listed, example: org.openehr::compositions", required = true) @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
                                                                             @ApiParam(value = "query version (SEMVER), example: 1.2.3") @PathVariable(value = "version") Optional<String> version
    ) {

        log.debug("getStoredQueryVersion invoked with the following input: " +  qualifiedQueryName + ", version:"+version);


        QueryDefinitionResponseData queryDefinitionResponseData = new QueryDefinitionResponseData(queryService.retrieveStoredQuery(qualifiedQueryName, version.isPresent() ? version.get() : null));

        return ResponseEntity.ok(queryDefinitionResponseData);
    }

    @RequestMapping(value = {"/{qualified_query_name}/{version}{?type}", "/{qualified_query_name}{?type}"}, method = RequestMethod.PUT)
    @ApiOperation(value = "Store a query", response = QueryDefinitionResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class)
                    }),
            @ApiResponse(code = 400, message = "Invalid input, e.g. a request with missing required field q or invalid query syntax."),
            @ApiResponse(code = 409, message = "Query already exists.")})
    public ResponseEntity<QueryDefinitionResponseData> putStoreQuery(@ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false) String accept,
                                                          @ApiParam(value = "query name to store, example: org.openehr::compositions", required = true) @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
                                                          @ApiParam(value = "query version (SEMVER), example: 1.2.3") @PathVariable(value = "version") Optional<String> version,
                                                          @ApiParam(value = "query type (default AQL)") @RequestParam(value = "type", required=false, defaultValue = "AQL") String type,
                                                          @ApiParam(value = "The query to store", required = true) @RequestBody String queryPayload
    ) {

        log.debug("putStoreQuery invoked with the following input: " +  qualifiedQueryName + ", version:"+version+", query:"+queryPayload+", type="+type);

        //use the payload from adhoc POST:
        //get the query and parameters if any
        Gson gson = new GsonBuilder().create();

        Map<String, Object> mapped = gson.fromJson(queryPayload, Map.class);
        String aql = (String) mapped.get("q");

        if (aql == null || aql.isEmpty())
            return new ResponseEntity(new ErrorBodyPayload("Invalid query", "no aql query provided in payload").toString(), HttpStatus.BAD_REQUEST);

        QueryDefinitionResponseData queryDefinitionResponseData = new QueryDefinitionResponseData(queryService.createStoredQuery(qualifiedQueryName, version.isPresent() ? version.get() : null, aql));

        return ResponseEntity.ok(queryDefinitionResponseData);

    }

    @RequestMapping(value = {"/{qualified_query_name}/{version}"}, method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete a query", response = QueryDefinitionResponseData.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success.",
                    responseHeaders = {
                            @ResponseHeader(name = CONTENT_TYPE, description = RESP_CONTENT_TYPE_DESC, response = MediaType.class),
                            @ResponseHeader(name = ETAG, description = RESP_ETAG_DESC, response = String.class)
                    }),
            @ApiResponse(code = 400, message = "Invalid input, e.g. a request with missing required field q or invalid query syntax."),
            @ApiResponse(code = 409, message = "Query already exists.")})
    public ResponseEntity<QueryDefinitionResponseData> deleteStoredQuery(@ApiParam(value = REQ_ACCEPT) @RequestHeader(value = ACCEPT, required = false) String accept,
                                                                     @ApiParam(value = "query name to store, example: org.openehr::compositions", required = true) @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
                                                                     @ApiParam(value = "query version (SEMVER), example: 1.2.3", required = true) @PathVariable(value = "version") String version

    ) {

        log.debug("deleteStoredQuery for the following input: " +  qualifiedQueryName + ", version:"+version);

        QueryDefinitionResponseData queryDefinitionResponseData = new QueryDefinitionResponseData(queryService.deleteStoredQuery(qualifiedQueryName, version));

        return ResponseEntity.ok(queryDefinitionResponseData);
    }
}
