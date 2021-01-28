package org.ehrbase.aql.sql.queryimpl;

import java.util.List;
import org.ehrbase.ehr.util.LocatableHelper;

public class VariableAqlPath {

  private static final String JSON_PATH_QUALIFIER_MATCHER = "value|name|time";

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
    return getSuffix().matches(JSON_PATH_QUALIFIER_MATCHER);
  }
}
