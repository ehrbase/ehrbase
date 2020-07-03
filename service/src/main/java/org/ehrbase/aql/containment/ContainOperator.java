package org.ehrbase.aql.containment;

/**
 * identified CONTAINS logical operators
 */
public class ContainOperator {

    public enum OPERATOR {AND, OR, XOR, NOT}

    private OPERATOR operator;

    public ContainOperator(String operator) {
        this.operator = OPERATOR.valueOf(operator.toUpperCase());
    }

    public OPERATOR operator(){
        return operator;
    }

    public String getOperator() {
        return operator.name();
    }

    public boolean isEqualTo(ContainOperator another){
        return this.getOperator().equals(another.getOperator());
    }
}
