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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.Predicate;
import io.reactivex.observers.TestObserver;
;

public class NbpOperatorTakeUntilPredicateTest {
    @Test
    public void takeEmpty() {
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
        Observable.empty().takeUntil(new Predicate<Object>() {
            @Override
            public boolean test(Object v) {
                return true;
            }
        }).subscribe(o);
        
        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void takeAll() {
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
        Observable.just(1, 2).takeUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return false;
            }
        }).subscribe(o);
        
        verify(o).onNext(1);
        verify(o).onNext(2);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void takeFirst() {
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
        Observable.just(1, 2).takeUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        }).subscribe(o);
        
        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void takeSome() {
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
        Observable.just(1, 2, 3).takeUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 == 2;
            }
        })
        .subscribe(o);
        
        verify(o).onNext(1);
        verify(o).onNext(2);
        verify(o, never()).onNext(3);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void functionThrows() {
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
        Predicate<Integer> predicate = (new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                    throw new TestException("Forced failure");
            }
        });
        Observable.just(1, 2, 3).takeUntil(predicate).subscribe(o);
        
        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onNext(3);
        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
    }
    @Test
    public void sourceThrows() {
        Observer<Object> o = TestHelper.mockNbpSubscriber();
        
        Observable.just(1)
        .concatWith(Observable.<Integer>error(new TestException()))
        .concatWith(Observable.just(2))
        .takeUntil(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return false;
            }
        }).subscribe(o);
        
        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
    }
    
    @Test
    public void testErrorIncludesLastValueAsCause() {
        TestObserver<String> ts = new TestObserver<String>();
        final TestException e = new TestException("Forced failure");
        Predicate<String> predicate = (new Predicate<String>() {
            @Override
            public boolean test(String t) {
                    throw e;
            }
        });
        Observable.just("abc").takeUntil(predicate).subscribe(ts);
        
        ts.assertTerminated();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
        // FIXME last cause value is not saved
//        assertTrue(ts.errors().get(0).getCause().getMessage().contains("abc"));
    }
}