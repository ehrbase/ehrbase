package org.ehrbase.ehr.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocatableHelperTest {

    @Test
    void testDividePathIntoSegments() {
        List<String> pathSegments = LocatableHelper.dividePathIntoSegments("context/other_context[at0001]/items[at0003]/value/value");
        assertEquals(5, pathSegments.size());

        pathSegments = LocatableHelper.dividePathIntoSegments("content[openEHR-EHR-SECTION.adhoc.v1 and name/value='Medication Summary']/items[openEHR-EHR-ACTION.medication.v1]/description[at0017]/items[openEHR-EHR-CLUSTER.medication.v1]/items[at0152]");
        assertEquals(5, pathSegments.size());

        pathSegments = LocatableHelper.dividePathIntoSegments("/content[openEHR-EHR-SECTION.adhoc.v1 and name/value='Advanced Directives']/items[openEHR-EHR-EVALUATION.limitation_of_treatment.v0]/data[at0001]/items[at0006]");
        assertEquals(4, pathSegments.size());
    }
}
