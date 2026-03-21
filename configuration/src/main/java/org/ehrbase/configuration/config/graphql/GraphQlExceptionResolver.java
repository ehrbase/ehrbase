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
package org.ehrbase.configuration.config.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.ehrbase.api.exception.ObjectNotFoundException;
import org.ehrbase.api.exception.PreconditionFailedException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

/**
 * Maps domain exceptions to GraphQL errors with consistent error codes.
 * Aligns with RFC 7807 Problem Details pattern used by the REST API.
 */
@Component
public class GraphQlExceptionResolver extends DataFetcherExceptionResolverAdapter {

    private static final Logger log = LoggerFactory.getLogger(GraphQlExceptionResolver.class);

    @Override
    protected List<GraphQLError> resolveToMultipleErrors(Throwable ex, DataFetchingEnvironment env) {
        ErrorType errorType;
        String code;

        if (ex instanceof ObjectNotFoundException) {
            errorType = ErrorType.NOT_FOUND;
            code = "NOT_FOUND";
        } else if (ex instanceof PreconditionFailedException) {
            errorType = ErrorType.BAD_REQUEST;
            code = "CONFLICT";
        } else if (ex instanceof StateConflictException) {
            errorType = ErrorType.BAD_REQUEST;
            code = "CONFLICT";
        } else if (ex instanceof ValidationException) {
            errorType = ErrorType.BAD_REQUEST;
            code = "VALIDATION_ERROR";
        } else if (ex instanceof SecurityException) {
            errorType = ErrorType.FORBIDDEN;
            code = "FORBIDDEN";
        } else if (ex instanceof IllegalArgumentException) {
            errorType = ErrorType.BAD_REQUEST;
            code = "BAD_REQUEST";
        } else {
            log.error("Unhandled GraphQL error in {}", env.getField().getName(), ex);
            errorType = ErrorType.INTERNAL_ERROR;
            code = "INTERNAL_ERROR";
        }

        GraphQLError error = GraphqlErrorBuilder.newError(env)
                .message(ex.getMessage())
                .errorType(errorType)
                .extensions(java.util.Map.of("code", code))
                .build();

        return List.of(error);
    }
}
