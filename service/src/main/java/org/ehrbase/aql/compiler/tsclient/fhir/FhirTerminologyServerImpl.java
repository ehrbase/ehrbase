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

import org.ehrbase.aql.compiler.tsclient.TerminologyServer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
/***
 *@Created by Luis Marco-Ruiz on Feb 12, 2020
 */
public class FhirTerminologyServerImpl  implements TerminologyServer<String, String>{

	/*@Override
	public List expand(String valueSetId) {
		
		RestTemplate rest = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set("accept","application/fhir+json");
		HttpEntity<ValueSet> entity =  new HttpEntity<ValueSet>(headers);
		ResponseEntity<String> responseEntity = rest.exchange("https://r4.ontoserver.csiro.au/fhir/ValueSet/942e1d78-d481-416f-bebd-5754ba4d0b69/$expand/",
				HttpMethod.GET,
				entity,
				String.class);
		String response = responseEntity.getBody();

		System.out.println("THE RESPONSE FROM THE EXTERNAL FHIR SERVER IS: "+response);
		List<String> result = new ArrayList();
		result.add("48377-6");
		result.add("27478-7");
		result.add("52539-9");
		
		String jsonCodePath = "$[\"expansion\"][\"contains\"][*][\"code\"]";
		DocumentContext jsonContext = JsonPath.parse(response);
		List<String> jsonpathCreatorName = jsonContext.read(jsonCodePath);
		System.out.println(jsonpathCreatorName);
		
		return result;
	}*/
	
	@Override
	public List expand(String valueSetId) {
		
		RestTemplate rest = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set("accept","application/fhir+json");
		HttpEntity<String> entity =  new HttpEntity<String>(headers);
		ResponseEntity<String> responseEntity = rest.exchange(valueSetId,
				HttpMethod.GET,
				entity,
				String.class);
		String response = responseEntity.getBody();
		String jsonCodePath = "$[\"expansion\"][\"contains\"][*][\"code\"]";
		DocumentContext jsonContext = JsonPath.parse(response);
		List<String> jsonpathCreatorName = jsonContext.read(jsonCodePath);
		return jsonpathCreatorName;
	}

	@Override
	public Boolean validate(String concept, String valueSetId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SubsumptionResult subsumes(String conceptA, String conceptB) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lookUp(String conceptId) {
		// TODO Auto-generated method stub
		return null;
	}

}
