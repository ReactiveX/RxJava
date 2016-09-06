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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.*;

public class ObservableAllTest {

    @Test
    public void testAll() {
        Observable<String> obs = Observable.just("one", "two", "six");

        SingleObserver <Boolean> NbpObserver = TestHelper.mockSingleObserver();
        
        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(NbpObserver);

        verify(NbpObserver).onSubscribe((Disposable)any());
        verify(NbpObserver).onSuccess(true);
        verifyNoMoreInteractions(NbpObserver);
    }

    @Test
    public void testNotAll() {
        Observable<String> obs = Observable.just("one", "two", "three", "six");

        SingleObserver <Boolean> NbpObserver = TestHelper.mockSingleObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(NbpObserver);

        verify(NbpObserver).onSubscribe((Disposable)any());
        verify(NbpObserver).onSuccess(false);
        verifyNoMoreInteractions(NbpObserver);
    }

    @Test
    public void testEmpty() {
        Observable<String> obs = Observable.empty();

        SingleObserver <Boolean> NbpObserver = TestHelper.mockSingleObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(NbpObserver);

        verify(NbpObserver).onSubscribe((Disposable)any());
        verify(NbpObserver).onSuccess(true);
        verifyNoMoreInteractions(NbpObserver);
    }

    @Test
    public void testError() {
        Throwable error = new Throwable();
        Observable<String> obs = Observable.error(error);

        SingleObserver <Boolean> NbpObserver = TestHelper.mockSingleObserver();

        obs.all(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return s.length() == 3;
            }
        })
        .subscribe(NbpObserver);

        verify(NbpObserver).onSubscribe((Disposable)any());
        verify(NbpObserver).onError(error);
        verifyNoMoreInteractions(NbpObserver);
    }

    @Test
    public void testFollowingFirst() {
        Observable<Integer> o = Observable.fromArray(1, 3, 5, 6);
        Single<Boolean> allOdd = o.all(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 1;
            }
        });
        
        assertFalse(allOdd.blockingGet());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        Single<Integer> source = Observable.just(1)
            .all(new Predicate<Integer>() {
                @Override
                public boolean test(Integer t1) {
                    return false;
                }
            })
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
        
        Observable.just("Boo!").all(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        })
        .subscribe(ts);
        
        verify(ts, never()).onSuccess(anyBoolean());
        verify(ts, times(1)).onError(ex);
        // FIXME need to decide about adding the value that probably caused the crash in some way
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }
}