/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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
package org.ehrbase.cli.cmd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CliDataBaseCommandTest {

    private final HikariDataSource dataSource = mock();
    private final Flyway flyway = mock();

    private final CliDataBaseCommand cmd = spy(new CliDataBaseCommand(dataSource, flyway));

    @BeforeEach
    void setUp() {
        Mockito.reset(cmd, dataSource, flyway);
        doNothing().when(cmd).exit(any(Integer.class));
        doNothing().when(cmd).println(any());
    }

    @Test
    void commandNameIsHelp() {
        assertEquals("database", cmd.getName());
    }

    @Test
    void runWithoutArgumentError() throws Exception {
        cmd.run(List.of());
        verify(cmd, times(1)).printUsage();
        verify(cmd, times(1)).exitFail("No argument provided");
    }

    @Test
    void runWithUnknownArgumentError() throws Exception {
        cmd.run(List.of("--illegal"));
        verify(cmd, times(1)).printUsage();
        verify(cmd, times(1)).exitFail("Unknown argument [illegal]");
        verify(cmd, times(1)).exit(-1);
    }

    @Test
    void runHelp() throws Exception {
        cmd.run(List.of("--help"));
        verify(cmd, times(2)).printUsage();
        verify(cmd, never()).exitFail(any());
        verify(cmd, never()).exit(any(Integer.class));
    }

    @Test
    void runCheckConnection() throws Exception {
        var jdbcUrl = "jdbc:test//localhost:1234/db";
        doReturn(jdbcUrl).when(dataSource).getJdbcUrl();

        DatabaseMetaData metaData = mock();
        doReturn(jdbcUrl).when(metaData).getURL();

        Connection connection = mock();
        doReturn(metaData).when(connection).getMetaData();
        doReturn(connection).when(dataSource).getConnection();

        cmd.run(List.of("--check-connection"));

        verify(cmd, never()).printUsage();
        verify(cmd, never()).exit(any(Integer.class));
    }

    @Test
    void runMigrationValidate() throws Exception {
        cmd.run(List.of("--migration-validate"));
        verify(flyway, times(1)).validate();
    }

    @Test
    void runMigrationMigrate() throws Exception {
        cmd.run(List.of("--migration-migrate"));
        verify(flyway, times(1)).migrate();
    }
}
