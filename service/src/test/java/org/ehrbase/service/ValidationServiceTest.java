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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ehrbase.test.fixtures.EhrStatusDtoFixture.ehrStatusDto;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.nedap.archie.rm.RMObject;
import com.nedap.archie.rm.archetyped.Archetyped;
import com.nedap.archie.rm.archetyped.FeederAudit;
import com.nedap.archie.rm.archetyped.TemplateId;
import com.nedap.archie.rm.changecontrol.OriginalVersion;
import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datastructures.ItemList;
import com.nedap.archie.rm.datatypes.CodePhrase;
import com.nedap.archie.rm.datavalues.DvCodedText;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.generic.AuditDetails;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.ArchetypeID;
import com.nedap.archie.rm.support.identification.GenericId;
import com.nedap.archie.rm.support.identification.HierObjectId;
import com.nedap.archie.rm.support.identification.ObjectRef;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import com.nedap.archie.rm.support.identification.PartyRef;
import com.nedap.archie.rm.support.identification.TerminologyId;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.ehrbase.api.dto.EhrStatusDto;
import org.ehrbase.api.exception.ValidationException;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.openehr.sdk.response.dto.ContributionCreateDto;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.test_data.composition.CompositionTestDataCanonicalJson;
import org.ehrbase.openehr.sdk.test_data.contribution.ContributionTestDataCanonicalJson;
import org.ehrbase.openehr.sdk.test_data.ehr.EhrTestDataCanonicalJson;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.ehrbase.openehr.sdk.util.functional.Try;
import org.ehrbase.openehr.sdk.validation.ConstraintViolation;
import org.ehrbase.openehr.sdk.validation.ConstraintViolationException;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.openehr.sdk.validation.terminology.TerminologyParam;
import org.ehrbase.openehr.sdk.webtemplate.model.WebTemplate;
import org.ehrbase.openehr.sdk.webtemplate.parser.OPTParser;
import org.ehrbase.service.contribution.ContributionServiceHelperTest;
import org.ehrbase.service.validation.ValidationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;
import org.springframework.beans.factory.ObjectProvider;

class ValidationServiceTest {

    private final KnowledgeCacheServiceImp knowledgeCacheService = mock();

    private final ValidationProperties serverConfig = new ValidationProperties(true, true);

    private final ObjectProvider<ExternalTerminologyValidation> objectProvider = mock();

    private class NopTerminologyValidation implements ExternalTerminologyValidation {

        private final ConstraintViolation err = new ConstraintViolation("Terminology validation is disabled");

        public Try<Boolean, ConstraintViolationException> validate(TerminologyParam param) {
            return Try.failure(new ConstraintViolationException(List.of(err)));
        }

        public boolean supports(TerminologyParam param) {
            return false;
        }

        public List<DvCodedText> expand(TerminologyParam param) {
            return Collections.emptyList();
        }
    }

    private final ValidationService spyService = spy(new ValidationServiceImp(
            knowledgeCacheService, new TerminologyServiceImp(), serverConfig, objectProvider, false));

    @BeforeEach
    void setUp() {
        Mockito.reset(knowledgeCacheService, objectProvider, spyService);
        doReturn(new NopTerminologyValidation()).when(objectProvider).getIfAvailable();
    }

    private ValidationService service() {
        return spyService;
    }

    private static AuditDetails validAuditDetails() {
        AuditDetails audit = new AuditDetails();

        audit.setSystemId("local");
        audit.setChangeType(new DvCodedText("creation", new CodePhrase(new TerminologyId("openehr"), "249")));
        audit.setDescription(new DvText("lorem ipsum"));
        audit.setCommitter(new PartySelf(new PartyRef(new GenericId("123-abc", "test"), "de.vitagroup", "PERSON")));

        return audit;
    }

    private List<OriginalVersion<? extends RMObject>> testVersions() {

        OriginalVersion<RMObject> version = new OriginalVersion<>();
        version.setLifecycleState(new DvCodedText("complete", new CodePhrase(new TerminologyId("openehr"), "532")));
        version.setCommitAudit(validAuditDetails());
        return List.of(version);
    }

