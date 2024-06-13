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
package org.ehrbase.openehr.dbformat;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public final class StructureIndex {

    /**
     * For the cap a letter is chosen that is lexicographically larger than all employed symbols [0-9, A-Z, a-z]
     */
    public static final String CAP_SYMBOL = "~";

    /**
     * For the index delimiter a letter is chosen that is lexicographically smaller than all employed symbols [0-9, A-Z, a-z]
     */
    public static final String INDEX_DELIMITER = ".";

    public static final class Node {
        final String attribute;
        final Integer idx;

        private Node(String attribute, Integer idx) {
            this.attribute = attribute;
            this.idx = idx;
        }

        public static Node of(String attribute, Integer idx) {
            return new Node(attribute, idx);
        }

        public String getAttribute() {
            return attribute;
        }

        public Integer getIdx() {
            return idx;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            Node that = (Node) o;

            return new EqualsBuilder()
                    .append(idx, that.idx)
                    .append(attribute, that.attribute)
                    .isEquals();
        }

        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(attribute).append(idx).toHashCode();
        }

        @Override
        public String toString() {
            return "Node{'" + attribute + "' " + idx + '}';
        }
    }

    private final Node[] index;

    public StructureIndex(Node... index) {
        this.index = ArrayUtils.clone(index);
    }

    public StructureIndex createChild(Node i) {
        return new StructureIndex(ArrayUtils.add(index, i));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StructureIndex index1 = (StructureIndex) o;
        return Arrays.equals(index, index1.index);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(index);
    }

    public static StructureIndex of(Node... index) {
        return new StructureIndex(index);
    }

    public static StructureIndex of(String attribute, Integer index) {
        return new StructureIndex(new Node(attribute, index));
    }

    public Stream<Node> stream() {
        return Arrays.stream(index);
    }

    public int length() {
        return index.length;
    }

    public boolean startsWith(StructureIndex prefix) {
        Node[] pIdx = prefix.index;
        int pLen = pIdx.length;
        if (this.length() < pLen) {
            return false;
        }
        return Arrays.mismatch(index, pIdx) == pLen;
    }

    /**
     * For the cap a letter needs to be selected that is lexicographically larger than all digits.
     *
     * @param cap
     * @param withIndex
     * @return
     */
    public String printIndexString(boolean cap, boolean withIndex) {
        if (this.length() == 0) {
            return cap ? CAP_SYMBOL : "";
        } else {
            return this.stream()
                    .map(n -> getNodeString(n, withIndex))
                    .collect(Collectors.joining(
                            INDEX_DELIMITER, "", cap ? (INDEX_DELIMITER + CAP_SYMBOL) : INDEX_DELIMITER));
        }
    }

    /**
     * For the cap a letter needs to be selected that is lexicographically larger than all digits.
     *
     * @return
     */
    public String printLastAttribute() {
        if (index.length == 0) {
            return null;
        }
        var lastNode = index[index.length - 1];
        return getNodeString(lastNode, false);
    }

    private static String getNodeString(Node node, boolean withIndex) {
        String att = RmAttribute.getAlias(node.attribute);
        return (withIndex && Objects.nonNull(node.idx)) ? (att + node.idx) : att;
    }

    @Override
    public String toString() {
        return Arrays.toString(index);
    }
}
