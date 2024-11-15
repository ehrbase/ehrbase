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
package org.ehrbase.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.archetyped.Locatable;
import com.nedap.archie.rm.archetyped.TemplateId;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.support.identification.ArchetypeID;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.ehrbase.api.knowledge.KnowledgeCacheService;
import org.ehrbase.jooq.pg.tables.records.CompVersionHistoryRecord;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.test_data.composition.CompositionTestDataCanonicalJson;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;
import org.ehrbase.service.TimeProvider;
import org.jooq.CSVFormat;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CompositionRepositoryTest {

    private static final UUID EHR_ID = UUID.fromString("a6080b1b-da89-4992-b179-279a06ebe0e5");
    private static final UUID CONTRIBUTION_ID = UUID.fromString("6c8f92e4-7562-4962-9065-83c6d1e94dfb");
    private static final UUID AUDIT_ID = UUID.fromString("ce70b0d9-99ac-4ca2-a017-d69284dde509");

    @Nested
    class Conformance {

        private static final String CONFORMANCE_MAX_JSON = "conformance_ehrbase.de.v0_max";

        /**
         * Regression test to make sure that the db format does not change
         *
         * @throws IOException
         */
        @Test
        void testIpsDbFormat() throws IOException {
            Locatable c = getComposition(CompositionTestDataCanonicalJson.IPS);
            String expectedVersionCsv = loadExpectedCsv("ips", true);
            String expectedDataCsv = loadExpectedCsv("ips", false);
            var csv = toCsv(c);
            assertThat(csv.getLeft()).isEqualToIgnoringNewLines(expectedVersionCsv);
            assertThat(csv.getRight()).isEqualToIgnoringNewLines(expectedDataCsv);
        }

        /**
         * Regression test to make sure that the db format does not change
         *
         * @throws IOException
         */
        @Test
        void testConformanceMaxDbFormat() throws IOException {
            Composition c = loadLocatable(CONFORMANCE_MAX_JSON, Composition.class);
            String expectedVersionCsv = loadExpectedCsv(CONFORMANCE_MAX_JSON, true);
            String expectedDataCsv = loadExpectedCsv(CONFORMANCE_MAX_JSON, false);
            var csv = toCsv(c);
            assertThat(csv.getLeft()).isEqualToIgnoringNewLines(expectedVersionCsv);
            assertThat(csv.getRight()).isEqualToIgnoringNewLines(expectedDataCsv);
        }
    }

    @Nested
    class Update
            extends AbstractVersionedObjectRepositoryUpdateTest<
                    CompositionRepository, Composition, CompVersionHistoryRecord> {

        private static Composition Composition(ObjectVersionId versionId) {
            Composition composition = new Composition();
            composition.setUid(versionId);
            composition.setArchetypeDetails(
                    new Archetyped(new ArchetypeID(), mockTemplateId(), RmConstants.RM_VERSION_1_0_4));
            return composition;
        }

        private static TemplateId mockTemplateId() {
            TemplateId templateId = new TemplateId();
            templateId.setValue("dca5b9e5-610e-4af2-a823-e905b98a08bd");
            doReturn(Optional.of(UUID.fromString("dca5b9e5-610e-4af2-a823-e905b98a08bd")))
                    .when(mockKnowledgeCache)
                    .findUuidByTemplateId("dca5b9e5-610e-4af2-a823-e905b98a08bd");
            return templateId;
        }

        private static final KnowledgeCacheService mockKnowledgeCache = mock();

        public Update() {
            super(spy(new CompositionRepository(
                    mock(), mock(), () -> SYSTEM_ID, mockKnowledgeCache, OffsetDateTime::now)));
        }

        @Override
        void setUp() {
            super.setUp();
            Mockito.reset(mockKnowledgeCache);
        }

        @Override
        protected Composition versionedObject(ObjectVersionId objectVersionId) {
            return Composition(objectVersionId);
        }

        @Override
        protected String formatUpdateErrorNotExistMessage() {
            return "No COMPOSITION with given id: %s".formatted(VERSIONED_OBJECT_ID);
        }

        @Override
        protected CompVersionHistoryRecord versionRecord() {
            return new CompVersionHistoryRecord();
        }

        @Override
        protected void callUpdate(CompositionRepository repository, UUID ehrId, Composition versionedObject) {
            repository.update(ehrId, versionedObject, null, null);
        }

        @Test
        void updateErrorArchetypeDetailsNotDefined() {

            runUnknownOrMissingTemplateTest(comp -> comp.setArchetypeDetails(null));
        }

        @Test
        void updateErrorTemplateIdNotDefined() {

            runUnknownOrMissingTemplateTest(
                    comp -> Objects.requireNonNull(comp.getArchetypeDetails()).setTemplateId(null));
        }

        @Test
        void updateErrorTemplateIdValueNotDefined() {

            runUnknownOrMissingTemplateTest(
                    comp -> Objects.requireNonNull(comp.getArchetypeDetails().getTemplateId())
                            .setValue(null));
        }

        private void runUnknownOrMissingTemplateTest(Consumer<Composition> block) {
            Composition composition = versionedObject(objectVersionId(1));
            block.accept(composition);

            assertThatThrownBy(() -> repository.update(EHR_ID, composition, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Unknown or missing template in composition to be stored");
        }
    }

    private static Pair<String, String> toCsv(Locatable versionDataObject) {
        TimeProvider timeProvider = Mockito.mock(TimeProvider.class);
        OffsetDateTime now = OffsetDateTime.of(2022, 3, 21, 23, 45, 10, 123_456_780, ZoneOffset.ofHours(2));
        Mockito.when(timeProvider.getNow()).thenReturn(now);

        DefaultDSLContext context = new DefaultDSLContext(SQLDialect.POSTGRES);
        CompositionRepository repo = new CompositionRepository(context, null, null, null, timeProvider);

        VersionDataDbRecord versionData = repo.toRecords(EHR_ID, versionDataObject, CONTRIBUTION_ID, AUDIT_ID);

        String versionCsv = toCsv(context, List.of(versionData.versionRecord()));
        String dataCsv = toCsv(context, versionData.dataRecords().get().map(Pair::getValue).toList());
        return Pair.of(versionCsv, dataCsv);
    }

    private static <R extends org.jooq.Record> String toCsv(DefaultDSLContext context, List<R> dataRecords) {
        Result<R> result = context.newResult(DSL.table(dataRecords.get(0)));
        result.addAll(dataRecords);
        return result.formatCSV(
                CSVFormat.DEFAULT.nullString("").emptyString("''").quoteString("'"));
    }

    private static String loadExpectedCsv(String name, boolean version) throws IOException {
        try (InputStream in =
                CompositionRepositoryTest.class.getResourceAsStream(name + (version ? ".version" : ".data") + ".csv")) {
            return IOUtils.toString(in, StandardCharsets.UTF_8);
        }
    }

    private <L extends Locatable> L loadLocatable(String name, Class<L> type) throws IOException {
        try (InputStream in = CompositionRepositoryTest.class.getResourceAsStream(name + ".json")) {
            return toLocatable(in, type);
        }
    }

    private static Composition getComposition(CompositionTestDataCanonicalJson composition) throws IOException {
        try (InputStream stream = composition.getStream()) {
            return toLocatable(stream, Composition.class);
        }
    }

    private static <L extends Locatable> L toLocatable(InputStream stream, Class<L> type) throws IOException {
        return new CanonicalJson().unmarshal(IOUtils.toString(stream, StandardCharsets.UTF_8), type);
    }
}
