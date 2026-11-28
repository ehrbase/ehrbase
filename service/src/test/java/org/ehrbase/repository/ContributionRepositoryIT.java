/*
 * Copyright (c) 2026 vitasystems GmbH.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.util.UUID;
import org.ehrbase.api.audit.CommitAuditHeaders;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.test.ServiceIntegrationTest;
import org.ehrbase.util.UuidGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@ServiceIntegrationTest
class ContributionRepositoryIT {

    @Autowired
    EhrRepository ehrRepository;

    @Autowired
    ContributionRepository contributionRepository;

    private final UUID ehrId = UuidGenerator.randomUUID();

    @Test
    void keepsTheDefaultsWhenNothingWasSent() {
        ehrRepository.commit(ehrId, ehrStatus(), null, null);

        AuditDetails commitAudit = commitAudit();
        assertThat(commitAudit.getChangeType().getDefiningCode().getCodeString())
                .isEqualTo("249");
        assertThat(commitAudit.getDescription().getValue()).isNull();
        assertThat(((PartyIdentified) commitAudit.getCommitter()).getName()).startsWith("EHRbase Internal ");
    }

    @Test
    void writesWhatWasSentIntoTheVersionAndContributionAudits() {
        CommitAuditHeaders sent = new CommitAuditHeaders(
                "532", "249", "Created by the test", null, "Dr. Jane Doe", "BC81", "demographic", "PERSON");

        ScopedValue.where(CommitAuditHeaders.SENT, sent)
                .run(() -> ehrRepository.commit(ehrId, ehrStatus(), null, null));

        AuditDetails commitAudit = commitAudit();
        AuditDetails contributionAudit = contributionRepository.findAuditDetailsForContribution(
                ehrId,
                UUID.fromString(originalVersion().getContribution().getId().getValue()));
        for (AuditDetails audit : new AuditDetails[] {commitAudit, contributionAudit}) {
            assertThat(audit.getChangeType().getDefiningCode().getCodeString()).isEqualTo("249");
            assertThat(audit.getDescription().getValue()).isEqualTo("Created by the test");
            PartyIdentified committer = (PartyIdentified) audit.getCommitter();
            assertThat(committer.getName()).isEqualTo("Dr. Jane Doe");
            assertThat(committer.getExternalRef().getId().getValue()).isEqualTo("BC81");
        }
    }

    @Test
    void rejectsABrokenRuleAndRollsTheCommitBack() {
        CommitAuditHeaders sent = new CommitAuditHeaders("553", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> ScopedValue.where(CommitAuditHeaders.SENT, sent)
                        .run(() -> ehrRepository.commit(ehrId, ehrStatus(), null, null)))
                .isInstanceOf(InvalidApiParameterException.class);
        assertThat(ehrRepository.hasEhr(ehrId)).isFalse();
    }

    private EhrStatus ehrStatus() {
        EhrStatus status = new EhrStatus();
        status.setUid(new ObjectVersionId(ehrId.toString(), "integration-test", "1"));
        status.setArchetypeNodeId("openEHR-EHR-EHR_STATUS.generic.v1");
        status.setName(new DvText("EHR Status"));
        status.setSubject(new PartySelf(null));
        status.setModifiable(true);
        status.setQueryable(true);
        return status;
    }

    private OriginalVersion<EhrStatus> originalVersion() {
        return ehrRepository.getOriginalVersionStatus(ehrId, ehrId, 1).orElseThrow();
    }

    private AuditDetails commitAudit() {
        return originalVersion().getCommitAudit();
    }
}
