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
public final class FhirTerminologyServerR4AdaptorImpl
		implements org.ehrbase.dao.access.interfaces.I_OpenehrTerminologyServer <String, String> {

	private static volatile FhirTerminologyServerR4AdaptorImpl  instance = null;//thread safety is ensure in the getInstance method.
	/**
	 * Returns an instance of {@link org.ehrbase.service.FhirTerminologyServerR4AdaptorImpl} with default properties or creates a new one it does not exist.
	 * @return the instance of {@link org.ehrbase.service.FhirTerminologyServerR4AdaptorImpl}.
	 */
	public static FhirTerminologyServerR4AdaptorImpl getInstance() {
		return FhirTerminologyServerR4AdaptorImpl.getInstance(null);
	}
	/**
	 * Returns an instance of {@link org.ehrbase.service.FhirTerminologyServerR4AdaptorImpl} with the properties provided or creates a new one it does not exist.
	 * @return the instance of {@link org.ehrbase.service.FhirTerminologyServerR4AdaptorImpl}.
	 */
	public static FhirTerminologyServerR4AdaptorImpl getInstance(FhirTsProps properties) {
		if(properties == null) {//if Spring did not do autowiring, take the default ones.
			properties = new FhirTsPropsImpl(); 
		}
		if(instance == null) {
			synchronized(FhirTerminologyServerR4AdaptorImpl.class) {
				if(instance == null) {
					instance = new FhirTerminologyServerR4AdaptorImpl(properties);
				}
			}
		}
		return instance;
	}

	private FhirTsPropsImpl props;

	@Autowired
	private FhirTerminologyServerR4AdaptorImpl(FhirTsProps props2) {
		super();
		this.props = (FhirTsPropsImpl) props2;
		synchronized(FhirTerminologyServerR4AdaptorImpl.class) {
			instance=this;//we need this for Spring initialization. Constructor set to private so other users rely on the getInstance method.
		}
	}


	@Override
	public final List<DvCodedText> expand(final String valueSetId) {
		RestTemplate rest = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set("accept","application/fhir+json");
		HttpEntity<String> entity =  new HttpEntity<>(headers);
		ResponseEntity<String> responseEntity = rest.exchange(valueSetId,
				HttpMethod.GET,
				entity,
				String.class);
		String response = responseEntity.getBody();
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
	public final List<DvCodedText> expandWithParameters(final String valueSetId, String...operationParams) {
		RestTemplate rest = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.set("accept","application/fhir+json");
		HttpEntity<String> entity =  new HttpEntity<>(headers);
		ResponseEntity<String> responseEntity = rest.exchange(valueSetId,
				HttpMethod.GET,
				entity,
				String.class);
		String response = responseEntity.getBody();
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
	public final DvCodedText lookUp(final String conceptId) {
		// TODO Auto-generated method stub
		return null;

	}

	@Override
	public final Boolean validate(final DvCodedText concept, final String valueSetId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public final SubsumptionResult subsumes(final DvCodedText conceptA, final DvCodedText conceptB) {
		// TODO Auto-generated method stub
		return null;
	}

}
