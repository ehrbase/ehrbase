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
package org.ehrbase.aql.compiler;

import java.util.Set;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.ehrbase.aql.containment.ContainPropositions;
import org.ehrbase.aql.containment.IdentifierMapper;
import org.ehrbase.service.KnowledgeCacheService;

/**
 * main entry point for CONTAINS clause resolution
 */
public class Contains {

    private ParseTree parseTree;
    private IdentifierMapper identifierMapper;

    // list of templates satisfying the CONTAINS expressions
    private Set<String> templates;

    private final KnowledgeCacheService knowledgeCache;

    private boolean requiresTemplateWhereClause = false;
    private boolean hasContains;

    public Contains(ParseTree parseTree, KnowledgeCacheService knowledgeCache) {
        this.parseTree = parseTree;
        this.knowledgeCache = knowledgeCache;
    }

    /**
     * resolve CONTAINS clause:
     * - identify the templates satisfying the clause
     * - set the respective path into variable definitions
     * @return
     */
    public Contains process() {
        QueryCompilerPass1 queryCompilerPass1 = new QueryCompilerPass1();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(queryCompilerPass1, parseTree);

        ContainPropositions containPropositions = queryCompilerPass1.containPropositions();
        containPropositions.evaluate(knowledgeCache);
        this.templates = containPropositions.resolvedTemplates();
        this.identifierMapper = queryCompilerPass1.getIdentifierMapper();
        this.requiresTemplateWhereClause = containPropositions.requiresTemplateWhereClause();
        this.hasContains = containPropositions.hasContains();

        return this;
    }

    // for tests purpose
    public Set<String> getTemplates() {
        return templates;
    }

    public IdentifierMapper getIdentifierMapper() {
        return identifierMapper;
    }

    public boolean requiresTemplateWhereClause() {
        return requiresTemplateWhereClause;
    }

    public boolean hasContains() {
        return hasContains;
    }
}
