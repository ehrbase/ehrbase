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
package org.ehrbase.openehr.aqlengine.querywrapper.contains;

import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;

public final class VersionContainsWrapper implements ContainsWrapper {
    private final String alias;
    private final RmContainsWrapper child;
    private ContainsWrapper parent;

    public VersionContainsWrapper(String alias, RmContainsWrapper child) {
        this.alias = alias;
        this.child = child;
    }

    @Override
    public String getRmType() {
        return RmConstants.ORIGINAL_VERSION;
    }

    @Override
    public String alias() {
        return alias;
    }

    @Override
    public boolean isAtCode() {
        return false;
    }

    @Override
    public boolean isArchetype() {
        return false;
    }

    @Override
    public ContainsWrapper getParent() {
        return parent;
    }

    @Override
    public void setParent(ContainsWrapper parent) {
        this.parent = parent;
    }

    public RmContainsWrapper child() {
        return child;
    }

    @Override
    public String toString() {
        return "VersionContainsWrapper[" + "alias=" + alias + ", " + "child=" + child + ']';
    }
}
