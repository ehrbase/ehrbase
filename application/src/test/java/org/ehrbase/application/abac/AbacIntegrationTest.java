package org.ehrbase.application.abac;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    properties = {"abac.disabled=false", "abac.server=http://localhost:8080/abac/"})
@AutoConfigureMockMvc
class AbacIntegrationTest {

  private static final String PATIENT_ID = "55773424-a6b8-43f3-952c-c63ac5cbf048";
  private static final String ORGA_ID = "f47bfc11-ec8d-412e-aebf-c6953cc23e7d";
  @MockBean
  private AbacCheck abacCheck;
  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private AbacConfig abacConfig;
  private String ehrId = "";

  @Test
  @EnabledIfEnvironmentVariable(named = "EHRBASE_ABAC_IT_TEST", matches = "true")
  public void testAbacIntegrationTest() throws Exception {
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

    ehrId = JsonPath.read(result.getResponse().getContentAsString(), "$.ehr_id.value");
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
    //.andExpect(status().isOk() || status().isConflict());

    stream = CompositionTestDataCanonicalJson.CORONA.getStream();
    Assertions.assertNotNull(stream);
    streamString = IOUtils.toString(stream, UTF_8);

    mockMvc.perform(post(String.format("/rest/openehr/v1/ehr/%s/composition", ehrId))
        .with(jwt().authorities(new OAuth2UserAuthority("ROLE_USER", attributes)).
            jwt(token -> token.claim(abacConfig.getPatientClaim(), externalSubjectRef)
                .claim(abacConfig.getOrganizationClaim(), ORGA_ID)))
        .content(streamString)
        .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());

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
  }
}