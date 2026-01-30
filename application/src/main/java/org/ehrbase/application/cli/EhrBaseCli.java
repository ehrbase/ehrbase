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
package org.ehrbase.application.cli;

import java.util.Map;
import org.ehrbase.cli.CliConfiguration;
import org.ehrbase.cli.CliRunner;
import org.ehrbase.configuration.EhrBaseCliConfiguration;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Import;

@SpringBootApplication(exclude = {WebMvcAutoConfiguration.class, RedisAutoConfiguration.class})
@Import({EhrBaseCliConfiguration.class, CliConfiguration.class})
public class EhrBaseCli implements CommandLineRunner {

    public static SpringApplication build(String[] args) {
        return new SpringApplicationBuilder(EhrBaseCli.class)
                .web(WebApplicationType.NONE)
                .headless(true)
                .properties(Map.of(
                        "spring.main.allow-bean-definition-overriding", "true",
                        "spring.banner.location", "classpath:banner-cli.txt"))
                .bannerMode(Banner.Mode.CONSOLE)
                .logStartupInfo(false)
                .build(args);
    }

    private final CliRunner cliRunner;

    public EhrBaseCli(CliRunner cliRunner) {
        this.cliRunner = cliRunner;
    }

    @Override
    public void run(String... args) {
        cliRunner.run(args);
    }
}
