package org.ehrbase.rest.openehr.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.ehrbase.api.service.QueryService;
import org.ehrbase.response.openehr.ErrorBodyPayload;
import org.ehrbase.response.openehr.QueryDefinitionListResponseData;
import org.ehrbase.response.openehr.QueryDefinitionResponseData;
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
    public ResponseEntity<QueryDefinitionListResponseData>getStoredQueryList(@RequestHeader(value = ACCEPT, required = false) String accept,
                                                               @PathVariable(value = "qualified_query_name", required = false) String qualifiedQueryName) {

        log.debug("getStoredQueryList invoked with the following input: " + qualifiedQueryName);

        QueryDefinitionListResponseData responseData = new QueryDefinitionListResponseData(queryService.retrieveStoredQueries(qualifiedQueryName));
        return ResponseEntity.ok(responseData);
    }

    @RequestMapping(value = {"/{qualified_query_name}/{version}"}, method = RequestMethod.GET)
    public ResponseEntity<QueryDefinitionResponseData> getStoredQueryVersion(@RequestHeader(value = ACCEPT, required = false) String accept,
                                                                             @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
                                                                             @PathVariable(value = "version") Optional<String> version) {

        log.debug("getStoredQueryVersion invoked with the following input: " +  qualifiedQueryName + ", version:"+version);


        QueryDefinitionResponseData queryDefinitionResponseData = new QueryDefinitionResponseData(queryService.retrieveStoredQuery(qualifiedQueryName, version.isPresent() ? version.get() : null));

        return ResponseEntity.ok(queryDefinitionResponseData);
    }

    @RequestMapping(value = {"/{qualified_query_name}/{version}{?type}", "/{qualified_query_name}{?type}"}, method = RequestMethod.PUT)
    public ResponseEntity<QueryDefinitionResponseData> putStoreQuery(String accept,
                                                          @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
                                                          @PathVariable(value = "version") Optional<String> version,
                                                          @RequestParam(value = "type", required=false, defaultValue = "AQL") String type,
                                                          @RequestBody String queryPayload) {

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
    public ResponseEntity<QueryDefinitionResponseData> deleteStoredQuery(@RequestHeader(value = ACCEPT, required = false) String accept,
                                                                     @PathVariable(value = "qualified_query_name") String qualifiedQueryName,
                                                                     @PathVariable(value = "version") String version) {

        log.debug("deleteStoredQuery for the following input: " +  qualifiedQueryName + ", version:"+version);

        QueryDefinitionResponseData queryDefinitionResponseData = new QueryDefinitionResponseData(queryService.deleteStoredQuery(qualifiedQueryName, version));

        return ResponseEntity.ok(queryDefinitionResponseData);
    }
}
