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
package org.ehrbase.openehr.aqlengine.pathanalysis;

import java.util.Set;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.path.AqlObjectPath;

public class PathQueryDescriptor {

    public Set<String> getRmType() {
        return rmType;
    }

    public enum PathType {
        // Represents an extracted column (always a leaf)
        EXTRACTED,
        // Navigation to a structure node
        STRUCTURE,
        // Special case for element
        ITEM,
        // Json object (only leaf)
        OBJECT,
        // primitive value (only leaf)
        PRIMITIVE
    }

    private final ContainmentClassExpression root;
    private final PathQueryDescriptor parent;
    private final AqlObjectPath representedPath;
    private final PathType type;
    private final Set<String> rmType;

    public PathQueryDescriptor(
            ContainmentClassExpression root,
            PathQueryDescriptor parent,
            AqlObjectPath representedPath,
            PathType type,
            Set<String> rmType) {
        this.root = root;
        this.parent = parent;
        this.representedPath = representedPath;
        this.type = type;
        this.rmType = rmType;
    }

    public ContainmentClassExpression getRoot() {
        return root;
    }

    public PathQueryDescriptor getParent() {
        return parent;
    }

    public AqlObjectPath getRepresentedPath() {
        return representedPath;
    }

    public PathType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "PathQueryDescriptor{" + "root="
                + root + ", parent="
                + parent + ", representedPath="
                + representedPath + ", type="
                + type + ", aliasedRmType="
                + rmType + '}';
    }
}
