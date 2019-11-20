package org.ehrbase.aql.sql.queryImpl;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.util.Strings;
import org.ehrbase.ehr.util.LocatableHelper;

import java.util.List;

public class VariableAqlPath {

    private final String jsonPathQualifierMatcher = "value|name|time";

    String path;

    public VariableAqlPath(String path) {
        this.path = path;
    }

    public String getSuffix(){
        List<String> segments = LocatableHelper.dividePathIntoSegments(path);
        return segments.get(segments.size() - 1);
    }

    public String getInfix(){
        List<String> segments = LocatableHelper.dividePathIntoSegments(path);
        return String.join("/", segments.subList(0, segments.size() - 1));
    }

    public boolean isPartialAqlDataValuePath(){
        return getSuffix().matches(jsonPathQualifierMatcher);
    }

}
