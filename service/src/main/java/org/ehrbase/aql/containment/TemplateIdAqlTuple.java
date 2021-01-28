package org.ehrbase.aql.containment;

import java.io.Serializable;
import java.util.Objects;

public class TemplateIdAqlTuple implements Serializable {
  private static final long serialVersionUID = 1L;

  private final String templateId;
  private final String aql;

  public TemplateIdAqlTuple(String templateId, String aql) {
    this.templateId = templateId;
    this.aql = aql;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TemplateIdAqlTuple that = (TemplateIdAqlTuple) o;
    return templateId.equals(that.templateId) && aql.equals(that.aql);
  }

  @Override
  public int hashCode() {
    return Objects.hash(templateId, aql);
  }
}
