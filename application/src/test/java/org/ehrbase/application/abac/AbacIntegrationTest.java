/*
 * Copyright (c) 2021 Vitasystems GmbH and Jake Smolka (Hannover Medical School).
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

package org.ehrbase.application.abac;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.ehrbase.application.EhrBase;
import org.ehrbase.test_data.composition.CompositionTestDataCanonicalJson;
import org.ehrbase.test_data.operationaltemplate.OperationalTemplateTestData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    classes = EhrBase.class,
    properties = {"abac.enabled=true"})
@AutoConfigureMockMvc
class AbacIntegrationTest {

  private static final String ORGA_ID = "f47bfc11-ec8d-412e-aebf-c6953cc23e7d";
  @MockBean
  private AbacConfig.AbacCheck abacCheck;
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private AbacConfig abacConfig;

  @Test
  @EnabledIfEnvironmentVariable(named = "EHRBASE_ABAC_IT_TEST", matches = "true")
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

    String externalSubjectRef = UUID.randomUUID().toString();
    String ehrStatus = String.format("{\n"
        + "            \"_type\": \"EHR_STATUS\",\n"
        + "            \"archetype_node_id\": \"openEHR-EHR-EHR_STATUS.generic.v1\",\n"
        + "            \"name\": {\n"
        + "                \"value\": \"EHR Status\"\n"
        + "            },\n"
        + "            \"subject\": {\n"
        + "                \"external_ref\": {\n"
        + "                    \"id\": {\n"
        + "                        \"_type\": \"GENERIC_ID\",\n"
        + "                        \"value\": \"%s\",\n"
        + "                        \"scheme\": \"id_scheme\"\n"
        + "                    },\n"
        + "                    \"namespace\": \"examples\",\n"
        + "                    \"type\": \"PERSON\"\n"
        + "                }\n"
        + "            },\n"
        + "            \"is_queryable\": true,\n"
        + "            \"is_modifiable\": true\n"
        + "        }", externalSubjectRef);

    MvcResult result = mockMvc.perform(post("/rest/openehr/v1/ehr")
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .header("PREFER", "return=representation")
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .contentType(MediaType.APPLICATION_JSON)
        .content(ehrStatus)
    )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.ehr_id.value").exists())
        .andReturn();

    String ehrId = JsonPath.read(result.getResponse().getContentAsString(), "$.ehr_id.value");
    Assertions.assertNotNull(ehrId);
    assertNotEquals("", ehrId);

    InputStream stream = OperationalTemplateTestData.CORONA_ANAMNESE.getStream();
    Assertions.assertNotNull(stream);
    String streamString = IOUtils.toString(stream, UTF_8);

    mockMvc.perform(post("/rest/openehr/v1/definition/template/adl1.4/")
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .content(streamString)
        .contentType(MediaType.APPLICATION_XML)
        .header("PREFER", "return=representation")
        .accept(MediaType.APPLICATION_XML)
    )
        .andExpect(r -> assertTrue(
            // created 201 or conflict 409 are okay
            r.getResponse().getStatus() == HttpStatus.CREATED.value() ||
                r.getResponse().getStatus() == HttpStatus.CONFLICT.value()));

    stream = CompositionTestDataCanonicalJson.CORONA.getStream();
    Assertions.assertNotNull(stream);
    streamString = IOUtils.toString(stream, UTF_8);

    /*
          ----------------- TEST CASES -----------------
     */

    /*
          GET EHR
     */
    mockMvc.perform(get(String.format("/rest/openehr/v1/ehr/%s", ehrId))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .header("PREFER", "return=representation")
        .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk());

    /*
          GET EHR_STATUS
     */
    result = mockMvc.perform(get(String.format("/rest/openehr/v1/ehr/%s/ehr_status", ehrId))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .header("PREFER", "return=representation")
        .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andReturn();

    String ehrStatusVersionUid = JsonPath.read(result.getResponse().getContentAsString(), "$.uid.value");
    Assertions.assertNotNull(ehrStatusVersionUid);
    assertNotEquals("", ehrStatusVersionUid);

    /*
          PUT EHR_STATUS
     */
    mockMvc.perform(put(String.format("/rest/openehr/v1/ehr/%s/ehr_status", ehrId))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .header("If-Match", ehrStatusVersionUid)
        .header("PREFER", "return=representation")
        .content(ehrStatus)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk());

    /*
          GET VERSIONED_EHR_STATUS
     */
    mockMvc.perform(get(String.format("/rest/openehr/v1/ehr/%s/versioned_ehr_status/version", ehrId))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .header("PREFER", "return=representation")
        .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andReturn();

    /*
          POST COMPOSITION
     */
    result = mockMvc.perform(post(String.format("/rest/openehr/v1/ehr/%s/composition", ehrId))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
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

    /*
          GET VERSIONED_COMPOSITION
     */
    mockMvc.perform(get(String.format("/rest/openehr/v1/ehr/%s/versioned_composition/%s/version/%s",
        ehrId, compositionVersionUid.split("::")[0], compositionVersionUid))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .header("PREFER", "return=representation")
        .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andReturn();

    /*
          DELETE COMPOSITION
     */
    mockMvc.perform(delete(String.format("/rest/openehr/v1/ehr/%s/composition/%s", ehrId, compositionVersionUid))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .header("PREFER", "return=representation")
        .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isNoContent());

    /*
          GET COMPOSITION (here of deleted composition)
     */
    mockMvc.perform(get(String.format("/rest/openehr/v1/ehr/%s/composition/%s", ehrId, compositionVersionUid))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .header("PREFER", "return=representation")
        .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isNoContent());


    String contribution = String.format("{\n"
        + "  \"_type\": \"CONTRIBUTION\",\n"
        + "  \"versions\": [\n"
        + "    {\n"
        + "      \"_type\": \"ORIGINAL_VERSION\",\n"
        + "      \"commit_audit\": {\n"
        + "        \"_type\": \"AUDIT_DETAILS\",\n"
        + "        \"system_id\": \"test-system-id\",\n"
        + "        \"committer\": {\n"
        + "          \"_type\": \"PARTY_IDENTIFIED\",\n"
        + "          \"name\": \"<optional name of the committer>\",\n"
        + "          \"external_ref\": {\n"
        + "            \"id\": {\n"
        + "              \"_type\": \"GENERIC_ID\",\n"
        + "              \"value\": \"<OBJECT_ID>\",\n"
        + "              \"scheme\": \"<ID SCHEME NAME>\"\n"
        + "            },\n"
        + "            \"namespace\": \"demographic\",\n"
        + "            \"type\": \"PERSON\"\n"
        + "          }\n"
        + "        },\n"
        + "        \"change_type\": {\n"
        + "          \"value\": \"creation\",\n"
        + "          \"defining_code\": {\n"
        + "            \"terminology_id\": {\n"
        + "              \"value\": \"openehr\"\n"
        + "            },\n"
        + "            \"code_string\": \"249\"\n"
        + "          }\n"
        + "        },\n"
        + "        \"description\": {\n"
        + "          \"value\": \"<optional audit description>\"\n"
        + "        }\n"
        + "      },\n"
        + "      \"data\": \n"
        + "        %s"
        + "      ,\n"
        + "      \"lifecycle_state\": {\n"
        + "        \"value\": \"complete\",\n"
        + "        \"defining_code\": {\n"
        + "          \"terminology_id\": {\n"
        + "            \"value\": \"openehr\"\n"
        + "          },\n"
        + "          \"code_string\": \"532\"\n"
        + "        }\n"
        + "      }\n"
        + "    }\n"
        + "  ],\n"
        + "  \"audit\": {\n"
        + "    \"_type\": \"AUDIT_DETAILS\",\n"
        + "    \"system_id\": \"test-system-id\",\n"
        + "    \"committer\": {\n"
        + "      \"_type\": \"PARTY_IDENTIFIED\",\n"
        + "      \"name\": \"<optional name of the committer>\",\n"
        + "      \"external_ref\": {\n"
        + "        \"id\": {\n"
        + "          \"_type\": \"GENERIC_ID\",\n"
        + "          \"value\": \"<OBJECT_ID>\",\n"
        + "          \"scheme\": \"<ID SCHEME NAME>\"\n"
        + "        },\n"
        + "        \"namespace\": \"demographic\",\n"
        + "        \"type\": \"PERSON\"\n"
        + "      }\n"
        + "    },\n"
        + "    \"change_type\": {\n"
        + "      \"value\": \"creation\",\n"
        + "      \"defining_code\": {\n"
        + "        \"terminology_id\": {\n"
        + "          \"value\": \"openehr\"\n"
        + "        },\n"
        + "        \"code_string\": \"249\"\n"
        + "      }\n"
        + "    },\n"
        + "    \"description\": {\n"
        + "      \"value\": \"<optional audit description>\"\n"
        + "    }\n"
        + "  }\n"
        + "}\n"
        + "\n", streamString);

    /*
          POST CONTRIBUTION
     */
    mockMvc.perform(post(String.format("/rest/openehr/v1/ehr/%s/contribution", ehrId))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .content(contribution)
        .contentType(MediaType.APPLICATION_JSON)
        .header("PREFER", "return=representation")
        .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isCreated())
        .andReturn();

    /*
          POST QUERY
     */
    mockMvc.perform(post("/rest/openehr/v1/query/aql")
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .content("{\n"
            + "  \"q\": \"select\n"
            + "    e/ehr_id/value, c/uid/value, c/archetype_details/template_id/value, c/feeder_audit from EHR e CONTAINS composition c\n"
            + "   \"\n"
            + "}")
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    /*
          GET QUERY
     */
    String pathQuery = "select e/ehr_id/value, c/uid/value, c/archetype_details/template_id/value, c/feeder_audit from EHR e CONTAINS composition c";

    mockMvc.perform(get(String.format("/rest/openehr/v1/query/aql?q=%s", pathQuery))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        )
        .andExpect(status().isOk());

    /*
          GET QUERY WITH MULTIPLE EHRs AND TEMPLATES (incl. posting those)
     */
    // post another template
    stream = OperationalTemplateTestData.MINIMAL_EVALUATION.getStream();
    Assertions.assertNotNull(stream);
    streamString = IOUtils.toString(stream, UTF_8);

    mockMvc.perform(post("/rest/openehr/v1/definition/template/adl1.4/")
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .content(streamString)
        .contentType(MediaType.APPLICATION_XML)
        .header("PREFER", "return=representation")
        .accept(MediaType.APPLICATION_XML)
    )
        .andExpect(r -> assertTrue(
            // created 201 or conflict 409 are okay
            r.getResponse().getStatus() == HttpStatus.CREATED.value() ||
                r.getResponse().getStatus() == HttpStatus.CONFLICT.value()));

    // post another composition with that template
    stream = CompositionTestDataCanonicalJson.MINIMAL_EVAL.getStream();
    Assertions.assertNotNull(stream);
    streamString = IOUtils.toString(stream, UTF_8);

    mockMvc.perform(post(String.format("/rest/openehr/v1/ehr/%s/composition", ehrId))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .content(streamString)
        .contentType(MediaType.APPLICATION_JSON)
        .header("PREFER", "return=representation")
        .accept(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isCreated())
        .andReturn();

    mockMvc.perform(get(String.format("/rest/openehr/v1/query/aql?q=%s", pathQuery))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
    )
        .andExpect(status().isOk());

  }
}