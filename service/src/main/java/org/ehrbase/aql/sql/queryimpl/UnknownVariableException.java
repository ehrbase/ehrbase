package org.ehrbase.aql.sql.queryimpl;

public class UnknownVariableException extends Exception{

    public UnknownVariableException(String variable) {
        super("Unknown variable:"+variable);
    }
}
