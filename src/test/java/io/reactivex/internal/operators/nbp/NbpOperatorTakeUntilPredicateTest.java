/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.function.Predicate;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;
import io.reactivex.exceptions.TestException;
import io.reactivex.subscribers.nbp.NbpTestSubscriber;
;

public class NbpOperatorTakeUntilPredicateTest {
    @Test
    public void takeEmpty() {
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        
        NbpObservable.empty().takeUntil(v -> true).subscribe(o);
        
        verify(o, never()).onNext(any());
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void takeAll() {
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        
        NbpObservable.just(1, 2).takeUntil(v -> false).subscribe(o);
        
        verify(o).onNext(1);
        verify(o).onNext(2);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void takeFirst() {
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        
        NbpObservable.just(1, 2).takeUntil(v -> true).subscribe(o);
        
        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void takeSome() {
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        
        NbpObservable.just(1, 2, 3).takeUntil(t1 -> t1 == 2)
        .subscribe(o);
        
        verify(o).onNext(1);
        verify(o).onNext(2);
        verify(o, never()).onNext(3);
        verify(o, never()).onError(any(Throwable.class));
        verify(o).onComplete();
    }
    @Test
    public void functionThrows() {
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        
        NbpObservable.just(1, 2, 3).takeUntil((Predicate<Integer>)(t1 -> {
                throw new TestException("Forced failure");
        })).subscribe(o);
        
        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o, never()).onNext(3);
        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
    }
    @Test
    public void sourceThrows() {
        NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
        
        NbpObservable.just(1)
        .concatWith(NbpObservable.<Integer>error(new TestException()))
        .concatWith(NbpObservable.just(2))
        .takeUntil(v -> false).subscribe(o);
        
        verify(o).onNext(1);
        verify(o, never()).onNext(2);
        verify(o).onError(any(TestException.class));
        verify(o, never()).onComplete();
    }
    
    @Test
    public void testErrorIncludesLastValueAsCause() {
        NbpTestSubscriber<String> ts = new NbpTestSubscriber<>();
        final TestException e = new TestException("Forced failure");
        NbpObservable.just("abc").takeUntil((Predicate<String>)(t -> {
                throw e;
        })).subscribe(ts);
        
        ts.assertTerminated();
        ts.assertNotComplete();
        ts.assertError(TestException.class);
        // FIXME last cause value is not saved
//        assertTrue(ts.errors().get(0).getCause().getMessage().contains("abc"));
    }
}