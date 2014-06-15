/**
 * Copyright 2014 Netflix, Inc.
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

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class OperatorSingleTest {

    @Test
    public void testSingle() {
        Observable<Integer> observable = Observable.from(1).single();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithTooManyElements() {
        Observable<Integer> observable = Observable.from(1, 2).single();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithEmpty() {
        Observable<Integer> observable = Observable.<Integer> empty().single();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicate() {
        Observable<Integer> observable = Observable.from(1, 2).single(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndTooManyElements() {
        Observable<Integer> observable = Observable.from(1, 2, 3, 4).single(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndEmpty() {
        Observable<Integer> observable = Observable.from(1).single(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });
        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefault() {
        Observable<Integer> observable = Observable.from(1).singleOrDefault(2);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithTooManyElements() {
        Observable<Integer> observable = Observable.from(1, 2).singleOrDefault(
                3);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithEmpty() {
        Observable<Integer> observable = Observable.<Integer> empty()
                .singleOrDefault(1);

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicate() {
        Observable<Integer> observable = Observable.from(1, 2).singleOrDefault(
                4, new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndTooManyElements() {
        Observable<Integer> observable = Observable.from(1, 2, 3, 4)
                .singleOrDefault(6, new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndEmpty() {
        Observable<Integer> observable = Observable.from(1).singleOrDefault(2,
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 % 2 == 0;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = (Observer<Integer>) mock(Observer.class);
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testUnsubscribe() throws InterruptedException {
        final AtomicBoolean hasBeenInterrupted = new AtomicBoolean();

        final CountDownLatch waitForSubscribeCalled = new CountDownLatch(1);
        final CountDownLatch WaitForHasBeenInterruptedSet = new CountDownLatch(1);

        Observable<Integer> observable = Observable.from(1, 2).single()
                .onErrorResumeNext(new Func1<Throwable, Observable<Integer>>() {

                    @Override
                    public Observable<Integer> call(Throwable ignored) {
                        // If single `unsubscribe` the whole Subscription chain by mistake,
                        // the Action in `Schedulers.io()` will be cancelled by calling "Future.cancel(true)".
                        // So we will observe `InterruptedException` here.
                        // This is not the desired behavior.
                        return Observable.create(new Observable.OnSubscribe<Integer>() {

                            @Override
                            public void call(Subscriber<? super Integer> subscriber) {
                                try {
                                    waitForSubscribeCalled.await();
                                    hasBeenInterrupted.set(false);
                                } catch (InterruptedException e) {
                                    hasBeenInterrupted.set(true);
                                }
                                WaitForHasBeenInterruptedSet.countDown();
                            }
                        }).subscribeOn(Schedulers.io());
                    }
                });

        observable.subscribe();

        waitForSubscribeCalled.countDown();
        WaitForHasBeenInterruptedSet.await();

        assertFalse(hasBeenInterrupted.get());
    }
}
