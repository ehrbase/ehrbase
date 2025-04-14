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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.ContainsWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.RmContainsWrapper;
import org.ehrbase.openehr.aqlengine.querywrapper.contains.VersionContainsWrapper;
import org.ehrbase.openehr.sdk.aql.dto.containment.ContainmentClassExpression;
import org.ehrbase.openehr.sdk.aql.dto.operand.IdentifiedPath;
import org.ehrbase.openehr.sdk.aql.dto.path.ComparisonOperatorPredicate;
import org.ehrbase.openehr.sdk.util.rmconstants.RmConstants;

/**
 * Contains backtracking information for the original AQL query types.
 */
public abstract sealed class AslTypeOrigin {

    /**
     * Factory method to create a new type {@link AslTypeOrigin} like an <code>EHR, COMPOSITION, ACTION</code> for the
     * given {@link ContainsWrapper}.
     * @param containsWrapper used to extract the alias and type information for.
     * @return origin
     */
    public static AslTypeOrigin ofContainsWrapper(ContainsWrapper containsWrapper) {
        return switch (containsWrapper) {
            case RmContainsWrapper rmContainsWrapper -> ofRmContains(rmContainsWrapper.alias(), rmContainsWrapper);
            case VersionContainsWrapper versionContainsWrapper -> ofRmContains(
                    containsWrapper.alias(), versionContainsWrapper.child());
        };
    }

    private static AslRmTypeOrigin ofRmContains(String alias, RmContainsWrapper containsWrapper) {

        List<IdentifiedPath> identifiedPaths = List.of();
        ContainmentClassExpression containment = containsWrapper.containment();

        // Convert containment into identified paths expression to be cary this information as the query origin
        if (containment.hasPredicates()) {
            identifiedPaths = containment.getPredicates().stream()
                    .flatMap(predicate -> {
                        List<ComparisonOperatorPredicate> operands = predicate.getOperands();
                        return operands.stream().map(comparisonOperatorPredicate -> {
                            IdentifiedPath identifiedPath = new IdentifiedPath();
                            identifiedPath.setRoot(containment);
                            identifiedPath.setRootPredicate(List.of(predicate));
                            identifiedPath.setPath(comparisonOperatorPredicate.getPath());
                            return identifiedPath;
                        });
                    })
                    .toList();
        }
        return new AslRmTypeOrigin(alias, containsWrapper.getRmType(), identifiedPaths);
    }

    /** AQL alias */
    protected final String alias;
    /** AQL type */
    protected final String rmType;
    /** AQL fields this type is used to query for */
    protected List<IdentifiedPath> fieldPaths;

    protected AslTypeOrigin(@Nullable String alias, @Nonnull String rmType, @Nonnull List<IdentifiedPath> fieldPaths) {
        this.alias = alias;
        this.rmType = rmType;
        this.fieldPaths = fieldPaths;
    }

    /**
     * AQL alias of this type
     */
    @Nullable
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
     * Add the given <code>path</code> to the fields paths
     * @param path to add
     */
    public void addPath(IdentifiedPath... path) {
        this.fieldPaths =
                Stream.concat(fieldPaths.stream(), Arrays.stream(path)).toList();
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
