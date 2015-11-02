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

import java.util.concurrent.TimeUnit;

import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;

/**
 * Delays the emission of onNext events by a given amount of time.
 * 
 * @param <T>
 *            the value type
 */
public final class OperatorDelay<T> implements Operator<T, T> {

    final long delay;
    final TimeUnit unit;
    final Scheduler scheduler;

    public OperatorDelay(long delay, TimeUnit unit, Scheduler scheduler) {
        this.delay = delay;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        final Worker worker = scheduler.createWorker();
        child.add(worker);
        return new Subscriber<T>(child) {
            // indicates an error cut ahead
            // accessed from the worker thread only
            boolean done;
            @Override
            public void onCompleted() {
                worker.schedule(new Action0() {

                    @Override
                    public void call() {
                        if (!done) {
                            done = true;
                            child.onCompleted();
                        }
                    }

                }, delay, unit);
            }

            @Override
            public void onError(final Throwable e) {
                worker.schedule(new Action0() {
                    @Override
                    public void call() {
                        if (!done) {
                            done = true;
                            child.onError(e);
                            worker.unsubscribe();
                        }
                    }
                });
            }

            @Override
            public void onNext(final T t) {
                worker.schedule(new Action0() {

                    @Override
                    public void call() {
                        if (!done) {
                            child.onNext(t);
                        }
                    }

                }, delay, unit);
            }

        };
    }

}
