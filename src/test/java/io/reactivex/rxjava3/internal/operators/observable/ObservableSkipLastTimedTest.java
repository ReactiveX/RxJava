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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.*;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableSkipLastTimedTest extends RxJavaTest {

    @Test
    public void skipLastTimed() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        // FIXME the timeunit now matters due to rounding
        Observable<Integer> result = source.skipLast(1000, TimeUnit.MILLISECONDS, scheduler);

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        source.onNext(4);
        source.onNext(5);
        source.onNext(6);

        scheduler.advanceTimeBy(950, TimeUnit.MILLISECONDS);
        source.onComplete();

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o, never()).onNext(4);
        inOrder.verify(o, never()).onNext(5);
        inOrder.verify(o, never()).onNext(6);
        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();

        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void skipLastTimedErrorBeforeTime() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> result = source.skipLast(1, TimeUnit.SECONDS, scheduler);

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        source.onError(new TestException());

        scheduler.advanceTimeBy(1050, TimeUnit.MILLISECONDS);

        verify(o).onError(any(TestException.class));

        verify(o, never()).onComplete();
        verify(o, never()).onNext(any());
    }

    @Test
    public void skipLastTimedCompleteBeforeTime() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> result = source.skipLast(1, TimeUnit.SECONDS, scheduler);

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        source.onComplete();

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();

        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void skipLastTimedWhenAllElementsAreValid() {
        TestScheduler scheduler = new TestScheduler();

        PublishSubject<Integer> source = PublishSubject.create();

        Observable<Integer> result = source.skipLast(1, TimeUnit.MILLISECONDS, scheduler);

        Observer<Object> o = TestHelper.mockObserver();

        result.subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);

        scheduler.advanceTimeBy(500, TimeUnit.MILLISECONDS);

        source.onComplete();

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void skipLastTimedDefaultScheduler() {
        Observable.just(1).concatWith(Observable.just(2).delay(500, TimeUnit.MILLISECONDS))
        .skipLast(300, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void skipLastTimedDefaultSchedulerDelayError() {
        Observable.just(1).concatWith(Observable.just(2).delay(500, TimeUnit.MILLISECONDS))
        .skipLast(300, TimeUnit.MILLISECONDS, true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void skipLastTimedCustomSchedulerDelayError() {
        Observable.just(1).concatWith(Observable.just(2).delay(500, TimeUnit.MILLISECONDS))
        .skipLast(300, TimeUnit.MILLISECONDS, Schedulers.io(), true)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(PublishSubject.create().skipLast(1, TimeUnit.DAYS));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.skipLast(1, TimeUnit.DAYS);
            }
        });
    }

    @Test
    public void onNextDisposeRace() {
        TestScheduler scheduler = new TestScheduler();
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishSubject<Integer> ps = PublishSubject.create();

            final TestObserver<Integer> to = ps.skipLast(1, TimeUnit.DAYS, scheduler).test();

            Runnable r1 = new Runnable() {
                @Override
                public void run() {
                    ps.onComplete();
                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    to.dispose();
                }
            };

            TestHelper.race(r1, r2);
        }
    }

    @Test
    public void errorDelayed() {
        Observable.error(new TestException())
        .skipLast(1, TimeUnit.DAYS, new TestScheduler(), true)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void take() {
        Observable.just(1)
        .skipLast(0, TimeUnit.SECONDS)
        .take(1)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1);
    }
}
