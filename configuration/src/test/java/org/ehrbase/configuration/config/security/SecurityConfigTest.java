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
package org.ehrbase.configuration.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

class SecurityConfigTest {

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setServletPath(path);
        return request;
    }

    @Test
    void validPathPatternUsesPathPatternMatcher() {
        RequestMatcher matcher = SecurityConfig.requestMatcherFor("/rest/**");

        assertThat(matcher).isInstanceOf(PathPatternRequestMatcher.class);
        assertThat(matcher.matches(request("/rest/openehr/v1/ehr"))).isTrue();
        assertThat(matcher.matches(request("/other"))).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "/rest/**/composition, /rest/openehr/v1/composition, true",
        "/rest/**/composition, /rest/a/b/c/composition, true",
        "/rest/**/composition, /rest/openehr/v1/ehr, false",
        "/rest/**/admin/**, /rest/openehr/admin/delete, true",
        "/rest/**/admin/**, /rest/openehr/v1/ehr, false",
    })
    void antOnlyPatternFallsBackToAntMatching(String pattern, String path, boolean expected) {
        RequestMatcher matcher = SecurityConfig.requestMatcherFor(pattern);

        assertThat(matcher).isNotInstanceOf(PathPatternRequestMatcher.class);
        assertThat(matcher.matches(request(path))).isEqualTo(expected);
    }

    @Test
    void patternWithoutLeadingSlashFallsBackWithoutFailingStartup() {
        RequestMatcher matcher = SecurityConfig.requestMatcherFor("plugin/**");

        assertThat(matcher.matches(request("/plugin/event-trigger"))).isFalse();
    }

    @Test
    void bareDoubleWildcardMatchesEveryRequest() {
        RequestMatcher matcher = SecurityConfig.requestMatcherFor("**");

        assertThat(matcher.matches(request("/rest/openehr/v1/ehr"))).isTrue();
        assertThat(matcher.matches(request("/"))).isTrue();
    }

    @Test
    void emptyPatternFailsAtStartup() {
        assertThatThrownBy(() -> SecurityConfig.requestMatcherFor(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SecurityConfig.requestMatcherFor(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
