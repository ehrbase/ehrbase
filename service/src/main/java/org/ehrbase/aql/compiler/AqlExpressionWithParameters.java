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

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AqlExpressionWithParameters extends AqlExpression {

    public AqlExpressionWithParameters parse(String query, Map<String, Object> parameterValues) {
        String query1 = substitute(query, parameterValues);
        super.parse(query1);
        return this;
    }

    /**
     * Substitute parameters in aql expression
     * A parameter symbol starts with a '$' and a combination of alphanumeric with '-' or '_'
     * If the parameter value is a string or a UUID it is single quoted.
     * @param query
     * @param parameterValues
     * @return
     */
    public String substitute(String query, Map<String, Object> parameterValues) {

        StringBuffer stringBuffer = new StringBuffer();

        // match a string starting with '$' and followed by a number of alphanumeric or '-' or '_'
        Matcher matcher = Pattern.compile("\\$([\\w|\\-|_|]+)").matcher(query);

        while (matcher.find()) {
            String variable = matcher.group();
            if (parameterValues.get(variable.substring(1)) == null)
                throw new IllegalArgumentException(
                        "Could not substitute parameter in AQL expression: '" + variable + "'");
            Object parameterValue = parameterValues.get(variable.substring(1));

            if (isSingleQuotedArgument(parameterValue)) parameterValue = "'" + parameterValue + "'";

            matcher.appendReplacement(stringBuffer, String.valueOf(parameterValue));
        }

        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }

    private boolean isSingleQuotedArgument(Object parameterValue) {
        return parameterValue instanceof UUID || parameterValue instanceof String;
    }
}
