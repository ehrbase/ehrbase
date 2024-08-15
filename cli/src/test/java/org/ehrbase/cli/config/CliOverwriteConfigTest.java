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
package org.ehrbase.cli.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.ehrbase.configuration.config.validation.NopExternalTerminologyValidation;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CliOverwriteConfigTest {

    private final CliOverwriteConfig config = new CliOverwriteConfig();

    @Test
    void nopFlywayMigrationStrategy() {

        Flyway flyway = mock();
        config.flywayMigrationStrategy().migrate(flyway);
        verifyNoInteractions(flyway);
    }

    @Test
    void nopExternalTerminologyValidator() {

        Assertions.assertInstanceOf(NopExternalTerminologyValidation.class, config.externalTerminologyValidator());
    }
}
