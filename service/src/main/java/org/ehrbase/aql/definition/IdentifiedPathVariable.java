/*
* Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

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

package org.ehrbase.aql.definition;

import org.ehrbase.aql.parser.AqlParser;

/** Created by christian on 9/22/2017. */
public class IdentifiedPathVariable {

  private AqlParser.IdentifiedPathContext identifiedPathContext;
  private AqlParser.SelectExprContext selectExprContext;
  private boolean isDistinct;

  public IdentifiedPathVariable(
      AqlParser.IdentifiedPathContext identifiedPathContext,
      AqlParser.SelectExprContext selectExprContext,
      boolean isDistinct) {
    this.identifiedPathContext = identifiedPathContext;
    this.selectExprContext = selectExprContext;
    this.isDistinct = isDistinct;
  }

  public VariableDefinition definition() {
    String identifier = identifiedPathContext.IDENTIFIER().getText();
    String path = null;
    if (identifiedPathContext.objectPath() != null && !identifiedPathContext.objectPath().isEmpty())
      path = identifiedPathContext.objectPath().getText();
    String alias = null;
    // get an alias if any
    if (selectExprContext.AS() != null) {
      alias = selectExprContext.IDENTIFIER().getText();
    }

    VariableDefinition variableDefinition =
        new VariableDefinition(path, alias, identifier, isDistinct);
    return variableDefinition;
  }
}
