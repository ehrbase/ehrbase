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
package org.ehrbase.dao.access.jooq;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.ehrbase.aql.compiler.*;
import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.AqlResult;
import org.ehrbase.aql.sql.QueryProcessor;
import org.ehrbase.dao.access.interfaces.I_DomainAccess;
import org.ehrbase.dao.access.support.DataAccess;
import org.ehrbase.openehr.sdk.validation.terminology.ExternalTerminologyValidation;
import org.ehrbase.service.KnowledgeCacheService;
import org.jooq.Record;
import org.jooq.Result;

/**
 * Created by christian on 6/9/2016.
 */
public class AqlQueryHandler extends DataAccess {

    private ExternalTerminologyValidation tsAdapter;
    private Map<String, Set<Object>> auditResultMap =
            new LinkedHashMap<>(); // we add a map of audit related data (f.e. ehr_id/value)

    public AqlQueryHandler(I_DomainAccess domainAccess, ExternalTerminologyValidation tsAdapter) {
        super(domainAccess);
        this.tsAdapter = tsAdapter;
    }

    public AqlResult process(String query) {
        AqlExpression aqlExpression = new AqlExpression().parse(query);
        return execute(aqlExpression);
    }

    public AqlResult process(String query, Map<String, Object> parameters) {
        AqlExpression aqlExpression = new AqlExpressionWithParameters().parse(query, parameters);
        return execute(aqlExpression);
    }

    @SuppressWarnings("unchecked")
    private AqlResult execute(AqlExpression aqlExpression) {

        AuditVariables auditVariables = new AuditVariables();

        Contains contains = new Contains(aqlExpression.getParseTree(), (KnowledgeCacheService)
                        this.getDataAccess().getIntrospectService())
                .process();

        Statements statements =
                new Statements(aqlExpression.getParseTree(), contains.getIdentifierMapper(), tsAdapter).process();

        QueryProcessor queryProcessor = new QueryProcessor(
                this,
                this.getIntrospectService(),
                contains,
                statements,
                getDataAccess().getServerConfig().getNodename());

        AqlResult aqlResult = queryProcessor.execute();

        // add the variable from statements
        Map<String, String> variables = new LinkedHashMap<>();

        Iterator<I_VariableDefinition> iterator = statements.getVariables().iterator();
        int serial = 0;
        while (iterator.hasNext()) {
            I_VariableDefinition variableDefinition = iterator.next();

            if (auditVariables.isAuditVariable(variableDefinition)) {
                // add the result to the list of audit variables
                auditVariables.addResults(
                        auditResultMap,
                        variableDefinition.getPath(),
                        resultSetForVariable(variableDefinition, aqlResult.getRecords()));
            }

            if (!variableDefinition.isHidden()) {
                String key = variableDefinition.getAlias() == null || variableDefinition.isVoidAlias()
                        ? "#" + serial++
                        : variableDefinition.getAlias();
                String value = StringUtils.isNotBlank(variableDefinition.getPath())
                        ? "/" + variableDefinition.getPath()
                        : variableDefinition.getIdentifier();
                variables.put(key, value);
            }
        }
        aqlResult.setVariables(variables);
        aqlResult.setAuditResultMap(auditResultMap);
        return aqlResult;
    }

    @Override
    public DataAccess getDataAccess() {
        return this;
    }

    public Set<Object> resultSetForVariable(I_VariableDefinition variableDefinition, Result<Record> recordResult) {
        var alias = Optional.of(variableDefinition).map(I_VariableDefinition::getAlias);

        // if the variable is a function parameter, ignore it (f.e. count())
        boolean fctAlias = alias.filter(a -> a.startsWith("_FCT")).isPresent();
        if (fctAlias) {
            return new LinkedHashSet<>();
        }

        String columnIdentifier = alias.orElseGet(() -> "/" + variableDefinition.getPath());

        return recordResult.stream()
                .map(r -> r.get(columnIdentifier))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Map<String, Set<Object>> getAuditResultMap() {
        return auditResultMap;
    }
}
