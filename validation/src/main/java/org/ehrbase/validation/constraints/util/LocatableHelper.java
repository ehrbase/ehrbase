/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School

 * This file is part of Project EHRbase

 * Copyright (c) 2015 Christian Chevalley
 * This file is part of Project Ethercis
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.validation.constraints.util;


import com.nedap.archie.rm.archetyped.Locatable;

import java.util.*;

/**
 * ETHERCIS Project ehrservice
 * Created by Christian Chevalley on 8/18/2015.
 */
public class LocatableHelper {

    public static final String AND_NAME_VALUE_TOKEN = "and name/value=";
    public static final String COMMA_TOKEN = ",";
    public static final String INDEX_PREFIX_TOKEN = "#";
    public static final String OPEN_BRACKET = "[";
    public static final String CLOSE_BRACKET = "]";
    public static final String FORWARD_SLASH = "/";
    /**
     * Separator used to delimit segments in the path
     */
    public static final String PATH_SEPARATOR = "/";
    public static final String ROOT = PATH_SEPARATOR;

    private Map<String, Integer> arrayItemPathMap = new HashMap<>(); //contains the list of array insertion paths for a template

    public LocatableHelper() {
    }


    public static String incrementPathNodeId(String fromPathId) {
        Integer id = Integer.parseInt(fromPathId.substring(2)); //skip the "at" bit
        String newNodeId = "at" + String.format("%04d", ++id);
        return newNodeId;
    }


    private static void findLastNodeIdInSibblings(String lastNodeId, List<Locatable> siblings) {

        for (Object sibling : siblings) {

            if (sibling instanceof Locatable) {
                String nodeId = ((Locatable) sibling).getArchetypeNodeId();
                if (!nodeId.contains("openEHR")) {
                    Integer last = Integer.parseInt(lastNodeId.substring(2));
                    Integer current = Integer.parseInt(nodeId.substring(2));

                    if (current > last)
                        lastNodeId = "at" + String.format("%04d", current);
                } else {
                    //check if it contains a '#'
                    if (nodeId.contains(INDEX_PREFIX_TOKEN)) {
                        ;
                    }
                }
            }

        }
    }

    private static String extractLastAtPath(String itemPath) {
        if (itemPath.contains("[at")) {
            String path = LocatableHelper.simplifyPath(itemPath);
            return path.substring(path.lastIndexOf(OPEN_BRACKET) + 1, path.lastIndexOf(CLOSE_BRACKET));
        } else
            return "at0000";

    }


    static public class NodeItem {
        private Locatable node;
        //        private Locatable child;
        private String childPath;
        private String insertionPath;

        public NodeItem(Locatable node, String childPath, String insertionPath) {
            this.node = node;
            this.childPath = childPath;
            this.insertionPath = insertionPath;
        }

        public Locatable getNode() {
            return node;
        }

        public String getChildPath() {
            return childPath;
        }

        public String getInsertionPath() {
            return insertionPath;
        }
    }

    /**
     * identify the attribute in the last path segment
     *
     * @param path
     * @return
     */
    private static String identifyAttribute(String path) {
        //get the last segment of this path
        List<String> segments = dividePathIntoSegments(path);

        String pathSegment = segments.get(segments.size() - 1);

        int index = pathSegment.indexOf(OPEN_BRACKET);
        String expression = null;
        String attributeName = null;

        // has [....] predicate expression
        if (index > 0) {

            assert (pathSegment.indexOf(CLOSE_BRACKET) > index);

            attributeName = pathSegment.substring(0, index);
            expression = pathSegment.substring(index + 1, pathSegment.indexOf(CLOSE_BRACKET));
        } else {
            attributeName = pathSegment;
        }

        return attributeName;
    }

    /**
     * identify the name in the last path segment
     *
     * @param path
     * @return
     */
    public static String indentifyName(String path) {
        List<String> segments = dividePathIntoSegments(path);
        String pathSegment = segments.get(segments.size() - 1);

        int index = pathSegment.indexOf(OPEN_BRACKET);

        if (index < 0)
            return null; //path such as /ism_transition f.ex.

        String expression = pathSegment.substring(index + 1, pathSegment.indexOf(CLOSE_BRACKET));

        String archetypeNodeId;
        String name = null;

        if (expression.contains(" AND ") || expression.contains(" and ") || expression.contains(COMMA_TOKEN)) {

            // OG - 20100401: Fixed bug where the name contained 'AND' or 'and',
            // i.e. 'MEDICINSK BEHANDLING'.
            if (expression.contains(" AND ")) {
                index = expression.indexOf(" AND ");
            } else if (expression.contains("and")) {
                index = expression.indexOf(" and ");
            } else
                index = expression.indexOf(COMMA_TOKEN);


            archetypeNodeId = expression.substring(0, index).trim();
            name = expression.substring(expression.indexOf("'") + 1, expression.lastIndexOf("'"));
            // just name, ['standing']
        } else if (expression.startsWith("'") && expression.endsWith("'")) {
            name = expression.substring(1, expression.length() - 1);
        }
        return name;
    }


