/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2024 Stephan Pauxberger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.blackbuild.annodocimal.generator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable inclusion policy for source projection.
 *
 * <p>Every policy retains signature closure and valid Java output. Those safety rules are not configurable.</p>
 */
@NullMarked
public final class ProjectionPolicy {

    private static final ProjectionPolicy DOCUMENTATION = new Builder()
            .includedVisibilities(EnumSet.of(DeclarationVisibility.PUBLIC, DeclarationVisibility.PROTECTED))
            .includeNestedDeclarations(true)
            .includeSyntheticDeclarations(false)
            .includeGroovyRuntimeArtifacts(false)
            .build();

    private final Set<DeclarationVisibility> includedVisibilities;
    private final boolean nestedDeclarationsIncluded;
    private final boolean syntheticDeclarationsIncluded;
    private final boolean groovyRuntimeArtifactsIncluded;

    private ProjectionPolicy(Builder builder) {
        EnumSet<DeclarationVisibility> visibilities = builder.includedVisibilities.isEmpty()
                ? EnumSet.noneOf(DeclarationVisibility.class)
                : EnumSet.copyOf(builder.includedVisibilities);
        includedVisibilities = Collections.unmodifiableSet(visibilities);
        nestedDeclarationsIncluded = builder.nestedDeclarationsIncluded;
        syntheticDeclarationsIncluded = builder.syntheticDeclarationsIncluded;
        groovyRuntimeArtifactsIncluded = builder.groovyRuntimeArtifactsIncluded;
    }

    /**
     * Returns the documentation-oriented preset: public and protected declarations, named nested declarations,
     * signature closure, and no synthetic or Groovy runtime scaffolding.
     *
     * @return the reusable documentation policy
     */
    public static ProjectionPolicy documentation() {
        return DOCUMENTATION;
    }

    /**
     * Creates a mutable builder initialized from {@link #documentation()}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return DOCUMENTATION.toBuilder();
    }

    /**
     * Creates a mutable builder initialized from this policy.
     *
     * @return a new independent builder
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Returns the member visibility levels selected by this policy. The projection root is always selected.
     *
     * @return an immutable visibility set
     */
    public Set<DeclarationVisibility> getIncludedVisibilities() {
        return includedVisibilities;
    }

    /**
     * Returns whether otherwise-selected named member types are recursively projected.
     *
     * <p>Named members required by signature closure remain included. Local and anonymous classes are never included.</p>
     *
     * @return whether named nested declarations are included
     */
    public boolean isNestedDeclarationsIncluded() {
        return nestedDeclarationsIncluded;
    }

    /**
     * Returns whether otherwise-selected synthetic declarations are included.
     *
     * @return whether synthetic declarations are included
     */
    public boolean isSyntheticDeclarationsIncluded() {
        return syntheticDeclarationsIncluded;
    }

    /**
     * Returns whether Groovy runtime interfaces, metadata accessors, and similarly named scaffolding are included.
     * Visible language-level APIs such as property accessors do not count as runtime scaffolding.
     *
     * @return whether Groovy runtime artifacts are included
     */
    public boolean isGroovyRuntimeArtifactsIncluded() {
        return groovyRuntimeArtifactsIncluded;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) return true;
        if (!(other instanceof ProjectionPolicy)) return false;
        ProjectionPolicy that = (ProjectionPolicy) other;
        return nestedDeclarationsIncluded == that.nestedDeclarationsIncluded
                && syntheticDeclarationsIncluded == that.syntheticDeclarationsIncluded
                && groovyRuntimeArtifactsIncluded == that.groovyRuntimeArtifactsIncluded
                && includedVisibilities.equals(that.includedVisibilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(includedVisibilities, nestedDeclarationsIncluded, syntheticDeclarationsIncluded,
                groovyRuntimeArtifactsIncluded);
    }

    @Override
    public String toString() {
        return "ProjectionPolicy{" +
                "includedVisibilities=" + includedVisibilities +
                ", nestedDeclarationsIncluded=" + nestedDeclarationsIncluded +
                ", syntheticDeclarationsIncluded=" + syntheticDeclarationsIncluded +
                ", groovyRuntimeArtifactsIncluded=" + groovyRuntimeArtifactsIncluded +
                '}';
    }

    /**
     * Mutable, non-thread-safe builder for independent {@link ProjectionPolicy} snapshots.
     */
    public static final class Builder {
        private EnumSet<DeclarationVisibility> includedVisibilities =
                EnumSet.of(DeclarationVisibility.PUBLIC, DeclarationVisibility.PROTECTED);
        private boolean nestedDeclarationsIncluded = true;
        private boolean syntheticDeclarationsIncluded;
        private boolean groovyRuntimeArtifactsIncluded;

        private Builder() {
        }

        private Builder(ProjectionPolicy policy) {
            includedVisibilities = policy.includedVisibilities.isEmpty()
                    ? EnumSet.noneOf(DeclarationVisibility.class)
                    : EnumSet.copyOf(policy.includedVisibilities);
            nestedDeclarationsIncluded = policy.nestedDeclarationsIncluded;
            syntheticDeclarationsIncluded = policy.syntheticDeclarationsIncluded;
            groovyRuntimeArtifactsIncluded = policy.groovyRuntimeArtifactsIncluded;
        }

        /**
         * Replaces the selected member visibility levels. An empty collection selects no members except declarations
         * required by signature closure.
         *
         * @param visibilities visibility levels to select
         * @return this builder
         */
        public Builder includedVisibilities(Collection<DeclarationVisibility> visibilities) {
            Objects.requireNonNull(visibilities, "visibilities");
            EnumSet<DeclarationVisibility> copy = EnumSet.noneOf(DeclarationVisibility.class);
            visibilities.forEach(visibility -> copy.add(Objects.requireNonNull(visibility, "visibility")));
            includedVisibilities = copy;
            return this;
        }

        /**
         * Controls recursive inclusion of otherwise-selected named member types.
         *
         * @param include whether to include named nested declarations
         * @return this builder
         */
        public Builder includeNestedDeclarations(boolean include) {
            nestedDeclarationsIncluded = include;
            return this;
        }

        /**
         * Controls inclusion of otherwise-selected synthetic declarations. Projection still fails rather than emit
         * invalid Java when selected bytecode declarations cannot coexist in source.
         *
         * @param include whether to include synthetic declarations
         * @return this builder
         */
        public Builder includeSyntheticDeclarations(boolean include) {
            syntheticDeclarationsIncluded = include;
            return this;
        }

        /**
         * Controls inclusion of Groovy runtime scaffolding independently of language-level generated APIs.
         *
         * @param include whether to include Groovy runtime artifacts
         * @return this builder
         */
        public Builder includeGroovyRuntimeArtifacts(boolean include) {
            groovyRuntimeArtifactsIncluded = include;
            return this;
        }

        /**
         * Creates an immutable policy snapshot.
         *
         * @return a new policy value
         */
        public ProjectionPolicy build() {
            return new ProjectionPolicy(this);
        }
    }
}
