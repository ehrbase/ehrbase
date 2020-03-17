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
package org.ehrbase.aql.compiler.tsclient.fhir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ehrbase.aql.compiler.tsclient.OpenehrTerminologyServer;
import org.ehrbase.aql.compiler.tsclient.TerminologyServer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.support.identification.TerminologyId;
/***
 *@Created by Luis Marco-Ruiz on Feb 12, 2020
 */

public class FhirTerminologyServerAdaptorImpl  implements OpenehrTerminologyServer<DvCodedText, String>{
	
	private String codePath = "$[\"expansion\"][\"contains\"][*][\"code\"]";
	private String systemPath = "$[\"expansion\"][\"contains\"][*][\"system\"]";
	private String displayPath = "$[\"expansion\"][\"contains\"][*][\"display\"]";
	
	@ConfigurationProperties(prefix="terminology_server")
	@Override
	public List<DvCodedText> expand(String valueSetId) {
		
		RestTemplate rest = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set("accept","application/fhir+json");
		HttpEntity<String> entity =  new HttpEntity<String>(headers);
		ResponseEntity<String> responseEntity = rest.exchange(valueSetId,
				HttpMethod.GET,
				entity,
				String.class);
		String response = responseEntity.getBody();


		DocumentContext jsonContext = JsonPath.parse(response);
		List<String> codeList = jsonContext.read(codePath);
		List<String> systemList = jsonContext.read(systemPath);
		List<String> displayList = jsonContext.read(displayPath);
		
		List<DvCodedText> expansionList = new ArrayList<>();
		for(int i = 0; i< codeList.size(); i++) {
			TerminologyId termId = new TerminologyId(systemList.get(i));
			CodePhrase codePhrase = new CodePhrase(termId, codeList.get(i));
			DvCodedText codedText = new DvCodedText(displayList.get(i), codePhrase);
			expansionList.add(codedText);
		}
		return expansionList;
	}


	@Override
	public DvCodedText lookUp(String conceptId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean validate(DvCodedText concept, String valueSetId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SubsumptionResult subsumes(DvCodedText conceptA, DvCodedText conceptB) {
		// TODO Auto-generated method stub
		return null;
	}

}
