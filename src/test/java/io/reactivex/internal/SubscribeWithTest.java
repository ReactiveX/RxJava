/**
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal;

import org.junit.Test;
import static org.junit.Assert.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;

public class SubscribeWithTest {

    @Test
    public void withFlowable() {
        Flowable.range(1, 10)
        .subscribeWith(new TestSubscriber<Integer>())
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }


    @Test
    public void withObservable() {
        Observable.range(1, 10)
        .subscribeWith(new TestObserver<Integer>())
        .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }


    class ObserverImpl implements SingleObserver<Object>, CompletableObserver {
        Object value;

        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onComplete() {
            this.value = 100;
        }

        @Override
        public void onSuccess(Object value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable e) {
            this.value = e;
        }
    }

    @Test
    public void withSingle() {
        assertEquals(1, Single.just(1).subscribeWith(new ObserverImpl()).value);
    }

    @Test
    public void withCompletable() {
        assertEquals(100, Completable.complete().subscribeWith(new ObserverImpl()).value);
    }

}
