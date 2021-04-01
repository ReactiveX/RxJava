/*
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

package io.reactivex.rxjava3.single;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class SingleCacheTest extends RxJavaTest {

    @Test
    public void normal() {
        Single<Integer> cache = Single.just(1).cache();

        cache
        .test()
        .assertResult(1);

        cache
        .test()
        .assertResult(1);
    }

    @Test
    public void error() {
        Single<Object> cache = Single.error(new TestException())
        .cache();

        cache
        .test()
        .assertFailure(TestException.class);

        cache
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void delayed() {
        PublishSubject<Integer> ps = PublishSubject.create();
        Single<Integer> cache = ps.single(-99).cache();

        TestObserver<Integer> to1 = cache.test();

        TestObserver<Integer> to2 = cache.test();

        ps.onNext(1);
        ps.onComplete();

        to1.assertResult(1);
        to2.assertResult(1);
    }

    @Test
    public void delayedDisposed() {
        PublishSubject<Integer> ps = PublishSubject.create();
        Single<Integer> cache = ps.single(-99).cache();

        TestObserver<Integer> to1 = cache.test();

        TestObserver<Integer> to2 = cache.test();

        to1.dispose();

        ps.onNext(1);
        ps.onComplete();

        to1.assertNoValues().assertNoErrors().assertNotComplete();
        to2.assertResult(1);
    }

    @Test
    public void crossCancel() {
        PublishSubject<Integer> ps = PublishSubject.create();
        Single<Integer> cache = ps.single(-99).cache();

        final TestSubscriber<Integer> ts1 = new TestSubscriber<>();

        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                ts1.cancel();
            }
        };

        cache.toFlowable().subscribe(ts2);
        cache.toFlowable().subscribe(ts1);

        ps.onNext(1);
        ps.onComplete();

        ts1.assertNoValues().assertNoErrors().assertNotComplete();
        ts2.assertResult(1);
    }

    @Test
    public void crossCancelOnError() {
        PublishSubject<Integer> ps = PublishSubject.create();
        Single<Integer> cache = ps.single(-99).cache();

        final TestSubscriber<Integer> ts1 = new TestSubscriber<>();

        TestSubscriber<Integer> ts2 = new TestSubscriber<Integer>() {
            @Override
            public void onError(Throwable t) {
                super.onError(t);
                ts1.cancel();
            }
        };

        cache.toFlowable().subscribe(ts2);
        cache.toFlowable().subscribe(ts1);

        ps.onError(new TestException());

        ts1.assertNoValues().assertNoErrors().assertNotComplete();
        ts2.assertFailure(TestException.class);
    }

}
