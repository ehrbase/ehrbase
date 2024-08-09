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
package org.ehrbase.cli;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import org.ehrbase.cli.cmd.CliCommand;
import org.ehrbase.cli.cmd.CliHelpCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CliRunner {

    public static final String CLI = "cli";

    private final Logger logger = LoggerFactory.getLogger(CliRunner.class);

    private final CliHelpCommand helpCommand;
    private final List<CliCommand> commands;

    public CliRunner(List<CliCommand> commands, CliHelpCommand helpCommand) {

        this.helpCommand = helpCommand;
        this.commands = commands.stream()
                .sorted(Comparator.comparing(CliCommand::getName))
                .toList();
    }

    public void run(String... args) {
        run(
                (t, t2) -> {
                    throw new IllegalStateException(String.format(
                            "Duplicate command for name %s (attempted merging values %s and %s)", t.getName(), t, t2));
                },
                args);
    }

    public void run(BinaryOperator<CliCommand> onDuplicatedCmd, String... args) {

        List<String> argList = Arrays.asList(args);

        Iterator<String> argIter = argList.iterator();
        Optional<String> commandName = extractCmd(argIter);

        Map<String, CliCommand> namedCommands =
                commands.stream().collect(Collectors.toMap(CliCommand::getName, item -> item, onDuplicatedCmd));

        commandName.ifPresentOrElse(
                name -> Optional.ofNullable(namedCommands.get(name))
                        .ifPresentOrElse(
                                command -> runCommand(command, Lists.newArrayList(argIter)),
                                () -> helpCommand.exitFail("Unknown command %s".formatted(name))),
                () -> helpCommand.exitFail("No command specified"));
    }

    private Optional<String> extractCmd(Iterator<String> argIter) {

        if (!argIter.hasNext()) {
            return Optional.empty();
        }

        // consume until cli commands
        while (argIter.hasNext()) {
            String next = argIter.next();
            if (next.equals(CLI)) {
                break;
            }
        }

        if (!argIter.hasNext()) {
            return Optional.empty();
        }
        return Optional.of(argIter.next());
    }

    private void runCommand(CliCommand command, List<String> args) {
        try {
            command.run(args);
        } catch (Exception e) {
            logger.error("Failed to execute [%s]".formatted(command.getName()), e);
            helpCommand.exitFail(e.getMessage());
        }
    }
}
