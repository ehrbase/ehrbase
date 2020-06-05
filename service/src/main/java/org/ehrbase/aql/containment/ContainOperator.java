package org.ehrbase.aql.containment;

public class ContainOperator {

    private enum OPERATOR {AND, OR, XOR}

    private OPERATOR operator;

    public ContainOperator(String operator) {
        this.operator = OPERATOR.valueOf(operator);
    }

    public String getOperator() {
        return operator.name();
    }

    public boolean isEqualTo(ContainOperator another){
        return this.getOperator().equals(another.getOperator());
    }
}
