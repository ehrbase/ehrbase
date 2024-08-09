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

import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import org.ehrbase.cli.util.ExceptionFriendlyFunction;

@SuppressWarnings("java:S5803")
public abstract class CliCommand {

    /**
     * Represents a command CliArgument
     * @param arg   arg line
     * @param key   argument key=
     * @param value argument =value
     */
    public record CliArgument(String arg, String key, @Nullable String value) {}

    /**
     * Represents the Result of a {@link CliArgument} execution
     */
    public sealed interface Result permits Result.OK, Result.Unknown {

        Result OK = new OK();
        Result Unknown = new Unknown();

        final class OK implements Result {}

        final class Unknown implements Result {}
    }

    protected final String name;

    protected CliCommand(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void run(List<String> args) throws Throwable;

    @SuppressWarnings("java:S106")
    void println(String line) {
        System.out.println(line);
    }

    protected void printStep(String line) {
        println("---------------------------------------------------------------------------");
        println(line);
        println("---------------------------------------------------------------------------");
    }

    @SuppressWarnings("java:S106")
    public void exitFail(String reason) {

        System.err.println(reason);
        printUsage();
        exit(-1);
    }

    void exit(int code) {
        System.exit(-1);
    }

    protected abstract void printUsage();

    protected void consumeArgs(Iterable<String> args, ExceptionFriendlyFunction<CliArgument, Result> consumer)
            throws Exception {

        Iterator<String> argIter = args.iterator();
        if (!argIter.hasNext()) {
            exitFail("No argument provided");
            return;
        }

        String next;
        while (argIter.hasNext()) {
            next = argIter.next();
            String[] split = next.split("=");
            CliArgument arg = new CliArgument(next, split[0].replace("--", ""), split.length > 1 ? split[1] : null);

            Result result = consumer.apply(arg);
            if (result instanceof Result.Unknown) {
                exitFail("Unknown argument [%s]".formatted(arg.arg()));
            }
        }
    }
}
