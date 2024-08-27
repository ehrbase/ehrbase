/*
 * Copyright (c) 2019-2024 vitasystems GmbH.
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

import static org.assertj.core.api.Assertions.assertThat;

import com.nedap.archie.rm.composition.Composition;
import com.nedap.archie.rm.datavalues.DvText;
import com.nedap.archie.rm.ehr.EhrStatus;
import com.nedap.archie.rm.generic.PartySelf;
import com.nedap.archie.rm.support.identification.ObjectVersionId;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.xml.bind.JAXBException;
import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlException;
import org.ehrbase.api.service.TemplateService;
import org.ehrbase.openehr.sdk.response.dto.ehrscape.TemplateMetaDataDto;
import org.ehrbase.openehr.sdk.serialisation.jsonencoding.CanonicalJson;
import org.ehrbase.openehr.sdk.test_data.operationaltemplate.OperationalTemplateTestData;
import org.ehrbase.repository.CompositionRepository;
import org.ehrbase.repository.EhrRepository;
import org.ehrbase.test.ServiceIntegrationTest;
import org.ehrbase.util.UuidGenerator;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;
import org.openehr.schemas.v1.TemplateDocument;
import org.springframework.beans.factory.annotation.Autowired;

@ServiceIntegrationTest
class TemplateServiceIT {
    @Autowired
    TemplateService templateService;

    @Autowired
    EhrRepository ehrRepository;

    @Autowired
    CompositionRepository compositionRepository;

    @BeforeEach
    void setUp() {

        createEhr();
    }

    private UUID createEhr() {
        final UUID ehrId = UuidGenerator.randomUUID();
        EhrStatus status = new EhrStatus();
        status.setUid(new ObjectVersionId(UuidGenerator.randomUUID().toString(), "integration-test", "1"));
        status.setArchetypeNodeId("openEHR-EHR-EHR_STATUS.generic.v1");
        status.setName(new DvText("EHR Status"));
        status.setSubject(new PartySelf(null));
        status.setModifiable(true);
        status.setQueryable(true);

        ehrRepository.commit(ehrId, status, null, null);
        return ehrId;
    }

    @Test
    void adminDeleteAllTemplates() throws IOException, JAXBException {

        Stream.of(OperationalTemplateTestData.CONFORMANCE, OperationalTemplateTestData.MINIMAL_ACTION)
                .map(this::getOperationalTemplate)
                .forEach(templateService::create);

        UUID ehrId = createEhr();
        UUID compId =
                storeComposition(ehrId, getComposition("org/ehrbase/repository/conformance_ehrbase.de.v0_max.json"));

        int deleted = templateService.adminDeleteAllTemplates();

        assertThat(deleted).isEqualTo(1);
        List<TemplateMetaDataDto> remainingTemplates = templateService.getAllTemplates();
        assertThat(remainingTemplates).hasSize(1);
        assertThat(remainingTemplates.getFirst().getTemplateId())
                .isEqualTo(OperationalTemplateTestData.CONFORMANCE.getTemplateId());

        compositionRepository.adminDelete(compId);

        assertThat(deleted).isEqualTo(1);
        assertThat(remainingTemplates).isEmpty();
    }

    private @NotNull UUID storeComposition(UUID ehrId, Composition composition) {
        UUID compId = UUID.randomUUID();
        ObjectVersionId uid = new ObjectVersionId(compId.toString(), "nirvana", Integer.toString(1));
        composition.setUid(uid);
        compositionRepository.commit(ehrId, composition, null, null);
        return compId;
    }

    private Composition getComposition(String path) {

        String cStr;
        try {
            cStr = IOUtils.resourceToString(
                    path, StandardCharsets.UTF_8, getClass().getClassLoader());
        } catch (IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
        return new CanonicalJson().unmarshal(cStr, Composition.class);
    }

    private OPERATIONALTEMPLATE getOperationalTemplate(OperationalTemplateTestData template) {
        try {
            return TemplateDocument.Factory.parse(template.getStream()).getTemplate();
        } catch (XmlException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private OPERATIONALTEMPLATE getOperationalTemplate(String name) {
        try {
            return TemplateDocument.Factory.parse(
                            new FileInputStream("./src/test/resources/operational_templates/" + name))
                    .getTemplate();
        } catch (XmlException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
