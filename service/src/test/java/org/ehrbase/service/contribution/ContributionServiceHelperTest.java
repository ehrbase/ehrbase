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
package org.ehrbase.service.contribution;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.ehrbase.api.service.ContributionService.ContributionChangeType;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;
import org.ehrbase.openehr.sdk.test_data.contribution.ContributionTestDataCanonicalJson;
import org.junit.jupiter.api.Test;

public class ContributionServiceHelperTest {

    @Test
    void unmarshalContributionOneComposition() {
        ContributionCreateDto c = loadContribution(ContributionTestDataCanonicalJson.ONE_ENTRY_COMPOSITION);

        assertThat(c.getAudit()).satisfies(a -> assertThat(a.getSystemId()).isEqualTo("test-system-id"));

        assertVersions(
                c,
                v -> assertOriginalVersion(v, ContributionChangeType.CREATION, Composition.class, d -> {
                    assertThat(d.getArchetypeNodeId()).isEqualTo("openEHR-EHR-COMPOSITION.minimal.v1");
                }));
    }

    @Test
    void unmarshalContributionTwoCompositions() {
        ContributionCreateDto c = loadContribution(ContributionTestDataCanonicalJson.TWO_ENTRIES_COMPOSITION);

        assertThat(c.getAudit()).satisfies(a -> assertThat(a.getSystemId()).isEqualTo("test-system-id"));

        assertVersions(
                c,
                v -> assertOriginalVersion(v, ContributionChangeType.CREATION, Composition.class, d -> {
                    assertThat(d.getArchetypeNodeId()).isEqualTo("openEHR-EHR-COMPOSITION.minimal.v1");
                }),
                v -> assertOriginalVersion(v, ContributionChangeType.CREATION, Composition.class, d -> {
                    assertThat(d.getArchetypeNodeId()).isEqualTo("openEHR-EHR-COMPOSITION.minimal.v1");
                }));
    }

    @Test
    void unmarshalContributionOneEntryCompositionModification() {
        ContributionCreateDto c =
                loadContribution(ContributionTestDataCanonicalJson.ONE_ENTRY_COMPOSITION_MODIFICATION_LATEST);

        assertThat(c.getAudit()).satisfies(a -> assertThat(a.getSystemId()).isEqualTo("test-system-id"));

        assertVersions(
                c,
                v -> assertOriginalVersion(v, ContributionChangeType.MODIFICATION, Composition.class, d -> {
                    assertThat(d.getArchetypeNodeId()).isEqualTo("openEHR-EHR-COMPOSITION.minimal.v1");
                }));
    }

    private static void assertVersions(
            ContributionCreateDto contribution, Consumer<OriginalVersion<?>>... versionAssertions) {
        List<OriginalVersion<? extends RMObject>> versions = contribution.getVersions();
        assertThat(versions).hasSize(versionAssertions.length);
        for (int i = 0; i < versions.size(); i++) {
            versionAssertions[i].accept(versions.get(i));
        }
    }

    private static <D extends RMObject> void assertOriginalVersion(
            OriginalVersion<?> v, ContributionChangeType changetype, Class<D> dataType, Consumer<D> dataAssertions) {
        assertThat(v.getCommitAudit().getChangeType().getDefiningCode().getCodeString())
                .isEqualTo(Integer.toString(changetype.getCode()));
        Object data = v.getData();
        assertThat(data).isInstanceOf(dataType);
        dataAssertions.accept((D) data);
    }

    public static ContributionCreateDto loadContribution(ContributionTestDataCanonicalJson contributionData) {
        try {
            ContributionWrapper contributionWrapper =
                    ContributionServiceHelper.unmarshalContribution(loadContributionString(contributionData));
            ContributionCreateDto contributionCreateDto = contributionWrapper.getContributionCreateDto();
            assertNotNull(contributionCreateDto);
            return contributionCreateDto;
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    private static String loadContributionString(ContributionTestDataCanonicalJson contributionData)
            throws IOException {
        try (InputStream in = contributionData.getStream()) {
            assertNotNull(in);
            return IOUtils.toString(in, UTF_8);
        }
    }
}
