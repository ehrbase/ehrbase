package org.ehrbase.aql.sql.queryImpl.attribute;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GenericJsonPath {

    final String path;

    public GenericJsonPath(String path) {
        this.path = path;
    }

    public String jqueryPath(){
        if (path == null || path.isEmpty())
            return path;

        List<String> jqueryPaths = Arrays.asList(path.split("/|,"));
        List<String> actualPaths = new ArrayList<>();

        for (int i = 0; i < jqueryPaths.size(); i++){
            String segment = jqueryPaths.get(i);
            if (segment.startsWith("items")){
                actualPaths.add("/"+ segment);
                //takes care of array expression (unless the occurrence is specified (TODO)
                actualPaths.add("0");
            }
            else if (segment.startsWith("content")){
                actualPaths.add("content,/"+ segment);
                actualPaths.add("0"); //as above
            }
            else if (segment.equals("value") && (i < jqueryPaths.size() - 1) && jqueryPaths.get(i + 1).equals("value")){
                actualPaths.add("/"+ segment);
            }
            else
                actualPaths.add(segment);
        }

        return "'{"+String.join(",", actualPaths)+"}'";
    }
}
