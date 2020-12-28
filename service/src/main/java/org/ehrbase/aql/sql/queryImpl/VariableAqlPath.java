package org.ehrbase.aql.sql.queryImpl;

import java.util.List;
import org.ehrbase.ehr.util.LocatableHelper;

public class VariableAqlPath {

  private final String jsonPathQualifierMatcher = "value|name|time";

  String path;

  public VariableAqlPath(String path) {
    this.path = path;
  }

  public String getSuffix() {
    List<String> segments = LocatableHelper.dividePathIntoSegments(path);
    return segments.get(segments.size() - 1);
  }

  public String getInfix() {
    List<String> segments = LocatableHelper.dividePathIntoSegments(path);
    return String.join("/", segments.subList(0, segments.size() - 1));
  }

  public boolean isPartialAqlDataValuePath() {
    return getSuffix().matches(jsonPathQualifierMatcher);
  }
}
