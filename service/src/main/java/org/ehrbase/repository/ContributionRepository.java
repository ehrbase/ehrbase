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

import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.exception.UnexpectedSwitchCaseException;
import org.ehrbase.api.service.TenantService;
import org.ehrbase.jooq.pg.enums.ContributionChangeType;
import org.ehrbase.jooq.pg.enums.ContributionDataType;
import org.ehrbase.jooq.pg.enums.ContributionState;
import org.ehrbase.jooq.pg.tables.AuditDetails;
import org.ehrbase.jooq.pg.tables.Contribution;
import org.ehrbase.jooq.pg.tables.records.AuditDetailsRecord;
import org.ehrbase.jooq.pg.tables.records.ContributionRecord;
import org.ehrbase.service.IUserService;
import org.ehrbase.service.PartyService;
import org.ehrbase.service.SystemService;
import org.ehrbase.util.UuidGenerator;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles DB-Access to {@link Contribution} and {@link AuditDetails}
 * @author Stefan Spiska
 */
@Repository
public class ContributionRepository {

    private final DSLContext context;
    private final SystemService systemService;
    private final IUserService userService;

    private final PartyService partyService;

    private final TenantService tenantService;

    public ContributionRepository(
            DSLContext context,
            SystemService systemService,
            IUserService userService,
            PartyService partyService,
            TenantService tenantService) {
        this.context = context;
        this.systemService = systemService;
        this.userService = userService;
        this.partyService = partyService;
        this.tenantService = tenantService;
    }

    /**
     * Create the default contribution in the DB for usage in case data is not saved via explicit provided contribution. Sets the committer from the auth context.
     * @param ehrId
     * @param contributionType
     * @param contributionChangeType
     * @return {@link UUID} of the corresponding Database Record.
     */
    @Transactional
    public UUID createDefault(
            UUID ehrId, ContributionDataType contributionType, ContributionChangeType contributionChangeType) {

        UUID auditDetailsRecordId = createDefaultAudit(contributionChangeType);

        ContributionRecord contributionRecord = context.newRecord(Contribution.CONTRIBUTION);

        contributionRecord.setId(UuidGenerator.randomUUID());
        contributionRecord.setEhrId(ehrId);
        contributionRecord.setContributionType(contributionType);
        contributionRecord.setState(ContributionState.complete);
        contributionRecord.setHasAudit(auditDetailsRecordId);
        contributionRecord.setSysTenant(tenantService.getCurrentSysTenant());

        contributionRecord.store();

        return contributionRecord.getId();
    }

    /**
     * Create the default audit in the DB  for usage in case data is not saved via explicit provided contribution. Sets the committer from the auth context.
     * @param contributionChangeType
     * @return {@link UUID} of the corresponding Database Record.
     */
    @Transactional
    public UUID createDefaultAudit(ContributionChangeType contributionChangeType) {
        AuditDetailsRecord auditDetailsRecord = context.newRecord(AuditDetails.AUDIT_DETAILS);

        auditDetailsRecord.setId(UuidGenerator.randomUUID());
        auditDetailsRecord.setTimeCommitted(Timestamp.from(Instant.now()));
        auditDetailsRecord.setTimeCommittedTzid(ZonedDateTime.now().getZone().getId());
        auditDetailsRecord.setSystemId(systemService.getSystemUuid());
        auditDetailsRecord.setCommitter(userService.getCurrentUserId());
        auditDetailsRecord.setChangeType(contributionChangeType);
        auditDetailsRecord.setSysTenant(tenantService.getCurrentSysTenant());

        auditDetailsRecord.store();
        return auditDetailsRecord.getId();
    }

    /**
     * Creates a Audit in the Database
     * @param auditDetails {@link AuditDetails} from which to take the data.
     * @return {@link UUID} of the corresponding Database Record.
     */
    @Transactional
    public UUID createAudit(com.nedap.archie.rm.generic.AuditDetails auditDetails) {

        AuditDetailsRecord auditDetailsRecord = context.newRecord(AuditDetails.AUDIT_DETAILS);

        auditDetailsRecord.setId(UuidGenerator.randomUUID());
        auditDetailsRecord.setTimeCommitted(Timestamp.from(Instant.now()));
        auditDetailsRecord.setTimeCommittedTzid(ZonedDateTime.now().getZone().getId());
        // according to https://specifications.openehr.org/releases/RM/latest/common.html#_audit_details_class
        // this should be set to Identifier of the logical EHR system where the change was committed.
        auditDetailsRecord.setSystemId(systemService.getSystemUuid());
        auditDetailsRecord.setCommitter(partyService.findOrCreateParty(auditDetails.getCommitter()));
        auditDetailsRecord.setChangeType(to(auditDetails.getChangeType()));
        // We just save the text here wich is not 100 % correct here.
        auditDetailsRecord.setDescription(Optional.ofNullable(auditDetails.getDescription())
                .map(DvText::getValue)
                .orElse(null));
        auditDetailsRecord.setSysTenant(tenantService.getCurrentSysTenant());

        auditDetailsRecord.store();
        return auditDetailsRecord.getId();
    }

    private ContributionChangeType to(DvCodedText changeType) {

        return switch (changeType.getDefiningCode().getCodeString()) {
            case "249" -> ContributionChangeType.creation;
            case "250" -> ContributionChangeType.amendment;
            case "251" -> ContributionChangeType.modification;
            case "252" -> ContributionChangeType.synthesis;
            case "253" -> ContributionChangeType.Unknown;
            case "523" -> ContributionChangeType.deleted;
            default -> throw new UnexpectedSwitchCaseException(changeType.toString());
        };
    }
}
