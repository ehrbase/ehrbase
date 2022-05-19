/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.aql.sql;

import static org.ehrbase.aql.sql.QueryProcessor.NIL_TEMPLATE;

import java.util.Set;
import java.util.TreeSet;
import org.ehrbase.aql.containment.Containment;
import org.ehrbase.aql.containment.IdentifierMapper;
import org.ehrbase.aql.containment.Templates;
import org.ehrbase.service.KnowledgeCacheService;

/**
 * Resolve the path corresponding to a symbol in a given context
 * NB. Path are resolved using WebTemplates
 * Created by christian on 5/3/2016.
 */
public class PathResolver {

    private final IdentifierMapper mapper;
    private final KnowledgeCacheService knowledgeCache;

    public PathResolver(KnowledgeCacheService knowledgeCache, IdentifierMapper mapper) {
        this.knowledgeCache = knowledgeCache;
        this.mapper = mapper;
    }

    public Set<String> pathOf(String templateId, String identifier) {
        Set<String> result;

        if (!getMapper().hasPathExpression()
                && getMapper().getClassName(identifier).equals("COMPOSITION")) {
            // assemble a fake path for composition
            StringBuilder stringBuilder = new StringBuilder();
            Containment containment = (Containment) getMapper().getContainer(identifier);
            stringBuilder.append("/composition[");
            stringBuilder.append(containment.getArchetypeId());
            stringBuilder.append("]");
            result = new TreeSet<>();
            result.add(stringBuilder.toString());
        } else result = getMapper().getPath(templateId, identifier);

        return result;
    }

    public String entryRoot(String templateId) {
        Containment root = getMapper().getRootContainment();
        String result = null;
        if (!templateId.equals(NIL_TEMPLATE) && root != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/composition[");
            if (root.getArchetypeId().isEmpty()) {
                // resolve the archetype node id according to the template
                stringBuilder.append(new Templates(knowledgeCache).rootArchetypeNodeId(templateId));
            } else stringBuilder.append(root.getArchetypeId());
            stringBuilder.append("]");
            result = stringBuilder.toString();
        }
        return result;
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
