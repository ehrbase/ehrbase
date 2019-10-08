/*
 * Copyright (c) 2019 Vitasystems GmbH and Hannover Medical School.
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

package org.ehrbase.aql.compiler;

import org.ehrbase.aql.containment.ContainmentSet;
import org.ehrbase.aql.containment.IdentifierMapper;
import org.ehrbase.aql.sql.binding.ContainBinder;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.List;

public class Contains {

    private ParseTree parseTree;
    private IdentifierMapper identifierMapper;

    private String containClause;

    //this is the list of nested sets from the CONTAINS expressions
    private List<ContainmentSet> nestedSets;

    private boolean useSimpleCompositionContainment = false;

    public Contains(ParseTree parseTree) {
        this.parseTree = parseTree;
    }

    public Contains process() {
        QueryCompilerPass1 queryCompilerPass1 = new QueryCompilerPass1();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(queryCompilerPass1, parseTree);

        this.identifierMapper = queryCompilerPass1.getIdentifierMapper();
        nestedSets = queryCompilerPass1.getClosedSetList();


        //bind the nested sets to SQL (it should be an configuration btw)
        ContainBinder containBinder = new ContainBinder(nestedSets);
        this.containClause = containBinder.bind();

        useSimpleCompositionContainment = containBinder.isUseSimpleCompositionContainment();
        return this;
    }


    //for tests purpose
    public List<ContainmentSet> getNestedSets() {
        return nestedSets;
    }

    public String getContainClause() {
        return containClause;
    }

    public boolean isUseSimpleCompositionContainment() {
        return useSimpleCompositionContainment;
    }

    public IdentifierMapper getIdentifierMapper() {
        return identifierMapper;
    }
}
