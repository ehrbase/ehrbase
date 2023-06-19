/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.changecontrol.Version;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.CompositionFormat;
import org.ehrbase.openehr.sdk.test_data.contribution.ContributionTestDataCanonicalJson;
import org.junit.Test;

public class ContributionServiceHelperTest {

    @Test
    public void splitContent() throws IOException {
        // call splitContent with first test data input
        InputStream stream = ContributionTestDataCanonicalJson.ONE_ENTRY_COMPOSITION.getStream();
        assertNotNull(stream);
        String streamString = IOUtils.toString(stream, UTF_8);
        Map<String, Object> splitContent = ContributionServiceHelper.splitContent(streamString, CompositionFormat.JSON);
        splitContentCheck(splitContent);

        // second test data input
        InputStream stream2 = ContributionTestDataCanonicalJson.TWO_ENTRIES_COMPOSITION.getStream();
        assertNotNull(stream2);
        String streamString2 = IOUtils.toString(stream2, UTF_8);
        Map<String, Object> splitContent2 =
                ContributionServiceHelper.splitContent(streamString2, CompositionFormat.JSON);
        splitContentCheck(splitContent2);

        // TODO add more test data and XML when ready
    }

    // helper method with the actual logical tests
    private void splitContentCheck(Map<String, Object> splitContent) {
        // should have "_type", "versions" and "audit" entries
        assertEquals(3, splitContent.size());
        // the "version" one should be existing and of type List
        Object versionsContent = splitContent.get("versions");
        assertTrue(versionsContent instanceof List);
        // the "audit" one should be existing and of type map
        Object auditContent = splitContent.get("audit");
        assertTrue(auditContent instanceof Map);
    }

    @Test
    public void extractVersionObjects() {
        // pre-step: call splitContent with first test data input (assumed to be correct as tested separately)
        Map<String, Object> splitContent = extractVersionObjectsPreStep(
                ContributionTestDataCanonicalJson.ONE_ENTRY_COMPOSITION.getStream(), CompositionFormat.JSON);
        // actual test
        Object versionsContent = splitContent.get("versions");
        List<Version> versions =
                ContributionServiceHelper.extractVersionObjects((ArrayList) versionsContent, CompositionFormat.JSON);
        extractVersionObjectsCheck(versions, 1);

        // pre-step: call splitContent with second test data input (assumed to be correct as tested separately)
        Map<String, Object> splitContent2 = extractVersionObjectsPreStep(
                ContributionTestDataCanonicalJson.TWO_ENTRIES_COMPOSITION.getStream(), CompositionFormat.JSON);
        // actual test
        Object versionsContent2 = splitContent2.get("versions");
        List<Version> versions2 =
                ContributionServiceHelper.extractVersionObjects((ArrayList) versionsContent2, CompositionFormat.JSON);
        extractVersionObjectsCheck(versions2, 2);

        // TODO add more test data and XML when ready
    }

    // pre-step helper. called method is tested separately
    private Map<String, Object> extractVersionObjectsPreStep(InputStream stream, CompositionFormat format) {
        assertNotNull(stream);
        String streamString = null;
        try {
            streamString = IOUtils.toString(stream, UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("error within scope of splitContent()");
        }
        return ContributionServiceHelper.splitContent(streamString, format);
    }

    // helper method with the actual logical tests
    private void extractVersionObjectsCheck(List<Version> versions, int numVersions) {
        assertEquals(numVersions, versions.size());
        for (Version version : versions) {
            assertNotNull(version.getData());
            assertTrue(version.getData() instanceof LinkedHashMap);
        }
    }

    @Test
    public void unmarshalMapContentToRmObject() {
        // pre-step: calling above's splitContent and extractVersionObjects (both tested separately) to prepare
        // environment for unmarshalMapContentToRmObject with first input
        List<Version> versions = unmarshalMapContentToRmObjectPreStep(
                ContributionTestDataCanonicalJson.ONE_ENTRY_COMPOSITION.getStream(), CompositionFormat.JSON);
        // actual test
        unmarshalMapContentToRmObjectCheck(
                versions, CompositionFormat.JSON, ContributionServiceImp.SupportedClasses.COMPOSITION);

        // pre-step: calling above's splitContent and extractVersionObjects (both tested separately) to prepare
        // environment for unmarshalMapContentToRmObject with second input
        List<Version> versions2 = unmarshalMapContentToRmObjectPreStep(
                ContributionTestDataCanonicalJson.TWO_ENTRIES_COMPOSITION.getStream(), CompositionFormat.JSON);
        // actual test
        unmarshalMapContentToRmObjectCheck(
                versions, CompositionFormat.JSON, ContributionServiceImp.SupportedClasses.COMPOSITION);

        // TODO add more test data and XML when ready
    }

    // pre-step helper. called method is tested separately
    private List<Version> unmarshalMapContentToRmObjectPreStep(InputStream stream, CompositionFormat format) {
        Map<String, Object> splitContent = extractVersionObjectsPreStep(stream, format);
        Object versionsContent = splitContent.get("versions");
        return ContributionServiceHelper.extractVersionObjects((ArrayList) versionsContent, format);
    }

    // helper method with the actual logical tests
    private void unmarshalMapContentToRmObjectCheck(
            List<Version> versions, CompositionFormat format, ContributionServiceImp.SupportedClasses classType) {
        for (Version version : versions) {
            RMObject versionRmObject =
                    ContributionServiceHelper.unmarshalMapContentToRmObject((LinkedHashMap) version.getData(), format);
            assertNotNull(ContributionServiceImp.SupportedClasses.valueOf(
                    versionRmObject.getClass().getSimpleName().toUpperCase()));
            assertEquals(
                    classType,
                    ContributionServiceImp.SupportedClasses.valueOf(
                            versionRmObject.getClass().getSimpleName().toUpperCase()));
        }
    }

    @Test
    public void getVersions() throws IOException {
        // comparable test method that creates list of version manually. should be the same as the one created with
        // getVersions
        InputStream stream = ContributionTestDataCanonicalJson.ONE_ENTRY_COMPOSITION.getStream();
        assertNotNull(stream);
        String streamString = IOUtils.toString(stream, UTF_8);
        List<Version> versionsA = unmarshalMapContentToRmObjectPreStep(
                ContributionTestDataCanonicalJson.ONE_ENTRY_COMPOSITION.getStream(), CompositionFormat.JSON);
        List<Version> versionsB = ContributionServiceHelper.parseVersions(streamString, CompositionFormat.JSON);

        assertEquals(versionsA, versionsB);

        // second test input
        InputStream stream2 = ContributionTestDataCanonicalJson.TWO_ENTRIES_COMPOSITION.getStream();
        assertNotNull(stream2);
        String streamString2 = IOUtils.toString(stream2, UTF_8);
        List<Version> versionsA2 = unmarshalMapContentToRmObjectPreStep(
                ContributionTestDataCanonicalJson.TWO_ENTRIES_COMPOSITION.getStream(), CompositionFormat.JSON);
        List<Version> versionsB2 = ContributionServiceHelper.parseVersions(streamString2, CompositionFormat.JSON);

        assertEquals(versionsA2, versionsB2);

        // TODO add more test data and XML when ready
    }
}
