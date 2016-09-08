/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.NoSuchElementException;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.Exceptions;
import rx.functions.Func2;
import rx.plugins.RxJavaHooks;

public final class OnSubscribeReduce<T> implements OnSubscribe<T> {

    final Observable<T> source;

    final Func2<T, T, T> reducer;

    public OnSubscribeReduce(Observable<T> source, Func2<T, T, T> reducer) {
        this.source = source;
        this.reducer = reducer;
    }

    @Override
    public void call(Subscriber<? super T> t) {
        final ReduceSubscriber<T> parent = new ReduceSubscriber<T>(t, reducer);
        t.add(parent);
        t.setProducer(new Producer() {
            @Override
            public void request(long n) {
                parent.downstreamRequest(n);
            }
        });
        source.unsafeSubscribe(parent);
    }

    static final class ReduceSubscriber<T> extends Subscriber<T> {

        final Subscriber<? super T> actual;

        final Func2<T, T, T> reducer;

        T value;

        static final Object EMPTY = new Object();

        boolean done;

        @SuppressWarnings("unchecked")
        public ReduceSubscriber(Subscriber<? super T> actual, Func2<T, T, T> reducer) {
            this.actual = actual;
            this.reducer = reducer;
            this.value = (T)EMPTY;
            this.request(0);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            Object o = value;
            if (o == EMPTY) {
                value = t;
            } else {
                try {
                    value = reducer.call((T)o, t);
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    unsubscribe();
                    onError(ex);
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            if (!done) {
                done = true;
                actual.onError(e);
            } else {
                RxJavaHooks.onError(e);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onCompleted() {
            if (done) {
                return;
            }
            done = true;
            Object o = value;
            if (o != EMPTY) {
                actual.onNext((T)o);
                actual.onCompleted();
            } else {
                actual.onError(new NoSuchElementException());
            }
        }

        void downstreamRequest(long n) {
            if (n < 0L) {
                throw new IllegalArgumentException("n >= 0 required but it was " + n);
            }
            if (n != 0L) {
                request(Long.MAX_VALUE);
            }
        }
    }
}
