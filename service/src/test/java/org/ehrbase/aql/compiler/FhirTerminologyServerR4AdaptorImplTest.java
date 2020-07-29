package org.ehrbase.aql.compiler;


/*
 * Copyright (c) 2020 Luis Marco-Ruiz (Hannover Medical School) and Vitasystems GmbH.
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

import com.nedap.archie.rm.datavalues.DvCodedText;
import org.ehrbase.dao.access.interfaces.I_OpenehrTerminologyServer;
import org.ehrbase.service.FhirTerminologyServerR4AdaptorImpl;
import org.ehrbase.service.FhirTsProps;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//@RunWith(SpringRunner.class)
//@SpringBootTest//(classes= {org.ehrbase.application.EhrBase.class})
//@ActiveProfiles("test")
public class FhirTerminologyServerR4AdaptorImplTest {

	//@Autowired
	private I_OpenehrTerminologyServer tsserver;

	@Ignore("This test runs against ontoserver sample inteance. It is deactivated until we have a test FHIR terminology server and the architecture allows to run Spring integration tests.")
	@Test
	public void shouldRetrieveValueSet() {

		FhirTsProps props = new FhirTsProps();
		props.setCodePath("$[\"expansion\"][\"contains\"][*][\"code\"]");
		props.setDisplayPath("$[\"expansion\"][\"contains\"][*][\"display\"]");
		props.setSystemPath("$[\"expansion\"][\"contains\"][*][\"system\"]");
		props.setTsUrl("https://r4.ontoserver.csiro.au/fhir/");
		try {
			tsserver = new FhirTerminologyServerR4AdaptorImpl(props);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		List<DvCodedText> result = tsserver.expandWithParameters("http://hl7.org/fhir/ValueSet/surface","ValueSet/$expand");
		result.forEach((e)->System.out.println(e.getValue()));
		assertThat(result.get(0).getValue()).isEqualTo("Occlusal");
		assertThat(result.get(0).getDefiningCode().getCodeString()).isEqualTo("O");
		assertThat(result.get(1).getDefiningCode().getCodeString()).isEqualTo("M");
		assertThat(result.get(1).getValue()).isEqualTo("Mesial");
		assertThat(result.get(2).getValue()).isEqualTo("Distoclusal");
		assertThat(result.get(2).getDefiningCode().getCodeString()).isEqualTo("DO");
		assertThat(result.get(3).getDefiningCode().getCodeString()).isEqualTo("L");
		assertThat(result.get(4).getDefiningCode().getCodeString()).isEqualTo("I");
		assertThat(result.get(5).getDefiningCode().getCodeString()).isEqualTo("V");
		assertThat(result.get(6).getDefiningCode().getCodeString()).isEqualTo("MOD");
		assertThat(result.get(6).getValue()).isEqualTo("Mesioclusodistal");
	}
}
