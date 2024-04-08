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

import static org.junit.Assert.assertThrows;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.datavalues.quantity.datetime.DvDateTime;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.PartyProxy;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.PartyRef;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;
import org.ehrbase.openehr.sdk.test_data.contribution.ContributionTestDataCanonicalJson;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.service.contribution.ContributionServiceHelperTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;

class ValidationServiceImpTest {

    private static ValidationServiceImp createValidationServiceImp() {
        ServerConfig serverConfig = Mockito.mock(ServerConfig.class);
        ObjectProvider<ExternalTerminologyValidation> objectProvider = Mockito.mock(ObjectProvider.class);
        ValidationServiceImp cut = new ValidationServiceImp(null, null, serverConfig, objectProvider, false);
        return cut;
    }

    @Test
    void contributionValidId() {
        ValidationServiceImp cut = createValidationServiceImp();
        cut.check(new ContributionCreateDto(HierObjectId.createRandomUUID(), dummyVersions(), anyValidAuditDetails()));
    }

    private List<OriginalVersion<? extends RMObject>> dummyVersions() {
        OriginalVersion<RMObject> version = new OriginalVersion<>();
        version.setLifecycleState(new DvCodedText("complete", new CodePhrase(new TerminologyId("openehr"), "532")));

        version.setCommitAudit(anyValidAuditDetails());
        return List.of(version);
    }

    @Test
    void contributionValidAuditDetails() {
        ValidationServiceImp cut = createValidationServiceImp();
        cut.check(new ContributionCreateDto(null, dummyVersions(), anyValidAuditDetails()));
    }

    @Test
    void contributionMissingVersions() {
        ValidationServiceImp cut = createValidationServiceImp();
        assertThrows(ValidationException.class, () -> {
            cut.check(new ContributionCreateDto(null, new ArrayList<>(), anyValidAuditDetails()));
        });
    }

    @Test
    void validContribution() {
        ValidationServiceImp cut = createValidationServiceImp();
        ContributionCreateDto contribution =
                ContributionServiceHelperTest.loadContribution(ContributionTestDataCanonicalJson.ONE_ENTRY_COMPOSITION);
        //        contribution.getAudit().setTimeCommitted(new DvDateTime(LocalDateTime.now()));
        //        OriginalVersion<? extends RMObject> version = contribution.getVersions().getFirst();
        //        version.setUid(new ObjectVersionId(UUID.randomUUID() + "::" + "ehrbase" + "::" + 1));
        //        version.getCommitAudit().setTimeCommitted(new DvDateTime(LocalDateTime.now()));
        //        version.setData(null);
        //        version.setContribution(new ObjectRef<>(HierObjectId.createRandomUUID(), "local", "contribution"));
        cut.check(contribution);

        cut.check(ContributionServiceHelperTest.loadContribution(
                ContributionTestDataCanonicalJson.TWO_ENTRIES_COMPOSITION));
        cut.check(ContributionServiceHelperTest.loadContribution(
                ContributionTestDataCanonicalJson.TWO_ENTRIES_COMPOSITION));
        cut.check(ContributionServiceHelperTest.loadContribution(
                ContributionTestDataCanonicalJson.STATUS_COMPOITION_MODIFICATION));
    }

    private static AuditDetails anyValidAuditDetails() {
        AuditDetails audit = new AuditDetails();

        audit.setSystemId("local");
        audit.setChangeType(anyChangeType());
        audit.setDescription(anyDescription());
        audit.setCommitter(anyCommitter());

        return audit;
    }

    private static DvDateTime anyDvDateTime() {
        return new DvDateTime(LocalDateTime.now());
    }

    private static DvCodedText anyChangeType() {
        return new DvCodedText("creation", new CodePhrase(new TerminologyId("openehr"), "249"));
    }

    private static DvText anyDescription() {
        return new DvText("lorem ipsum");
    }

    private static PartyProxy anyCommitter() {
        PartyRef ref = new PartyRef(new GenericId("123-abc", "test"), "de.vitagroup", "PERSON");

        return new PartySelf(ref);
    }
}
