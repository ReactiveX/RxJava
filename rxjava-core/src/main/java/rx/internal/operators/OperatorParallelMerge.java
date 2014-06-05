/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.internal.operators;


import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;

/**
 * Combines multiple {@link Observable}s into a smaller number of {@code Observable}s, to facilitate
 * parallelism.
 */
public final class OperatorParallelMerge {
    private OperatorParallelMerge() { throw new IllegalStateException("No instances!"); }

    /**
     * Recombines multiple {@link Observable}s into a smaller number of {@code Observable}s, to facilitate
     * parallelism.
     *
     * @param source
     *          the source {@code Observable} that emits the {@code Observable}s to recombine
     * @param parallelObservables
     *          the number of {@code Observable}s you want the source sequence of {@code Observables} to be
     *          recombined into
     * @return an {@code Observable} that emits no more than {@code parallelObservables} {@code Observable}s,
     *         the set of which emits the same set of items emitted by the sequence of {@code Observable}s
     *         emitted by {@code source}
     */
    public static <T> Observable<Observable<T>> parallelMerge(final Observable<Observable<T>> source, final int parallelObservables) {
        return parallelMerge(source, parallelObservables, Schedulers.immediate());
    }

    /**
     * Recombines multiple {@link Observable}s into a smaller number of {@code Observable}s on a particular
     * {@code Scheduler}, to facilitate parallelism.
     *
     * @param source
     *          the source {@code Observable} that emits the {@code Observable}s to recombine
     * @param parallelObservables
     *          the number of {@code Observable}s you want the source sequence of {@code Observables} to be
     *          recombined into
     * @param scheduler
     *          the {@link Scheduler} to do the work on
     * @return an {@code Observable} that emits no more than {@code parallelObservables} {@code Observable}s,
     *         the set of which emits the same set of items emitted by the sequence of {@code Observable}s
     *         emitted by {@code source}
     */
    public static <T> Observable<Observable<T>> parallelMerge(final Observable<Observable<T>> source, final int parallelObservables, final Scheduler scheduler) {

        return source.groupBy(new StrideMapper<T>(parallelObservables))
        .map(new Func1<GroupedObservable<Integer, Observable<T>>, Observable<T>>() {

            @Override
            public Observable<T> call(GroupedObservable<Integer, Observable<T>> o) {
                return Observable.merge(o).observeOn(scheduler);
            }

        });
    }

    /** Maps source observables in a round-robin fashion to streaming groups. */
    static final class StrideMapper<T> implements Func1<Observable<T>, Integer> {
        final int parallelObservables;
        
        volatile long rollingCount;
        @SuppressWarnings("rawtypes")
        static final AtomicLongFieldUpdater<StrideMapper> ROLLING_COUNT_UPDATER
                = AtomicLongFieldUpdater.newUpdater(StrideMapper.class, "rollingCount");

        public StrideMapper(int parallelObservables) {
            this.parallelObservables = parallelObservables;
        }
        
        @Override
        public Integer call(Observable<T> t1) {
            return (int)ROLLING_COUNT_UPDATER.incrementAndGet(this) % parallelObservables;
        }
        
    }
}
