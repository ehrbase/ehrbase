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
import org.ehrbase.configuration.client.ClientProperties;
import org.ehrbase.configuration.client.WebClientConfiguration;
import org.ehrbase.dao.access.interfaces.I_OpenehrTerminologyServer;
import org.ehrbase.service.FhirTerminologyServerR4AdaptorImpl;
import org.ehrbase.service.FhirTsProps;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.web.reactive.function.client.WebClient;

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
            tsserver = new FhirTerminologyServerR4AdaptorImpl(WebClient.create(), props);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<DvCodedText> result = tsserver.expandWithParameters("http://hl7.org/fhir/ValueSet/surface", "expand");
        result.forEach((e) -> System.out.println(e.getValue()));
        // 1: Buccal
        assertThat(result.get(0).getDefiningCode().getCodeString()).isEqualTo("B");
        assertThat(result.get(0).getValue()).isEqualTo("Buccal");
        // 2: Distal
        assertThat(result.get(1).getDefiningCode().getCodeString()).isEqualTo("D");
        assertThat(result.get(1).getValue()).isEqualTo("Distal");
        // 3: Distoclusal
        assertThat(result.get(2).getDefiningCode().getCodeString()).isEqualTo("DO");
        assertThat(result.get(2).getValue()).isEqualTo("Distoclusal");
        // 4: Distoincisal
        assertThat(result.get(3).getDefiningCode().getCodeString()).isEqualTo("DI");
        assertThat(result.get(3).getValue()).isEqualTo("Distoincisal");

        assertThat(result.size()).isEqualTo(11);
    }

    @Ignore("Requires SSL configuration")
    @Test
    public void expandValueSetUsingSsl() {
        WebClient.Builder webClientBuilder = WebClient.builder();

        ClientProperties properties = new ClientProperties();
        ClientProperties.Ssl ssl = properties.getSsl();
//        ssl.setEnabled(true);
//        ssl.setKeyPassword("");
//        ssl.setKeyStore("");
//        ssl.setKeyStorePassword("");
//        ssl.setKeyStoreType("");
//        ssl.setTrustStore("");
//        ssl.setTrustStorePassword("");
//        ssl.setTrustStoreType("");

        WebClientConfiguration configuration = new WebClientConfiguration();
        WebClientCustomizer customizer = configuration.httpClientCustomizer(properties);
        customizer.customize(webClientBuilder);

        WebClient webClient = webClientBuilder.build();

        FhirTsProps props = new FhirTsProps();
        props.setCodePath("$[\"expansion\"][\"contains\"][*][\"code\"]");
        props.setDisplayPath("$[\"expansion\"][\"contains\"][*][\"display\"]");
        props.setSystemPath("$[\"expansion\"][\"contains\"][*][\"system\"]");
        props.setTsUrl("https://terminology-highmed.medic.medfak.uni-koeln.de/fhir/");
        try {
            tsserver = new FhirTerminologyServerR4AdaptorImpl(webClient, props);
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<DvCodedText> result = tsserver.expandWithParameters("https://www.netzwerk-universitaetsmedizin.de/fhir/ValueSet/frailty-score", "expand");
        result.forEach((e) -> System.out.println(e.getValue()));
        // 1: Very Severely Frail
        assertThat(result.get(0).getDefiningCode().getCodeString()).isEqualTo("8");
        assertThat(result.get(0).getValue()).isEqualTo("Very Severely Frail");
        // 2: Severely Frail
        assertThat(result.get(1).getDefiningCode().getCodeString()).isEqualTo("7");
        assertThat(result.get(1).getValue()).isEqualTo("Severely Frail");
        // 3: Terminally Ill
        assertThat(result.get(2).getDefiningCode().getCodeString()).isEqualTo("9");
        assertThat(result.get(2).getValue()).isEqualTo("Terminally Ill");
        // 4: Vulnerable
        assertThat(result.get(3).getDefiningCode().getCodeString()).isEqualTo("4");
        assertThat(result.get(3).getValue()).isEqualTo("Vulnerable");

        assertThat(result.size()).isEqualTo(9);
    }
}
