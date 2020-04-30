package org.ehrbase.aql.sql.queryImpl.attribute.eventcontext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OtherContextPredicate {

    String path;

    public OtherContextPredicate(String path) {
        this.path = path;
    }

    public String adjustForQuery(){
        if (path.matches("^.*other_context\\[.*\\].*")){
            //strip other_context predicate
            Pattern pattern = Pattern.compile("other_context\\[(.*?)\\]");
            Matcher matcher = pattern.matcher(path);
            if (matcher.find()) {
                path = path.substring(0, matcher.start() + "other_context[".length() - 1) + (matcher.end() == path.length() ? "" : path.substring(matcher.end()));
            }
        }

        return path;
    }
}
