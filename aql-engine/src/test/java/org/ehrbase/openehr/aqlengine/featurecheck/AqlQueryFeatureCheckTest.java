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
package org.ehrbase.openehr.aqlengine.featurecheck;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.ehrbase.api.exception.AqlFeatureNotImplementedException;
import org.ehrbase.api.exception.IllegalAqlException;
import org.ehrbase.openehr.aqlengine.AqlConfigurationProperties;
import org.ehrbase.openehr.dbformat.StructureRmType;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class AqlQueryFeatureCheckTest {

    @ParameterizedTest
    @ValueSource(
            strings = {
                "SELECT s FROM EHR e CONTAINS EHR_STATUS s",
                "SELECT e/ehr_id/value FROM EHR e CONTAINS COMPOSITION LIMIT 10 OFFSET 20",
                "SELECT c from EHR [ehr_id/value='b037bf7c-0ecb-40fb-aada-fc7d559815ea'] CONTAINS COMPOSITION c",
                "SELECT c from EHR [ehr_id/value='b037bf7c-0ecb-40fb-aada-fc7d559815ea'] CONTAINS COMPOSITION c CONTAINS OBSERVATION",
                """
                    SELECT c, it from EHR [ehr_id/value='b037bf7c-0ecb-40fb-aada-fc7d559815ea']
                    CONTAINS COMPOSITION c CONTAINS OBSERVATION[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]
                    CONTAINS ITEM_TREE it""",
                """
                    SELECT c from EHR [ehr_id/value='b037bf7c-0ecb-40fb-aada-fc7d559815ea']
                    CONTAINS COMPOSITION c
                    CONTAINS OBSERVATION[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]""",
                """
                   SELECT e/ehr_id/value,
                       c/uid/value, c/name/value, c/archetype_node_id, c/archetype_details/template_id/value,
                       o/name/value, o/archetype_node_id
                   FROM EHR e CONTAINS COMPOSITION c CONTAINS OBSERVATION o""",
                """
                    SELECT c from EHR [ehr_id/value='b037bf7c-0ecb-40fb-aada-fc7d559815ea']
                    CONTAINS COMPOSITION c
                    CONTAINS OBSERVATION[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]""",
                """
                    SELECT c from EHR [ehr_id/value='b037bf7c-0ecb-40fb-aada-fc7d559815ea']
                    CONTAINS COMPOSITION c
                    CONTAINS OBSERVATION[name/value='Blood pressure (Training sample)']""",
                """
                    SELECT c from EHR [ehr_id/value='b037bf7c-0ecb-40fb-aada-fc7d559815ea']
                    CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.sample_blood_pressure.v1,'Blood pressure (Training sample)']
                    CONTAINS OBSERVATION[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1,'Blood pressure (Training sample)']""",
                """
                    SELECT c from EHR [ehr_id/value='b037bf7c-0ecb-40fb-aada-fc7d559815ea' OR ehr_id/value!='b037bf7c-0ecb-40fb-aada-fc7d559815ea']
                    CONTAINS COMPOSITION c[name/value!='Blood pressure (Training sample)' AND archetype_node_id='openEHR-EHR-COMPOSITION.sample_blood_pressure.v1' OR uid/value='b037bf7c-0ecb-40fb-aada-fc7d559815ea']
                    CONTAINS OBSERVATION[name/value!='Blood pressure (Training sample)' AND archetype_node_id='openEHR-EHR-COMPOSITION.sample_blood_pressure.v1' OR name/value!='Blood pressure (Training sample)']""",
                """
                   SELECT o
                   FROM EHR e CONTAINS COMPOSITION c CONTAINS OBSERVATION o
                   WHERE e/ehr_id/value MATCHES {'b037bf7c-0ecb-40fb-aada-fc7d559815ea'}
                     AND (o/archetype_node_id LIKE 'openEHR-EHR-OBSERVATION.sample_blood_pressure.*'
                       OR o/name/value = 'Blood pressure (Training sample)')
                     AND c/uid/value != 'b037bf7c-0ecb-40fb-aada-fc7d559815ea'
                     AND c/archetype_details/template_id/value = 'some-template.v1'""",
                """
                   SELECT e/ehr_id/value, c1, c2, o, ev, a
                   FROM EHR e CONTAINS(
                   (COMPOSITION c1
                     CONTAINS OBSERVATION o
                     AND EVALUATION ev)
                   AND COMPOSITION c2 CONTAINS ADMIN_ENTRY a)""",
                """
                   SELECT e/ehr_id/value, c1/content/name/value, c1/content/data/name/value, o, ev
                   FROM EHR e CONTAINS
                   COMPOSITION c1
                     CONTAINS OBSERVATION o
                     CONTAINS EVALUATION ev
                     WHERE c1/content/name/value = 'My Observation'""",
                """
                    SELECT e/ehr_id/value, c/content/name/value
                    FROM EHR e CONTAINS COMPOSITION c
                    ORDER BY e/ehr_id/value, c/content/name/value""",
                """
                   SELECT c/context/start_time
                   FROM COMPOSITION c
                   ORDER BY c/context/start_time
                """,
                """
                    SELECT ec/start_time/value
                    FROM EHR e CONTAINS COMPOSITION c CONTAINS EVENT_CONTEXT ec
                    ORDER BY ec/start_time ASC
                """,
                //                """
                //                   SELECT c
                //                   FROM COMPOSITION c
                //                   ORDER BY c/language/code_string
                //                """,
                """
                    SELECT e/ehr_id/value, c/content
                    FROM EHR e CONTAINS COMPOSITION c
                """,
                "SELECT c/setting/defining_code/code_string FROM EVENT_CONTEXT c",
                """
                    SELECT
                    o/name/mappings,
                    o/name/mappings/target,
                    o/name/mappings/purpose/mappings,
                    o/name/mappings/purpose/mappings/target
                    FROM OBSERVATION o
                """,
                "SELECT c/start_time/value, e/value/value, e/value/magnitude FROM EVENT_CONTEXT c CONTAINS ELEMENT e",
                """
                   SELECT c
                   FROM EVENT_CONTEXT c CONTAINS ELEMENT e
                   WHERE e/value = '1' AND c/start_time < '2023-10-13'
                """,
                """
                    SELECT l/name/value
                    FROM EHR e
                    CONTAINS EHR_STATUS
                    CONTAINS ELEMENT l
                """,
                """
                    SELECT s/subject/external_ref/id/value, s/other_details/items[at0001]/value/id
                    FROM EHR e
                    CONTAINS EHR_STATUS s
                """,
                """
                   SELECT s/other_details/items[at0001]/value/id
                   FROM EHR e
                   CONTAINS EHR_STATUS s
                   WHERE e/ehr_id/value = '10f23be7-fd39-4e71-a0a5-9d1624d662b7'
                """,
                """
                   SELECT t FROM ENTRY t
                """,
                """
                   SELECT
                       e/ehr_id/value,
                       -- All allowed usages of aggregate functions
                       COUNT(*),
                       COUNT(DISTINCT c/uid/value),
                       COUNT(el),
                       COUNT(el/name/mappings),
                       COUNT(el/value),
                       COUNT(el/value/value),
                       MAX(el/value/value),
                       MIN(el/value/value),
                       MAX(el/value),
                       MIN(el/value),
                       AVG(el/value/value),
                       SUM(el/value/value)
                   FROM EHR e CONTAINS COMPOSITION c CONTAINS ELEMENT el
                """,
                "SELECT 1 FROM EHR e",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = 'b037bf7c-0ecb-40fb-aada-fc7d559815ea::node::1'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = 'b037bf7c-0ecb-40fb-aada-fc7d559815ea::::1'",
                "SELECT c[openEHR-EHR-COMPOSITION.sample_blood_pressure.v1,'Blood pressure (Training sample)'] FROM COMPOSITION c",
                "SELECT e/ehr_id/value, e/time_created, e/time_created/value FROM EHR e WHERE e/time_created > '2021-01-02T12:13:14+01:00' ORDER BY e/time_created",
                """
                    SELECT
                     e/ehr_id/value,
                     e/system_id,
                     e/system_id/value
                    FROM EHR e
                    WHERE e/system_id/value = 'abc'
                    ORDER BY e/system_id/value
                """
            })
    void ensureQuerySupported(String aql) {

        assertDoesNotThrow(() -> runEnsureQuerySupported(aql));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "SELECT f FROM FOLDER f",
                """
                      SELECT f/uid/value, f/name/value, f/archetype_node_id
                      FROM FOLDER f[openEHR-EHR-FOLDER.generic.v1]
                    """,
                """
                      SELECT f2/uid/value, f2/name/value
                      FROM FOLDER f1[openEHR-EHR-FOLDER.generic.v1,'root']
                      CONTAINS FOLDER f2[openEHR-EHR-FOLDER.generic.v1,'Encounter']
                    """,
                """
                      SELECT e/ehr_id/value, f/uid/value
                      FROM EHR e
                      CONTAINS FOLDER f[openEHR-EHR-FOLDER.generic.v1,'Encounter']
                    """,
                """
                      SELECT c/uid/value, f/name/value
                      FROM FOLDER f
                      CONTAINS COMPOSITION c
                    """
            })
    void ensureQuerySupportedAqlOnFolderEnabled(String aql) {
        assertDoesNotThrow(() -> runEnsureQuerySupportedAqlOnFolderEnabled(aql));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "SELECT f/items FROM FOLDER f",
                "SELECT f/folders/items FROM FOLDER f",
                "SELECT f/items/namespace FROM FOLDER f",
                "SELECT f/items/type FROM FOLDER f",
                "SELECT f/items/id FROM FOLDER f",
                "SELECT f/items/id/value FROM FOLDER f"
            })
    void ensureQueryNotSupportedAqlOnFolderEnabled(String aql) {
        assertThrows(AqlFeatureNotImplementedException.class, () -> runEnsureQuerySupportedAqlOnFolderEnabled(aql));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "SELECT e FROM EHR e",
                "SELECT e/ehr_id FROM EHR e",
                "SELECT e[ehr_status/subject/external_ref/id/value='abc']/ehr_id/value FROM EHR e",
                "SELECT c[category/defining_code/code_string='433'] FROM COMPOSITION c",
                // ehr_status is rewritten as CONTAINS
                "SELECT e/ehr_status FROM EHR e",
                "SELECT e/compositions FROM EHR e",
                "SELECT e/directory FROM EHR e",
                "SELECT e/folders FROM EHR e",
                """
                   SELECT f
                   FROM FOLDER f
                """,
                """
                   SELECT c
                   FROM COMPOSITION c
                   WHERE c/uid/value = c/name/value
                """,
                """
                   SELECT c
                   FROM COMPOSITION c
                   WHERE c/uid = '1'
                """,
                """
                   SELECT c
                   FROM COMPOSITION c
                   WHERE EXISTS c/uid/value
                """,
                """
                   SELECT c
                   FROM COMPOSITION c
                   ORDER BY c/context/start_time/value
                """,
                """
                   SELECT o
                   FROM EHR e CONTAINS COMPOSITION c CONTAINS OBSERVATION o
                   WHERE e/ehr_id/value MATCHES {'b037bf7c-0ecb-40fb-aada-fc7d559815ea'}
                     AND (o/archetype_node_id LIKE 'openEHR-EHR-OBSERVATION.sample_blood_pressure.*'
                       OR o/name/value = 'Blood pressure (Training sample)')
                     AND c/uid/value != 'b037bf7c-0ecb-40fb-aada-fc7d559815ea'
                     AND EXISTS c/name/value
                     AND c/archetype_details/template_id/value = 'some-template.v1'""",
                """
                   SELECT e/ehr_id/value, AVG(c/context/start_time)
                   FROM EHR e CONTAINS COMPOSITION c
                """,
                """
                   SELECT e/ehr_id/value, SUM(c/context/start_time)
                   FROM EHR e CONTAINS COMPOSITION c
                """,
                """
                   SELECT e/ehr_id/value, MAX(c/uid/value)
                   FROM EHR e CONTAINS COMPOSITION c
                """,
                """
                   SELECT e/ehr_id/value, MIN(c/uid/value)
                   FROM EHR e CONTAINS COMPOSITION c
                """,
                """
                   SELECT e/ehr_id/value, AVG(c/uid/value)
                   FROM EHR e CONTAINS COMPOSITION c
                """,
                """
                   SELECT e/ehr_id/value, SUM(c/uid/value)
                   FROM EHR e CONTAINS COMPOSITION c
                """,
                "SELECT e/ehr_id/value FROM EHR e WHERE e/time_created/value > '2021-01-02T12:13:14+01:00'",
                "SELECT e/ehr_id/value FROM EHR e ORDER BY e/time_created/value"
            })
    void ensureQueryNotSupported(String aql) {

        assertThrows(AqlFeatureNotImplementedException.class, () -> runEnsureQuerySupported(aql));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                """
                   SELECT c/content/content/name/value
                   FROM COMPOSITION c
                """,
                """
                   SELECT c
                   FROM COMPOSITION c
                   WHERE c/content/content/name/value = 'invalid'
                """
            })
    void ensureInvalidPathRejected(String aql) {

        assertThatThrownBy(() -> runEnsureQuerySupported(aql))
                .isInstanceOf(IllegalAqlException.class)
                .hasMessageEndingWith(" is not a valid RM path");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = 'foo'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = 'foo::node::1'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = 'foo::node'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = 'foo::::'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = 'foo::::1'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = ''",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = '::node::1'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = '::node'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = '::::'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = '::::1'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = 'b037bf7c-0ecb-40fb-aada-fc7d559815ea::invalid::1'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = 'b037bf7c-0ecb-40fb-aada-fc7d559815ea::node::foo'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = 'b037bf7c-0ecb-40fb-aada-fc7d559815ea::node::0'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = 'b037bf7c-0ecb-40fb-aada-fc7d559815ea::::foo'",
                "SELECT c FROM COMPOSITION c WHERE c/uid/value = 'b037bf7c-0ecb-40fb-aada-fc7d559815ea::::0'"
            })
    void ensureInvalidConditionRejected(String aql) {

        assertThrows(IllegalAqlException.class, () -> runEnsureQuerySupported(aql));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "SELECT c FROM COMPOSITION c CONTAINS EHR_STATUS",
                "SELECT c FROM COMPOSITION c CONTAINS ELEMENT CONTAINS EHR_STATUS",
                "SELECT e FROM EHR CONTAINS COMPOSITION CONTAINS EHR_STATUS CONTAINS ELEMENT e"
            })
    void ensureContainsRejected(String aql) {

        assertThatThrownBy(() -> runEnsureQuerySupported(aql))
                .isInstanceOf(IllegalAqlException.class)
                .hasMessageContainingAll("Structure ", " cannot CONTAIN ", " (of structure ");
    }

    @ParameterizedTest
    @EnumSource(
            value = StructureRmType.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"INSTRUCTION_DETAILS", "FEEDER_AUDIT_DETAILS"})
    void ensureContainsRejectedNonStructureEntries(StructureRmType structureRmType) {

        String aql = "SELECT f FROM COMPOSITION f CONTAINS %s".formatted(structureRmType.name());
        assertThatThrownBy(() -> runEnsureQuerySupported(aql))
                .isInstanceOf(AqlFeatureNotImplementedException.class)
                .hasMessage(
                        "Not implemented: CONTAINS %s is currently not supported".formatted(structureRmType.name()));
    }

    @Test
    void ensureContainsRejectedExperimentalAqlOnFolderDisabled() {

        assertThatThrownBy(() -> runEnsureQuerySupported("SELECT f FROM FOLDER f"))
                .isInstanceOf(AqlFeatureNotImplementedException.class)
                .hasMessageContainingAll("CONTAINS FOLDER is an experimental feature and currently disabled.");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"SELECT f FROM COMPOSITION CONTAINS FOLDER f", "SELECT f FROM EHR_STATUS CONTAINS FOLDER f"})
    void ensureContainsRejectedExperimentalAqlOnFolder(String aql) {

        assertThatThrownBy(() -> runEnsureQuerySupportedAqlOnFolderEnabled(aql))
                .isInstanceOf(IllegalAqlException.class)
                .hasMessageContainingAll("Structure ", " cannot CONTAIN ", " (of structure ");
    }

    @ParameterizedTest
    @EnumSource(
            value = StructureRmType.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"FOLDER", "COMPOSITION", "INSTRUCTION_DETAILS", "FEEDER_AUDIT_DETAILS"})
    void ensureContainsExperimentalAqlOnFolderRestrictedToTypes(StructureRmType structureRmType) {

        String aql = "SELECT f FROM FOLDER f CONTAINS %s".formatted(structureRmType.name());
        assertThatThrownBy(() -> runEnsureQuerySupportedAqlOnFolderEnabled(aql))
                .isInstanceOf(AqlFeatureNotImplementedException.class)
                .hasMessage("Not implemented: FOLDER CONTAINS %s is currently not supported"
                        .formatted(structureRmType.name()));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                """
                   SELECT c/uid/value
                   FROM VERSION cv CONTAINS COMPOSITION c
                """,
                """
                   SELECT c/uid/value
                   FROM VERSION cv[LATEST_VERSION] CONTAINS COMPOSITION c
                """,
                """
                   SELECT e/ehr_id/value, c/uid/value
                   FROM EHR e CONTAINS VERSION cv[LATEST_VERSION] CONTAINS COMPOSITION c
                """,
                // all supported usages of all paths for (ORIGNINAL_)VERSION
                """
                   SELECT
                     cv/uid/value,
                     cv/commit_audit/time_committed,
                     cv/commit_audit/time_committed/value,
                     cv/commit_audit/system_id,
                     cv/commit_audit/description,
                     cv/commit_audit/description/value,
                     cv/commit_audit/change_type,
                     cv/commit_audit/change_type/value,
                     cv/commit_audit/change_type/defining_code/code_string,
                     cv/commit_audit/change_type/defining_code/preferred_term,
                     cv/commit_audit/change_type/defining_code/terminology_id/value,
                     cv/contribution/id/value
                   FROM VERSION cv[LATEST_VERSION] CONTAINS COMPOSITION c
                   WHERE cv/uid/value = 'b037bf7c-0ecb-40fb-aada-fc7d559815ea::node::2'
                     AND cv/commit_audit/time_committed < '2021'
                     AND cv/commit_audit/system_id = 'system'
                     AND cv/commit_audit/description/value = 'description'
                     AND cv/commit_audit/change_type/value = 'ct'
                     AND cv/commit_audit/change_type/defining_code/code_string = 'ct'
                     AND cv/commit_audit/change_type/defining_code/preferred_term = 'ct'
                     AND cv/commit_audit/change_type/defining_code/terminology_id/value = 'ct'
                     AND cv/contribution/id/value = 'c037bf7c-0ecb-40fb-aada-fc7d559815eb'
                   ORDER BY
                     cv/commit_audit/change_type/defining_code/code_string,
                     cv/commit_audit/change_type/defining_code/preferred_term,
                     cv/commit_audit/change_type/value,
                     cv/commit_audit/description/value,
                     cv/commit_audit/time_committed,
                     cv/uid/value
                """,
                """
                   SELECT es/uid/value
                   FROM VERSION cv[LATEST_VERSION] CONTAINS EHR_STATUS es
                """,
                """
                   SELECT e/ehr_id/value, c1/uid/value, c2/uid/value
                   FROM EHR e CONTAINS
                     (VERSION[LATEST_VERSION] CONTAINS COMPOSITION c1)
                     OR (VERSION[LATEST_VERSION] CONTAINS COMPOSITION c2)
                """,
                """
                   SELECT e/ehr_id/value, c1/uid/value, c2/uid/value
                   FROM EHR e CONTAINS
                     (VERSION[LATEST_VERSION] CONTAINS COMPOSITION c1)
                     AND (VERSION[LATEST_VERSION] CONTAINS COMPOSITION c2)
                """,
            })
    void ensureVersionSupported(String aql) {

        assertDoesNotThrow(() -> runEnsureQuerySupported(aql));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                """
                   SELECT cv/commit_audit/time_committed/value
                   FROM VERSION cv[LATEST_VERSION]
                """,
                """
                   SELECT el/name/value
                   FROM VERSION cv[LATEST_VERSION] CONTAINS ELEMENT el
                """,
                """
                   SELECT c/name/value
                   FROM VERSION cv[LATEST_VERSION] CONTAINS VERSION cv2[LATEST_VERSION] CONTAINS COMPOSITION c
                """,
                """
                   SELECT c/name/value
                   FROM COMPOSITION c CONTAINS VERSION cv[LATEST_VERSION]
                """
            })
    void checkIllegalVersion(String aql) {

        assertThatThrownBy(() -> runEnsureQuerySupported(aql))
                .isInstanceOf(IllegalAqlException.class)
                .message()
                .isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                """
                   SELECT c/uid/value
                   FROM VERSION cv[ALL_VERSIONS] CONTAINS COMPOSITION c
                """,
                """
                   SELECT c/uid/value
                   FROM VERSION cv[commit_audit/time_committed > '2021-12-13'] CONTAINS COMPOSITION c
                """,
                """
                   SELECT f/uid/value
                   FROM VERSION cv[LATEST_VERSION] CONTAINS FOLDER f
                """,
                """
                   SELECT c1/name/value, c2/name/value
                   FROM VERSION cv[LATEST_VERSION] CONTAINS COMPOSITION c1 OR COMPOSITION c2
                """,
                """
                   SELECT c1/name/value, c2/name/value
                   FROM VERSION cv[LATEST_VERSION] CONTAINS COMPOSITION c1 AND COMPOSITION c2
                """,
                """
                   SELECT cv/preceding_version_uid
                   FROM VERSION cv[LATEST_VERSION] CONTAINS COMPOSITION c
                """,
                """
                   SELECT cv/other_input_version_uids
                   FROM VERSION cv[LATEST_VERSION] CONTAINS COMPOSITION c
                """,
                """
                   SELECT cv/data
                   FROM VERSION cv[LATEST_VERSION] CONTAINS COMPOSITION c
                """,
                """
                   SELECT cv/attestations
                   FROM VERSION cv[LATEST_VERSION] CONTAINS COMPOSITION c
                """,
                """
                   SELECT cv/lifecycle_state
                   FROM VERSION cv[LATEST_VERSION] CONTAINS COMPOSITION c
                """,
                """
                   SELECT cv/signature
                   FROM VERSION cv[LATEST_VERSION] CONTAINS COMPOSITION c
                """,
                """
                   SELECT cv
                   FROM VERSION cv[LATEST_VERSION] CONTAINS COMPOSITION c
                """
            })
    void ensureVersionNotSupported(String aql) {

        assertThatThrownBy(() -> runEnsureQuerySupported(aql))
                .isInstanceOf(AqlFeatureNotImplementedException.class)
                .hasMessageStartingWith("Not implemented: ");
    }

    @Test
    void ensureVersionSupportedAqlOnFolderEnabled() {

        assertDoesNotThrow(
                () -> runEnsureQuerySupportedAqlOnFolderEnabled(
                        """
                   SELECT f/uid/value
                   FROM VERSION cv[LATEST_VERSION] CONTAINS FOLDER f
                """));
    }

    private void runEnsureQuerySupported(String aql) {

        runEnsureQuerySupported(propertiesWithAqlOnFolderEnabled(false), aql);
    }

    private void runEnsureQuerySupportedAqlOnFolderEnabled(String aql) {

        runEnsureQuerySupported(propertiesWithAqlOnFolderEnabled(true), aql);
    }

    private void runEnsureQuerySupported(AqlConfigurationProperties aqlFeature, String aql) {

        AqlQuery aqlQuery = AqlQueryParser.parse(aql);
        AqlQueryFeatureCheck aqlQueryFeatureCheck = new AqlQueryFeatureCheck(() -> "node", aqlFeature);
        aqlQueryFeatureCheck.ensureQuerySupported(aqlQuery);
    }

    private AqlConfigurationProperties propertiesWithAqlOnFolderEnabled(boolean aqlOnFolderEnabled) {
        return new AqlConfigurationProperties(
                false,
                new AqlConfigurationProperties.Experimental(
                        new AqlConfigurationProperties.Experimental.AqlOnFolder(aqlOnFolderEnabled)));
    }
}
