/*
 * Copyright (c) 2024 vitasystems GmbH.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.openehr.util;

import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

public class TreeUtils {

    /**
     * Draws a tree. One node per line. Indentation: 2 spaces
     *
     * @param <T>
     * @param root
     * @param childOrder      sort the children in the output. May help when comparing rendered trees.
     * @param nodeRenderer    renders the contents of a node. Must not start with spaces or contain line breaks.
     * @return
     */
    public static <T extends TreeNode<T>> String renderTree(
            T root, Comparator<T> childOrder, Function<T, String> nodeRenderer) {
        var sb = new StringBuilder();
        renderTreeNode(root, sb, 0, childOrder, nodeRenderer);
        return sb.toString();
    }

    private static <T extends TreeNode<T>> void renderTreeNode(
            T node, StringBuilder sb, int level, Comparator<T> childOrder, Function<T, String> nodeRenderer) {
        if (!sb.isEmpty()) {
            sb.append("\n");
        }
        for (int l = 0; l < level; l++) {
            sb.append("  ");
        }
        String nodeStr = nodeRenderer.apply(node);
        if (StringUtils.isBlank(nodeStr)) {
            throw new IllegalArgumentException("rendered node must not be blank");
        } else if (Pattern.compile("^\\s").matcher(nodeStr).find()) {
            throw new IllegalArgumentException("rendered node must not start with whitespace");
        } else if (Pattern.compile("\\R").matcher(nodeStr).find()) {
            throw new IllegalArgumentException("rendered node must not contain line breaks");
        } else {
            sb.append(nodeStr);
        }
        Stream<T> childStream = node.streamChildren();
        if (childOrder != null) {
            childStream = childStream.sorted(childOrder);
        }
        childStream.forEach(n -> renderTreeNode(n, sb, level + 1, childOrder, nodeRenderer));
    }

    /**
     * Parses a tree from a String.
     * One node per line. Indentation: 2 spaces
     *
     * @param treeGraph
     * @param nodeParser parses the contents of a node
     * @return
     * @param <T>
     */
    public static <T extends TreeNode<T>> T parseTree(String treeGraph, Function<String, T> nodeParser) {
        Pattern p = Pattern.compile("((?:  )*)(.+)");
        Iterator<Pair<Integer, String>> it = treeGraph
                .lines()
                .map(l -> {
                    Matcher matcher = p.matcher(l);
                    if (!matcher.matches()) {
                        throw new IllegalArgumentException("illegal line: %s".formatted(l));
                    }
                    return matcher;
                })
                .map(m -> Pair.of(m.group(1).length() / 2, m.group(2)))
                .iterator();

        T root = nodeParser.apply(it.next().getRight());

        T lastNode = root;
        int lastLevel = 0;

        while (it.hasNext()) {
            Pair<Integer, String> next = it.next();
            int level = next.getLeft();

            if (level <= 0) {
                throw new IllegalArgumentException("Only one root allowed: %s".formatted(next.getRight()));
            }
            int parentLevel = level - 1;
            if (parentLevel > lastLevel) {
                throw new IllegalArgumentException(
                        "Inconsistent level of %s: %d >>  %d".formatted(next.getRight(), level, lastLevel));
            }

            var parent = lastNode;
            for (int i = lastLevel; i > parentLevel; i--) {
                parent = parent.parent;
            }

            lastNode = parent.addChild(nodeParser.apply(next.getRight()));
            lastLevel = level;
        }

        return root;
    }
}
