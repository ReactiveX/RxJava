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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.function.Predicate;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;

public class NbpOperatorSkipWhileTest {

    NbpSubscriber<Integer> w = TestHelper.mockNbpSubscriber();

    private static final Predicate<Integer> LESS_THAN_FIVE = new Predicate<Integer>() {
        @Override
        public boolean test(Integer v) {
            if (v == 42)
                throw new RuntimeException("that's not the answer to everything!");
            return v < 5;
        }
    };

    private static final Predicate<Integer> INDEX_LESS_THAN_THREE = new Predicate<Integer>() {
        int index = 0;
        @Override
        public boolean test(Integer value) {
            return index++ < 3;
        }
    };

    @Test
    public void testSkipWithIndex() {
        NbpObservable<Integer> src = NbpObservable.just(1, 2, 3, 4, 5);
        src.skipWhile(INDEX_LESS_THAN_THREE).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext(4);
        inOrder.verify(w, times(1)).onNext(5);
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSkipEmpty() {
        NbpObservable<Integer> src = NbpObservable.empty();
        src.skipWhile(LESS_THAN_FIVE).subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testSkipEverything() {
        NbpObservable<Integer> src = NbpObservable.just(1, 2, 3, 4, 3, 2, 1);
        src.skipWhile(LESS_THAN_FIVE).subscribe(w);
        verify(w, never()).onNext(anyInt());
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testSkipNothing() {
        NbpObservable<Integer> src = NbpObservable.just(5, 3, 1);
        src.skipWhile(LESS_THAN_FIVE).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext(5);
        inOrder.verify(w, times(1)).onNext(3);
        inOrder.verify(w, times(1)).onNext(1);
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSkipSome() {
        NbpObservable<Integer> src = NbpObservable.just(1, 2, 3, 4, 5, 3, 1, 5);
        src.skipWhile(LESS_THAN_FIVE).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, times(1)).onNext(5);
        inOrder.verify(w, times(1)).onNext(3);
        inOrder.verify(w, times(1)).onNext(1);
        inOrder.verify(w, times(1)).onNext(5);
        inOrder.verify(w, times(1)).onComplete();
        inOrder.verify(w, never()).onError(any(Throwable.class));
    }

    @Test
    public void testSkipError() {
        NbpObservable<Integer> src = NbpObservable.just(1, 2, 42, 5, 3, 1);
        src.skipWhile(LESS_THAN_FIVE).subscribe(w);

        InOrder inOrder = inOrder(w);
        inOrder.verify(w, never()).onNext(anyInt());
        inOrder.verify(w, never()).onComplete();
        inOrder.verify(w, times(1)).onError(any(RuntimeException.class));
    }
    
    @Test
    public void testSkipManySubscribers() {
        NbpObservable<Integer> src = NbpObservable.range(1, 10).skipWhile(LESS_THAN_FIVE);
        int n = 5;
        for (int i = 0; i < n; i++) {
            NbpSubscriber<Object> o = TestHelper.mockNbpSubscriber();
            InOrder inOrder = inOrder(o);
            
            src.subscribe(o);
            
            for (int j = 5; j < 10; j++) {
                inOrder.verify(o).onNext(j);
            } 
            inOrder.verify(o).onComplete();
            verify(o, never()).onError(any(Throwable.class));
        }
    }
}