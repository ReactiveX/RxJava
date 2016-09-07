/**
 * Copyright 2016 Netflix, Inc.
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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.subscriptions.BooleanSubscription;

public class FlowableCreateTest {

    @Test(expected = NullPointerException.class)
    public void sourceNull() {
        Flowable.create(null, FlowableEmitter.BackpressureMode.BUFFER);
    }

    @Test(expected = NullPointerException.class)
    public void modeNull() {
        Flowable.create(new FlowableOnSubscribe<Object>() {
            @Override
            public void subscribe(FlowableEmitter<Object> s) throws Exception { }
        }, null);
    }

    @Test
    public void basic() {
        final Disposable d = Disposables.empty();

        Flowable.<Integer>create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
                e.onError(new TestException());
                e.onNext(4);
                e.onError(new TestException());
                e.onComplete();
            }
        }, FlowableEmitter.BackpressureMode.BUFFER)
        .test()
        .assertResult(1, 2, 3);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicWithError() {
        final Disposable d = Disposables.empty();

        Flowable.<Integer>create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onError(new TestException());
                e.onComplete();
                e.onNext(4);
                e.onError(new TestException());
            }
        }, FlowableEmitter.BackpressureMode.BUFFER)
        .test()
        .assertFailure(TestException.class, 1, 2, 3);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicSerialized() {
        final Disposable d = Disposables.empty();

        Flowable.<Integer>create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e = e.serialize();

                e.setDisposable(d);

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onComplete();
                e.onError(new TestException());
                e.onNext(4);
                e.onError(new TestException());
                e.onComplete();
            }
        }, FlowableEmitter.BackpressureMode.BUFFER)
        .test()
        .assertResult(1, 2, 3);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicWithErrorSerialized() {
        final Disposable d = Disposables.empty();

        Flowable.<Integer>create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                e = e.serialize();

                e.setDisposable(d);

                e.onNext(1);
                e.onNext(2);
                e.onNext(3);
                e.onError(new TestException());
                e.onComplete();
                e.onNext(4);
                e.onError(new TestException());
            }
        }, FlowableEmitter.BackpressureMode.BUFFER)
        .test()
        .assertFailure(TestException.class, 1, 2, 3);

        assertTrue(d.isDisposed());
    }

    @Test
    public void wrap() {
        Flowable.fromPublisher(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> observer) {
                observer.onSubscribe(new BooleanSubscription());
                observer.onNext(1);
                observer.onNext(2);
                observer.onNext(3);
                observer.onNext(4);
                observer.onNext(5);
                observer.onComplete();
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test
    public void unsafe() {
        Flowable.unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> observer) {
                observer.onSubscribe(new BooleanSubscription());
                observer.onNext(1);
                observer.onNext(2);
                observer.onNext(3);
                observer.onNext(4);
                observer.onNext(5);
                observer.onComplete();
            }
        })
        .test()
        .assertResult(1, 2, 3, 4, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsafeWithFlowable() {
        Flowable.unsafeCreate(Flowable.just(1));
    }

}
