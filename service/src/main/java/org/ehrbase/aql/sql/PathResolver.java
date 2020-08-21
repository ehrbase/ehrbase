/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

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

package org.ehrbase.aql.sql;

import org.ehrbase.aql.containment.Containment;
import org.ehrbase.aql.containment.IdentifierMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehrbase.aql.containment.Templates;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.service.KnowledgeCacheService;

import static org.ehrbase.aql.sql.QueryProcessor.NIL_TEMPLATE;

/**
 * Resolve the path corresponding to a symbol in a given context
 * <p>
 * Path are resolved at runtime by performing a query on the CONTAINMENT table.
 * For example to resolve the path of contained archetype 'openEHR_EHR_OBSERVATION_laboratory_test_v0'
 * in composition 'openEHR_EHR_COMPOSITION_report_result_v1', the following query is executed
 * <pre><code>
 *      select "ehr"."containment"."path"
 *          from "ehr"."containment"
 *          where (
 *              "ehr"."containment"."comp_id" = 'b97e9fde-d994-4874-b671-8b1cd81b811c'
 *              and (label ~ 'openEHR_EHR_COMPOSITION_report_result_v1.*.openEHR_EHR_OBSERVATION_laboratory_test_v0')
 *          )
 *      </code></pre>
 * The found path is for example: <code>/content[openEHR-EHR-OBSERVATION.laboratory_test.v0 and name/value='Laboratory test']</code>
 * it is used then to build the actual path to a datavalue
 * </p>
 * Created by christian on 5/3/2016.
 */
public class PathResolver {
    private Logger logger = LogManager.getLogger(PathResolver.class);
//    private DSLContext context;

    private final IdentifierMapper mapper;
    private final KnowledgeCacheService knowledgeCache;
//    private Map<String, String> resolveMap = new HashMap<>();

    public PathResolver(KnowledgeCacheService knowledgeCache, IdentifierMapper mapper) {
        this.knowledgeCache = knowledgeCache;
        this.mapper = mapper;
    }

//    static String buildLquery(Containment containment) {
//        int depth = 0;
//        StringBuilder lquery = new StringBuilder();
//        //traverse up the containments and assemble the lquery expression
//
//        String archetypeId = containment.getArchetypeId();
//        if (archetypeId.isEmpty()) //use the class name
//            lquery.append(containment.getClassName() + "%");
//        else
//            lquery.append(ContainBinder.labelize(archetypeId));
//
////        Containment parent = containment.getEnclosingContainment();
//        Containment parent = null;
//
//        while (parent != null) {
//            depth++;
//            if (parent.getClassName().equals("COMPOSITION")) { //COMPOSITION is not part of the label
//                lquery.insert(0, ContainBinder.LEFT_WILDCARD);
//                break;
//            }
//            archetypeId = parent.getArchetypeId();
//            if (archetypeId == null || archetypeId.isEmpty()) //substitute by the class name
//                archetypeId = parent.getClassName() + "%";
//            else
//                archetypeId = ContainBinder.labelize(archetypeId);
//            lquery.insert(0, archetypeId + ContainBinder.INNER_WILDCARD);
////            parent = parent.getEnclosingContainment();
//        }
//
////        if (depth == 0)
////            lquery.append(RIGHT_WILDCARD);
//        return lquery.toString();
//    }

//    private String lqueryExpression(String identifier) {
//        Object containment = getMapper().getContainer(identifier);
//
//        if (!(containment instanceof Containment))
//            throw new IllegalArgumentException("No path found for identifier:" + identifier);
//
//        return buildLquery((Containment) containment);
//    }


    public String pathOf(String templateId, String identifier) {
        String result =  getMapper().getPath(templateId, identifier);
        if (result == null && getMapper().getClassName(identifier).equals("COMPOSITION")){
            //assemble a fake path for composition
            StringBuilder stringBuilder = new StringBuilder();
            Containment containment = (Containment)getMapper().getContainer(identifier);
            stringBuilder.append("/composition[");
            stringBuilder.append(containment.getArchetypeId());
            stringBuilder.append("]");
            result = stringBuilder.toString();
        }
        return result;
    }

