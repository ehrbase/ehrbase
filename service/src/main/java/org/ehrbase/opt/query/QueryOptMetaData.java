/*
 * Modifications copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Copyright (c) 2015 Christian Chevalley
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

package org.ehrbase.opt.query;

import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.containment.Containment;
import org.ehrbase.webtemplate.OPTParser;
import org.ehrbase.webtemplate.WebTemplate;
import org.ehrbase.webtemplate.WebTemplateNode;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by christian on 5/7/2018.
 */
public class QueryOptMetaData implements I_QueryOptMetaData {


    private WebTemplate webTemplate;


    private QueryOptMetaData(WebTemplate webTemplate) {

        this.webTemplate = webTemplate;

    }

    @Override
    public WebTemplate getWebTemplate() {
        return webTemplate;
    }

    private Set<Set<Containment>> findSets(WebTemplateNode tree) {
        Set<Set<Containment>> containments = new LinkedHashSet<>();
        final Containment currentContainment;
        if (buildContainment(tree.getNodeId()).isPresent()) {


            currentContainment = buildContainment(tree.getNodeId()).get();


            containments.add(new LinkedHashSet<>(Set.of(currentContainment)));

        } else {
            currentContainment = null;
        }

        for (WebTemplateNode child : tree.getChildren()) {
            Set<Set<Containment>> subSets = findSets(child);

            containments.addAll(subSets);
            if (currentContainment != null) {
                containments.addAll(subSets.stream()
                        .map(s -> {
                            Set<Containment> list = new LinkedHashSet<>(Set.of(currentContainment));
                            list.addAll(s);
                            return list;
                        })
                        .collect(Collectors.toSet()));
                }

            }


        return containments;
    }

    @Override
    public Set<Set<Containment>> getContainmentSet() {

        return findSets(webTemplate.getTree());
    }

    private Optional<Containment> buildContainment(String nodeId) {
        String className = StringUtils.substringBetween(nodeId, "openEHR-EHR-", ".");
        if (StringUtils.isNotBlank(className)) {
            return Optional.of(new Containment(className, "dummy", nodeId));
        } else {
            return Optional.empty();
        }
    }


    /**
     * prepare a document for querying
     *
     * @return
     */
    public static QueryOptMetaData initialize(OPERATIONALTEMPLATE operationaltemplate) throws Exception {

        return new QueryOptMetaData(new OPTParser(operationaltemplate).parse());
    }

    public static I_QueryOptMetaData getInstance(OPERATIONALTEMPLATE operationaltemplate) throws Exception {
        return initialize(operationaltemplate);
    }
















}
