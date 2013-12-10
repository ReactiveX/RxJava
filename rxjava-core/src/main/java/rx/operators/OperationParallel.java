/**
 * Copyright 2013 Netflix, Inc.
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
package rx.operators;

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Scheduler;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

/**
 * Identifies unit of work that can be executed in parallel on a given Scheduler.
 */
public final class OperationParallel<T> {

    public static <T, R> Observable<R> parallel(Observable<T> source, Func1<Observable<T>, Observable<R>> f) {
        return parallel(source, f, Schedulers.threadPoolForComputation());
    }

    public static <T, R> Observable<R> parallel(final Observable<T> source, final Func1<Observable<T>, Observable<R>> f, final Scheduler s) {
        return Observable.defer(new Func0<Observable<R>>() {

            @Override
            public Observable<R> call() {
                final AtomicInteger i = new AtomicInteger(0);
                return source.groupBy(new Func1<T, Integer>() {

                    @Override
                    public Integer call(T t) {
                        return i.incrementAndGet() % s.degreeOfParallelism();
                    }

                }).flatMap(new Func1<GroupedObservable<Integer, T>, Observable<R>>() {

                    @Override
                    public Observable<R> call(GroupedObservable<Integer, T> group) {
                        return f.call(group.observeOn(s));
                    }
                }).synchronize();
            }
        });
    }
}
