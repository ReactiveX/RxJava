/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.operators;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import rx.Observable.Operator;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;

/**
 * Skips elements until a specified time elapses.
 * @param <T> the value type
 */
public final class OperatorSkipTimed<T> implements Operator<T, T> {
    final long time;
    final TimeUnit unit;
    final Scheduler scheduler;

    public OperatorSkipTimed(long time, TimeUnit unit, Scheduler scheduler) {
        this.time = time;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        final Worker worker = scheduler.createWorker();
        child.add(worker);
        final AtomicBoolean gate = new AtomicBoolean();
        worker.schedule(new Action0() {
            @Override
            public void call() {
                gate.set(true);
            }
        }, time, unit);
        return new Subscriber<T>(child) {

            @Override
            public void onNext(T t) {
                if (gate.get()) {
                    child.onNext(t);
                }
            }

            @Override
            public void onError(Throwable e) {
                try {
                    child.onError(e);
                } finally {
                    unsubscribe();
                }
            }

            @Override
            public void onCompleted() {
                try {
                    child.onCompleted();
                } finally {
                    unsubscribe();
                }
            }
        };
    }
}
