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

import java.util.Arrays;
import java.util.concurrent.atomic.*;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.Exceptions;
import rx.functions.FuncN;
import rx.observers.SerializedSubscriber;
import rx.plugins.RxJavaHooks;

public final class OperatorWithLatestFromMany<T, R> implements OnSubscribe<R> {
    final Observable<T> main;

    final Observable<?>[] others;

    final Iterable<Observable<?>> othersIterable;

    final FuncN<R> combiner;

    public OperatorWithLatestFromMany(Observable<T> main, Observable<?>[] others, Iterable<Observable<?>> othersIterable, FuncN<R> combiner) {
        this.main = main;
        this.others = others;
        this.othersIterable = othersIterable;
        this.combiner = combiner;
    }

    @Override
    public void call(Subscriber<? super R> t) {
        SerializedSubscriber<R> serial = new SerializedSubscriber<R>(t);


        Observable<?>[] sources;
        int n = 0;

        if (others != null) {
            sources = others;
            n = sources.length;
        } else {
            sources = new Observable[8];
            for (Observable<?> o : othersIterable) {
                if (n == sources.length) {
                    sources = Arrays.copyOf(sources, n + (n >> 2));
                }
                sources[n++] = o;
            }
        }

        WithLatestMainSubscriber<T, R> parent = new WithLatestMainSubscriber<T, R>(t, combiner, n);

        serial.add(parent);


        for (int i = 0; i < n; i++) {
            if (serial.isUnsubscribed()) {
                return;
            }

            WithLatestOtherSubscriber inner = new WithLatestOtherSubscriber(parent, i + 1);
            parent.add(inner);

            Observable<?> o = sources[i];
            o.unsafeSubscribe(inner);
        }

        main.unsafeSubscribe(parent);
    }

    static final class WithLatestMainSubscriber<T, R> extends Subscriber<T> {
        final Subscriber<? super R> actual;

        final FuncN<R> combiner;

        final AtomicReferenceArray<Object> current;

        static final Object EMPTY = new Object();

        final AtomicInteger ready;

        boolean done;

        public WithLatestMainSubscriber(Subscriber<? super R> actual, FuncN<R> combiner, int n) {
            this.actual = actual;
            this.combiner = combiner;

            AtomicReferenceArray<Object> array = new AtomicReferenceArray<Object>(n + 1);
            for (int i = 0; i <= n; i++) {
                array.lazySet(i, EMPTY);
            }
            this.current = array;

            this.ready = new AtomicInteger(n);
            this.request(0);
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            if (ready.get() == 0) {

                AtomicReferenceArray<Object> array = current;
                int n = array.length();
                array.lazySet(0, t);

                Object[] copy = new Object[array.length()];
                for (int i = 0; i < n; i++) {
                    copy[i] = array.get(i);
                }

                R result;

                try {
                    result = combiner.call(copy);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    onError(ex);
                    return;
                }

                actual.onNext(result);
            } else {
                request(1);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaHooks.onError(e);
                return;
            }
            done = true;
            unsubscribe();
            actual.onError(e);
        }

        @Override
        public void onCompleted() {
            if (done) {
                return;
            }
            done = true;
            unsubscribe();
            actual.onCompleted();
        }

        @Override
        public void setProducer(Producer p) {
            super.setProducer(p);
            actual.setProducer(p);
        }

        void innerNext(int index, Object o) {
            Object last = current.getAndSet(index, o);
            if (last == EMPTY) {
                ready.decrementAndGet();
            }
        }

        void innerError(int index, Throwable e) {
            onError(e);
        }

        void innerComplete(int index) {
            if (current.get(index) == EMPTY) {
                onCompleted();
            }
        }
    }

    static final class WithLatestOtherSubscriber extends Subscriber<Object> {
        final WithLatestMainSubscriber<?, ?> parent;

        final int index;

        public WithLatestOtherSubscriber(WithLatestMainSubscriber<?, ?> parent, int index) {
            this.parent = parent;
            this.index = index;
        }

        @Override
        public void onNext(Object t) {
            parent.innerNext(index, t);
        }

        @Override
        public void onError(Throwable e) {
            parent.innerError(index, e);
        }

        @Override
        public void onCompleted() {
            parent.innerComplete(index);
        }
    }
}
