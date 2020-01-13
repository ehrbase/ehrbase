/*
 * Modifications copyright (C) 2019 Christian Chevalley, Vitasystems GmbH and Hannover Medical School.

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
package org.ehrbase.ehr.encode;

import org.ehrbase.serialisation.CompositionSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

/**
 * ETHERCIS Project ehrservice
 * Created by Christian Chevalley on 8/3/2015.
 */
public class ItemStack {


    private static final String archetypePrefix = "[openEHR-";
    private static final String namedItemPrefix = " and name/value='";
    private static final String namedItemSuffix = "']";

    private Logger log = LoggerFactory.getLogger(this.getClass());

    //contains the ADL path to an element
    private Stack<String> pathStack = new Stack<>();
    //contains the named path to an element (used to bind Flat JSON)
    private Stack<String> namedStack = new Stack<>();


    //used to resolve and index containments
    private class ContainmentStruct {
        private String label;
        private String fullPath; //full path

        ContainmentStruct(String archetype, String path) {
            this.label = archetype;
            this.fullPath = path;
        }

        public String getLabel() {
            return label;
        }

        String getFullPath() {
            return fullPath;
        }
    }

    private Stack<ContainmentStruct> containmentStack = new Stack<>();

    private Map<String, String> ltreeMap = new TreeMap<>();

    public Map<String, String> getLtreeMap() {
        return ltreeMap;
    }

    //replace all dots by underscore and keep only the archetype name part
    public static String normalizeLabel(String path) {
        String label = path.substring(path.indexOf("[") + 1);
        int namedIndex = label.indexOf(namedItemPrefix);
        if (namedIndex >= 0)
            label = label.substring(0, namedIndex);

        //replace all dots by underscores since it is used as delimiter in a dotted labels expression for ltree
        //only A-Za-z0-9_ are allowed to express a label
        label = label.replaceAll("\\.", "_").replaceAll("-", "_");
        if (label.endsWith("]"))
            label = label.substring(0, label.indexOf("]"));
        return label;
    }

    public static String getLabelType(String path) {
        if (path.contains("["))
            return path.substring(1, path.indexOf("["));
        return path;
    }

    private boolean isArchetypeSlot(String path) {
        return path.contains(archetypePrefix);
    }

    private void flushContainmentMap() {
        //get the last element on stack
        ContainmentStruct containmentStruct = containmentStack.lastElement();
//        System.out.println(containmentStruct.getLabel() + "-->" + containmentStruct.getFullPath());
        if (!ltreeMap.containsKey(containmentStruct.getLabel()))
            ltreeMap.put(containmentStruct.getLabel(), containmentStruct.getFullPath());
    }

    public void pushStacks(String path, String name) {
        //specify name/value for path in the format /something[openEHR-EHR-blablah...] for disambiguation
        log.debug("-- PUSH PATH:" + path + "::" + name);
        if (path.contains(archetypePrefix) || path.contains(CompositionSerializer.TAG_ACTIVITIES) || path.contains(CompositionSerializer.TAG_ITEMS) || path.contains(CompositionSerializer.TAG_EVENTS)) {
            //add name in path
//            if (!name.contains("'"))
            if (name != null)
                path = path.substring(0, path.indexOf("]")) + namedItemPrefix + name + namedItemSuffix;
//            else
//                log.warn("Ignoring entry/item name:"+name);
        }
        pushStack(pathStack, path);
        if (name != null)
            pushStack(namedStack, name.toLowerCase().replaceAll(" ", "_"));
        if (isArchetypeSlot(path)) {

            String label = normalizeLabel(path);
            //get the previous label if any
            String previousLabel = null;

            if (!containmentStack.isEmpty()) {
                previousLabel = containmentStack.lastElement().getLabel();
            }
            if (previousLabel != null) {
                label = previousLabel + "." + label;
            }
            ContainmentStruct containmentStruct = new ContainmentStruct(label, pathStackDump());
            containmentStack.push(containmentStruct);
        }

//        pushNamedStack(name);
    }

    public void popStacks() {
        log.debug("-- POP PATH:" + (pathStack.isEmpty() ? "*empty*" : pathStack.lastElement()));
        String path = popStack(pathStack);
        if (path != null && isArchetypeSlot(path)) {
            flushContainmentMap();
            containmentStack.pop();
        }
        popStack(namedStack);
    }

    private void pushStack(Stack<String> stack, String s) {
        stack.push(s);
    }

    private String popStack(Stack<String> stack) {
        if (!stack.empty()) {
            return stack.pop();
        }
        return null;
    }

    private String stackDump(Stack stack) {
        StringBuilder b = new StringBuilder();
        for (Object s : stack.toArray()) b.append((String) s);
        return b.toString();
    }

    public String namedStackDump() {
        StringBuilder b = new StringBuilder();
        for (Object s : namedStack.toArray()) b.append(s).append("/");
        return b.toString();
    }

    public String expandedStackDump() {
        StringBuilder b = new StringBuilder();
        int i = 0;
        for (Object s : namedStack.toArray()) {
            b.append(s).append("{{").append(pathStack.get(i++)).append("}}/");
        }
        return b.toString();

    }

    public String pathStackDump() {
        return stackDump(pathStack);
    }
}
