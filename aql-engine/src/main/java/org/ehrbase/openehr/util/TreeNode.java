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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A simple base for creating tree structures.
 *
 * @param <T>
 */
public abstract class TreeNode<T extends TreeNode<T>> {

    protected T parent;
    final List<T> children = new ArrayList<>();
    protected int depth = 0;

    public T getParent() {
        return parent;
    }

    /**
     *
     * Add the child to children.
     * If it is already contained, nothing is changed.
     * If it already has a parent, it is removed from the parent.
     *
     * In order to keep the tree acyclic, a IllegalArgumentException is thrown if the child ia an ancestor of this node.
     *
     * @param child
     * @return
     */
    protected T addChild(T child) {
        if (child.parent == this) {
            return child;
        }

        if (!child.children.isEmpty()) {
            // check ancestors
            var a = this.parent;
            while (a != null) {
                if (a == child) {
                    throw new IllegalArgumentException("The child is an ancestor of the current node");
                }
                a = a.parent;
            }
        }

        child.removeFromParent();

        child.parent = (T) this;
        child.depth = depth + 1;
        children.add(child);
        return child;
    }

    public int getDepth() {
        return depth;
    }

    public List<T> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void sortChildren(Comparator<T> comparator) {
        children.sort(comparator);
    }

    void removeFromParent() {
        if (parent != null) {
            parent.children.remove(this);
            parent = null;
        }
    }

    public Stream<T> streamDepthFirst() {
        return Stream.of(Stream.of((T) this), getChildren().stream().flatMap(TreeNode::streamDepthFirst))
                .flatMap(s -> s);
    }
}
