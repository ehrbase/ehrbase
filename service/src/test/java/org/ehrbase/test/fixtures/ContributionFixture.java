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
package org.ehrbase.test.fixtures;

import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.PartyIdentified;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Factory methods for creating Contribution test data.
 */
public final class ContributionFixture {

    private ContributionFixture() {}

    /**
     * Creates a contribution with a single composition (CREATION change type).
     */
    public static List<OriginalVersion<Composition>> singleComposition(String templateId) {
        var composition = CompositionFixture.minimal(templateId);

        var version = new OriginalVersion<>();
        version.setUid(new ObjectVersionId(UUID.randomUUID().toString(), "test-system", "1"));
        version.setData(composition);
        version.setCommitAudit(createAudit("creation"));

        List<OriginalVersion<Composition>> versions = new ArrayList<>();
        versions.add(version);
        return versions;
    }

    /**
     * Creates a contribution with two compositions.
     */
    public static List<OriginalVersion<Composition>> twoCompositions(String templateId) {
        var versions = singleComposition(templateId);
        var composition2 = CompositionFixture.minimal(templateId);
        composition2.setName(new DvText("Second Composition"));

        var version2 = new OriginalVersion<>();
        version2.setUid(new ObjectVersionId(UUID.randomUUID().toString(), "test-system", "1"));
        version2.setData(composition2);
        version2.setCommitAudit(createAudit("creation"));
        versions.add(version2);
        return versions;
    }

    private static AuditDetails createAudit(String changeType) {
        var audit = new AuditDetails();
        audit.setCommitter(new PartyIdentified(null, "Test Committer", null));
        audit.setChangeType(new DvCodedText(changeType, new CodePhrase(new TerminologyId("openehr"), "249")));
        return audit;
    }
}
