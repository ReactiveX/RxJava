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

import java.util.NoSuchElementException;

import rx.*;
import rx.Observable.OnSubscribe;
import rx.plugins.RxJavaHooks;

/**
 * Wrap an Observable.OnSubscribe and expose it as a Single.OnSubscribe.
 *
 * @param <T> the value type
 */
public final class SingleFromObservable<T> implements Single.OnSubscribe<T> {

    final Observable.OnSubscribe<T> source;

    public SingleFromObservable(OnSubscribe<T> source) {
        this.source = source;
    }

    @Override
    public void call(SingleSubscriber<? super T> t) {
        WrapSingleIntoSubscriber<T> parent = new WrapSingleIntoSubscriber<T>(t);
        t.add(parent);
        source.call(parent);
    }

    static final class WrapSingleIntoSubscriber<T> extends Subscriber<T> {

        final SingleSubscriber<? super T> actual;

        T value;
        int state;

        static final int STATE_EMPTY = 0;
        static final int STATE_HAS_VALUE = 1;
        static final int STATE_DONE = 2;

        WrapSingleIntoSubscriber(SingleSubscriber<? super T> actual) {
            this.actual = actual;
        }

        @Override
        public void onNext(T t) {
            int s = state;
            if (s == STATE_EMPTY) {
                state = STATE_HAS_VALUE;
                value = t;
            } else if (s == STATE_HAS_VALUE) {
                state = STATE_DONE;
                actual.onError(new IndexOutOfBoundsException("The upstream produced more than one value"));
            }
        }

        @Override
        public void onError(Throwable e) {
            if (state == STATE_DONE) {
                RxJavaHooks.onError(e);
            } else {
                value = null;
                actual.onError(e);
            }
        }

        @Override
        public void onCompleted() {
            int s = state;
            if (s == STATE_EMPTY) {
                actual.onError(new NoSuchElementException());
            } else if (s == STATE_HAS_VALUE) {
                state = STATE_DONE;
                T v = value;
                value = null;
                actual.onSuccess(v);
            }
        }
    }
}
