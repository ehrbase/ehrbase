package org.ehrbase.aql.containment;


import org.ehrbase.webtemplate.parser.NodeId;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

public class TemplateIdQueryTuple implements Serializable {

    private final String templateId;
    private final Collection<NodeId> jsonQueryExpression;

    public TemplateIdQueryTuple(String templateId, Collection<NodeId> jsonQueryExpression) {

        this.templateId = templateId;
        this.jsonQueryExpression = jsonQueryExpression;
    }

    public String getTemplateId() {
        return templateId;
    }

    public Collection<NodeId> getJsonQueryExpression() {
        return jsonQueryExpression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateIdQueryTuple that = (TemplateIdQueryTuple) o;
        return templateId.equals(that.templateId) &&
                jsonQueryExpression.equals(that.jsonQueryExpression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateId, jsonQueryExpression);
    }
}
