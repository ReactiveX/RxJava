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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.observers.TestObserver;

public class ObservableAnyTest {

    @Test
    public void testAnyWithTwoItems() {
        Observable<Integer> w = Observable.just(1, 2);
        Observable<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        Observer<Boolean> NbpObserver = TestHelper.mockObserver();
        
        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(false);
        verify(NbpObserver, times(1)).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithTwoItems() {
        Observable<Integer> w = Observable.just(1, 2);
        Observable<Boolean> NbpObservable = w.isEmpty();

        Observer<Boolean> NbpObserver = TestHelper.mockObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(true);
        verify(NbpObserver, times(1)).onNext(false);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testAnyWithOneItem() {
        Observable<Integer> w = Observable.just(1);
        Observable<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        Observer<Boolean> NbpObserver = TestHelper.mockObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(false);
        verify(NbpObserver, times(1)).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithOneItem() {
        Observable<Integer> w = Observable.just(1);
        Observable<Boolean> NbpObservable = w.isEmpty();

        Observer<Boolean> NbpObserver = TestHelper.mockObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(true);
        verify(NbpObserver, times(1)).onNext(false);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testAnyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        Observer<Boolean> NbpObserver = TestHelper.mockObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onNext(false);
        verify(NbpObserver, never()).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> NbpObservable = w.isEmpty();

        Observer<Boolean> NbpObserver = TestHelper.mockObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onNext(true);
        verify(NbpObserver, never()).onNext(false);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testAnyWithPredicate1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });

        Observer<Boolean> NbpObserver = TestHelper.mockObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(false);
        verify(NbpObserver, times(1)).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testExists1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });

        Observer<Boolean> NbpObserver = TestHelper.mockObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onNext(false);
        verify(NbpObserver, times(1)).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testAnyWithPredicate2() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Observable<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 1;
            }
        });

        Observer<Boolean> NbpObserver = TestHelper.mockObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onNext(false);
        verify(NbpObserver, never()).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testAnyWithEmptyAndPredicate() {
        // If the source is empty, always output false.
        Observable<Integer> w = Observable.empty();
        Observable<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t) {
                return true;
            }
        });

        Observer<Boolean> NbpObserver = TestHelper.mockObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onNext(false);
        verify(NbpObserver, never()).onNext(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(NbpObserver, times(1)).onComplete();
    }

    @Test
    public void testWithFollowingFirst() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Observable<Boolean> anyEven = o.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 0;
            }
        });
        
        assertTrue(anyEven.blockingFirst());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        Observable<Integer> source = Observable.just(1).isEmpty()
            .flatMap(new Function<Boolean, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Boolean t1) {
                    return Observable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });
        
        assertEquals((Object)2, source.blockingFirst());
    }
    
    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessage() {
        TestObserver<Boolean> ts = new TestObserver<Boolean>();
        final IllegalArgumentException ex = new IllegalArgumentException();
        
        Observable.just("Boo!").any(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        }).subscribe(ts);
        
        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME value as last cause?
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }
}