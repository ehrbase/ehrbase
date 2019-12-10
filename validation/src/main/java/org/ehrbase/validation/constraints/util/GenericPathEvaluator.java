package org.ehrbase.validation.constraints.util;

import com.nedap.archie.rm.archetyped.Locatable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static org.ehrbase.validation.constraints.util.LocatableHelper.dividePathIntoSegments;

public class GenericPathEvaluator {

    /**
     * Separator used to delimit segments in the path
     */
    public static final String PATH_SEPARATOR = "/";
    public static final String ROOT = PATH_SEPARATOR;

    /**
     * The item at a path that is relative to this item.
     *
     * @param path
     * @return the item
     * @throws IllegalArgumentException if path invalid
     */
    public Object itemAtPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("invalid path: null");
        }
        if (GenericPathEvaluator.ROOT.equals(path) || path.equals(whole())) {
            return this;
        }
        return pathEvaluate(path, this);
    }

    /**
     * Return path of current whole node
     */
    public String whole() {
        return ROOT;// + "[" + getName().getValue() + "]";
    }

    /*
     * generic path evaluation covers all rmClass
     */
    private Object pathEvaluate(String path, Object object) {
        if(path == null || object == null) {
            return null;
        }
        List<String> segments = dividePathIntoSegments(path);
        return evaluatePathSegment(segments, object);
    }

    /*
     * Evaluate recursively the path segments
     */
    private Object evaluatePathSegment(List<String> pathSegments, Object object) {
        if(pathSegments.isEmpty()) {
            return object;
        }
        String pathSegment = pathSegments.remove(0);
        Object value =  null;

        int index = pathSegment.indexOf("[");
        String expression = null;
        String attributeName = null;

        // has [....] predicate expression
        if(index > 0) {

            assert(pathSegment.indexOf("]") > index);

            attributeName = pathSegment.substring(0, index);
            expression = pathSegment.substring(index + 1,
                    pathSegment.indexOf("]"));
        } else {
            attributeName = pathSegment;
        }

        value = getAttributeValue(object, attributeName);
        if(expression != null && value != null ) {
            value = processPredicate(expression, value);
        }
        if(expression == null && value instanceof ArrayList && !pathSegments.isEmpty()) {
            ArrayList arrayList = ((ArrayList)value);
            if (!arrayList.isEmpty()){
                value = arrayList.get(0);
            }
        }
        if(value != null) {
            return evaluatePathSegment(pathSegments, value);
        }
        return null;
    }

    /*
     * Retrieves the value of named attribute of given object
     */
    private Object getAttributeValue(Object obj, String attribute) {
        Class rmClass = obj.getClass();
        Object value = null;
        Method getter = null;
        String getterName = "get" + toFirstUpperCaseCamelCase(attribute);

        try {
            getter = rmClass.getMethod(getterName, null);
            value = getter.invoke(obj, null);

        } catch(Exception e) {
            // TODO log as kernel warning
            // e.printStackTrace();
        }
        return value;
    }

    /**
     * Processes the predicate expression on given object
     * 1. if the object is a container, select the _first_ matching one
     * 2. only return the object if itself meets the predicate
     *
     * only shortcut expressions for at0000 and name are supported
     * for example: [at0001, 'node name']
     *
     * @param expression
     * @return null if there is no match
     */
    Object processPredicate(String expression, Object object) {
        String name = null;
        String archetypeNodeId = null;
        expression = expression.trim();
        int index;

        // shortcut syntax, [at0001, 'standing']
        if(expression.contains(",")
                // avoid [at0001 and/value='status, 2nd']
                && expression.indexOf(",") < expression.indexOf("'")) {
            index = expression.indexOf(",");
            archetypeNodeId = expression.substring(0, index).trim();
            name = expression.substring(expression.indexOf("'") + 1,
                    expression.lastIndexOf("'"));

            // [at0006 and name/value='any event']
            // [at0006 AND name/value='any event']
        } else if(expression.contains(" AND ")
                || expression.contains(" and ")) {

            // OG - 20100401: Fixed bug where the name contained 'AND' or 'and',
            // i.e. 'MEDICINSK BEHANDLING'.
            if(expression.contains(" AND ")) {
                index = expression.indexOf(" AND ");
            } else {
                index = expression.indexOf(" and ");
            }
            archetypeNodeId = expression.substring(0, index).trim();
            name = expression.substring(expression.indexOf("'") + 1,
                    expression.lastIndexOf("'"));
            // just name, ['standing']
        } else if (expression.startsWith("'") && expression.endsWith("'")) {
            name = expression.substring(1, expression.length() - 1);

            // archetyped root node id or at-coded node
            // [at0006] or [openEHR-EHR-OBSERVATION.laboratory-lipids.v1]
        } else {
            archetypeNodeId = expression;
        }

        Iterable collection = null;
        if(object instanceof Iterable) {
            collection = (Iterable) object;
        } else {
            List list = new ArrayList();
            list.add(object);
            collection = list;
        }

        for(Object item : collection) {
            if(item instanceof Locatable) {
                Locatable locatable = (Locatable) item;
                if(archetypeNodeId != null
                        && !locatable.getArchetypeNodeId().equals(archetypeNodeId)) {
                    continue;
                }
                if(name != null && !locatable.getName().getValue().equals(name)) {
                    continue;
                }
            }
            // TODO other non-locatable predicates!!
            // e.g. time > 10:20:15
            return item; // found a match!
        }
        return null;
    }


    public String toFirstUpperCaseCamelCase(String name) {
        name = toCamelCase(name);
        return name.substring(0, 1).toUpperCase()
                + name.substring(1);
    }

    public String toCamelCase(String underscoreSeparated) {
        if( ! underscoreSeparated.contains("_")) {
            return underscoreSeparated;
        }
        StringTokenizer tokens = new StringTokenizer(underscoreSeparated, "_");
        StringBuffer buf = new StringBuffer();
        while (tokens.hasMoreTokens()) {
            String word = tokens.nextToken();
            if (buf.length() == 0) {
                buf.append(word);
            } else {
                buf.append(word.substring(0, 1).toUpperCase());
                buf.append(word.substring(1));
            }
        }
        return buf.toString();
    }

}
