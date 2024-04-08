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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

class TreeNodeTest {

    private static final class MyNode extends TreeNode<MyNode> {

        public final int id;

        private MyNode(int id) {
            this.id = id;
        }

        public MyNode addChild(int id) {
            return addChild(new MyNode(id));
        }

        public static MyNode root(int id) {
            return new MyNode(id);
        }
    }

    @Test
    void testSimpleTree() {
        MyNode root = MyNode.root(0);
        MyNode n1 = root.addChild(1);
        MyNode n11 = n1.addChild(11);
        MyNode n12 = n1.addChild(12);
        MyNode n123 = n12.addChild(123);
        MyNode n13 = n1.addChild(13);

        MyNode n2 = root.addChild(2);
        MyNode n3 = root.addChild(3);
        MyNode n3_1 = n3.addChild(3_1);

        assertTreeMatches(
                root,
                """
        0
          1
            11
            12
              123
            13
          2
          3
            31""");
    }

    @Test
    void testMoveChild() {
        MyNode root = parseTree(
                """
                0
                  1
                    11
                    12
                      123
                    13
                  2
                  3
                    31
                        """);

        var n1 = root.getChildren().get(0);
        var n12 = n1.getChildren().get(1);
        var n3 = root.getChildren().get(2);

        assertThatThrownBy(() -> n12.addChild(root)).isInstanceOf(IllegalArgumentException.class);

        n3.addChild(n12);

        assertTreeMatches(
                root,
                """
        0
          1
            11
            13
          2
          3
            31
            12
              123""");
    }

    @Test
    void testCreateTree() {
        var tree =
                """
        0
          1
            11
            12
              123
            13
          2
          3
            31""";

        assertThat(renderTree(parseTree(tree))).matches(tree);
    }

    private static MyNode parseTree(String treeGraph) {
        return TreeUtils.parseTree(treeGraph, s -> MyNode.root(Integer.parseInt(s)));
    }

    private static String renderTree(MyNode node) {
        return TreeUtils.renderTree(node, null, n -> Integer.toString(n.id));
    }

    private static AbstractStringAssert<?> assertTreeMatches(MyNode root, String expected) {
        return assertThat(renderTree(root)).isEqualTo(expected);
    }
}
