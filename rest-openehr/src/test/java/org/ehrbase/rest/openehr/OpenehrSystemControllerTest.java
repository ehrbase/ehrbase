/*
 * Copyright (c) 2026 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.rest.openehr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.ehrbase.api.dto.SystemOptionsDto;
import org.ehrbase.api.service.StatusService;
import org.ehrbase.rest.admin.AdminApiConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OpenehrSystemControllerTest {

    private final StatusService statusService = mock();
    private final AdminApiConfiguration adminApiConfiguration = new AdminApiConfiguration();

    private ResponseEntity<SystemOptionsDto> options(String buildVersion) {
        when(statusService.getEhrbaseVersion()).thenReturn(buildVersion);
        return new OpenehrSystemController(statusService, adminApiConfiguration).options();
    }

    @Test
    void options() {
        adminApiConfiguration.setActive(true);

        var response = options("2.38.0");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getAllow())
                .containsExactly(
                        HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.OPTIONS);
        assertThat(response.getBody())
                .isEqualTo(new SystemOptionsDto(
                        "EHRbase",
                        "v2.38.0",
                        "Vitagroup",
                        "1.0.3",
                        "STANDARD",
                        List.of("/ehr", "/definition", "/query", "/admin")));
    }

    @Test
    void endpointsOmitAdminWhenAdminApiIsInactive() {
        assertThat(options("2.38.0").getBody())
                .extracting(SystemOptionsDto::endpoints)
                .isEqualTo(List.of("/ehr", "/definition", "/query"));
    }

    @ParameterizedTest
    @CsvSource({"2.38.0-SNAPSHOT, v2.38.0-SNAPSHOT", "<unknown>, <unknown>"})
    void solutionVersion(String buildVersion, String expected) {
        assertThat(options(buildVersion).getBody())
                .extracting(SystemOptionsDto::solutionVersion)
                .isEqualTo(expected);
    }
}
