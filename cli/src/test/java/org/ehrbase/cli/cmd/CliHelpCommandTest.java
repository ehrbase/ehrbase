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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CliHelpCommandTest {

    private final CliHelpCommand cmd = spy(new CliHelpCommand());

    @BeforeEach
    void setUp() {
        Mockito.reset(cmd);
        doNothing().when(cmd).exit(any(Integer.class));
        doNothing().when(cmd).println(any());
    }

    @Test
    void commandNameIsHelp() {
        assertEquals("help", cmd.getName());
    }

    @Test
    void runWithArgumentError() {

        cmd.run(List.of("invalid"));

        verify(cmd, times(1)).exitFail("illegal arguments [invalid]");
        verify(cmd, times(1)).exit(-1);
    }

    @Test
    void runWithoutArgument() {

        cmd.run(List.of());

        verify(cmd, times(1)).printUsage();
        verify(cmd, never()).exit(any(Integer.class));
    }
}
