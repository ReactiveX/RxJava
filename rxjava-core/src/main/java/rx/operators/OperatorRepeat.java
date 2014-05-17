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
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.Subscribers;
import rx.schedulers.Schedulers;

public class OperatorRepeat<T> implements Operator<T, Observable<T>> {

    private final Scheduler scheduler;
    private final long count;

    public OperatorRepeat(long count, Scheduler scheduler) {
        this.scheduler = scheduler;
        this.count = count;
    }

    public OperatorRepeat(Scheduler scheduler) {
        this(-1, scheduler);
    }

    public OperatorRepeat(long count) {
        this(count, Schedulers.trampoline());
    }

    public OperatorRepeat() {
        this(-1, Schedulers.trampoline());
    }

    @Override
    public Subscriber<? super Observable<T>> call(final Subscriber<? super T> child) {
        if (count == 0) {
            child.onCompleted();
            return Subscribers.empty();
        }
        return new Subscriber<Observable<T>>(child) {

            int executionCount = 0;

            @Override
            public void onCompleted() {
                // ignore as we will keep repeating
            }

            @Override
            public void onError(Throwable e) {
                // we should never receive this but if we do we pass it on
                child.onError(new IllegalStateException("Error received on nested Observable.", e));
            }

            @Override
            public void onNext(final Observable<T> t) {
                // will only be invoked once since we're nested
                final Worker inner = scheduler.createWorker();
                // cleanup on unsubscribe
                add(inner);
                inner.schedule(new Action0() {

                    final Action0 self = this;

                    @Override
                    public void call() {
                        executionCount++;
                        t.unsafeSubscribe(new Subscriber<T>(child) {

                            @Override
                            public void onCompleted() {
                                if (count == -1 || executionCount < count) {
                                    inner.schedule(self);
                                } else {
                                    child.onCompleted();
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                child.onError(e);
                            }

                            @Override
                            public void onNext(T t) {
                                child.onNext(t);
                            }

                        });
                    }

                });
            }

        };
    }
}