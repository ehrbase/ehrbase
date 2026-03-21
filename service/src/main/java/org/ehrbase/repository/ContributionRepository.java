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
package org.ehrbase.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.service.RequestContext;
import org.ehrbase.service.TimeProvider;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for {@code ehr_system.contribution} table.
 * Simplified from legacy — no separate audit_details table.
 * Contribution groups version changes with committer + change_type.
 */
@Repository
public class ContributionRepository {

    private static final Logger log = LoggerFactory.getLogger(ContributionRepository.class);

    private static final org.jooq.Table<?> CONTRIBUTION = table(name("ehr_system", "contribution"));

    private final DSLContext dsl;
    private final TimeProvider timeProvider;
    private final RequestContext requestContext;

    public ContributionRepository(DSLContext dsl, TimeProvider timeProvider, RequestContext requestContext) {
        this.dsl = dsl;
        this.timeProvider = timeProvider;
        this.requestContext = requestContext;
    }

    @Transactional
    public UUID createContribution(UUID ehrId, String contributionType, String changeType) {
        OffsetDateTime now = timeProvider.getNow();
        short tenantId = requestContext.getTenantId();
        String committerName = requestContext.getUserId();

        Record1<UUID> result = dsl.insertInto(CONTRIBUTION)
                .set(field(name("ehr_id"), UUID.class), ehrId)
                .set(field(name("contribution_type"), String.class), contributionType)
                .set(field(name("change_type"), String.class), changeType)
                .set(field(name("committer_name"), String.class), committerName)
                .set(field(name("committer_id"), String.class), committerName)
                .set(field(name("time_committed"), OffsetDateTime.class), now)
                .set(field(name("sys_tenant"), Short.class), tenantId)
                .returningResult(field(name("id"), UUID.class))
                .fetchOne();

        UUID contributionId = result != null ? result.value1() : null;
        log.debug(
                "Created contribution: id={} ehr={} type={} change={}",
                contributionId,
                ehrId,
                contributionType,
                changeType);
        return contributionId;
    }

    public Optional<ContributionRecord> findById(UUID contributionId) {
        Record row = dsl.select()
                .from(CONTRIBUTION)
                .where(field(name("id"), UUID.class).eq(contributionId))
                .fetchOne();

        return row != null ? Optional.of(mapToRecord(row)) : Optional.empty();
    }

    public List<ContributionRecord> findByEhr(UUID ehrId) {
        return dsl
                .select()
                .from(CONTRIBUTION)
                .where(field(name("ehr_id"), UUID.class).eq(ehrId))
                .orderBy(field(name("time_committed")).desc())
                .fetch()
                .stream()
                .map(this::mapToRecord)
                .toList();
    }

    private ContributionRecord mapToRecord(Record row) {
        return new ContributionRecord(
                row.get(field(name("id"), UUID.class)),
                row.get(field(name("ehr_id"), UUID.class)),
                row.get(field(name("contribution_type"), String.class)),
                row.get(field(name("change_type"), String.class)),
                row.get(field(name("committer_name"), String.class)),
                row.get(field(name("committer_id"), String.class)),
                row.get(field(name("time_committed"), OffsetDateTime.class)),
                row.get(field(name("sys_tenant"), Short.class)));
    }

    public record ContributionRecord(
            UUID id,
            UUID ehrId,
            String contributionType,
            String changeType,
            String committerName,
            String committerId,
            OffsetDateTime timeCommitted,
            short sysTenant) {}
}
