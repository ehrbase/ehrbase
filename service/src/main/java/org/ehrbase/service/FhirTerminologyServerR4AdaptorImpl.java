/*
 * Copyright (c) 2020 Vitasystems GmbH, Hannover Medical School, and Luis Marco-Ruiz (Hannover Medical School).
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
package org.ehrbase.service;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.support.identification.TerminologyId;
import net.minidev.json.JSONArray;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.ehrbase.api.exception.InternalServerException;
import org.ehrbase.dao.access.interfaces.I_OpenehrTerminologyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Luis Marco-Ruiz
 */
@Component
@SuppressWarnings("java:S6212")
public class FhirTerminologyServerR4AdaptorImpl implements I_OpenehrTerminologyServer {

    private static final String FHIR_JSON_MEDIA_TYPE = "application/fhir+json";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final HttpClient httpClient;

    private final FhirTsProps props;

    public FhirTerminologyServerR4AdaptorImpl(HttpClient httpClient, FhirTsProps props) {
        this.httpClient = httpClient;
        this.props = props;
    }

    @Override
    public List<DvCodedText> expand(final String valueSetId) {
        String responseBody;
        try {
            responseBody = internalGet(valueSetId);
        } catch (IOException e) {
            throw new InternalServerException("An error occurred while expanding ValueSet: " + valueSetId, e);
        }

        DocumentContext jsonContext = JsonPath.parse(responseBody);
        List<String> codeList = jsonContext.read(props.getCodePath().replace("\\", ""));
        List<String> systemList = jsonContext.read(props.getSystemPath());
        List<String> displayList = jsonContext.read(props.getDisplayPath());

        List<DvCodedText> expansionList = new ArrayList<>();
        for (int i = 0; i < codeList.size(); i++) {
            TerminologyId termId = new TerminologyId(systemList.get(i));
            CodePhrase codePhrase = new CodePhrase(termId, codeList.get(i));
            DvCodedText codedText = new DvCodedText(displayList.get(i), codePhrase);
            expansionList.add(codedText);
        }

        return expansionList;
    }

    @Override
    public List<DvCodedText> expandWithParameters(final String valueSetId, String... operationParams) {
        //build URL
        String urlTsServer = props.getTsUrl();
        urlTsServer += "ValueSet/$" + operationParams[0] + "?url=" + valueSetId;

        String responseBody;
        try {
            responseBody = internalGet(urlTsServer);
        } catch (IOException e) {
            throw new InternalServerException("An error occurred while expanding ValueSet " + valueSetId, e);
        }

        DocumentContext jsonContext = JsonPath.parse(responseBody);
        List<String> codeList = jsonContext.read(props.getCodePath().replace("\\", ""));
        List<String> systemList = jsonContext.read(props.getSystemPath());
        List<String> displayList = jsonContext.read(props.getDisplayPath());

        List<DvCodedText> expansionList = new ArrayList<>();
        for (int i = 0; i < codeList.size(); i++) {
            TerminologyId termId = new TerminologyId(systemList.get(i));
            CodePhrase codePhrase = new CodePhrase(termId, codeList.get(i));
            DvCodedText codedText = new DvCodedText(displayList.get(i), codePhrase);
            expansionList.add(codedText);
        }
        return expansionList;
    }

    @Override
    public DvCodedText lookUp(final String conceptId) {
        // TODO Auto-generated method stub
        return null;

    }

    @Override
    public Boolean validate(final DvCodedText concept, final String valueSetId) {
        // TODO Auto-generated method stub
        logger.debug("inside the validate method of R4 implementation");
        return null;
    }

    @Override
    public SubsumptionResult subsumes(final DvCodedText conceptA, final DvCodedText conceptB) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean validate(String... operationParams) {
        //build URL
        String urlTsServer = props.getTsUrl();
        urlTsServer += "ValueSet/$" + "validate-code" + "?" + operationParams[0];

        String response;
        try {
            response = internalGet(urlTsServer);
        } catch (IOException e) {
            throw new InternalServerException("An error occurred while validating the code: " + operationParams[0], e);
        }

        DocumentContext jsonContext = JsonPath.parse(response);
        return (Boolean) ((JSONArray) jsonContext.read(props.getValidationResultPath()/* "$.parameter[:1].valueBoolean" */)).get(0);
    }

    private String internalGet(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setHeader(HttpHeaders.ACCEPT, FHIR_JSON_MEDIA_TYPE);

        HttpResponse response = httpClient.execute(request);
        String responseBody = EntityUtils.toString(response.getEntity());
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new InternalServerException("Error response received from FHIR terminology server. HTTP status: " + statusCode + ". Body: " + responseBody);
        }

        return responseBody;
    }
}
