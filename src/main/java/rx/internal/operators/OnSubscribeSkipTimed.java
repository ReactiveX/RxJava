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
package rx.internal.operators;

import java.util.concurrent.TimeUnit;

import rx.Observable.OnSubscribe;
import rx.Observable;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Action0;

/**
 * Skips elements until a specified time elapses.
 * @param <T> the value type
 */
public final class OnSubscribeSkipTimed<T> implements OnSubscribe<T> {
    final long time;
    final TimeUnit unit;
    final Scheduler scheduler;
    final Observable<T> source;

    public OnSubscribeSkipTimed(Observable<T> source, long time, TimeUnit unit, Scheduler scheduler) {
        this.source = source;
        this.time = time;
        this.unit = unit;
        this.scheduler = scheduler;
    }

    @Override
    public void call(final Subscriber<? super T> child) {
        final Worker worker = scheduler.createWorker();
        SkipTimedSubscriber<T> subscriber = new SkipTimedSubscriber<T>(child);
        subscriber.add(worker);
        child.add(subscriber);
        worker.schedule(subscriber, time, unit);
        source.unsafeSubscribe(subscriber);
    }

    final static class SkipTimedSubscriber<T> extends Subscriber<T> implements Action0 {

        final Subscriber<? super T> child;
        volatile boolean gate;

        SkipTimedSubscriber(Subscriber<? super T> child) {
            this.child = child;
        }

        @Override
        public void call() {
            gate = true;
        }

        @Override
        public void onNext(T t) {
            if (gate) {
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

    }
}
