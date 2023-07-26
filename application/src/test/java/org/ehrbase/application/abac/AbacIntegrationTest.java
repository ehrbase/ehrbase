/*
 * Copyright (c) 2021 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.application.abac;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.ehrbase.openehr.sdk.test_data.composition.CompositionTestDataCanonicalJson;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.ResourceUtils;

@SpringBootTest
@ActiveProfiles({"local", "test"})
// @EnabledIfEnvironmentVariable(named = "EHRBASE_ABAC_IT_TEST", matches = "true")
@AutoConfigureMockMvc
@Disabled("see https://jira.vitagroup.ag/browse/CDR-368")
class AbacIntegrationTest {

    private static final String ORGA_ID = "f47bfc11-ec8d-412e-aebf-c6953cc23e7d";

    @MockBean
    private AbacConfig.AbacCheck abacCheck;

    @MockBean
    private BuildProperties buildProperties;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AbacConfig abacConfig;

    @Test
    /*
     * This test requires a new and clean DB state to run successfully.
     */

    void testAbacIntegrationTest() throws Exception {
        /*
             ----------------- TEST CONTEXT SETUP -----------------
        */
        // Configure the mock bean of the ABAC server, so we can test with this external service.
        given(this.abacCheck.execute(anyString(), anyMap())).willReturn(true);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "my-id");
        attributes.put("email", "test@test.org");

        // Counters to keep track of number of requests to mock ABAC server bean
        int hasConsentPatientCount = 0;
        int hasConsentTemplateCount = 0;

        String externalSubjectRef = UUID.randomUUID().toString();

        String ehrStatus = String.format(
                IOUtils.toString(ResourceUtils.getURL("classpath:ehr_status.json"), StandardCharsets.UTF_8),
                externalSubjectRef);

        MvcResult result = mockMvc.perform(post("/rest/openehr/v1/ehr")
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .header("PREFER", "return=representation")
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ehrStatus))
                .andExpectAll(status().isCreated(), jsonPath("$.ehr_id.value").exists())
                .andReturn();

        String ehrId = JsonPath.read(result.getResponse().getContentAsString(), "$.ehr_id.value");
        Assertions.assertNotNull(ehrId);
        assertNotEquals("", ehrId);

        InputStream stream = OperationalTemplateTestData.CORONA_ANAMNESE.getStream();
        Assertions.assertNotNull(stream);
        String streamString = IOUtils.toString(stream, StandardCharsets.UTF_8);

        mockMvc.perform(post("/rest/openehr/v1/definition/template/adl1.4/")
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .content(streamString)
                        .contentType(MediaType.APPLICATION_XML)
                        .header("PREFER", "return=representation")
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(r -> assertTrue(
                        // created 201 or conflict 409 are okay
                        r.getResponse().getStatus() == HttpStatus.CREATED.value()
                                || r.getResponse().getStatus() == HttpStatus.CONFLICT.value()));

        stream = CompositionTestDataCanonicalJson.CORONA.getStream();
        Assertions.assertNotNull(stream);
        streamString = IOUtils.toString(stream, StandardCharsets.UTF_8);

        /*
             ----------------- TEST CASES -----------------
        */

        /*
             GET EHR
        */
        mockMvc.perform(get(String.format("/rest/openehr/v1/ehr/%s", ehrId))
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .header("PREFER", "return=representation")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        verify(abacCheck, times(++hasConsentPatientCount))
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_patient", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                    }
                });
        /*
             GET EHR_STATUS
        */
        result = mockMvc.perform(get(String.format("/rest/openehr/v1/ehr/%s/ehr_status", ehrId))
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .header("PREFER", "return=representation")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn();

        verify(abacCheck, times(++hasConsentPatientCount))
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_patient", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                    }
                });

        String ehrStatusVersionUid = JsonPath.read(result.getResponse().getContentAsString(), "$.uid.value");
        Assertions.assertNotNull(ehrStatusVersionUid);
        assertNotEquals("", ehrStatusVersionUid);

        /*
             PUT EHR_STATUS
        */
        mockMvc.perform(put(String.format("/rest/openehr/v1/ehr/%s/ehr_status", ehrId))
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .header("If-Match", ehrStatusVersionUid)
                        .header("PREFER", "return=representation")
                        .content(ehrStatus)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        verify(abacCheck, times(++hasConsentPatientCount))
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_patient", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                    }
                });
        /*
             GET VERSIONED_EHR_STATUS
        */
        mockMvc.perform(get(String.format("/rest/openehr/v1/ehr/%s/versioned_ehr_status/version", ehrId))
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .header("PREFER", "return=representation")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        verify(abacCheck, times(++hasConsentPatientCount))
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_patient", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                    }
                });
        /*
             POST COMPOSITION
        */
        result = mockMvc.perform(post(String.format("/rest/openehr/v1/ehr/%s/composition", ehrId))
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .content(streamString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("PREFER", "return=representation")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andReturn();

        String compositionVersionUid = JsonPath.read(result.getResponse().getContentAsString(), "$.uid.value");
        Assertions.assertNotNull(compositionVersionUid);
        assertNotEquals("", compositionVersionUid);
        assertTrue(compositionVersionUid.contains("::"));

        verify(abacCheck, times(++hasConsentTemplateCount))
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_template", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                        put("template", "Corona_Anamnese");
                    }
                });
        /*
             GET VERSIONED_COMPOSITION
        */
        mockMvc.perform(get(String.format(
                                "/rest/openehr/v1/ehr/%s/versioned_composition/%s/version/%s",
                                ehrId, compositionVersionUid.split("::")[0], compositionVersionUid))
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .header("PREFER", "return=representation")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        verify(abacCheck, times(++hasConsentTemplateCount))
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_template", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                        put("template", "Corona_Anamnese");
                    }
                });

        /*
             GET COMPOSITION (here of deleted composition)
        */
        mockMvc.perform(get(String.format("/rest/openehr/v1/ehr/%s/composition/%s", ehrId, compositionVersionUid))
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .header("PREFER", "return=representation")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        // Failing: Does not call ABAC Server. Deleted composition does not need to call ABAC server?
        verify(abacCheck, times(++hasConsentTemplateCount))
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_template", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                        put("template", "Corona_Anamnese");
                    }
                });

        /*
             DELETE COMPOSITION
        */
        mockMvc.perform(delete(String.format("/rest/openehr/v1/ehr/%s/composition/%s", ehrId, compositionVersionUid))
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .header("PREFER", "return=representation")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isNoContent());

        verify(abacCheck, times(++hasConsentTemplateCount))
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_template", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                        put("template", "Corona_Anamnese");
                    }
                });

        String contribution = String.format(
                IOUtils.toString(ResourceUtils.getURL("classpath:contribution.json"), StandardCharsets.UTF_8),
                streamString);
        /*
             POST CONTRIBUTION
        */
        mockMvc.perform(post(String.format("/rest/openehr/v1/ehr/%s/contribution", ehrId))
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .content(contribution)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("PREFER", "return=representation")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andReturn();

        verify(abacCheck, times(++hasConsentTemplateCount))
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_template", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                        put("template", "Corona_Anamnese");
                    }
                });

        /*
             POST QUERY
        */
        mockMvc.perform(post("/rest/openehr/v1/query/aql")
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .content("{\n"
                                + "    \"q\": \"select e/ehr_id/value, c/uid/value, c/archetype_details/template_id/value, c/feeder_audit from EHR e CONTAINS composition c\"\n"
                                + "}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk());

        verify(abacCheck, times(++hasConsentTemplateCount))
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_template", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                        put("template", "Corona_Anamnese");
                    }
                });

        /*
             GET QUERY
        */
        String pathQuery =
                "select e/ehr_id/value, c/uid/value, c/archetype_details/template_id/value, c/feeder_audit from EHR e CONTAINS composition c";

        mockMvc.perform(get(String.format("/rest/openehr/v1/query/aql?q=%s", pathQuery))
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID))))
                .andExpect(status().isOk());

        verify(abacCheck, times(++hasConsentTemplateCount))
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_template", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                        put("template", "Corona_Anamnese");
                    }
                });

        /*
             GET QUERY WITH MULTIPLE EHRs AND TEMPLATES (incl. posting those)
        */
        // post another template
        stream = OperationalTemplateTestData.MINIMAL_EVALUATION.getStream();
        Assertions.assertNotNull(stream);
        streamString = IOUtils.toString(stream, StandardCharsets.UTF_8);

        mockMvc.perform(post("/rest/openehr/v1/definition/template/adl1.4/")
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .content(streamString)
                        .contentType(MediaType.APPLICATION_XML)
                        .header("PREFER", "return=representation")
                        .accept(MediaType.APPLICATION_XML))
                .andExpect(r -> assertTrue(
                        // created 201 or conflict 409 are okay
                        r.getResponse().getStatus() == HttpStatus.CREATED.value()
                                || r.getResponse().getStatus() == HttpStatus.CONFLICT.value()));

        streamString = IOUtils.toString(ResourceUtils.getURL("classpath:composition.json"), StandardCharsets.UTF_8);

        mockMvc.perform(post(String.format("/rest/openehr/v1/ehr/%s/composition", ehrId))
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
                        .content(streamString)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("PREFER", "return=representation")
                        .accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isCreated())
                .andReturn();

        verify(abacCheck)
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_template", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                        put("template", "minimal_evaluation.en.v1");
                    }
                });

        mockMvc.perform(get(String.format("/rest/openehr/v1/query/aql?q=%s", pathQuery))
                        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes))
                                .jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                                        .claim(abacConfig.getOrganizationClaim(), ORGA_ID))))
                .andExpect(status().isOk());

        //    verify(abacCheck, times(++hasConsentTemplateCount)).execute(
        //        "http://localhost:3001/rest/v1/policy/execute/name/has_consent_template", new HashMap<>() {{
        //          put("patient", externalSubjectRef);
        //          put("organization", ORGA_ID);
        //          put("template", "Corona_Anamnese");
        //        }});

        verify(abacCheck, times(3))
                .execute("http://localhost:3001/rest/v1/policy/execute/name/has_consent_template", new HashMap<>() {
                    {
                        put("patient", externalSubjectRef);
                        put("organization", ORGA_ID);
                        put("template", "minimal_evaluation.en.v1");
                    }
                });
    }
}
