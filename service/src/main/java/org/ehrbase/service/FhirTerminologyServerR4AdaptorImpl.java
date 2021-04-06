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
import org.ehrbase.dao.access.interfaces.I_OpenehrTerminologyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Luis Marco-Ruiz
 */
@Component
public class FhirTerminologyServerR4AdaptorImpl implements I_OpenehrTerminologyServer {

    private static final String FHIR_JSON_MEDIA_TYPE = "application/fhir+json";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final WebClient webClient;

    private final FhirTsProps props;

    public FhirTerminologyServerR4AdaptorImpl(WebClient webClient, FhirTsProps props) {
        this.webClient = webClient;
        this.props = props;
    }

    @Override
    public List<DvCodedText> expand(final String valueSetId) {
        String response = webClient.get()
                .uri(valueSetId)
                .header(HttpHeaders.ACCEPT, FHIR_JSON_MEDIA_TYPE)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        DocumentContext jsonContext = JsonPath.parse(response);
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

        String response = webClient.get()
                .uri(urlTsServer.replace("'", ""))
                .header(HttpHeaders.ACCEPT, FHIR_JSON_MEDIA_TYPE)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        DocumentContext jsonContext = JsonPath.parse(response);
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

        String response = webClient.get()
                .uri(urlTsServer.replace("'", ""))
                .header(HttpHeaders.ACCEPT, FHIR_JSON_MEDIA_TYPE)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        DocumentContext jsonContext = JsonPath.parse(response);
        return (Boolean) ((JSONArray) jsonContext.read(props.getValidationResultPath()/* "$.parameter[:1].valueBoolean" */)).get(0);
    }
}
