package org.ehrbase.aql.sql.queryimpl.attribute;

import org.ehrbase.aql.sql.queryimpl.NodeIds;
import org.ehrbase.serialisation.dbencoding.wrappers.json.I_DvTypeAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GenericJsonPath {

    public static final String CONTEXT = "context";
    public static final String FEEDER_AUDIT = "feeder_audit";
    public static final String ORIGINATING_SYSTEM_ITEM_IDS = "originating_system_item_ids";
    public static final String FEEDER_SYSTEM_ITEM_IDS = "feeder_system_item_ids";
    public static final String ORIGINAL_CONTENT = "original_content";
    public static final String ORIGINATING_SYSTEM_AUDIT = "originating_system_audit";
    public static final String FEEDER_SYSTEM_AUDIT = "feeder_system_audit";
    public static final String SETTING = "setting";
    public static final String ITEMS = "items";
    public static final String CONTENT = "content";
    public static final String VALUE = "value";
    public static final String NAME = "name";
    public static final String OTHER_DETAILS = "other_details";
    public static final String OTHER_CONTEXT = "other_context";
    public static final String TERMINOLOGY_ID = "terminology_id";
    public static final String PURPOSE = "purpose";
    public static final String TARGET = "target";
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
            if (segment.startsWith(ITEMS)){
                actualPaths.add("/"+ segment);
                //takes care of array expression (unless the occurrence is specified)
                actualPaths.add("0");
            }
            else if (segment.startsWith(CONTENT)){
                actualPaths.add(CONTENT+",/"+ segment);
                actualPaths.add("0"); //as above
            }
            else if (segment.matches(VALUE + "|" + NAME) && !isTerminalValue(jqueryPaths, i)){
                actualPaths.add("/"+ segment);
                if (segment.matches(NAME))
                    actualPaths.add("0");
            }
            else if (segment.matches(OTHER_DETAILS + "|" + OTHER_CONTEXT))
                actualPaths.add(segment);
            else if (isNonItemStructureAttribute(path))
                actualPaths.add(segment);
            else
                actualPaths.add(NodeIds.toCamelCase(segment));

        }

        return "'{"+String.join(",", actualPaths)+"}'";
    }

    public boolean isTerminalValue(List<String> paths, int index){
        return paths.size() == 1
                || (paths.size() > 1
                        && index == paths.size() - 1
                        && paths.get(index).matches(VALUE + "|" + NAME + "|" + TERMINOLOGY_ID + "|" + PURPOSE + "|" + TARGET)
                        //check if this 'terminal attribute' is actually a node attribute
                        //match node predicate regexp starts with '/' which is not the case when splitting the path
                        && !paths.get(index - 1).matches(I_DvTypeAdapter.matchNodePredicate.substring(1)));
    }

    /**
     * identifies if the encoding comes from a db canonical function
     * NB. the DB encoding still uses camel case instead of snake case for datavalues
     * @param path locatable path
     * @return true if non structural
     */
    private boolean isNonItemStructureAttribute(String path){
        return (path.contains(CONTEXT)
                || path.contains(FEEDER_AUDIT)
                || path.contains(ORIGINATING_SYSTEM_ITEM_IDS)
                || path.contains(FEEDER_SYSTEM_ITEM_IDS)
                || path.contains(ORIGINAL_CONTENT)
                || path.contains(ORIGINATING_SYSTEM_AUDIT)
                || path.contains(FEEDER_SYSTEM_AUDIT)
                || path.contains(SETTING)
        );
    }
}
