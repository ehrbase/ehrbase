package org.ehrbase.aql.sql.queryimpl;

import org.ehrbase.aql.definition.I_VariableDefinition;

@SuppressWarnings("java:S3358")
public class DefaultColumnId {

    private DefaultColumnId(){}

    protected static int serial = 0;

    public static String value(I_VariableDefinition variableDefinition){
        return (variableDefinition.getPath() == null ? (variableDefinition.getIdentifier() == null ? ("field_"+ (serial++)) : variableDefinition.getIdentifier()) : "/"+variableDefinition.getPath());
    }
}
