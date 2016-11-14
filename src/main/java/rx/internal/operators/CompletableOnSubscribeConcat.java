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

import java.util.concurrent.atomic.*;

import rx.*;
import rx.Completable.OnSubscribe;
import rx.exceptions.MissingBackpressureException;
import rx.internal.subscriptions.SequentialSubscription;
import rx.internal.util.unsafe.SpscArrayQueue;
import rx.plugins.RxJavaHooks;

public final class CompletableOnSubscribeConcat implements OnSubscribe {
    final Observable<Completable> sources;
    final int prefetch;

    @SuppressWarnings("unchecked")
    public CompletableOnSubscribeConcat(Observable<? extends Completable> sources, int prefetch) {
        this.sources = (Observable<Completable>)sources;
        this.prefetch = prefetch;
    }

    @Override
    public void call(CompletableSubscriber s) {
        CompletableConcatSubscriber parent = new CompletableConcatSubscriber(s, prefetch);
        s.onSubscribe(parent);
        sources.unsafeSubscribe(parent);
    }

    static final class CompletableConcatSubscriber
    extends Subscriber<Completable> {
        final CompletableSubscriber actual;
        final SequentialSubscription sr;

        final SpscArrayQueue<Completable> queue;

        final ConcatInnerSubscriber inner;

        final AtomicBoolean once;

        volatile boolean done;

        volatile boolean active;

        public CompletableConcatSubscriber(CompletableSubscriber actual, int prefetch) {
            this.actual = actual;
            this.queue = new SpscArrayQueue<Completable>(prefetch);
            this.sr = new SequentialSubscription();
            this.inner = new ConcatInnerSubscriber();
            this.once = new AtomicBoolean();
            add(sr);
            request(prefetch);
        }

        @Override
        public void onNext(Completable t) {
            if (!queue.offer(t)) {
                onError(new MissingBackpressureException());
                return;
            }
            drain();
        }

        @Override
        public void onError(Throwable t) {
            if (once.compareAndSet(false, true)) {
                actual.onError(t);
                return;
            }
            RxJavaHooks.onError(t);
        }

        @Override
        public void onCompleted() {
            if (done) {
                return;
            }
            done = true;
            drain();
        }

        void innerError(Throwable e) {
            unsubscribe();
            onError(e);
        }

        void innerComplete() {
            active = false;
            drain();
        }

        void drain() {
            ConcatInnerSubscriber inner = this.inner;
            if (inner.getAndIncrement() != 0) {
                return;
            }

            do {
                if (isUnsubscribed()) {
                    return;
                }
                if (!active) {
                    boolean d = done;
                    Completable c = queue.poll();
                    boolean empty = c == null;

                    if (d && empty) {
                        actual.onCompleted();
                        return;
                    }

                    if (!empty) {
                        active = true;
                        c.subscribe(inner);

                        request(1);
                    }
                }
            } while (inner.decrementAndGet() != 0);
        }

        final class ConcatInnerSubscriber
        extends AtomicInteger
        implements CompletableSubscriber {
            private static final long serialVersionUID = 7233503139645205620L;

            @Override
            public void onSubscribe(Subscription d) {
                sr.set(d);
            }

            @Override
            public void onError(Throwable e) {
                innerError(e);
            }

            @Override
            public void onCompleted() {
                innerComplete();
            }
        }
    }
}
