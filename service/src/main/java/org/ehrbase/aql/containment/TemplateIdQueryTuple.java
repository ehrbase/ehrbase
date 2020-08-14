package org.ehrbase.aql.containment;

import java.util.Objects;

public class TemplateIdQueryTuple {

    private final String templateId;
    private final String jsonQueryExpression;

    public TemplateIdQueryTuple(String templateId, String jsonQueryExpression) {

        this.templateId = templateId;
        this.jsonQueryExpression = jsonQueryExpression;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getJsonQueryExpression() {
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
