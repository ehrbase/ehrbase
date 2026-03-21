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
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * GraphQL scalar for ISO 8601 date-time values (maps to PostgreSQL TIMESTAMPTZ).
 */
public final class DateTimeScalar {

    public static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("ISO 8601 date-time (e.g., 2026-03-21T10:30:00Z)")
            .coercing(new Coercing<OffsetDateTime, String>() {
                @Override
                public String serialize(Object dataFetcherResult, GraphQLContext context, Locale locale)
                        throws CoercingSerializeException {
                    if (dataFetcherResult instanceof OffsetDateTime odt) {
                        return odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    }
                    if (dataFetcherResult instanceof String s) {
                        return s;
                    }
                    throw new CoercingSerializeException("Cannot serialize " + dataFetcherResult + " as DateTime");
                }

                @Override
                public OffsetDateTime parseValue(Object input, GraphQLContext context, Locale locale)
                        throws CoercingParseValueException {
                    if (input instanceof String s) {
                        return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    }
                    throw new CoercingParseValueException("Cannot parse " + input + " as DateTime");
                }

                @Override
                public OffsetDateTime parseLiteral(
                        Value<?> input, CoercedVariables variables, GraphQLContext context, Locale locale)
                        throws CoercingParseLiteralException {
                    if (input instanceof StringValue sv) {
                        return OffsetDateTime.parse(sv.getValue(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    }
                    throw new CoercingParseLiteralException("Cannot parse literal " + input + " as DateTime");
                }
            })
            .build();

    private DateTimeScalar() {}
}
