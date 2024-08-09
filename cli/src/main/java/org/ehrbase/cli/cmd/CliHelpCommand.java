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

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CliHelpCommand extends CliCommand {

    public CliHelpCommand() {
        super("help");
    }

    @Override
    public void run(List<String> args) {
        if (!args.isEmpty()) {
            exitFail("illegal arguments %s".formatted(args));
        }

        printStep("Help");
        printUsage();
    }

    @Override
    protected void printUsage() {
        println(
                """
                Run with subcommand

                cli [sub-command] [arguments]

                Sub-commands:
                    database  database related operation
                    help      print this help message

                Examples:

                # show this help message
                cli help

                # show help message of a sub-command
                cli database
                cli database help

                # execute sub-command with arguments
                cli database --check-connection
                """);
    }
}
