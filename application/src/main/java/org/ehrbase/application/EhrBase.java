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
package org.ehrbase.application;

import org.ehrbase.ServiceModuleConfiguration;
import org.ehrbase.rest.RestModuleConfiguration;
import org.ehrbase.rest.ehrscape.RestEHRScapeModuleConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @author Stefan Spiska
 * @since 1.0
 */
@SpringBootApplication(
        exclude = {
            ManagementWebSecurityAutoConfiguration.class,
            R2dbcAutoConfiguration.class,
            SecurityAutoConfiguration.class
        })
@Import({ServiceModuleConfiguration.class, RestEHRScapeModuleConfiguration.class, RestModuleConfiguration.class})
public class EhrBase {

    public static void main(String[] args) {
        SpringApplication.run(EhrBase.class, args);
    }
}
