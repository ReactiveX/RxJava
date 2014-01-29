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
package rx.operators;

import rx.Observable;
import rx.Subscriber;
import rx.Scheduler;
import rx.observables.GroupedObservable;
import rx.util.functions.Func1;

/**
 * Identifies unit of work that can be executed in parallel on a given Scheduler.
 */
public final class OperatorParallel<T, R> implements Operator<R, T> {

    private final Scheduler scheduler;
    private final Func1<Observable<T>, Observable<R>> f;

    public OperatorParallel(Func1<Observable<T>, Observable<R>> f, Scheduler scheduler) {
        this.scheduler = scheduler;
        this.f = f;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super R> op) {

        Func1<Subscriber<? super GroupedObservable<Integer, T>>, Subscriber<? super T>> groupBy =
                new OperatorGroupBy<Integer, T>(new Func1<T, Integer>() {

                    int i = 0;

                    @Override
                    public Integer call(T t) {
                        return i++ % scheduler.degreeOfParallelism();
                    }

                });

        Func1<Subscriber<? super Observable<R>>, Subscriber<? super GroupedObservable<Integer, T>>> map =
                new OperatorMap<GroupedObservable<Integer, T>, Observable<R>>(
                        new Func1<GroupedObservable<Integer, T>, Observable<R>>() {

                            @Override
                            public Observable<R> call(GroupedObservable<Integer, T> g) {
                                return f.call(g.observeOn(scheduler));
                            }
                        });

        // bind together Observers
        return groupBy.call(map.call(new OperatorMerge<R>().call(op)));
    }
}
