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

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.ehrbase.configuration.config.flyway.MigrationStrategy;
import org.ehrbase.configuration.config.flyway.MigrationStrategyConfig;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.stereotype.Component;

@Component
public class CliDataBaseCommand extends CliCommand {

    protected final DataSource dataSource;

    protected final Flyway flyway;

    protected final MigrationStrategyConfig migrationStrategyConfig;

    public CliDataBaseCommand(DataSource dataSource, Flyway flyway, MigrationStrategyConfig migrationStrategyConfig) {
        super("database");
        this.dataSource = dataSource;
        this.flyway = flyway;
        this.migrationStrategyConfig = migrationStrategyConfig;
    }

    @Override
    public void run(List<String> args) throws Exception {

        consumeArgs(args, arg -> switch (arg.key()) {
            case "check-connection":
                yield executeCheckConnection();
            case "migration-validate":
                yield executeMigration(MigrationStrategy.VALIDATE);
            case "migration-migrate":
                yield executeMigration(MigrationStrategy.MIGRATE);
            default:
                yield Result.Unknown;
        });
    }

    protected Result executeCheckConnection() {

        printStep("executing Database connection check: %s".formatted(jdbUrl()));

        try (Connection connection = dataSource.getConnection()) {
            String url = connection.getMetaData().getURL();
            println("Connection established to %s".formatted(url));
            return Result.OK;
        } catch (Exception e) {
            exitFail("Failed to open connection %s".formatted(jdbUrl()));
            return Result.Unknown;
        }
    }

    protected Result executeMigration(MigrationStrategy migrationStrategy) {

        printStep("executing Flyway with strategy: %s".formatted(migrationStrategy));

        FlywayMigrationStrategy strategy =
                migrationStrategyConfig.flywayMigrationStrategy(migrationStrategy, migrationStrategy);
        strategy.migrate(flyway);
        return Result.OK;
    }

    protected String jdbUrl() {
        return ((HikariDataSource) dataSource).getJdbcUrl();
    }

    @Override
    protected void printUsage() {
        println(
                """
                Database related operation like connection verification or migration.

                Arguments:
                  --check-connection    verifies database access by open/close a connection
                  --migration-validate  validate flyway migration
                  --migration-migrate   executes flyway migration

                Example:

                database --check-connection --migration-validate
                """);
    }
}
