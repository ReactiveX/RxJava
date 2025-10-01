/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.testsupport;

import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * A test utility for verifying whether an object instance is reclaimable.
 *
 * @param <R> the type of referent
 */
public final class Reclaimable<R> {

    private static final ReclamationOps OPS = new ReclamationOps();

    private final String name;
    private R referent;
    private SoftReference<R> softReference;

    private Reclaimable(String name, R referent) {
        this.name = name;
        this.referent = referent;
    }

    /**
     * Constructs a {@code Reclaimable} instance with the specified {@code referent}.
     *
     * @param referent the object to be tested for reclaimability
     * @param <R> the type of referent
     * @return the new {@code Reclaimable} instance
     * @throws NullPointerException if {@code referent} is {@code null}
     */
    @NonNull
    public static <R> Reclaimable<R> of(@NonNull R referent) {
        Objects.requireNonNull(referent, "referent is null");
        return of(referent.toString(), referent);
    }

    /**
     * Constructs a {@code Reclaimable} instance with the specified {@code name} and {@code referent}.
     *
     * @param name a human-readable name for the {@code referent}. Must not be {@code null}.
     * @param referent the object to be tested for reclaimability
     * @param <R> the type of referent
     * @return the new {@code Reclaimable} instance
     * @throws NullPointerException if any of {@code name} or {@code referent} is {@code null}
     */
    @NonNull
    public static <R> Reclaimable<R> of(@NonNull String name, @NonNull R referent) {
        Objects.requireNonNull(name, "name is null");
        Objects.requireNonNull(referent, "referent is null");
        return new Reclaimable<>(name, referent);
    }

    /**
     * Transitions the referent from a strong reference to a soft reference and returns the referent. This method must
     * be invoked exactly once in order for the referent to become eligible for garbage collection.
     *
     * @return the non-{@code null} referent.
     * @throws IllegalStateException if the referent was already removed
     */
    @NonNull
    public R remove() {
        R r = referent;
        if (r == null) {
            throw new IllegalStateException("referent was already removed");
        }
        this.softReference = new SoftReference<>(r);
        this.referent = null;
        return r;
    }

    /**
     * Tests whether the referent associated with this {@code Reclaimable} instance has been successfully reclaimed by
     * the garbage collector. This method always returns {@code false} if {@link #remove()} has not yet been
     * invoked.
     *
     * @return {@code true} if the referent has been reclaimed
     */
    public boolean isReclaimed() {
        return softReference != null && softReference.get() == null;
    }

    boolean isRemoved() {
        return softReference != null;
    }

    /**
     * Fills the heap, forcing the collection of any softly-reachable objects.
     * <p>
     * Taken from the javadoc of {@link SoftReference}:
     * <blockquote>
     * All soft references to softly-reachable objects are guaranteed to have been cleared before the virtual machine
     * throws an OutOfMemoryError.
     * </blockquote>
     *
     * @return a set of fluent operators that can be used to verify the reclamation status of {@code Reclaimable} instances
     */
    @NonNull
    public static ReclamationOps forceGC() {
        try {
            List<long[]> list = new LinkedList<>();
            for (;;) {
                // fill the heap, 8 Gb at a time
                list.add(new long[1024 * 1024 * 1024]);
            }
        } catch (OutOfMemoryError ex) {
            // softly-reachable objects are now guaranteed to be collected
            return OPS;
        }
    }

    @Override
    public String toString() {
        return "Reclaimable(name=" + name + ", referent=" +  referent + ")";
    }

    /**
     * A set of fluent operators that can be used to verify the reclamation status of {@code Reclaimable} instances.
     */
    public static final class ReclamationOps {
        /**
         * Verifies that the referent held in {@code reclaimable} has been successfully reclaimed by the garbage
         * collector.
         *
         * @param reclaimable the {@code Reclaimable} whose {@code referent} is expected to have been reclaimed
         * @return this
         */
        @NonNull
        public ReclamationOps assertReclaimed(@NonNull Reclaimable<?> reclaimable) {
            Objects.requireNonNull(reclaimable, "reclaimable is null");
            if (!reclaimable.isRemoved()) {
                throw new IllegalStateException("referent has not been removed: " + reclaimable);
            }
            if (!reclaimable.isReclaimed()) {
                throw new AssertionError("expected referent to be reclaimed: " + reclaimable);
            }
            return this;
        }

        /**
         * Verifies that the referents held in the {@code reclaimables} have been successfully reclaimed by the garbage
         * collector.
         *
         * @param reclaimables the {@code Reclaimable} instances whose referents are expected to have been reclaimed
         * @return this
         */
        @NonNull
        public ReclamationOps assertAllReclaimed(@NonNull Iterable<? extends Reclaimable<?>> reclaimables) {
            Objects.requireNonNull(reclaimables, "reclaimables is null");
            for (Reclaimable<?> reclaimable : reclaimables) {
                assertReclaimed(reclaimable);
            }
            return this;
        }

        /**
         * Verifies that the referent held in {@code reclaimable} has <em>not</em> been reclaimed by the garbage
         * collector.
         *
         * @param reclaimable the {@code Reclaimable} whose {@code referent} is expected to <em>not</em> have been reclaimed
         * @return this
         */
        @NonNull
        public ReclamationOps assertUnreclaimed(@NonNull Reclaimable<?> reclaimable) {
            Objects.requireNonNull(reclaimable, "reclaimable is null");
            reclaimable.isRemoved();
            if (reclaimable.isReclaimed()) {
                throw new AssertionError("expected referent to NOT be reclaimed: " + reclaimable);
            }
            return this;
        }

        /**
         * Verifies that the referents held in the {@code reclaimables} have <em>not</em> been reclaimed by the garbage
         * collector.
         *
         * @param reclaimables the {@code Reclaimable} instances whose referents are expected to <em>not</em> have been reclaimed
         * @return this
         */
        @NonNull
        public ReclamationOps assertAllUnreclaimed(@NonNull Iterable<? extends Reclaimable<?>> reclaimables) {
            Objects.requireNonNull(reclaimables, "reclaimables is null");
            for (Reclaimable<?> reclaimable : reclaimables) {
                assertUnreclaimed(reclaimable);
            }
            return this;
        }
    }
}
