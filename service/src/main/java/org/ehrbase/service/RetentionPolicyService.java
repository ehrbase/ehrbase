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

import java.time.OffsetDateTime;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Background job that enforces data retention policies defined in {@code ehr_system.retention_policy}.
 * Runs periodically to identify and process data that has exceeded its retention period.
 *
 * <p>Policies define:
 * <ul>
 *   <li>{@code retention_period} — how long data is kept (INTERVAL)</li>
 *   <li>{@code action} — what happens when expired: "pseudonymize" or "physical"</li>
 *   <li>{@code requires_approval} — whether 4-eyes approval is needed before execution</li>
 * </ul>
 */
@Service
public class RetentionPolicyService {

    private static final Logger log = LoggerFactory.getLogger(RetentionPolicyService.class);

    private static final org.jooq.Table<?> RETENTION_POLICY = table(name("ehr_system", "retention_policy"));
    private static final org.jooq.Table<?> EHR = table(name("ehr_system", "ehr"));

    private final DSLContext dsl;
    private final AuditEventService auditService;

    public RetentionPolicyService(DSLContext dsl, AuditEventService auditService) {
        this.dsl = dsl;
        this.auditService = auditService;
    }

    /**
     * Runs every 24 hours to check retention policies and process expired data.
     */
    @Scheduled(fixedDelayString = "${ehrbase.retention.check-interval:86400000}")
    public void enforceRetentionPolicies() {
        log.info("Retention policy check started");

        Result<Record> policies = dsl.select().from(RETENTION_POLICY).fetch();

        if (policies.isEmpty()) {
            log.debug("No retention policies defined");
            return;
        }

        int processed = 0;
        for (Record policy : policies) {
            String policyName = policy.get(field(name("policy_name"), String.class));
            String action = policy.get(field(name("action"), String.class));
            boolean requiresApproval = Boolean.TRUE.equals(policy.get(field(name("requires_approval"), Boolean.class)));
            String approvedBy = policy.get(field(name("approved_by"), String.class));

            if (requiresApproval && approvedBy == null) {
                log.debug("Retention policy '{}' requires approval but is not yet approved — skipping", policyName);
                continue;
            }

            Object retentionPeriod = policy.get(field(name("retention_period")));
            if (retentionPeriod == null) {
                continue;
            }

            OffsetDateTime cutoff = OffsetDateTime.now().minus(java.time.Duration.parse("PT" + retentionPeriod));

            int count = processPolicy(policyName, action, cutoff);
            processed += count;

            if (count > 0) {
                log.info("Retention policy '{}' processed {} records (action: {})", policyName, count, action);
                auditService.recordEvent(
                        "admin_action",
                        "retention_policy",
                        policy.get(field(name("id"), java.util.UUID.class)),
                        action,
                        null,
                        java.util.Map.of("policy_name", policyName, "records_processed", count));
            }
        }

        log.info("Retention policy check completed: {} records processed", processed);
    }

    private int processPolicy(String policyName, String action, OffsetDateTime cutoff) {
        return switch (action) {
            case "pseudonymize" -> pseudonymizeExpiredData(cutoff);
            case "physical" -> physicalDeleteExpiredData(cutoff);
            default -> {
                log.warn("Unknown retention action '{}' for policy '{}'", action, policyName);
                yield 0;
            }
        };
    }

    private int pseudonymizeExpiredData(OffsetDateTime cutoff) {
        return dsl.update(EHR)
                .set(field(name("subject_id"), String.class), (String) null)
                .set(field(name("subject_namespace"), String.class), (String) null)
                .where(field(name("creation_date"), OffsetDateTime.class).lt(cutoff))
                .and(field(name("subject_id"), String.class).isNotNull())
                .execute();
    }

    private int physicalDeleteExpiredData(OffsetDateTime cutoff) {
        log.warn("Physical deletion is a destructive operation — only pseudonymization is currently implemented");
        return 0;
    }
}
