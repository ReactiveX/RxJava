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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observers.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class ObservableTakeLastTest extends RxJavaTest {

    @Test
    public void takeLastEmpty() {
        Observable<String> w = Observable.empty();
        Observable<String> take = w.takeLast(2);

        Observer<String> observer = TestHelper.mockObserver();
        take.subscribe(observer);
        verify(observer, never()).onNext(any(String.class));
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void takeLast1() {
        Observable<String> w = Observable.just("one", "two", "three");
        Observable<String> take = w.takeLast(2);

        Observer<String> observer = TestHelper.mockObserver();
        InOrder inOrder = inOrder(observer);
        take.subscribe(observer);
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        verify(observer, never()).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void takeLast2() {
        Observable<String> w = Observable.just("one");
        Observable<String> take = w.takeLast(10);

        Observer<String> observer = TestHelper.mockObserver();
        take.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void takeLastWithZeroCount() {
        Observable<String> w = Observable.just("one");
        Observable<String> take = w.takeLast(0);

        Observer<String> observer = TestHelper.mockObserver();
        take.subscribe(observer);
        verify(observer, never()).onNext("one");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void takeLastWithNegativeCount() {
        Observable.just("one").takeLast(-1);
    }

    @Test
    public void backpressure1() {
        TestObserver<Integer> to = new TestObserver<>();
        Observable.range(1, 100000).takeLast(1)
        .observeOn(Schedulers.newThread())
        .map(newSlowProcessor()).subscribe(to);
        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        to.assertValue(100000);
    }

    @Test
    public void backpressure2() {
        TestObserver<Integer> to = new TestObserver<>();
        Observable.range(1, 100000).takeLast(Flowable.bufferSize() * 4)
        .observeOn(Schedulers.newThread()).map(newSlowProcessor()).subscribe(to);
        to.awaitDone(5, TimeUnit.SECONDS);
        to.assertNoErrors();
        assertEquals(Flowable.bufferSize() * 4, to.values().size());
    }

    private Function<Integer, Integer> newSlowProcessor() {
        return new Function<Integer, Integer>() {
            int c;

            @Override
            public Integer apply(Integer i) {
                if (c++ < 100) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                    }
                }
                return i;
            }

        };
    }

    @Test
    public void issue1522() {
        // https://github.com/ReactiveX/RxJava/issues/1522
        assertNull(Observable
                .empty()
                .count()
                .filter(new Predicate<Long>() {
                    @Override
                    public boolean test(Long v) {
                        return false;
                    }
                })
                .blockingGet());
    }

    @Test
    public void unsubscribeTakesEffectEarlyOnFastPath() {
        final AtomicInteger count = new AtomicInteger();
        Observable.range(0, 100000).takeLast(100000).subscribe(new DefaultObserver<Integer>() {

            @Override
            public void onStart() {
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Integer integer) {
                count.incrementAndGet();
                cancel();
            }
        });
        assertEquals(1, count.get());
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.range(1, 10).takeLast(5));
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, ObservableSource<Object>>() {
            @Override
            public ObservableSource<Object> apply(Observable<Object> o) throws Exception {
                return o.takeLast(5);
            }
        });
    }

    @Test
    public void error() {
        Observable.error(new TestException())
        .takeLast(5)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void takeLastTake() {
        Observable.range(1, 10)
        .takeLast(5)
        .take(2)
        .test()
        .assertResult(6, 7);
    }
}
