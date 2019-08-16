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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableThrottleLatestTest extends RxJavaTest {

    @Test
    public void just() {
        Observable.just(1)
        .throttleLatest(1, TimeUnit.MINUTES)
        .test()
        .assertResult(1);
    }

    @Test
    public void range() {
        Observable.range(1, 5)
        .throttleLatest(1, TimeUnit.MINUTES)
        .test()
        .assertResult(1);
    }

    @Test
    public void rangeEmitLatest() {
        Observable.range(1, 5)
        .throttleLatest(1, TimeUnit.MINUTES, true)
        .test()
        .assertResult(1, 5);
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .throttleLatest(1, TimeUnit.MINUTES)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Observable<Object> f) throws Exception {
                return f.throttleLatest(1, TimeUnit.MINUTES);
            }
        });
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(
                Observable.never()
                .throttleLatest(1, TimeUnit.MINUTES)
        );
    }

    @Test
    public void normal() {
        TestScheduler sch = new TestScheduler();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps.throttleLatest(1, TimeUnit.SECONDS, sch).test();

        ps.onNext(1);

        to.assertValuesOnly(1);

        ps.onNext(2);

        to.assertValuesOnly(1);

        ps.onNext(3);

        to.assertValuesOnly(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3);

        ps.onNext(4);

        to.assertValuesOnly(1, 3);

        ps.onNext(5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3, 5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3, 5);

        ps.onNext(6);

        to.assertValuesOnly(1, 3, 5, 6);

        ps.onNext(7);
        ps.onComplete();

        to.assertResult(1, 3, 5, 6);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertResult(1, 3, 5, 6);
    }

    @Test
    public void normalEmitLast() {
        TestScheduler sch = new TestScheduler();
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps.throttleLatest(1, TimeUnit.SECONDS, sch, true).test();

        ps.onNext(1);

        to.assertValuesOnly(1);

        ps.onNext(2);

        to.assertValuesOnly(1);

        ps.onNext(3);

        to.assertValuesOnly(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3);

        ps.onNext(4);

        to.assertValuesOnly(1, 3);

        ps.onNext(5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3, 5);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertValuesOnly(1, 3, 5);

        ps.onNext(6);

        to.assertValuesOnly(1, 3, 5, 6);

        ps.onNext(7);
        ps.onComplete();

        to.assertResult(1, 3, 5, 6, 7);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertResult(1, 3, 5, 6, 7);
    }

    @Test
    public void take() throws Throwable {
        Action onCancel = mock(Action.class);

        Observable.range(1, 5)
        .doOnDispose(onCancel)
        .throttleLatest(1, TimeUnit.MINUTES)
        .take(1)
        .test()
        .assertResult(1);

        verify(onCancel).run();
    }

    @Test
    public void reentrantComplete() {
        TestScheduler sch = new TestScheduler();
        final PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = new TestObserver<Integer>() {
            @Override
            public void onNext(Integer t) {
                super.onNext(t);
                if (t == 1) {
                    ps.onNext(2);
                }
                if (t == 2) {
                    ps.onComplete();
                }
            }
        };

        ps.throttleLatest(1, TimeUnit.SECONDS, sch).subscribe(to);

        ps.onNext(1);

        sch.advanceTimeBy(1, TimeUnit.SECONDS);

        to.assertResult(1, 2);
    }
}
