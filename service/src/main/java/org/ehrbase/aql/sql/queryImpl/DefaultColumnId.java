package org.ehrbase.aql.sql.queryImpl;

import org.ehrbase.aql.definition.I_VariableDefinition;

public class DefaultColumnId {

  protected static int serial = 0;

  public String value(I_VariableDefinition variableDefinition) {
    return (variableDefinition.getPath() == null
        ? (variableDefinition.getIdentifier() == null
            ? ("field_" + (serial++))
            : variableDefinition.getIdentifier())
        : "/" + variableDefinition.getPath());
  }
}
