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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import rx.*;
import rx.functions.Func1;
import rx.internal.util.UtilityFunctions;
import rx.observers.TestSubscriber;

public class OperatorAnyTest {

    @Test
    public void testAnyWithTwoItems() {
        Observable<Integer> w = Observable.just(1, 2);
        Observable<Boolean> observable = w.exists(UtilityFunctions.alwaysTrue());

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testIsEmptyWithTwoItems() {
        Observable<Integer> w = Observable.just(1, 2);
        Observable<Boolean> observable = w.isEmpty();

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithOneItem() {
        Observable<Integer> w = Observable.just(1);
        Observable<Boolean> observable = w.exists(UtilityFunctions.alwaysTrue());

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testIsEmptyWithOneItem() {
        Observable<Integer> w = Observable.just(1);
        Observable<Boolean> observable = w.isEmpty();

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.exists(UtilityFunctions.alwaysTrue());

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testIsEmptyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.isEmpty();

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithPredicate1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> observable = w.exists(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 < 2;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testExists1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> observable = w.exists(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 < 2;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithPredicate2() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> observable = w.exists(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return t1 < 1;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testAnyWithEmptyAndPredicate() {
        // If the source is empty, always output false.
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> observable = w.exists(
                new Func1<Integer, Boolean>() {

                    @Override
                    public Boolean call(Integer t1) {
                        return true;
                    }
                });

        @SuppressWarnings("unchecked")
        Observer<Boolean> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testWithFollowingFirst() {
        Observable<Integer> o = Observable.from(Arrays.asList(1, 3, 5, 6));
        Observable<Boolean> anyEven = o.exists(new Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer i) {
                return i % 2 == 0;
            }
        });
        assertTrue(anyEven.toBlocking().first());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        Observable<Integer> source = Observable.just(1).isEmpty()
            .flatMap(new Func1<Boolean, Observable<Integer>>() {
                @Override
                public Observable<Integer> call(Boolean t1) {
                    return Observable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
        });
        assertEquals((Object)2, source.toBlocking().first());
    }
    
    @Test
    public void testBackpressureIfNoneRequestedNoneShouldBeDelivered() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>(0);
        Observable.just(1).exists(new Func1<Object, Boolean>() {
            @Override
            public Boolean call(Object t1) {
                return true;
            }
        }).subscribe(ts);
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }
    
    @Test
    public void testBackpressureIfOneRequestedOneShouldBeDelivered() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>(1);
        Observable.just(1).exists(new Func1<Object, Boolean>() {
            @Override
            public Boolean call(Object object) {
                return true;
            }
        }).subscribe(ts);
        ts.assertTerminalEvent();
        ts.assertNoErrors();
        ts.assertCompleted();
        ts.assertValue(true);
    }
    
    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessage() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>(0);
        final IllegalArgumentException ex = new IllegalArgumentException();
        Observable.just("Boo!").exists(new Func1<Object, Boolean>() {
            @Override
            public Boolean call(Object object) {
                throw ex;
            }
        }).subscribe(ts);
        ts.assertTerminalEvent();
        ts.assertNoValues();
        ts.assertNotCompleted();
        List<Throwable> errors = ts.getOnErrorEvents();
        assertEquals(1, errors.size());
        assertEquals(ex, errors.get(0));
        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }
}
