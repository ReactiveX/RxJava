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

import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.junit.*;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;

public class NbpOperatorFirstTest {

    NbpSubscriber<String> w;

    private static final Predicate<String> IS_D = new Predicate<String>() {
        @Override
        public boolean test(String value) {
            return "d".equals(value);
        }
    };

    @Before
    public void before() {
        w = TestHelper.mockNbpSubscriber();
    }

    @Test
    public void testFirstOrElseOfNone() {
        NbpObservable<String> src = NbpObservable.empty();
        src.first("default").subscribe(w);

        verify(w, times(1)).onNext(anyString());
        verify(w, times(1)).onNext("default");
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testFirstOrElseOfSome() {
        NbpObservable<String> src = NbpObservable.just("a", "b", "c");
        src.first("default").subscribe(w);

        verify(w, times(1)).onNext(anyString());
        verify(w, times(1)).onNext("a");
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testFirstOrElseWithPredicateOfNoneMatchingThePredicate() {
        NbpObservable<String> src = NbpObservable.just("a", "b", "c");
        src.filter(IS_D).first("default").subscribe(w);

        verify(w, times(1)).onNext(anyString());
        verify(w, times(1)).onNext("default");
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testFirstOrElseWithPredicateOfSome() {
        NbpObservable<String> src = NbpObservable.just("a", "b", "c", "d", "e", "f");
        src.filter(IS_D).first("default").subscribe(w);

        verify(w, times(1)).onNext(anyString());
        verify(w, times(1)).onNext("d");
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onComplete();
    }

    @Test
    public void testFirst() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3).first();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstWithOneElement() {
        NbpObservable<Integer> o = NbpObservable.just(1).first();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstWithEmpty() {
        NbpObservable<Integer> o = NbpObservable.<Integer> empty().first();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstWithPredicate() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3, 4, 5, 6)
                .filter(t1 -> t1 % 2 == 0)
                .first();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstWithPredicateAndOneElement() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2)
                .filter(t1 -> t1 % 2 == 0)
                .first();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstWithPredicateAndEmpty() {
        NbpObservable<Integer> o = NbpObservable.just(1)
                .filter(t1 -> t1 % 2 == 0)
                .first();
        
        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstOrDefault() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3)
                .first(4);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstOrDefaultWithOneElement() {
        NbpObservable<Integer> o = NbpObservable.just(1).first(2);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstOrDefaultWithEmpty() {
        NbpObservable<Integer> o = NbpObservable.<Integer> empty()
                .first(1);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstOrDefaultWithPredicate() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3, 4, 5, 6)
                .filter(t1 -> t1 % 2 == 0)
                .first(8);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstOrDefaultWithPredicateAndOneElement() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2)
                .filter(t1 -> t1 % 2 == 0)
                .first(4);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testFirstOrDefaultWithPredicateAndEmpty() {
        NbpObservable<Integer> o = NbpObservable.just(1)
                .filter(t1 -> t1 % 2 == 0)
                .first(2);
        
        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }
}