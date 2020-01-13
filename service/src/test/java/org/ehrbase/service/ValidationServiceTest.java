/*
 * Copyright (c) 2019 Vitasystems GmbH and Christian Chevalley (Hannover Medical School).
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ehrbase.service;

import com.nedap.archie.rm.composition.Composition;
import org.ehrbase.api.service.ValidationService;
import org.ehrbase.serialisation.CanonicalXML;
import org.apache.commons.io.IOUtils;
import org.ehrbase.test_data.operationaltemplate.OperationalTemplateTestData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.fail;

public class ValidationServiceTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public CacheRule cacheRule = new CacheRule();

    @Test
    public void testCheckComposition(){
        try {
            KnowledgeCacheService knowledgeCacheService = KnowledgeCacheServiceTest.buildKnowledgeCache(testFolder, cacheRule);
            knowledgeCacheService.addOperationalTemplate(IOUtils.toByteArray(OperationalTemplateTestData.RIPPLE_CONFORMANCE_TEST.getStream()));
            TerminologyServiceImp terminologyService = new TerminologyServiceImp();
            ValidationService validationService = new ValidationServiceImp(cacheRule.cacheManager, knowledgeCacheService, terminologyService);

            //set composition
            Composition composition = new CanonicalXML().unmarshal(IOUtils.toString(Files.newInputStream(Paths.get("./src/test/resources/samples/RIPPLE-ConformanceTest.xml")), UTF_8),Composition.class);

            //check validation using templateId
            validationService.check("RIPPLE - Conformance Test template", composition);

            //check validation using template UUID
            validationService.check(UUID.fromString("27441ebc-6d0e-4c12-a681-bbdd3c80fbe6"), composition);

        } catch (Throwable e){
            fail(e.getMessage());
        }

    }

}