    /**
     * identify the path of sibling in a item list or array corresponding to an unresolved path.
     *
     * @param unresolvedPath
     * @return
     */
    public static String siblingPath(String unresolvedPath) {
        //if the last path is qualified with a name/value, check if a similar item exists

        List<String> segments = dividePathIntoSegments(unresolvedPath);
        String last = segments.get(segments.size() - 1);
        if (last.contains(AND_NAME_VALUE_TOKEN) || last.contains(COMMA_TOKEN)) {
            last = last.contains(AND_NAME_VALUE_TOKEN) ? last.substring(0, last.indexOf(AND_NAME_VALUE_TOKEN)) + CLOSE_BRACKET : last.substring(0, last.indexOf(COMMA_TOKEN)) + CLOSE_BRACKET;
        }

        StringBuffer tentativePath = new StringBuffer();
        for (int i = 0; i < segments.size() - 1; i++) {
            tentativePath.append(segments.get(i) + FORWARD_SLASH);
        }
        tentativePath.append(last);

        return tentativePath.toString();
    }

    /**
     * return a sibling locatable for an unresolved path
     *
     * @param locatable
     * @param unresolvedPath
     * @return
     */
    public static Locatable siblingAtPath(Locatable locatable, String unresolvedPath) {
        //if the last path is qualified with a name/value, check if a similar item exists

        List<String> segments = dividePathIntoSegments(unresolvedPath);
        String last = segments.get(segments.size() - 1);
        if (last.contains(AND_NAME_VALUE_TOKEN)) {
            last = last.substring(0, last.indexOf(AND_NAME_VALUE_TOKEN)) + CLOSE_BRACKET;
        }

        StringBuffer tentativePath = new StringBuffer();
        for (int i = 0; i < segments.size() - 1; i++) {
            tentativePath.append(segments.get(i) + FORWARD_SLASH);
        }
        tentativePath.append(last);

        Object sibling = locatable.itemAtPath(tentativePath.toString());

        if (sibling != null)
            return (Locatable) sibling;

        return null;

    }

    /**
     * return the first parent matching an unresolved path by identify the first item which
     * path matches fully a partial path expression
     *
     * @param locatable
     * @param unresolvedPath
     * @return
     */
    public static NodeItem backtrackItemAtPath(Locatable locatable, String unresolvedPath) {


        //find first parent existing for this unresolved path
        String parentPath = parentPath(unresolvedPath);
        Object parentAtPath = locatable.itemAtPath(parentPath);
        Object childAtPath = null;
        String lastPath = unresolvedPath;

        while (parentAtPath == null && parentPath != null && parentPath.length() > 0) {
            lastPath = parentPath;
            parentPath = parentPath(parentPath);
            if (parentPath == null || parentPath.length() <= 0)
                break;
            childAtPath = parentAtPath;
            parentAtPath = locatable.itemAtPath(parentPath);
        }

        return new NodeItem((Locatable) parentAtPath, lastPath, FORWARD_SLASH + identifyAttribute(lastPath));
    }

    public static Locatable getLocatableParent(Locatable locatable, String path) {
        List<String> segments = dividePathIntoSegments(path);

        for (int i = segments.size() - 1; i >= 0; i--) {
            String parentPath = FORWARD_SLASH + String.join(FORWARD_SLASH, segments.subList(0, i));
            if (locatable.itemAtPath(parentPath) instanceof Locatable)
                return (Locatable) locatable.itemAtPath(parentPath);
        }
        return null;
    }

    public static String getLocatableParentPath(Locatable locatable, String path) {
        List<String> segments = dividePathIntoSegments(path);

        for (int i = segments.size() - 1; i >= 0; i--) {
            String parentPath = FORWARD_SLASH + String.join(FORWARD_SLASH, segments.subList(0, i));
            if (locatable.itemAtPath(parentPath) instanceof Locatable)
                return parentPath;
        }
        return null;
    }

    private static Object matchingItemInList(List<Locatable> itemList, String path) {
        if (path.contains(COMMA_TOKEN) || path.contains(AND_NAME_VALUE_TOKEN)) {
            List<String> segments = dividePathIntoSegments(path);
            String lastNodeid = segments.get(segments.size() - 1);

            if (lastNodeid.contains(AND_NAME_VALUE_TOKEN) || lastNodeid.contains(COMMA_TOKEN)) {
                if (lastNodeid.contains(INDEX_PREFIX_TOKEN)) {
                    String nameValueToken = trimIndexValue(extractNameValueToken(lastNodeid)).trim();
                    for (Locatable locatable : itemList) {
                        if (locatable.getName().getValue().equals(nameValueToken))
                            return locatable;
                    }
                }
            }
        }
        return itemList.get(0); //default
    }



