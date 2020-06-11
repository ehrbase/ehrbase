package org.ehrbase.aql.containment;

public class SimpleChainedCheck extends ContainsCheck {

    private ContainmentSet containmentSet;

    public SimpleChainedCheck(String label, ContainmentSet containmentSet) {
        super(label);
        this.containmentSet = containmentSet;
    }

    public String toString(){
        this.checkExpression = containmentSet.getContainmentList().toString();
        return checkExpression;
    }
}
