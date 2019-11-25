/*
 * Copyright (c) 2019 Jake Smolka (Hannover Medical School) and Vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.binding;

import com.nedap.archie.rm.datastructures.ItemStructure;
import org.ehrbase.serialisation.RawJson;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.Objects;
import java.util.Optional;

/**
 * Binding <T> = Object (unknown DB type), and <U> = {@link ItemStructure} (user type) for "other_details" column (of STATUS table).
 * See pom.xml of this module for further configuration, like what columns are linked with this binding.
 * Source: https://www.jooq.org/doc/3.12/manual/code-generation/custom-data-type-bindings/
 */
public class OtherDetailsJsonbBinder implements Binding<Object, ItemStructure> {

    // The converter does all the work
    @Override
    public Converter<Object, ItemStructure> converter() {
        return new Converter<Object, ItemStructure>() {
            @Override
            public ItemStructure from(Object databaseObject) {
                // null is valid "other_details" column's value
                return Optional.ofNullable(databaseObject)
                               .map(i -> new RawJson().unmarshal((String) i, ItemStructure.class))
                               .orElse(null);
            }

            @Override
            public Object to(ItemStructure userObject) {
                return Optional.ofNullable(userObject)
                               .map(i -> new RawJson().marshal(i))
                               .orElse(null);
            }

            @Override
            public Class<Object> fromType() {
                return Object.class;
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
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void sql(BindingSQLContext<ItemStructure> ctx) throws SQLException {
        // Depending on how you generate your SQL, you may need to explicitly distinguish
        // between jOOQ generating bind variables or inlined literals.
        if (ctx.render().paramType() == ParamType.INLINED)
            ctx.render().visit(DSL.inline(ctx.convert(converter()).value())).sql("::jsonb");
        else
            ctx.render().sql("?::jsonb");
    }

    /**
     * Registering VARCHAR types for JDBC CallableStatement OUT parameters
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void register(BindingRegisterContext<ItemStructure> ctx) throws SQLException {
        ctx.statement().registerOutParameter(ctx.index(), Types.VARCHAR);
    }

    /**
     * Converting the ItemStructure to a String value and setting that on a JDBC PreparedStatement
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void set(BindingSetStatementContext<ItemStructure> ctx) throws SQLException {
        ctx.statement().setString(ctx.index(), Objects.toString(ctx.convert(converter()).value(), null));
    }

    /**
     * Getting a String value from a JDBC ResultSet and converting that to a ItemStructure
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void get(BindingGetResultSetContext<ItemStructure> ctx) throws SQLException {
        ctx.convert(converter()).value(ctx.resultSet().getString(ctx.index()));
    }

    /**
     * Getting a String value from a JDBC CallableStatement and converting that to a ItemStructure
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void get(BindingGetStatementContext<ItemStructure> ctx) throws SQLException {
        ctx.convert(converter()).value(ctx.statement().getString(ctx.index()));
    }

    /**
     * Getting a value from a JDBC SQLInput (useful for Oracle OBJECT types)
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void get(BindingGetSQLInputContext<ItemStructure> ctx) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * Setting a value on a JDBC SQLOutput (useful for Oracle OBJECT types)
     * @param ctx internal DB context
     * @throws SQLException when SQL execution failed
     */
    @Override
    public void set(BindingSetSQLOutputContext<ItemStructure> ctx) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }
}
