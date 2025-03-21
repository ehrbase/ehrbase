/*
 * Copyright (c) 2019-2025 vitasystems GmbH.
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
package org.ehrbase.openehr.aqlengine.asl.meta;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.RmContainsWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.VersionContainsWrapper;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;

/**
 * Contains backtracking information for the original AQL query types.
 */
public abstract sealed class AslTypeOrigin permits AslTypeOrigin.AslRmTypeOrigin, AslTypeOrigin.AslVersionTypeOrigin {

    /**
     * Factory method to create a new type {@link AslTypeOrigin} like an <code>EHR, COMPOSITION, ACTION</code> for the
     * given {@link ContainsWrapper}.
     * @param containsWrapper used to extract the alias an type information for.
     * @return origin
     */
    public static AslTypeOrigin ofContainsWrapper(ContainsWrapper containsWrapper) {
        return switch (containsWrapper) {
            case RmContainsWrapper rmContainsWrapper -> new AslRmTypeOrigin(
                    containsWrapper.alias(), containsWrapper.getRmType(), List.of());
            case VersionContainsWrapper versionContainsWrapper -> new AslVersionTypeOrigin(new AslRmTypeOrigin(
                    containsWrapper.alias(), versionContainsWrapper.child().getRmType(), List.of()));
        };
    }

    /** AQL alias */
    protected final String alias;
    /** AQL type */
    protected final String rmType;
    /** AQL fields this type is used to query for */
    protected final List<IdentifiedPath> fieldPaths;

    public AslTypeOrigin(@Nonnull String alias, @Nonnull String rmType, @Nonnull List<IdentifiedPath> fieldPaths) {
        this.alias = alias;
        this.rmType = rmType;
        this.fieldPaths = fieldPaths;
    }

    /**
     * AQL alias of this type
     */
    @Nonnull
    public String getAlias() {
        return alias;
    }

    /**
     * AQL RM-Type
     */
    @Nonnull
    public String getRmType() {
        return rmType;
    }

    /**
     * AQL Field of this type
     */
    @Nonnull
    public List<IdentifiedPath> getFieldPaths() {
        return fieldPaths;
    }

    /**
     * Creates a new immutable instance of an {@link AslTypeOrigin} with the given field <code>paths</code>
     */
    public abstract AslTypeOrigin withPaths(@Nonnull List<IdentifiedPath> paths);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[alias=" + alias + ", rmType=" + rmType + ", fieldPaths="
                + fieldPaths.stream().map(IdentifiedPath::render).collect(Collectors.joining(",", "", "")) + ']';
    }

    /**
     * Concrete {@link AslTypeOrigin} for a direct AQL RM-Type
     */
    public static final class AslRmTypeOrigin extends AslTypeOrigin {

        public AslRmTypeOrigin(
                @Nonnull String alias, @Nonnull String rmType, @Nonnull List<IdentifiedPath> fieldPaths) {
            super(alias, rmType, fieldPaths);
        }

        @Override
        public AslTypeOrigin withPaths(@Nonnull List<IdentifiedPath> paths) {
            return new AslRmTypeOrigin(alias, rmType, paths);
        }
    }

    /**
     * Concrete {@link AslTypeOrigin} for a versioned AQL RM-Type
     */
    public static final class AslVersionTypeOrigin extends AslTypeOrigin {

        private final AslRmTypeOrigin rmTypeOrigin;

        public AslVersionTypeOrigin(AslRmTypeOrigin rmTypeOrigin) {
            super(rmTypeOrigin.alias, RmConstants.ORIGINAL_VERSION, rmTypeOrigin.fieldPaths);
            this.rmTypeOrigin = rmTypeOrigin;
        }

        @Override
        public AslTypeOrigin withPaths(@Nonnull List<IdentifiedPath> paths) {
            return new AslVersionTypeOrigin(new AslRmTypeOrigin(alias, rmType, paths));
        }

        /**
         * Provides the underlying AQL RM-Type origin
         */
        @Nonnull
        public AslRmTypeOrigin getRmTypeOrigin() {
            return rmTypeOrigin;
        }
    }
}
