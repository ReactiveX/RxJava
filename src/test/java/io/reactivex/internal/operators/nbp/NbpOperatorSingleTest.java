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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;
import java.util.function.Predicate;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;

public class NbpOperatorSingleTest {

    @Test
    public void testSingle() {
        NbpObservable<Integer> o = NbpObservable.just(1).single();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithTooManyElements() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2).single();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithEmpty() {
        NbpObservable<Integer> o = NbpObservable.<Integer> empty().single();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }
    
    @Test
    public void testSingleWithPredicate() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndTooManyElements() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3, 4)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndEmpty() {
        NbpObservable<Integer> o = NbpObservable.just(1)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single();
        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefault() {
        NbpObservable<Integer> o = NbpObservable.just(1).single(2);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithTooManyElements() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2).single(3);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithEmpty() {
        NbpObservable<Integer> o = NbpObservable.<Integer> empty()
                .single(1);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicate() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2)
                .filter(t1 -> t1 % 2 == 0)
                .single(4);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndTooManyElements() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3, 4)
                .filter(t1 -> t1 % 2 == 0)
                .single(6);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndEmpty() {
        NbpObservable<Integer> o = NbpObservable.just(1)
                .filter(t1 -> t1 % 2 == 0)
                .single(2);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 30000)
    public void testIssue1527() throws InterruptedException {
        //https://github.com/ReactiveX/RxJava/pull/1527
        NbpObservable<Integer> source = NbpObservable.just(1, 2, 3, 4, 5, 6);
        NbpObservable<Integer> reduced = source.reduce((i1, i2) -> i1 + i2);

        Integer r = reduced.toBlocking().first();
        assertEquals(21, r.intValue());
    }
}