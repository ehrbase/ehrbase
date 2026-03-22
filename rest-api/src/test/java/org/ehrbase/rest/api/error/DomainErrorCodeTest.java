/*
 * Copyright (c) 2024 vitasystems GmbH.
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
package org.ehrbase.rest.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.ProblemDetail;

class DomainErrorCodeTest {

    @ParameterizedTest
    @EnumSource(DomainErrorCode.class)
    void everyCodeHasValidTypeUri(DomainErrorCode code) {
        URI typeUri = code.typeUri();
        assertThat(typeUri.toString()).startsWith("https://ehrbase.org/errors/");
        assertThat(typeUri.toString()).doesNotContain("_");
    }

    @ParameterizedTest
    @EnumSource(DomainErrorCode.class)
    void everyCodeHasNonNullStatusAndTitle(DomainErrorCode code) {
        assertThat(code.status()).isNotNull();
        assertThat(code.title()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(DomainErrorCode.class)
    void toProblemDetailHasAllRequiredFields(DomainErrorCode code) {
        ProblemDetail pd = code.toProblemDetail("test detail");
        assertThat(pd.getStatus()).isEqualTo(code.status().value());
        assertThat(pd.getType()).isEqualTo(code.typeUri());
        assertThat(pd.getTitle()).isEqualTo(code.title());
        assertThat(pd.getDetail()).isEqualTo("test detail");
        assertThat(pd.getProperties()).containsEntry("code", code.name());
    }

    @Test
    void toProblemDetailWithInstance() {
        URI instance = URI.create("/api/v2/test/resource");
        ProblemDetail pd = DomainErrorCode.EHR_NOT_FOUND.toProblemDetail("msg", instance);
        assertThat(pd.getInstance()).isEqualTo(instance);
    }

    @Test
    void typeUriFormat() {
        assertThat(DomainErrorCode.EHR_NOT_FOUND.typeUri().toString())
                .isEqualTo("https://ehrbase.org/errors/ehr-not-found");
        assertThat(DomainErrorCode.COMPOSITION_VERSION_MISMATCH.typeUri().toString())
                .isEqualTo("https://ehrbase.org/errors/composition-version-mismatch");
    }
}
