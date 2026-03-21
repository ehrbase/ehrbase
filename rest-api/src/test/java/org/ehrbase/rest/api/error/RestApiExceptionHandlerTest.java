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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.NotAcceptableException;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.UnprocessableEntityException;
import org.ehrbase.api.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Tests that every domain exception maps to the correct HTTP status and RFC 7807 ProblemDetail.
 */
class RestApiExceptionHandlerTest {

    private final RestApiExceptionHandler handler = new RestApiExceptionHandler();
    private final HttpServletRequest request = mockRequest("/api/v1/test");

    private static HttpServletRequest mockRequest(String uri) {
        HttpServletRequest r = mock(HttpServletRequest.class);
        when(r.getRequestURI()).thenReturn(uri);
        return r;
    }

    @Test
    void objectNotFoundReturns404() {
        ProblemDetail pd = handler.handleNotFound(
                new ObjectNotFoundException("ehr", "No EHR found with ID: abc"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getType().toString()).contains("ehr-not-found");
        assertThat(pd.getDetail()).contains("No EHR found");
    }

    @Test
    void compositionNotFoundReturns404() {
        ProblemDetail pd = handler.handleNotFound(
                new ObjectNotFoundException("composition", "Composition not found"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getType().toString()).contains("composition-not-found");
    }

    @Test
    void templateNotFoundReturns404() {
        ProblemDetail pd = handler.handleNotFound(
                new ObjectNotFoundException("template", "Template not found"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getType().toString()).contains("template-not-found");
    }

    @Test
    void preconditionFailedReturns412() {
        ProblemDetail pd = handler.handlePreconditionFailed(
                new PreconditionFailedException("Version mismatch"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.PRECONDITION_FAILED.value());
        assertThat(pd.getType().toString()).contains("composition-version-mismatch");
    }

    @Test
    void stateConflictReturns409() {
        ProblemDetail pd = handler.handleConflict(new StateConflictException("EHR not modifiable"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    void validationErrorReturns400() {
        ProblemDetail pd = handler.handleValidation(
                new ValidationException("Missing mandatory field"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getType().toString()).contains("validation-error");
    }

    @Test
    void unprocessableEntityReturns422() {
        ProblemDetail pd = handler.handleUnprocessable(
                new UnprocessableEntityException("Cannot update template"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @Test
    void invalidParamReturns400() {
        ProblemDetail pd = handler.handleInvalidParam(
                new InvalidApiParameterException("Invalid UUID"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getType().toString()).contains("invalid-parameter");
    }

    @Test
    void notAcceptableReturns406() {
        ProblemDetail pd = handler.handleNotAcceptable(
                new NotAcceptableException("Unsupported format"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_ACCEPTABLE.value());
    }

    @Test
    void securityExceptionReturns403() {
        ProblemDetail pd = handler.handleSecurity(new SecurityException("Access denied"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void unsupportedOperationReturns404() {
        ProblemDetail pd = handler.handleUnsupported(
                new UnsupportedOperationException("ADL 2.4 not implemented"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getType().toString()).contains("feature-disabled");
    }

    @Test
    void genericExceptionReturns500() {
        ProblemDetail pd = handler.handleGeneric(new RuntimeException("unexpected"), request);
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(pd.getType().toString()).contains("internal-error");
    }

    // RFC 7807 structure validation
    @Test
    void problemDetailHasRequiredFields() {
        ProblemDetail pd = handler.handleNotFound(
                new ObjectNotFoundException("ehr", "No EHR found with ID: abc"), request);
        assertThat(pd.getType()).isNotNull();
        assertThat(pd.getTitle()).isNotNull();
        assertThat(pd.getDetail()).isEqualTo("No EHR found with ID: abc");
        assertThat(pd.getInstance().toString()).isEqualTo("/api/v1/test");
        assertThat(pd.getProperties()).containsKey("code");
        assertThat(pd.getProperties().get("code")).isEqualTo("EHR_NOT_FOUND");
    }
}
