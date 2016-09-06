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

public class ObservableAnyTest {

    @Test
    public void testAnyWithTwoItems() {
        Observable<Integer> w = Observable.just(1, 2);
        Single<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> NbpObserver = TestHelper.mockSingleObserver();
        
        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onSuccess(false);
        verify(NbpObserver, times(1)).onSuccess(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
    }

    @Test
    public void testIsEmptyWithTwoItems() {
        Observable<Integer> w = Observable.just(1, 2);
        Single<Boolean> NbpObservable = w.isEmpty();

        SingleObserver<Boolean> NbpObserver = TestHelper.mockSingleObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onSuccess(true);
        verify(NbpObserver, times(1)).onSuccess(false);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
    }

    @Test
    public void testAnyWithOneItem() {
        Observable<Integer> w = Observable.just(1);
        Single<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> NbpObserver = TestHelper.mockSingleObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onSuccess(false);
        verify(NbpObserver, times(1)).onSuccess(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
    }

    @Test
    public void testIsEmptyWithOneItem() {
        Observable<Integer> w = Observable.just(1);
        Single<Boolean> NbpObservable = w.isEmpty();

        SingleObserver<Boolean> NbpObserver = TestHelper.mockSingleObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onSuccess(true);
        verify(NbpObserver, times(1)).onSuccess(false);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
    }

    @Test
    public void testAnyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Single<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        SingleObserver<Boolean> NbpObserver = TestHelper.mockSingleObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onSuccess(false);
        verify(NbpObserver, never()).onSuccess(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
    }

    @Test
    public void testIsEmptyWithEmpty() {
        Observable<Integer> w = Observable.empty();
        Single<Boolean> NbpObservable = w.isEmpty();

        SingleObserver<Boolean> NbpObserver = TestHelper.mockSingleObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onSuccess(true);
        verify(NbpObserver, never()).onSuccess(false);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
    }

    @Test
    public void testAnyWithPredicate1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Single<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });

        SingleObserver<Boolean> NbpObserver = TestHelper.mockSingleObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onSuccess(false);
        verify(NbpObserver, times(1)).onSuccess(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
    }

    @Test
    public void testExists1() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Single<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });

        SingleObserver<Boolean> NbpObserver = TestHelper.mockSingleObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, never()).onSuccess(false);
        verify(NbpObserver, times(1)).onSuccess(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
    }

    @Test
    public void testAnyWithPredicate2() {
        Observable<Integer> w = Observable.just(1, 2, 3);
        Single<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 1;
            }
        });

        SingleObserver<Boolean> NbpObserver = TestHelper.mockSingleObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onSuccess(false);
        verify(NbpObserver, never()).onSuccess(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
    }

    @Test
    public void testAnyWithEmptyAndPredicate() {
        // If the source is empty, always output false.
        Observable<Integer> w = Observable.empty();
        Single<Boolean> NbpObservable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t) {
                return true;
            }
        });

        SingleObserver<Boolean> NbpObserver = TestHelper.mockSingleObserver();

        NbpObservable.subscribe(NbpObserver);
        
        verify(NbpObserver, times(1)).onSuccess(false);
        verify(NbpObserver, never()).onSuccess(true);
        verify(NbpObserver, never()).onError(org.mockito.Matchers.any(Throwable.class));
    }

    @Test
    public void testWithFollowingFirst() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Single<Boolean> anyEven = o.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 0;
            }
        });
        
        assertTrue(anyEven.blockingGet());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        Single<Integer> source = Observable.just(1).isEmpty()
            .flatMap(new Function<Boolean, Single<Integer>>() {
                @Override
                public Single<Integer> apply(Boolean t1) {
                    return Single.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });
        
        assertEquals((Object)2, source.blockingGet());
    }
    
    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessage() {
        SingleObserver<Boolean> ts = TestHelper.mockSingleObserver();
        final IllegalArgumentException ex = new IllegalArgumentException();
        
        
        Observable.just("Boo!").any(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        }).subscribe(ts);
        
        verify(ts, never()).onSuccess(anyBoolean());
        verify(ts, never()).onError(org.mockito.Matchers.any(Throwable.class));
        // FIXME value as last cause?
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }
}