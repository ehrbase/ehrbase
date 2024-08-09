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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import org.ehrbase.cli.cmd.CliCommand;
import org.ehrbase.cli.cmd.CliHelpCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class CliRunnerTest {

    private CliHelpCommand mockHelpCommand = mock(CliHelpCommand.class);

    private static class CliTestCommand extends CliCommand {

        List<String> args;

        public CliTestCommand(String name) {
            super(name);
        }

        @Override
        public void run(List<String> args) {
            this.args = args;
        }

        @Override
        protected void printUsage() {}
    }

    @BeforeEach
    void setUp() {
        Mockito.reset(mockHelpCommand);
    }

    private CliRunner cliRunner(CliCommand... commands) {
        return new CliRunner(Arrays.stream(commands).toList(), mockHelpCommand);
    }

    @Test
    void duplicateCommandError() {

        var cmd1 = new CliTestCommand("duplicate-cmd");
        var cmd2 = new CliTestCommand("duplicate-cmd");
        CliRunner cliRunner = cliRunner(cmd1, cmd2);
        String message =
                assertThrows(IllegalStateException.class, cliRunner::run).getMessage();
        assertEquals(
                "Duplicate command for name duplicate-cmd (attempted merging values %s and %s)".formatted(cmd1, cmd2),
                message);
    }

    @Test
    void runErrorWithoutArguments() {

        cliRunner().run();
        verify(mockHelpCommand).exitFail(eq("No command specified"));
    }

    @Test
    void runErrorWithoutCliArguments() {

        cliRunner().run("--some --other=true --command");
        verify(mockHelpCommand).exitFail(eq("No command specified"));
    }

    @Test
    void runErrorCommandNotExist() {

        cliRunner().run("cli", "does-not-exist");
        verify(mockHelpCommand).exitFail(eq("Unknown command does-not-exist"));
    }

    @Test
    void runCommand() {

        var cmd = new CliTestCommand("capture-the-flag");
        cliRunner(cmd).run(CliRunner.CLI, "capture-the-flag");

        assertNotNull(cmd.args);
    }

    @Test
    void runCommandWithArgument() {

        var cmd = new CliTestCommand("capture-the-flag");
        cliRunner(cmd).run(CliRunner.CLI, "capture-the-flag", "--flag=true");

        assertNotNull(cmd.args);
        assertEquals(1, cmd.args.size());
        assertEquals("--flag=true", cmd.args.getFirst());
    }
}
