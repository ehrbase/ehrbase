/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School, Luis Marco-Ruiz (Hannover Medical School).

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

package org.ehrbase.dao.access.jooq;

import org.ehrbase.aql.compiler.AqlExpression;
import org.ehrbase.aql.compiler.AqlExpressionWithParameters;
import org.ehrbase.aql.compiler.Contains;
import org.ehrbase.aql.compiler.Statements;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.AqlResult;
import org.ehrbase.aql.sql.QueryProcessor;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.support.DataAccess;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by christian on 6/9/2016.
 */
public class AqlQueryHandler extends DataAccess {

    private boolean usePgExtensions;

    public AqlQueryHandler(I_DomainAccess domainAccess, boolean usePgExtensions) {
        super(domainAccess);
        this.usePgExtensions = usePgExtensions;

    }


    public AqlResult process(String query) {
        AqlExpression aqlExpression = new AqlExpression().parse(query);
        return execute(aqlExpression);
    }

    public AqlResult process(String query, Map<String, Object> parameters) {
        AqlExpression aqlExpression = new AqlExpressionWithParameters().parse(query, parameters);
        return execute(aqlExpression);
    }

    private AqlResult execute(AqlExpression aqlExpression){
        Contains contains = new Contains(aqlExpression.getParseTree()).process();
        Statements statements = new Statements(aqlExpression.getParseTree(), contains.getIdentifierMapper()).process() ;

        QueryProcessor queryProcessor = new QueryProcessor(getContext(), this.getKnowledgeManager(), this.getIntrospectService(), contains, statements, getDataAccess().getServerConfig().getNodename(), usePgExtensions);

        AqlResult aqlResult =  queryProcessor.execute();

        //add the variable from statements
        Map<String, String> variables = new HashMap();
        for (I_VariableDefinition variableDefinition: statements.getVariables()) {
            variables.put(variableDefinition.getAlias(), "/"+variableDefinition.getPath());
        }
        aqlResult.setVariables(variables);
        return aqlResult;
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }
}
