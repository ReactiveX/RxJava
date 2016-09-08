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
import rx.exceptions.Exceptions;
import rx.functions.*;

public final class OnSubscribeCollect<T, R> implements OnSubscribe<R> {

    final Observable<T> source;

    final Func0<R> collectionFactory;

    final Action2<R, ? super T> collector;

    public OnSubscribeCollect(Observable<T> source, Func0<R> collectionFactory, Action2<R, ? super T> collector) {
        this.source = source;
        this.collectionFactory = collectionFactory;
        this.collector = collector;
    }

    @Override
    public void call(Subscriber<? super R> t) {
        R initialValue;

        try {
            initialValue = collectionFactory.call();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            t.onError(ex);
            return;
        }

        new CollectSubscriber<T, R>(t, initialValue, collector).subscribeTo(source);
    }

    static final class CollectSubscriber<T, R> extends DeferredScalarSubscriberSafe<T, R> {

        final Action2<R, ? super T> collector;

        public CollectSubscriber(Subscriber<? super R> actual, R initialValue, Action2<R, ? super T> collector) {
            super(actual);
            this.value = initialValue;
            this.hasValue = true;
            this.collector = collector;
        }

        @Override
        public void onNext(T t) {
            if (done) {
                return;
            }
            try {
                collector.call(value, t);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                unsubscribe();
                onError(ex);
            }
        }

    }
}
