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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.enums.ContributionState;
import org.ehrbase.jooq.pg.tables.AuditDetails;
import org.ehrbase.jooq.pg.tables.Contribution;
import org.ehrbase.jooq.pg.tables.records.AuditDetailsRecord;
import org.ehrbase.jooq.pg.tables.records.ContributionRecord;
import org.ehrbase.service.IUserService;
import org.ehrbase.service.SystemService;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Stefan Spiska
 */
@Repository
public class ContributionRepository {

    private final DSLContext context;
    private final SystemService systemService;
    private final IUserService userService;

    private final TenantService tenantService;

    public ContributionRepository(
            DSLContext context, SystemService systemService, IUserService userService, TenantService tenantService) {
        this.context = context;
        this.systemService = systemService;
        this.userService = userService;
        this.tenantService = tenantService;
    }

    @Transactional
    public UUID createDefault(
            UUID ehrId, ContributionDataType contributionType, ContributionChangeType contributionChangeType) {

        AuditDetailsRecord auditDetailsRecord = context.newRecord(AuditDetails.AUDIT_DETAILS);

        auditDetailsRecord.setId(UuidGenerator.randomUUID());
        auditDetailsRecord.setTimeCommitted(Timestamp.from(Instant.now()));
        auditDetailsRecord.setTimeCommittedTzid(ZonedDateTime.now().getZone().getId());
        auditDetailsRecord.setSystemId(systemService.getSystemUuid());
        auditDetailsRecord.setCommitter(userService.getCurrentUserId());
        auditDetailsRecord.setChangeType(contributionChangeType);
        auditDetailsRecord.setNamespace(tenantService.getCurrentTenantIdentifier());

        auditDetailsRecord.store();

        ContributionRecord contributionRecord = context.newRecord(Contribution.CONTRIBUTION);

        contributionRecord.setId(UuidGenerator.randomUUID());
        contributionRecord.setEhrId(ehrId);
        contributionRecord.setContributionType(contributionType);
        contributionRecord.setState(ContributionState.complete);
        contributionRecord.setHasAudit(auditDetailsRecord.getId());
        contributionRecord.setNamespace(tenantService.getCurrentTenantIdentifier());

        contributionRecord.store();

        return contributionRecord.getId();
    }
}
