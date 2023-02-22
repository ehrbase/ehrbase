/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.ehrbase.api.service.EhrService;
import org.ehrbase.aql.compiler.AuditVariables;
import org.ehrbase.rest.BaseController;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class CustomMethodSecurityExpressionRootTest {

    @Test
    void patientHandling() {
        String PAT_TOKEN = "myToken";
        Map<String, Object> reqMap = new HashMap<>();
        Assert.assertTrue(theTest(PAT_TOKEN, List.of(PAT_TOKEN, PAT_TOKEN), reqMap));
        Assert.assertTrue(reqMap.containsKey(CustomMethodSecurityExpressionRoot.PATIENT));
    }

    @Test
    void patientHandlingFail() {
        String PAT_TOKEN = "myToken";
        Assert.assertFalse(theTest("No_PAT_TOKEN", List.of(PAT_TOKEN, PAT_TOKEN), null));
    }

    @SuppressWarnings({"unchecked", "serial"})
    private boolean theTest(String theSubject, List<String> subjectExtRef, Map<String, Object> reqMap) {
        final String CLAIM = "myClaim";

        AbacConfig cfg = anyAbacConfig(c -> c.setPatientClaim(CLAIM));

        var token = Mockito.mock(JwtAuthenticationToken.class);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim(CLAIM, theSubject)
                .build();
        Mockito.when(((Jwt) token.getCredentials())).thenReturn(jwt);

        EhrService service = Mockito.mock(EhrService.class);
        Mockito.when(service.getSubjectExtRefs(Mockito.isA(Collection.class))).thenReturn(subjectExtRef);

        CustomMethodSecurityExpressionRoot expr =
                new CustomMethodSecurityExpressionRoot(Mockito.mock(Authentication.class), cfg, null);
        expr.setEhrService(service);

        Map<Object, Object> payload = new HashMap<>() {
            {
                put(AuditVariables.EHR_PATH, Set.of(UUID.randomUUID(), UUID.randomUUID()));
            }
        };

        return expr.patientHandling(token, theSubject, reqMap, BaseController.QUERY, payload);
    }

    @SuppressWarnings({"unchecked"})
    private AbacConfig anyAbacConfig(Consumer<AbacConfig>... constraints) {
        AbacConfig cfg = new AbacConfig();
        Stream.of(constraints).forEach(c -> c.accept(cfg));
        return cfg;
    }
}
