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

import java.util.ArrayList;
import java.util.List;

import org.ehrbase.api.definitions.FhirTsProps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.support.identification.TerminologyId;
/***
 *@Created by Luis Marco-Ruiz on Feb 12, 2020
 */
@Component
public class FhirTerminologyServerAdaptorImpl  implements org.ehrbase.dao.access.interfaces.I_OpenehrTerminologyServer<DvCodedText, String>{
	
	private static volatile FhirTerminologyServerAdaptorImpl  instance = null;
	public static FhirTerminologyServerAdaptorImpl getInstance() {
		return FhirTerminologyServerAdaptorImpl.getInstance(null);
	}
	public static FhirTerminologyServerAdaptorImpl getInstance(FhirTsProps properties) {
		if(properties == null) {//if Spring did not do autowiring, take the default ones.
			properties = new FhirTsPropsImpl(); 
		}
		if(instance == null) {
			synchronized(FhirTerminologyServerAdaptorImpl.class) {
				if(instance == null) {
					instance = new FhirTerminologyServerAdaptorImpl(properties);
				}
			}
		}
		return instance;
	}

	private FhirTsPropsImpl props;
	
	@Autowired
	private FhirTerminologyServerAdaptorImpl(FhirTsProps props2) {
		super();
		this.props = (FhirTsPropsImpl) props2;
		instance=this;
	}


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
		System.out.println("the response from CSIRO has been: "+response);

		DocumentContext jsonContext = JsonPath.parse(response);
		List<String> codeList = jsonContext.read(props.getCodePath().replace("\\", ""));
		List<String> systemList = jsonContext.read(props.getSystemPath());
		List<String> displayList = jsonContext.read(props.getDisplayPath());
		
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