    @Test
    void checkCompositionInvalidWithoutName() {

        assertThatThrownBy(() -> runCheckComposition(composition -> composition.setName(null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Composition missing mandatory attribute: name");
    }

    @Test
    void checkCompositionInvalidWithoutArchetypeNodeId() {

        assertThatThrownBy(() -> runCheckComposition(composition -> composition.setArchetypeNodeId(null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Composition missing mandatory attribute: archetype_node_id");
    }

    @Test
    void checkCompositionInvalidWithoutLanguage() {

        assertThatThrownBy(() -> runCheckComposition(composition -> composition.setLanguage((CodePhrase) null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Composition missing mandatory attribute: language");
    }

    @Test
    void checkCompositionInvalidWithoutCategory() {

        assertThatThrownBy(() -> runCheckComposition(composition -> composition.setCategory(null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Composition missing mandatory attribute: category");
    }

    @Test
    void checkCompositionInvalidWithoutComposer() {

        assertThatThrownBy(() -> runCheckComposition(composition -> composition.setComposer(null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Composition missing mandatory attribute: composer");
    }

    @Test
    void checkCompositionInvalidWithoutArchetype() {

        assertThatThrownBy(() -> runCheckComposition(composition -> composition.setArchetypeDetails(null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Composition missing mandatory attribute: archetype details");
    }

    @Test
    void checkCompositionInvalidWithoutArchetypeTemplateId() {

        assertThatThrownBy(() -> runCheckComposition(composition -> composition.setArchetypeDetails(new Archetyped())))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Composition missing mandatory attribute: archetype details/template_id");
    }

    @Test
    void checkCompositionInvalidConstrains() {

        assertThatThrownBy(() -> runCheckComposition(composition -> {}))
                .isInstanceOf(ConstraintViolationException.class)
                .extracting(t -> ((ConstraintViolationException) t).getConstraintViolations())
                .asList()
                .extracting(Object::toString)
                .containsExactlyInAnyOrder(
                        "ConstraintViolation{aqlPath='/', message='Invariant Language_valid failed on type COMPOSITION'}",
                        "ConstraintViolation{aqlPath='/', message='Invariant Category_validity failed on type COMPOSITION'}",
                        "ConstraintViolation{aqlPath='/archetype_details/template_id/value', message='Attribute value of class TEMPLATE_ID does not match existence 1..1'}",
                        "ConstraintViolation{aqlPath='/territory', message='Attribute territory of class COMPOSITION does not match existence 1..1'}");
    }

    private void runCheckComposition(Consumer<Composition> consumer) {

        Composition composition = new Composition();
        composition.setName(new DvText("Name"));
        composition.setArchetypeNodeId("archetype-node-id");
        composition.setLanguage("DE-de");
        composition.setCategory(new DvCodedText("cat", "42"));
        composition.setComposer(new PartySelf());
        composition.setArchetypeDetails(
                new Archetyped(new ArchetypeID("openEHR-EHR-COMPOSITION.test.v1"), new TemplateId(), "1.0.3"));
        consumer.accept(composition);
        service().check(composition);
    }

    @ParameterizedTest
    @EnumSource(
            value = CompositionTestDataCanonicalJson.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {
                "ALL_TYPES",
                "NESTED_EN_V1",
                "CORONA",
                "VIROLOGY_FINDING_WITH_SPECIMEN_NO_UPDATE",
                "CHOICE_ELEMENT",
                "CHOICE_DV_QUANTITY",
                "MINIMAL_ACTION_2",
                "MINIMAL_WITH_OPTIONAL_ATTRIBUTE",
                "MINIMAL_WITHOUT_OPTIONAL_ATTRIBUTE",
                "PARTICIPATION_NO_CONTENT",
                "IPS",
                "OTHER_PARTICIPATIONS",
            })
    void checkCompositionValidFixtures(CompositionTestDataCanonicalJson compositionData) {

        Composition composition = loadComposition(compositionData);
        composition.setUid(new ObjectVersionId("85379aa8-a16a-4d5b-97ad-242880066803", "test-system", "42"));
        String templateID = Objects.requireNonNull(
                        composition.getArchetypeDetails().getTemplateId())
                .getValue();
        OperationalTemplateTestData templateData = OperationalTemplateTestData.findByTemplateId(templateID);
        WebTemplate webTemplate = loadWebTemplate(templateData);

        doReturn(webTemplate).when(knowledgeCacheService).getQueryOptMetaData(templateID);
        service().check(composition);
    }

    @ParameterizedTest
    @EnumSource(
            value = CompositionTestDataCanonicalJson.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {
                "NESTED",
                "SUBJECT_PARTY_IDENTIFIED",
                "SUBJECT_PARTY_SELF",
                "SUBJECT_PARTY_RELATED",
                "NESTED_PROVIDER",
                "DATE_TIME_TESTS",
                "DURATION_TESTS",
                "VIROLOGY_FINDING_WITH_SPECIMEN",
                "MULTI_OCCURRENCE",
                "ALTERNATIVE_EVENTS",
                "FEEDER_AUDIT_DETAILS",
                "MINIMAL_INSTRUCTION",
                "IPS_INVALID",
                "SECTION_CARDINALITY"
            })
    void checkCompositionInvalidFixtures(CompositionTestDataCanonicalJson compositionData) {

        Composition composition = loadComposition(compositionData);
        composition.setUid(new ObjectVersionId("85379aa8-a16a-4d5b-97ad-242880066803", "test-system", "42"));
        String templateID = Objects.requireNonNull(
                        composition.getArchetypeDetails().getTemplateId())
                .getValue();
        OperationalTemplateTestData templateData = OperationalTemplateTestData.findByTemplateId(templateID);
        WebTemplate webTemplate = loadWebTemplate(templateData);

        doReturn(webTemplate).when(knowledgeCacheService).getQueryOptMetaData(templateID);
        ValidationService service = service();

        assertThatThrownBy(() -> service.check(composition)).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void checkEhrStatusInvalidSubjectMissing() {

        EhrStatusDto ehrStatusDto = ehrStatusDto((PartySelf) null);
        assertThatThrownBy(() -> runCheckEhrStatus(ehrStatusDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        "Message at /subject (/subject):  Attribute subject of class EHR_STATUS does not match existence 1..1");
    }

    @Test
    void checkEhrStatusInvalidIsQueryableMissing() {

        EhrStatusDto ehrStatusDto = ehrStatusDto(null, false);
        assertThatThrownBy(() -> runCheckEhrStatus(ehrStatusDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        "Message at /is_queryable (/is_queryable):  Attribute is_queryable of class EHR_STATUS does not match existence 1..1");
    }

    @Test
    void checkEhrStatusInvalidIsModifiableMissing() {

        EhrStatusDto ehrStatusDto = ehrStatusDto(false, null);
        assertThatThrownBy(() -> runCheckEhrStatus(ehrStatusDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        "Message at /is_modifiable (/is_modifiable):  Attribute is_modifiable of class EHR_STATUS does not match existence 1..1");
    }

    @Test
    void checkEhrStatusInvalidUID() {

        EhrStatusDto ehrStatusDto = ehrStatusDto(new HierObjectId());
        assertThatThrownBy(() -> runCheckEhrStatus(ehrStatusDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        "Message at /value (/uid/value):  Attribute value of class HIER_OBJECT_ID does not match existence 1..1");
    }

    @Test
    void checkEhrStatusInvalidName() {

        EhrStatusDto ehrStatusDto = ehrStatusDto(new DvText());
        assertThatThrownBy(() -> runCheckEhrStatus(ehrStatusDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        "Message at /value (/name/value):  Attribute value of class DV_TEXT does not match existence 1..1");
    }

    @Test
    void checkEhrStatusInvalidSubjectPartyRef() {

        EhrStatusDto ehrStatusDto = ehrStatusDto(new PartySelf(new PartyRef()));
        assertThatThrownBy(() -> runCheckEhrStatus(ehrStatusDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        """
                    Message at /namespace (/subject/external_ref/namespace):  Attribute namespace of class PARTY_REF does not match existence 1..1
                    Message at /id (/subject/external_ref/id):  Attribute id of class PARTY_REF does not match existence 1..1
                    Message at /type (/subject/external_ref/type):  Attribute type of class PARTY_REF does not match existence 1..1""");
    }

    @Test
    void checkEhrStatusInvalidSubjectPartyRefNaespace() {

        EhrStatusDto ehrStatusDto =
                ehrStatusDto(new PartySelf(new PartyRef(new HierObjectId("ext::42"), "not-[]-allowed", "PARTY")));
        assertThatThrownBy(() -> runCheckEhrStatus(ehrStatusDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        """
                        Message at /subject/external_ref (/subject/external_ref):  Invariant Namespace_valid failed on type PARTY_REF
                        Message at /subject/external_ref/namespace (/subject/external_ref/namespace):  Invariant namespace of class EHR_STATUS does not match pattern [[a-zA-Z][a-zA-Z0-9-_:/&+?]*]""");
    }

    @Test
    void checkEhrStatusInvalidArchetypeDetails() {

        EhrStatusDto ehrStatusDto = ehrStatusDto(new Archetyped());
        assertThatThrownBy(() -> runCheckEhrStatus(ehrStatusDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        """
                    Message at /rm_version (/archetype_details/rm_version):  Attribute rm_version of class ARCHETYPED does not match existence 1..1
                    Message at /archetype_id (/archetype_details/archetype_id):  Attribute archetype_id of class ARCHETYPED does not match existence 1..1""");
    }

    @Test
    void checkEhrStatusInvalidFeederAudit() {

        EhrStatusDto ehrStatusDto = ehrStatusDto(new FeederAudit());
        assertThatThrownBy(() -> runCheckEhrStatus(ehrStatusDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        """
                    Message at /originating_system_audit (/feeder_audit/originating_system_audit):  Attribute originating_system_audit of class FEEDER_AUDIT does not match existence 1..1""");
    }

    @Test
    void checkEhrStatusInvalidOtherDetails() {

        EhrStatusDto ehrStatusDto = ehrStatusDto(new ItemList());
        assertThatThrownBy(() -> runCheckEhrStatus(ehrStatusDto))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        """
                    Message at /archetype_node_id (/other_details/archetype_node_id):  Attribute archetype_node_id of class ITEM_LIST does not match existence 1..1
                    Message at /name (/other_details/name):  Attribute name of class ITEM_LIST does not match existence 1..1""");
    }

    @ParameterizedTest
    @EnumSource(value = EhrTestDataCanonicalJson.class)
    void checkEhrStatusValidFixtures(EhrTestDataCanonicalJson ehrData) {

        EhrStatusDto ehrStatus = loadEhrStatus(ehrData);
        service().check(ehrStatus);
    }

    private void runCheckEhrStatus(EhrStatusDto ehrStatusDto) {
        service().check(ehrStatusDto);
    }

    @Test
    void contributionInvalidMissingVersions() {

        assertThatThrownBy(() -> runCheckContribution(contribution -> contribution.setVersions(null)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Message at /versions (/versions):  Versions must not be empty");
        assertThatThrownBy(() -> runCheckContribution(contribution -> contribution.setVersions(List.of())))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Message at /versions (/versions):  Versions must not be empty");
    }

    @Test
    void contributionInvalidWithVersionContainsContribution() {

        OriginalVersion<RMObject> version = new OriginalVersion<>();
        version.setLifecycleState(new DvCodedText("complete", new CodePhrase(new TerminologyId("openehr"), "532")));
        version.setCommitAudit(validAuditDetails());
        version.setContribution(new ObjectRef<>(new HierObjectId("test"), "namespace", "type"));
        List<OriginalVersion<? extends RMObject>> versions = List.of(version);

        assertThatThrownBy(() -> runCheckContribution(contribution -> contribution.setVersions(versions)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        "Message at /version/contribution (/version/contribution):  Attribute contribution must not be set");
    }

    @Test
    void contributionInvalidWithMissingCommitter() {

        AuditDetails auditDetails = validAuditDetails();
        auditDetails.setCommitter(null);

        assertThatThrownBy(() -> runCheckContribution(contribution -> contribution.setAudit(auditDetails)))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        "Message at /committer (/committer):  Attribute committer of class AUDIT_DETAILS does not match existence 1..1");
    }

    @Test
    void contributionValid() {

        runCheckContribution(contribution -> {});
    }

    @ParameterizedTest
    @EnumSource(
            value = ContributionTestDataCanonicalJson.class,
            mode = EnumSource.Mode.INCLUDE,
            names = {"ONE_ENTRY_COMPOSITION", "TWO_ENTRIES_COMPOSITION", "STATUS_COMPOITION_MODIFICATION"})
    void contributionValidFixtures(ContributionTestDataCanonicalJson type) {
        service().check(ContributionServiceHelperTest.loadContribution(type));
    }

    @ParameterizedTest
    @EnumSource(
            value = ContributionTestDataCanonicalJson.class,
            mode = EnumSource.Mode.EXCLUDE,
            names = {"ONE_ENTRY_COMPOSITION", "TWO_ENTRIES_COMPOSITION", "STATUS_COMPOITION_MODIFICATION"})
    void contributionInvalidFixtures(ContributionTestDataCanonicalJson type) {

        ContributionCreateDto contribution = ContributionServiceHelperTest.loadContribution(type);
        ValidationService service = service();

        assertThatThrownBy(() -> service.check(contribution))
                .isInstanceOf(ValidationException.class)
                .hasMessage(
                        "Message at /version/contribution (/version/contribution):  Attribute contribution must not be set");
    }

    private void runCheckContribution(Consumer<ContributionCreateDto> consumer) {

        ContributionCreateDto contribution = new ContributionCreateDto();
        contribution.setUid(new HierObjectId());
        contribution.setVersions(testVersions());
        contribution.setAudit(validAuditDetails());
        consumer.accept(contribution);
        service().check(contribution);
    }

    // --- HELPER ---

    private static Composition loadComposition(CompositionTestDataCanonicalJson data) {
        try (var in = data.getStream()) {
            return CanonicalJson.MARSHAL_OM.readValue(in, Composition.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static EhrStatusDto loadEhrStatus(EhrTestDataCanonicalJson data) {
        try (var in = data.getStream()) {
            return CanonicalJson.MARSHAL_OM.readValue(in, EhrStatusDto.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<OperationalTemplateTestData, WebTemplate> webTemplates = new HashMap<>();

    private static WebTemplate loadWebTemplate(OperationalTemplateTestData data) {
        return webTemplates.computeIfAbsent(data, d -> {
            try (var in = d.getStream()) {
                TemplateDocument document = TemplateDocument.Factory.parse(in);
                OPERATIONALTEMPLATE template = document.getTemplate();
                return new OPTParser(template).parse();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
