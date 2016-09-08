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

import rx.*;
import rx.Observable.OnSubscribe;

public final class OnSubscribeTakeLastOne<T> implements OnSubscribe<T> {

    final Observable<T> source;

    public OnSubscribeTakeLastOne(Observable<T> source) {
        this.source = source;
    }

    @Override
    public void call(Subscriber<? super T> t) {
        new TakeLastOneSubscriber<T>(t).subscribeTo(source);
    }

    static final class TakeLastOneSubscriber<T> extends DeferredScalarSubscriber<T, T> {

        static final Object EMPTY = new Object();

        @SuppressWarnings("unchecked")
        public TakeLastOneSubscriber(Subscriber<? super T> actual) {
            super(actual);
            this.value = (T)EMPTY;
        }

        @Override
        public void onNext(T t) {
            value = t;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onCompleted() {
            Object o = value;
            if (o == EMPTY) {
                complete();
            } else {
                complete((T)o);
            }
        }
    }
}
