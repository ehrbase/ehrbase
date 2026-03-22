/*
 * Copyright (c) 2024 vitasystems GmbH.
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
package org.ehrbase.service;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import jakarta.annotation.PostConstruct;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Manages system identity by persisting {@code server.nodename} into {@code ehr_system.system}.
 *
 * <p>On startup:
 * <ol>
 *   <li>Read {@code server.nodename} from config</li>
 *   <li>If table is empty: INSERT nodename</li>
 *   <li>If exists with different nodename: FAIL FAST (data integrity)</li>
 *   <li>If exists with same: proceed</li>
 * </ol>
 */
@Component
@org.springframework.context.annotation.DependsOn("flywayInitializer")
public class SystemIdentityService {

    private static final Logger log = LoggerFactory.getLogger(SystemIdentityService.class);

    private static final org.jooq.Table<?> SYSTEM = table(name("ehr_system", "system"));

    private final DSLContext dsl;

    @Value("${server.nodename:local.ehrbase.org}")
    private String configuredNodename;

    private String systemId;

    public SystemIdentityService(DSLContext dsl) {
        this.dsl = dsl;
    }

    @PostConstruct
    void init() {
        Record1<String> existing = dsl.select(field(name("system_id"), String.class))
                .from(SYSTEM)
                .limit(1)
                .fetchOne();

        if (existing == null) {
            // First startup — persist nodename
            dsl.insertInto(SYSTEM)
                    .set(field(name("system_id"), String.class), configuredNodename)
                    .execute();
            this.systemId = configuredNodename;
            log.info("System identity initialized: {}", configuredNodename);
        } else {
            String persistedId = existing.value1();
            if (!configuredNodename.equals(persistedId)) {
                throw new IllegalStateException(
                        "System identity conflict: configured nodename '%s' does not match persisted '%s'. "
                                        .formatted(configuredNodename, persistedId)
                                + "The server.nodename must remain constant for ObjectVersionId integrity.");
            }
            this.systemId = persistedId;
            log.info("System identity verified: {}", persistedId);
        }
    }

    /**
     * Returns the system ID (nodename) used in ObjectVersionId construction.
     * Format: {@code {uuid}::{systemId}::{version}}
     */
    public String getSystemId() {
        return systemId;
    }
}
