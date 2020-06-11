package org.ehrbase.aql.containment;

public class ContainOperator {

    private enum OPERATOR {AND, OR, XOR, NOT}

    private OPERATOR operator;

    public ContainOperator(String operator) {
        this.operator = OPERATOR.valueOf(operator.toUpperCase());
    }

    public String getOperator() {
        return operator.name();
    }

    public boolean isEqualTo(ContainOperator another){
        return this.getOperator().equals(another.getOperator());
    }
}
