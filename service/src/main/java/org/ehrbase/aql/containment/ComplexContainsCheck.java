package org.ehrbase.aql.containment;

import java.util.List;

public class ComplexContainsCheck extends ContainsCheck {

    private List<Object> tokens;

    public ComplexContainsCheck(String label, List<Object> tokens) {
        super(label);
        this.tokens = tokens;
    }

    public String toString(){
        StringBuilder stringBuilder = new StringBuilder();
        for (Object item: tokens) {
            if (item instanceof SimpleChainedCheck)
                stringBuilder.append(item.toString());
            else if (item instanceof ContainOperator)
                stringBuilder.append(((ContainOperator) item).getOperator());
            else if (item instanceof ComplexContainsCheck)
                stringBuilder.append(item.toString());
            else
                stringBuilder.append(item.toString());
        }
        this.checkExpression = stringBuilder.toString();
        return checkExpression;
    }
}