    public String entryRoot(String templateId){
       Containment root = getMapper().getRootContainment();
       String result = null;
       if (!templateId.equals(NIL_TEMPLATE) && root != null) {
           StringBuilder stringBuilder = new StringBuilder();
           stringBuilder.append("/composition[");
           if (root.getArchetypeId().isEmpty()){
               //resolve the archetype node id according to the template
               stringBuilder.append(new Templates(knowledgeCache).rootArchetypeNodeId(templateId));
           }
           else
                stringBuilder.append(root.getArchetypeId());
           stringBuilder.append("]");
           result = stringBuilder.toString();
       }
       return result;
    }

//    /**
//     * resolve all the paths in the current containment mapper for a composition
//     *
//     * @param comp_id
//     */
//    public void resolvePaths(String templateId, UUID comp_id) {
//
//
//        for (String identifier : getMapper().identifiers()) {
//            try {
//                String lquery = lqueryExpression(identifier);
//
//                if (lquery.equals("COMPOSITION%")) //composition root, path is not used
//                    continue;
//
//                if (!resolveMap.containsKey(resolveMapKey(templateId, lquery))) {
//
//                    //query the DB to get the path
////                String labelWhere = "label ~ '"+lquery+"'";
//                    Result<?> records = context
//                            .select(CONTAINMENT.PATH, ENTRY.TEMPLATE_ID)
//                            .from(CONTAINMENT)
//                            .join(ENTRY)
//                            .on(ENTRY.COMPOSITION_ID.eq(comp_id))
//                            .where(CONTAINMENT.COMP_ID.eq(ENTRY.COMPOSITION_ID))
//                            .and(CONTAINMENT.LABEL + "~ '" + lquery + "'")
//                            .fetch().into(CONTAINMENT.PATH, ENTRY.TEMPLATE_ID);
//
//
//                    if (records.isEmpty()) {
//                        continue;
//                    }
//
//                    resolveMap.put(resolveMapKey((String) records.getValue(0, ENTRY.TEMPLATE_ID.getName()), lquery), records.getValue(0, CONTAINMENT.PATH));
//
//                    if (records.isEmpty()) {
//                        logger.debug("No path found for identifier (query return no records):" + identifier);
//                    }
//                    if (records.size() > 1) {
//                        logger.debug("Multiple paths found for identifier, returning first one:" + identifier);
//                    }
//
//                    String path = records.getValue(0, CONTAINMENT.PATH);
//                    getMapper().setPath(null, identifier, path);
//                    if (((Containment) getMapper().getContainer(identifier)).getClassName().equals("COMPOSITION")) {
//                        getMapper().setQueryStrategy(identifier, CompositionAttributeQuery.class);
//                    } else
//                        getMapper().setQueryStrategy(identifier, JsonbEntryQuery.class);
//                } else { //already cached
//                    String path = resolveMap.get(resolveMapKey(templateId, lquery));
//                    getMapper().setPath(null, identifier, path);
//                    if (((Containment) getMapper().getContainer(identifier)).getClassName().equals("COMPOSITION")) {
//                        getMapper().setQueryStrategy(identifier, CompositionAttributeQuery.class);
//                    } else
//                        getMapper().setQueryStrategy(identifier, JsonbEntryQuery.class);
//                }
//            } catch (IllegalArgumentException e) {
//                logger.debug("No path for:" + e);
//            }
//
//        }
//    }

    //ensure the same convention is used
    private String resolveMapKey(String templateId, String lquery) {
        return templateId + "::" + lquery;
    }


    public boolean hasPathExpression() {
        return getMapper().hasPathExpression();
    }

    public String rootOf(String identifier) {
        return getMapper().getArchetypeId(identifier);
    }

    public String classNameOf(String identifier) {
        return getMapper().getClassName(identifier);
    }

    public IdentifierMapper getMapper() {
        return mapper;
    }
}
