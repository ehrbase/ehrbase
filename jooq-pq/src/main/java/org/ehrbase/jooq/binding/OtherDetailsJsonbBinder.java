/*
 * Copyright (c) 2019-2022 vitasystems GmbH and Hannover Medical School.
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

import com.nedap.archie.rm.datastructures.ItemStructure;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.Objects;
import java.util.Optional;
import org.ehrbase.jooq.dbencoding.RawJson;
import org.jooq.Binding;
import org.jooq.BindingGetResultSetContext;
import org.jooq.BindingGetSQLInputContext;
import org.jooq.BindingGetStatementContext;
import org.jooq.BindingRegisterContext;
import org.jooq.BindingSQLContext;
import org.jooq.BindingSetSQLOutputContext;
import org.jooq.BindingSetStatementContext;
import org.jooq.Converter;
import org.jooq.JSONB;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

/**
 * Binding <T> = Object (unknown DB type), and <U> = {@link ItemStructure} (user type) for "other_details" column (of STATUS table).
 * See pom.xml of this module for further configuration, like what columns are linked with this binding.
 * https://www.jooq.org/doc/3.12/manual/code-generation/custom-data-type-bindings/
 *
 * @author Jake Smolka
 * @author Renaud Subiger
 * @since 1.0
 */
public class OtherDetailsJsonbBinder implements Binding<JSONB, ItemStructure> {

    // The converter does all the work
    @Override
    public Converter<JSONB, ItemStructure> converter() {
        return new Converter<>() {

            @Override
            public ItemStructure from(org.jooq.JSONB databaseObject) {
                // null is valid "other_details" column's value
                return Optional.ofNullable(databaseObject)
                        .map(JSONB::data)
                        .map(i -> new RawJson().unmarshal(i, ItemStructure.class))
                        .orElse(null);
            }

            @Override
            public JSONB to(ItemStructure userObject) {
                return Optional.ofNullable(userObject)
                        .map(i -> JSONB.valueOf(new RawJson().marshal(i)))
                        .orElse(null);
            }

            @Override
            public Class<JSONB> fromType() {
                return org.jooq.JSONB.class;
            }

            @Override
            public Class<ItemStructure> toType() {
                return ItemStructure.class;
            }
        };
    }

    // Methods below are mapping the converter from above to specific scenarios, i.e. calling via different jooq and
    // jooq-compatible sql executions. For instance, .store() or manually building a InsertQuery.

    /**
     * Rending a bind variable for the binding context's value and casting it to the ItemStructure type
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void sql(BindingSQLContext<ItemStructure> ctx) throws SQLException {
        // Depending on how you generate your SQL, you may need to explicitly distinguish
        // between jOOQ generating bind variables or inlined literals.
        if (ctx.render().paramType() == ParamType.INLINED)
            ctx.render().visit(DSL.inline(ctx.convert(converter()).value())).sql("::jsonb");
        else ctx.render().sql("?::jsonb");
    }

    /**
     * Registering VARCHAR types for JDBC CallableStatement OUT parameters
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void register(BindingRegisterContext<ItemStructure> ctx) throws SQLException {
        ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR);
    }

    /**
     * Converting the ItemStructure to a String value and setting that on a JDBC PreparedStatement
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void set(BindingSetStatementContext<ItemStructure> ctx) throws SQLException {
        ctx.statement()
                .setString(
                        ctx.index(), Objects.toString(ctx.convert(converter()).value(), null));
    }

    /**
     * Getting a String value from a JDBC ResultSet and converting that to a ItemStructure
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void get(BindingGetResultSetContext<ItemStructure> ctx) throws SQLException {
        ctx.convert(converter()).value(JSONB.jsonbOrNull(ctx.resultSet().getString(ctx.index())));
    }

    /**
     * Getting a String value from a JDBC CallableStatement and converting that to a ItemStructure
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void get(BindingGetStatementContext<ItemStructure> ctx) throws SQLException {
        ctx.convert(converter()).value(JSONB.jsonbOrNull(ctx.statement().getString(ctx.index())));
    }

    /**
     * Getting a value from a JDBC SQLInput (useful for Oracle OBJECT types)
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void get(BindingGetSQLInputContext<ItemStructure> ctx) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Setting a value on a JDBC SQLOutput (useful for Oracle OBJECT types)
     *
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void set(BindingSetSQLOutputContext<ItemStructure> ctx) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }
}
