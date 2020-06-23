package org.ehrbase.aql.sql.queryImpl.attribute;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

//TODO: add a clause allowing to get the RmType FROM the DB (f.e. ELEMENT/value doesn't get the type)
public class GenericJsonPath {

    private final String path;

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
            else if (segment.matches("value|name") && !isTerminalValue(jqueryPaths, i)){
                actualPaths.add("/"+ segment);
                if (segment.matches("name"))
                    actualPaths.add("0");
            }
            else
                actualPaths.add(segment);
        }

        return "'{"+String.join(",", actualPaths)+"}'";
    }

    private boolean isTerminalValue(List paths, int index){
        return paths.size() == 1 || (paths.size() > 1 && index == paths.size() - 1 && paths.get(index - 1).toString().matches("value|name|id|terminology_id"));
    }
}
