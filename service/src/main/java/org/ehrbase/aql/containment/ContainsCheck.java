package org.ehrbase.aql.containment;

import java.util.List;

public abstract class ContainsCheck {
    String label;
    String checkExpression;

    public ContainsCheck(String label) {
        this.label = label;
    }
}
