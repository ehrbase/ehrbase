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
package org.ehrbase.openehr.aqlengine.sql;

import java.util.Optional;
import java.util.UUID;
import org.ehrbase.api.definitions.ServerConfig;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.openehr.aqlengine.asl.AqlSqlLayer;
import org.ehrbase.openehr.aqlengine.asl.AslGraph;
import org.ehrbase.openehr.aqlengine.asl.model.query.AslRootQuery;
import org.ehrbase.openehr.aqlengine.querywrapper.AqlQueryWrapper;
import org.ehrbase.openehr.sdk.aql.dto.AqlQuery;
import org.ehrbase.openehr.sdk.aql.parser.AqlQueryParser;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.SelectQuery;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AqlSqlQueryBuilderTest {

    public static class TestServerConfig implements ServerConfig {
        @Override
        public int getPort() {
            return 0;
        }

        @Override
        public void setPort(int port) {}

        @Override
        public boolean isDisableStrictValidation() {
            return false;
        }
    }

    @Disabled
    @Test
    void printSqlQuery() {
        AqlQuery aqlQuery = AqlQueryParser.parse(
                """
        -- SELECT o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude FROM EHR e CONTAINS OBSERVATION o where e/ehr_id/value ='9d6c1af0-ec2c-4cdb-bd79-d261b6fcc879'  limit 5
        -- SELECT DISTINCT c/uid/value, c/archetype_node_id FROM EHR e CONTAINS COMPOSITION c CONTAINS ELEMENT o
        -- ORDER BY
        -- c/uid/value
        -- ,
        -- c/archetype_node_id
        -- LIMIT 1

        -- SELECT c from EHR e contains COMPOSITION c where e/ehr_id/value = 'a6080b1b-da89-4992-b179-279a06ebe0e5'
        -- SELECT o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude, c/uid/value  from EHR e contains COMPOSITION c contains OBSERVATION o[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1] where e/ehr_id/value = 'c5db0694-5cd2-4fd1-a5bf-ed25f1c5d371' order by c/uid/value limit 20

        -- SELECT c from EHR e contains COMPOSITION c where e/ehr_id/value = '3236052f-2bda-4c3a-a413-602398d5f5e6'
        -- SELECT o from EHR e contains COMPOSITION c contains OBSERVATION o where e/ehr_id/value = '3236052f-2bda-4c3a-a413-602398d5f5e6'
        -- SELECT o from EHR e contains COMPOSITION c contains POINT_EVENT o where e/ehr_id/value = '3236052f-2bda-4c3a-a413-602398d5f5e6'
        -- SELECT o from EHR e contains COMPOSITION c contains ELEMENT o where e/ehr_id/value = '3236052f-2bda-4c3a-a413-602398d5f5e6'
        -- SELECT o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude, c/uid/value
        -- from EHR e contains COMPOSITION c contains OBSERVATION o[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]
        -- where e/ehr_id/value matches { '44e4bb26-3ee9-4450-856f-03f8ff437f53','2a32723d-656e-4b70-8def-f18dc9f09898'}
        -- order by c/uid/value limit 5
        -- SELECT o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/magnitude,
        -- o/data[at0001]/events[at0002]/data[at0003]/items[at0004]/value/units,
        -- o/data[at0001]/events[at0002]/data[at0003]/items[at0005]/value/magnitude,
        -- o/data[at0001]/events[at0002]/data[at0003]/items[at0005]/value/units,
        -- o/data[at0001]/events[at0002]/data[at0003]/items[at1006]/value/magnitude,
        -- o/data[at0001]/events[at0002]/data[at0003]/items[at1006]/value/units
        -- from EHR e
        -- contains COMPOSITION c
        -- contains OBSERVATION o[openEHR-EHR-OBSERVATION.sample_blood_pressure.v1]
        -- where c/uid/value = '3236052f-2bda-4c3a-a413-602398d5f5e6'
        -- SELECT o from EHR e contains COMPOSITION c contains ELEMENT o where e/ehr_id/value = '288b7cbc-a503-44c6-b41a-49f468ba29e1'
        -- SELECT
        -- o/name/mappings,
        -- o/name/mappings/target,
        -- o/name/mappings/purpose/mappings,
        -- o/name/mappings/purpose/mappings/target,
        -- o/other_participations/function
        -- FROM OBSERVATION o
        -- SELECT c/context/other_context/items,c,x, x/start_time, it1/items/value/value
        -- FROM EHR e CONTAINS (COMPOSITION c CONTAINS EVENT_CONTEXT x CONTAINS (ITEM_TREE it1 CONTAINS ELEMENT e) OR CLUSTER cl) OR (COMPOSITION c2 CONTAINS EVENT_CONTEXT x2 CONTAINS ITEM_TREE it2)
        -- SELECT
        -- 	e/ehr_id/value as PatientID,
        -- 	c/context/start_time/value as Befunddatum,
        -- 	y/items[at0001]/value/value as FallID,
        -- 	a/items[at0001]/value/id as LabordatenID,
        -- 	a/items[at0029]/value/defining_code/code_string as MaterialID,
        -- 	a/items[at0029]/value/value as Material_l,
        -- 	a/items[at0015]/value/value as ZeitpunktProbenentnahme,
        -- 	a/items[at0034]/value/value as ZeitpunktProbeneingang,
        -- 	d/items[at0024]/value/value as Keim_l,
        -- 	d/items[at0024]/value/defining_code/code_string as KeimID,
        -- 	d/items[at0001] as Befund,
        -- 	d/items[at0001] as BefundCode,
        -- 	d/items[at0001] as Viruslast,
        -- 	l/data[at0001]/events[at0002]/data[at0003]/items[at0101]/value/value as Befundkommentar
        -- FROM EHR e
        -- CONTAINS COMPOSITION c
        -- CONTAINS (
        -- 	CLUSTER y[openEHR-EHR-CLUSTER.case_identification.v0]
        -- 	AND OBSERVATION l[openEHR-EHR-OBSERVATION.laboratory_test_result.v1]
        -- 		CONTAINS (CLUSTER a[openEHR-EHR-CLUSTER.specimen.v1]
        -- 		AND CLUSTER b[openEHR-EHR-CLUSTER.laboratory_test_panel.v0]
        -- 			CONTAINS (CLUSTER d[openEHR-EHR-CLUSTER.laboratory_test_analyte.v1])
        -- 		)
        -- 	)
        -- WHERE
        -- 	-- c/name/value = 'Virologischer Befund'
        -- 	c/archetype_details/template_id/value = 'Virologischer Befund'
        -- 	AND e/ehr_id/value = 'd50c939a-7661-4ef1-a67b-5a57661263db'
        -- ORDER BY a/items[at0015]/value/value ASC
        -- SELECT e/ehr_id/value, c/uid/value, COUNT(*), COUNT(c/archetype_node_id), MAX(s/subject/external_ref/id/value),COUNT(DISTINCT c/archetype_node_id), COUNT(ec/other_context/items/value/value), COUNT(DISTINCT ec/other_context/items/value/value), MAX(ec/other_context/items/value/value), MIN(ec/other_context/items/value/value), AVG(ec/other_context/items/value/value), SUM(ec/other_context/items/value/value)
        -- FROM EHR e CONTAINS EHR_STATUS s AND COMPOSITION c CONTAINS EVENT_CONTEXT ec
        -- SELECT 1,count(c/uid/value), 'a', e/ehr_id/value FROM EHR e CONTAINS EHR_STATUS s AND COMPOSITION c WHERE e/ehr_id/value matches{ 'd50c939a-7661-4ef1-a67b-5a57661263db',1}
        -- SELECT el/value FROM COMPOSITION c CONTAINS ELEMENT el ORDER BY el/value
        -- SELECT o/data[at0001]/events[at0002]/data[at0003]/items[at0008]/value/magnitude FROM OBSERVATION o[openEHR-EHR-OBSERVATION.conformance_observation.v0] ORDER BY o/data[at0001]/events[at0002]/data[at0003]/items[at0008]/value ASC
        SELECT c/feeder_audit AS sisOrigenIds, c/uid/value AS compositionid, c/content[openEHR-EHR-SECTION.immunisation_list.v0]/items[openEHR-EHR-ACTION.medication.v1]/time/value AS dataAdmin, c/content[openEHR-EHR-SECTION.immunisation_list.v0]/items[openEHR-EHR-ACTION.medication.v1]/description[at0017]/items[at0020]/value/defining_code/code_string AS codiImmunitzacio, c/content[openEHR-EHR-SECTION.immunisation_list.v0]/items[openEHR-EHR-ACTION.medication.v1]/description[at0017]/items[openEHR-EHR-CLUSTER.medication.v2]/items[at0132]/value AS codiMarcaComercial, c/content[openEHR-EHR-SECTION.immunisation_list.v0]/items[openEHR-EHR-ACTION.medication.v1]/description[at0017]/items[openEHR-EHR-CLUSTER.medication.v2]/items[at0150]/value/value AS numLot, c/content[openEHR-EHR-SECTION.immunisation_list.v0]/items[openEHR-EHR-ACTION.medication.v1]/other_participations/performer AS dadesProfessional, c/context/other_context[at0004]/items[openEHR-EHR-CLUSTER.admin_salut.v0]/items[at0007]/items[at0014]/value/defining_code/code_string AS codiCentrePublicador, c/content[openEHR-EHR-SECTION.immunisation_list.v0]/items[openEHR-EHR-ACTION.medication.v1]/provider/identifiers/id AS origenInformacio, c/content[openEHR-EHR-SECTION.immunisation_list.v0]/items[openEHR-EHR-ACTION.medication.v1]/ism_transition/current_state AS estat, c/content[openEHR-EHR-SECTION.immunisation_list.v0]/items[openEHR-EHR-ACTION.medication.v1]/description[at0017]/items[at0021] AS reason FROM EHR e CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.vaccination_list.v0] WHERE e/ehr_id/value = 'e6fad8ba-fb4f-46a2-bf82-66edb43f142f'
        """);

        System.out.println("/*");
        System.out.println(aqlQuery.render());
        System.out.println("*/");

        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);

        KnowledgeCacheService kcs = Mockito.mock(KnowledgeCacheService.class);
        Mockito.when(kcs.findUuidByTemplateId(ArgumentMatchers.anyString())).thenReturn(Optional.of(UUID.randomUUID()));

        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(kcs, () -> "node");
        AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);

        System.out.println("/*");
        System.out.println(AslGraph.createAslGraph(aslQuery));
        System.out.println("*/");
        System.out.println();

        AqlSqlQueryBuilder sqlQueryBuilder =
                new AqlSqlQueryBuilder(new DefaultDSLContext(SQLDialect.YUGABYTEDB), kcs, Optional.empty());

        SelectQuery<Record> sqlQuery = sqlQueryBuilder.buildSqlQuery(aslQuery);
        System.out.println(sqlQuery);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                """
                SELECT o/data/events/data/items/value/magnitude
                FROM OBSERVATION o [openEHR-EHR-OBSERVATION.conformance_observation.v0]
                WHERE o/data[at0001]/events[at0002]/data[at0003]/items[at0008]/value = 82.0
                """
            })
    void canBuildSqlQuery(String aql) {

        AqlQuery aqlQuery = AqlQueryParser.parse(aql);
        AqlQueryWrapper queryWrapper = AqlQueryWrapper.create(aqlQuery);
        KnowledgeCacheService kcs = Mockito.mock(KnowledgeCacheService.class);
        Mockito.when(kcs.findUuidByTemplateId(ArgumentMatchers.anyString())).thenReturn(Optional.of(UUID.randomUUID()));

        AqlSqlLayer aqlSqlLayer = new AqlSqlLayer(kcs, () -> "node");
        AslRootQuery aslQuery = aqlSqlLayer.buildAslRootQuery(queryWrapper);
        AqlSqlQueryBuilder sqlQueryBuilder =
                new AqlSqlQueryBuilder(new DefaultDSLContext(SQLDialect.YUGABYTEDB), kcs, Optional.empty());

        Assertions.assertDoesNotThrow(() -> sqlQueryBuilder.buildSqlQuery(aslQuery));
    }
}
