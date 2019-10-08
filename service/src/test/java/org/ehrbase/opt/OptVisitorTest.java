/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School,
 * Jake Smolka (Hannover Medical School).

 * This file is part of Project EHRbase

 * Copyright (c) Ripple Foundation CIC Ltd, UK, 2017
 * Author: Christian Chevalley
 * This file is part of Project Ethercis
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

package org.ehrbase.opt;

import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.KnowledgeCacheService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Ignore;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Created by christian on 1/29/2018.
 */
@Ignore
public class OptVisitorTest extends TestCase {

    I_KnowledgeCache knowledge;

    @Before
    public void setUp() throws Exception {
        Properties props = new Properties();
        props.put("knowledge.path.archetype", "src/test/resources/knowledge");
        props.put("knowledge.path.template", "src/test/resources/knowledge");
        props.put("knowledge.path.opt", "src/test/resources/knowledge");
        props.put("knowledge.cachelocatable", "true");
        props.put("knowledge.forcecache", "true");
        knowledge = new KnowledgeCacheService(props);

        Pattern include = Pattern.compile(".*");

        knowledge.retrieveFileMap(include, null);
    }


    public void testTraverseAny() throws Exception {

        String expectedJson = new String(Files.readAllBytes(Paths.get("src/test/resources/RIPPLE-Conformance Test Introspected.json")));

//        OPERATIONALTEMPLATE operationaltemplate = (OPERATIONALTEMPLATE)knowledge.retrieveTemplate("IDCR Allergies List.v0");
//        OPERATIONALTEMPLATE operationaltemplate = (OPERATIONALTEMPLATE)knowledge.retrieveTemplate("IDCR Problem List.v1");
        Optional<OPERATIONALTEMPLATE> operationaltemplate = knowledge.retrieveOperationalTemplate("RIPPLE - Conformance Test template");
        Map map = new OptVisitor().traverse(operationaltemplate.orElseThrow(Exception::new));

        assertNotNull(map);

        assertEquals(expectedJson.replaceAll("\\n", "").replaceAll("\\r", ""), toJson(map).replaceAll("\\n", "").replaceAll("\\r", ""));
    }

    public void testTraverseWithDvParsable() throws Exception {

//        String expectedJson = new String(Files.readAllBytes(Paths.get("src/test/resources/RIPPLE-Conformance Test Introspected.json")));

//        OPERATIONALTEMPLATE operationaltemplate = (OPERATIONALTEMPLATE)knowledge.retrieveTemplate("IDCR Allergies List.v0");
//        OPERATIONALTEMPLATE operationaltemplate = (OPERATIONALTEMPLATE)knowledge.retrieveTemplate("IDCR Problem List.v1");
        Optional<OPERATIONALTEMPLATE> operationaltemplate = knowledge.retrieveOperationalTemplate("IDCR - Transfer of Care Summary TEST.v1");
        Map map = new OptVisitor().traverse(operationaltemplate.orElseThrow(Exception::new));

        assertNotNull(map);

//        assertEquals(expectedJson.replaceAll("\\n", "").replaceAll("\\r", ""), toJson(map).replaceAll("\\n", "").replaceAll("\\r", ""));
    }

    String toJson(Map<String, Object> map) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.setPrettyPrinting().create();
        return gson.toJson(map);
    }
}