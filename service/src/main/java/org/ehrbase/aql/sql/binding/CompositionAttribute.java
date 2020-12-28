/*
 * Copyright (c) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.
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
package org.ehrbase.aql.sql.binding;

import org.ehrbase.aql.definition.I_VariableDefinition;
import org.ehrbase.aql.sql.queryImpl.CompositionAttributeQuery;
import org.ehrbase.aql.sql.queryImpl.I_QueryImpl;
import org.ehrbase.aql.sql.queryImpl.JsonbEntryQuery;
import org.jooq.Field;

/**
 * convert a select or where AQL field into its SQL equivalent for a composition attribute. This
 * applies to standard attributes f.e. c/name/value etc.
 */
public class CompositionAttribute {

  private final CompositionAttributeQuery compositionAttributeQuery;
  private final JsonbEntryQuery jsonbEntryQuery;
  private final I_QueryImpl.Clause clause;
  private boolean containsJsonDataBlock;
  private String jsonbItemPath;
  private String optionalPath;

  public CompositionAttribute(
      CompositionAttributeQuery compositionAttributeQuery,
      JsonbEntryQuery jsonbEntryQuery,
      I_QueryImpl.Clause clause) {
    this.compositionAttributeQuery = compositionAttributeQuery;
    this.jsonbEntryQuery = jsonbEntryQuery;
    this.clause = clause;
  }

  public Field<?> toSql(
      I_VariableDefinition variableDefinition, String template_id, String identifier) {
    Field<?> field;

    if (variableDefinition.getPath() != null
        && variableDefinition.getPath().startsWith("content")) {
      field = jsonbEntryQuery.makeField(template_id, identifier, variableDefinition, clause);
      containsJsonDataBlock = jsonbEntryQuery.isJsonDataBlock();
      jsonbItemPath = jsonbEntryQuery.getJsonbItemPath();
      compositionAttributeQuery.setUseEntry(true);
    } else {
      field =
          compositionAttributeQuery.makeField(template_id, identifier, variableDefinition, clause);
      containsJsonDataBlock = compositionAttributeQuery.isJsonDataBlock();
    }
    optionalPath = variableDefinition.getPath();
    return field;
  }

  public boolean isContainsJsonDataBlock() {
    return containsJsonDataBlock;
  }

  public String getJsonbItemPath() {
    return jsonbItemPath;
  }

  public String getOptionalPath() {
    return optionalPath;
  }
}
