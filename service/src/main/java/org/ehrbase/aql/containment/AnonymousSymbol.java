package org.ehrbase.aql.containment;

/**
 * used to fulfill undefined symbol in AQL. F.e. whenever an expression such as:
 *  CONTAINS CLUSTER [openEHR-EHR-CLUSTER.location.v1] is passed.
 */
public class AnonymousSymbol {

    private int fieldId;

    public AnonymousSymbol() {
        this.fieldId = 0;
    }

    public AnonymousSymbol(int fieldId) {
        this.fieldId = fieldId;
    }

    public String generate(String prefix){
        return prefix + "_" + (++fieldId);
    }
}
