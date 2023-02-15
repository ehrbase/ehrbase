/*
 * Copyright (c) 2023 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.repository;

import java.util.Optional;
import java.util.UUID;
import org.ehrbase.jooq.pg.tables.System;
import org.ehrbase.jooq.pg.tables.records.SystemRecord;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles DB-Access to {@link org.ehrbase.jooq.pg.tables.System}
 * @author Stefan Spiska
 */
@Repository
public class SystemRepository {

    private final DSLContext context;

    public SystemRepository(DSLContext context) {
        this.context = context;
    }

    /**
     * Creates a new System in the DB
     * @param systemRecord
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void commit(SystemRecord systemRecord) {
        context.insertInto(System.SYSTEM).set(systemRecord).execute();
    }

    /**
     * Find the uuid of the system in the DB corresponding to <code>system</code>
     * @param system
     * @return
     */
    public Optional<UUID> findSystemId(String system) {

        return Optional.ofNullable(context.select(System.SYSTEM.ID)
                        .from(System.SYSTEM)
                        .where(System.SYSTEM.SETTINGS.eq(system))
                        .fetchOne())
                .map(Record1::value1);
    }

    public SystemRecord toRecord(String system, String description) {
        SystemRecord systemRecord = context.newRecord(System.SYSTEM);

        systemRecord.setId(UuidGenerator.randomUUID());
        systemRecord.setSettings(system);
        systemRecord.setDescription(description);

        return systemRecord;
    }
}
