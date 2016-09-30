/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.*;
import rx.functions.Action0;
import rx.plugins.RxJavaHooks;

public final class SingleTimeout<T> implements Single.OnSubscribe<T> {

    final Single.OnSubscribe<T> source;

    final long timeout;

    final TimeUnit unit;

    final Scheduler scheduler;

    final Single.OnSubscribe<? extends T> other;

    public SingleTimeout(Single.OnSubscribe<T> source, long timeout, TimeUnit unit, Scheduler scheduler,
            Single.OnSubscribe<? extends T> other) {
        this.source = source;
        this.timeout = timeout;
        this.unit = unit;
        this.scheduler = scheduler;
        this.other = other;
    }

    @Override
    public void call(SingleSubscriber<? super T> t) {
        TimeoutSingleSubscriber<T> parent = new TimeoutSingleSubscriber<T>(t, other);

        Scheduler.Worker w = scheduler.createWorker();
        parent.add(w);

        t.add(parent);

        w.schedule(parent, timeout, unit);

        source.call(parent);
    }

    static final class TimeoutSingleSubscriber<T> extends SingleSubscriber<T> implements Action0 {

        final SingleSubscriber<? super T> actual;

        final AtomicBoolean once;

        final Single.OnSubscribe<? extends T> other;

        TimeoutSingleSubscriber(SingleSubscriber<? super T> actual, Single.OnSubscribe<? extends T> other) {
            this.actual = actual;
            this.other = other;
            this.once = new AtomicBoolean();
        }

        @Override
        public void onSuccess(T value) {
            if (once.compareAndSet(false, true)) {
                try {
                    actual.onSuccess(value);
                } finally {
                    unsubscribe();
                }
            }
        }

        @Override
        public void onError(Throwable error) {
            if (once.compareAndSet(false, true)) {
                try {
                    actual.onError(error);
                } finally {
                    unsubscribe();
                }
            } else {
                RxJavaHooks.onError(error);
            }
        }

        @Override
        public void call() {
            if (once.compareAndSet(false, true)) {
                try {
                    Single.OnSubscribe<? extends T> o = other;

                    if (o == null) {
                        actual.onError(new TimeoutException());
                    } else {
                        OtherSubscriber<T> p = new OtherSubscriber<T>(actual);
                        actual.add(p);
                        o.call(p);
                    }
                } finally {
                    unsubscribe();
                }
            }
        }

        static final class OtherSubscriber<T> extends SingleSubscriber<T> {

            final SingleSubscriber<? super T> actual;

            OtherSubscriber(SingleSubscriber<? super T> actual) {
                this.actual = actual;
            }

            @Override
            public void onSuccess(T value) {
                actual.onSuccess(value);
            }

            @Override
            public void onError(Throwable error) {
                actual.onError(error);
            }
        }
    }
}
