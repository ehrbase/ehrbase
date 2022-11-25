/*
 * Copyright (c) 2020-2022 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.jooq.binding;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jooq.Binding;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingGetSQLInputContext;
import org.jooq.BindingGetStatementContext;
import org.jooq.BindingRegisterContext;
import org.jooq.BindingSQLContext;
import org.jooq.BindingSetSQLOutputContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

/**
 * Binding <T> = Object (unknown DB type), and <U> = {@link AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>} (user type) for "sys_period" column (of STATUS table).
 * See pom.xml of this module for further configuration, like what columns are linked with this binding.
 * Source: https://www.jooq.org/doc/3.12/manual/code-generation/custom-data-type-bindings/
 *
 * @author Jake Smolka
 * @author Renaud Subiger
 * @since 1.0
 */
@SuppressWarnings("serial")
public class SysPeriodBinder implements Binding<Object, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>> {

    // source of pattern: https://gist.github.com/gregopet/8d0feb4fe4075a8525c1175243ee38b0
    private static final String DATE_OR_EMPTY = "(?:(?:\"([^\"]+)\")?)";
    private static final Pattern PATTERN = Pattern.compile("[(\\[(]" + DATE_OR_EMPTY + "," + DATE_OR_EMPTY + "[\\])]");

    // The converter does all the work
    @Override
    public Converter<Object, AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>> converter() {
        return new Converter<>() {

            @SuppressWarnings("unchecked")
            @Override
            public AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime> from(Object databaseObject) {
                if (databaseObject == null) {
                    return null;
                } else if (databaseObject instanceof AbstractMap.SimpleEntry)
                    return (AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>) databaseObject;
                else {
                    Matcher m = PATTERN.matcher("" + databaseObject);
                    if (m.find()) {
                        String lowerStr = m.group(1).replace(" ", "T");
                        String upperStr = m.group(2);

                        OffsetDateTime lower = OffsetDateTime.parse(lowerStr);

                        if (upperStr != null) { // can be empty
                            upperStr = upperStr.replace(" ", "T");
                            OffsetDateTime upper = OffsetDateTime.parse(upperStr);
                            return new AbstractMap.SimpleEntry<>(lower, upper);
                        } else {
                            return new AbstractMap.SimpleEntry<>(lower, null);
                        }
                    } else {
                        throw new IllegalArgumentException("Unsupported range : " + databaseObject);
                    }
                }
            }

            @Override
            public Object to(AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime> userObject) {
                if (userObject == null) {
                    return null;
                }

                String lower = userObject.getKey().format(ISO_OFFSET_DATE_TIME).replace("T", " ");
                String upper = "";
                if (userObject.getValue() != null) // upper bound can be empty
                upper = userObject.getValue().format(ISO_OFFSET_DATE_TIME).replace("T", " ");

                if (upper.isEmpty()) {
                    return "[\"" + lower + "\",)";
                } else {
                    return "[\"" + lower + "\",)\"" + lower + "\")";
                }
            }

            @Override
            public Class<Object> fromType() {
                return Object.class;
            }

            @SuppressWarnings({"unchecked", "rawtypes"})
            @Override
            public Class<AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>> toType() {
                return (Class) AbstractMap.SimpleEntry.class;
            }
        };
    }

    // Methods below are mapping the converter from above to specific scenarios, i.e. calling via different jooq and
    // jooq-compatible sql executions. For instance, .store() or manually building a InsertQuery.

    /**
     * Rending a bind variable for the binding context's value and casting it to the AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime> type
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void sql(BindingSQLContext<AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>> ctx)
            throws SQLException {
        // Depending on how you generate your SQL, you may need to explicitly distinguish
        // between jOOQ generating bind variables or inlined literals.
        if (ctx.render().paramType() == ParamType.INLINED)
            ctx.render().visit(DSL.inline(ctx.convert(converter()).value())).sql("::tstzrange");
        else ctx.render().sql("?::tstzrange");
    }

    /**
     * Registering VARCHAR types for JDBC CallableStatement OUT parameters
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void register(BindingRegisterContext<AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>> ctx)
            throws SQLException {
        ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR);
    }

    /**
     * Converting the AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime> to a String value and setting that on a JDBC PreparedStatement
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void set(BindingSetStatementContext<AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>> ctx)
            throws SQLException {
        ctx.statement()
                .setString(
                        ctx.index(), Objects.toString(ctx.convert(converter()).value(), null));
    }

    /**
     * Getting a String value from a JDBC ResultSet and converting that to a AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void get(BindingGetResultSetContext<AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>> ctx)
            throws SQLException {
        ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()));
    }

    /**
     * Getting a String value from a JDBC CallableStatement and converting that to a AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void get(BindingGetStatementContext<AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>> ctx)
            throws SQLException {
        ctx.convert(converter()).value(ctx.statement().getString(ctx.index()));
    }

    /**
     * Getting a value from a JDBC SQLInput (useful for Oracle OBJECT types)
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void get(BindingGetSQLInputContext<AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>> ctx)
            throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Setting a value on a JDBC SQLOutput (useful for Oracle OBJECT types)
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void set(BindingSetSQLOutputContext<AbstractMap.SimpleEntry<OffsetDateTime, OffsetDateTime>> ctx)
            throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }
}
