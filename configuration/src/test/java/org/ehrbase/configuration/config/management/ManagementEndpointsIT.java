package org.ehrbase.configuration.config.management;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.ehrbase.configuration.test.EhrbaseConfigurationIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@EhrbaseConfigurationIntegrationTest
@TestPropertySource(
        properties = {
            "management.endpoints.web.access=PUBLIC",
            "management.endpoints.web.exposure.include=health,info,metrics,prometheus",
            "management.endpoint.health.enabled=true",
            "management.endpoint.info.enabled=true",
            "management.endpoint.metrics.enabled=true",
            "management.endpoint.prometheus.enabled=true",
            "management.endpoint.health.probes.enabled=true",
            "management.health.livenessState.enabled=true",
            "management.health.readinessState.enabled=true",
        })
public class ManagementEndpointsIT {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"info", "metrics", "health", "prometheus", "health/liveness", "health/readiness"})
    void actuatorEndpointsShouldBeAccessible(String endpoint) throws Exception {
        mockMvc.perform(get("/management/" + endpoint)).andExpect(status().isOk());
    }
}
