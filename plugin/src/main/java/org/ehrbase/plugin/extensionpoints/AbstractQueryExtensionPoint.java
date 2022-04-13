/*
 * Copyright (c) 2022. vitasystems GmbH and Hannover Medical School.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.ehrbase.plugin.extensionpoints;

import java.util.function.Function;
import org.ehrbase.plugin.dto.QueryWithParameters;
import org.ehrbase.response.ehrscape.QueryResultDto;

/**
 * Provides After and Before Interceptors for {@link QueryExtensionPointInterface}
 *
 * @author Stefan Spiska
 */
public abstract class AbstractQueryExtensionPoint implements QueryExtensionPointInterface {

  /**
   * Called before Query execution
   *
   * @param input Aql String to be executed
   * @return Aql String to be given to Query execute
   */
  public QueryWithParameters beforeQueryExecution(QueryWithParameters input) {
    return input;
  }

  /**
   * Called after Query execution
   *
   * @param output result of the Query execution {@link QueryResultDto}
   * @return result of the Query execution {@link QueryResultDto}
   */
  public QueryResultDto afterQueryExecution(QueryResultDto output) {
    return output;
  }

  @Override
  public QueryResultDto aroundQueryExecution(
      QueryWithParameters input, Function<QueryWithParameters, QueryResultDto> chain) {
    return afterQueryExecution(chain.apply(beforeQueryExecution(input)));
  }
}
