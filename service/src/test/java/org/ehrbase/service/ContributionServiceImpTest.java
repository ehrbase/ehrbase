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
package org.ehrbase.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.directory.Folder;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.AuditDetails;
import java.util.Arrays;
import org.ehrbase.api.exception.InvalidApiParameterException;
import org.ehrbase.api.exception.StateConflictException;
import org.ehrbase.api.service.SystemService;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;
import org.junit.jupiter.api.Test;

class ContributionServiceImpTest {

    @Test
    void isModifiableCheckNeeded() {
        // simple cases
        checkIsModifiableCheckNeeded(false);

        checkIsModifiableCheckNeeded(true, delete());
        checkIsModifiableCheckNeeded(true, composition());
        checkIsModifiableCheckNeeded(true, directory());
        checkIsModifiableCheckNeeded(true, delete(), composition(), directory());

        checkIsModifiableCheckNeeded(false, status(true));
        checkIsModifiableCheckNeeded(false, status(false));

        // status-modifiable=true
        checkIsModifiableCheckNeeded(false, status(true), delete(), composition(), directory());
        checkIsModifiableCheckNeeded(true, delete(), status(true), composition(), directory());

        // status-modifiable=false
        checkIsModifiableCheckNeeded(null, status(false), delete(), composition(), directory());
        checkIsModifiableCheckNeeded(null, delete(), composition(), status(false), directory());
        checkIsModifiableCheckNeeded(true, delete(), composition(), directory(), status(false));

        // status-modifiable=mixed
        checkIsModifiableCheckNeeded(false, status(true), composition(), status(false));
        checkIsModifiableCheckNeeded(null, status(false), composition(), status(true));
        checkIsModifiableCheckNeeded(null, status(true), composition(), status(false), composition());
        checkIsModifiableCheckNeeded(null, status(false), composition(), status(true), composition());
        checkIsModifiableCheckNeeded(false, status(false), status(true), composition());
        checkIsModifiableCheckNeeded(null, status(true), status(false), composition());
    }

    @Test
    void checkSystemIdsAcceptsOwnOrMissingSystemIds() {
        ContributionServiceImp service = serviceWithSystemId("local.ehrbase.org");
        ContributionCreateDto contribution = contribution(audit("local.ehrbase.org"), audit(null), null);
        contribution.getVersions().add(null);

        assertDoesNotThrow(() -> service.checkSystemIds(contribution));
    }

    @Test
    void checkSystemIdsRejectsAnotherSystemIdInTheAuditOrAnyVersion() {
        ContributionServiceImp service = serviceWithSystemId("local.ehrbase.org");

        assertThrows(
                InvalidApiParameterException.class,
                () -> service.checkSystemIds(contribution(audit("other.system"), audit("local.ehrbase.org"))));
        assertThrows(
                InvalidApiParameterException.class,
                () -> service.checkSystemIds(contribution(audit("local.ehrbase.org"), audit("other.system"))));
    }

    private static ContributionServiceImp serviceWithSystemId(String systemId) {
        SystemService systemService = mock(SystemService.class);
        when(systemService.getSystemId()).thenReturn(systemId);
        return new ContributionServiceImp(null, null, null, null, systemService, null, null, null, null);
    }

    /// A contribution with the given audit and one version per commit audit.
    private static ContributionCreateDto contribution(AuditDetails audit, AuditDetails... commitAudits) {
        ContributionCreateDto contribution = new ContributionCreateDto();
        contribution.setAudit(audit);
        for (AuditDetails commitAudit : commitAudits) {
            OriginalVersion<Composition> version = new OriginalVersion<>();
            version.setCommitAudit(commitAudit);
            contribution.getVersions().add(version);
        }
        return contribution;
    }

    private static AuditDetails audit(String systemId) {
        AuditDetails audit = new AuditDetails();
        audit.setSystemId(systemId);
        return audit;
    }

    @SafeVarargs
    private static void checkIsModifiableCheckNeeded(Boolean needed, OriginalVersion<? extends RMObject>... versions) {
        ContributionCreateDto contribution = new ContributionCreateDto();
        contribution.getVersions().addAll(Arrays.asList(versions));

        if (needed == null) {
            assertThrows(
                    StateConflictException.class, () -> ContributionServiceImp.isModifiableCheckNeeded(contribution));
        } else {
            assertEquals(needed, ContributionServiceImp.isModifiableCheckNeeded(contribution));
        }
    }

    private static OriginalVersion<? extends RMObject> delete() {
        return new OriginalVersion<Composition>();
    }

    private static OriginalVersion<Composition> composition() {
        OriginalVersion<Composition> ov = new OriginalVersion<>();
        ov.setData(new Composition());
        return ov;
    }

    private static OriginalVersion<Folder> directory() {
        OriginalVersion<Folder> ov = new OriginalVersion<>();
        ov.setData(new Folder());
        return ov;
    }

    private static OriginalVersion<EhrStatus> status(boolean modifiable) {
        OriginalVersion<EhrStatus> ov = new OriginalVersion<>();
        EhrStatus ehrStatus = new EhrStatus();
        ehrStatus.setModifiable(modifiable);
        ov.setData(ehrStatus);
        return ov;
    }
}
