package org.ehrbase.application.cors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.cache.type=simple"})
@AutoConfigureMockMvc
class CorsNoAuthIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCors() throws Exception {
        mockMvc.perform(options("/rest/openehr/v1/definition/template/adl1.4")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Origin", "https://client.ehrbase.org"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
