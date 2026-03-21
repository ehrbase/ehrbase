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
package org.ehrbase.service.graphql.scalars;

import graphql.GraphQLContext;
import graphql.execution.CoercedVariables;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.Coercing;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.util.Locale;

/**
 * GraphQL scalar for PostgreSQL TSTZRANGE values.
 * Serialized as a string like "[2026-01-01T00:00:00Z, 2026-12-31T23:59:59Z)".
 */
public final class DateTimeRangeScalar {

    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
            .name("DateTimeRange")
            .description("PostgreSQL TSTZRANGE — timestamp with timezone range")
            .coercing(new Coercing<Object, String>() {
                @Override
                public String serialize(Object dataFetcherResult, GraphQLContext context, Locale locale)
                        throws CoercingSerializeException {
                    return dataFetcherResult != null ? dataFetcherResult.toString() : null;
                }

                @Override
                public Object parseValue(Object input, GraphQLContext context, Locale locale) {
                    return input != null ? input.toString() : null;
                }

                @Override
                public Object parseLiteral(
                        Value<?> input, CoercedVariables variables, GraphQLContext context, Locale locale) {
                    if (input instanceof StringValue sv) {
                        return sv.getValue();
                    }
                    return null;
                }
            })
            .build();

    private DateTimeRangeScalar() {}
}
