package org.ehrbase.openehr.aqlengine.aql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.ehrbase.openehr.sdk.aql.dto.operand.PathPredicateOperand;
import org.ehrbase.openehr.sdk.aql.dto.operand.Primitive;
import org.ehrbase.openehr.sdk.util.Freezable;

public class ListPredicateOperand<T extends Primitive> implements PathPredicateOperand<ListPredicateOperand<T>> {

    protected boolean frozen = false;
    private List<T> values;

    public ListPredicateOperand(List<T> values) {
        this.values = values;
    }

    public List<T> getValues() {
        return values;
    }

    public void setValues(final List<T> values) {
        this.values = values;
    }

    @Override
    public void render(final StringBuilder sb) {

    }

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    @Override
    public ListPredicateOperand<T> thawed() {
        return new ListPredicateOperand<>(new ArrayList<>(values));
    }

    @Override
    public ListPredicateOperand<T> frozen() {
        return Freezable.frozen(this, t -> {
            ListPredicateOperand<T> clone = clone();
            clone.frozen = true;
            clone.values = Collections.unmodifiableList(values);
            return clone;
        });
    }

    @Override
    public ListPredicateOperand<T> clone() {
        return Freezable.clone(this, ListPredicateOperand::thawed);
    }
}