    /**
     * return a path without name/value in nodeId predicate
     *
     * @param path
     * @return
     */
    public static String simplifyPath(String path) {
        //if the last path is qualified with a name/value, check if a similar item exists
        StringBuffer tentativePath = new StringBuffer();
        tentativePath.append(FORWARD_SLASH);

        List<String> segments = dividePathIntoSegments(path);
        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            if (segment.contains(AND_NAME_VALUE_TOKEN) || segment.contains(COMMA_TOKEN)) {
                String trimmedNodeId = segment.contains(AND_NAME_VALUE_TOKEN) ? segment.split(AND_NAME_VALUE_TOKEN)[0] : segment.split(COMMA_TOKEN)[0];
                tentativePath.append(trimmedNodeId.trim());
                tentativePath.append(CLOSE_BRACKET);
            } else
                tentativePath.append(segment);
            if (i < segments.size() - 1)
                tentativePath.append(FORWARD_SLASH);
        }

        return tentativePath.toString();
    }

    /**
     * check if path contains expression like 'and name/value='iteration #1''
     *
     * @param path
     * @return
     */
    public static boolean hasDefinedOccurence(String path) {
        //if the last path is qualified with a name/value, check if a similar item exists
        List<String> segments = dividePathIntoSegments(path);
        for (int i = 0; i < segments.size(); i++) {
            String segment = segments.get(i);
            if (segment.contains(AND_NAME_VALUE_TOKEN) || segment.contains(COMMA_TOKEN)) {

                String namePart = segment.contains(AND_NAME_VALUE_TOKEN) ? segment.split(AND_NAME_VALUE_TOKEN)[1] : segment.split(COMMA_TOKEN)[1];
                if (namePart.contains(INDEX_PREFIX_TOKEN))
                    return true;
            }
        }

        return false;
    }

    public static Object itemAtPath(Locatable locatable, String path) {
        return locatable.itemAtPath(path.replaceAll(AND_NAME_VALUE_TOKEN, COMMA_TOKEN));
    }

    /**
     * retrieve the value of an array index in a nodeId predicate
     *
     * @param nodeId
     * @return
     */
    public static Integer retrieveIndexValue(String nodeId) {
        if (nodeId.contains(INDEX_PREFIX_TOKEN)) {
            Integer indexValue = Integer.valueOf((nodeId.split(INDEX_PREFIX_TOKEN)[1]).split("']")[0]);
            return indexValue;
        }
        return null;
    }

    public static String trimIndexValue(String nodeid) {
        if (nodeid.contains(INDEX_PREFIX_TOKEN)) {
            return nodeid.substring(0, nodeid.indexOf(INDEX_PREFIX_TOKEN));
        }
        return nodeid;
    }

    public static String trimNameValue(String nodeid) {
        if (nodeid.contains(AND_NAME_VALUE_TOKEN) || nodeid.contains(COMMA_TOKEN))
            return (nodeid.substring(0, nodeid.indexOf(nodeid.contains(AND_NAME_VALUE_TOKEN) ? AND_NAME_VALUE_TOKEN : COMMA_TOKEN)).trim() + CLOSE_BRACKET).trim();
        return nodeid.trim();
    }

    public static String extractNameValueToken(String nodeid) {
        if (nodeid.contains("'"))
            return nodeid.substring(nodeid.indexOf("'") + 1, nodeid.lastIndexOf("'"));
        return nodeid;
    }

    public Map<String, Integer> getArrayItemPathMap() {
        return arrayItemPathMap;
    }

    public void addItemPath(String itemPath) {
        if (!arrayItemPathMap.containsKey(itemPath))
            arrayItemPathMap.put(itemPath, 1);
        else
            arrayItemPathMap.put(itemPath, arrayItemPathMap.get(itemPath) + 1);
    }

    /*
     * Simple fix doesn't take care of "/" inside predicates
     * e.g. data/events[at0006 and name/value='any event']
     *
     * OG - 2010-03-15: Added fix that seems to solve this problem.
     */
    public static List<String> dividePathIntoSegments(String path) {
        List<String> segments = new ArrayList<String>();
        StringTokenizer tokens = new StringTokenizer(path, "/");
        while(tokens.hasMoreTokens()) {
            String next = tokens.nextToken();
            if (next.matches(".+\\[.+[^\\]]$")) {
                do {
                    next = next + "/" + tokens.nextToken();
                } while (!next.matches(".*]$"));
            }
            segments.add(next);
        }
        return segments;
    }

    /**
     * Computes the path of parent object
     *
     * @param path
     * @return
     */
    public static String parentPath(String path) {
        List<String> list = dividePathIntoSegments(path);
        int pathLevel = list.size();
        if(pathLevel == 0) {
            throw new IllegalArgumentException("Unable to compute parent path: "
                    + path);
        } else if(pathLevel == 1) {
            return PATH_SEPARATOR;
        }
        StringBuffer buf = new StringBuffer();
        for(int j = 0; j < pathLevel - 1; j++) {
            buf.append(PATH_SEPARATOR);
            buf.append(list.get(j));
        }
        return buf.toString();
    }
}
