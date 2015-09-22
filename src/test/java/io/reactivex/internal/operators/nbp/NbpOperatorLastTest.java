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

public class NbpOperatorLastTest {

    @Test
    public void testLastWithElements() {
        NbpObservable<Integer> last = NbpObservable.just(1, 2, 3).last();
        assertEquals(3, last.toBlocking().single().intValue());
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastWithNoElements() {
        NbpObservable<?> last = NbpObservable.empty().last();
        last.toBlocking().single();
    }

    @Test
    public void testLastMultiSubscribe() {
        NbpObservable<Integer> last = NbpObservable.just(1, 2, 3).last();
        assertEquals(3, last.toBlocking().single().intValue());
        assertEquals(3, last.toBlocking().single().intValue());
    }

    @Test
    public void testLastViaObservable() {
        NbpObservable.just(1, 2, 3).last();
    }

    @Test
    public void testLast() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3).last();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(3);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithOneElement() {
        NbpObservable<Integer> o = NbpObservable.just(1).last();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithEmpty() {
        NbpObservable<Integer> o = NbpObservable.<Integer> empty().last();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicate() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3, 4, 5, 6)
                .filter(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(6);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicateAndOneElement() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2)
            .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
            .last();

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicateAndEmpty() {
        NbpObservable<Integer> o = NbpObservable.just(1)
            .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                }).last();
        
        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefault() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3)
                .last(4);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(3);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithOneElement() {
        NbpObservable<Integer> o = NbpObservable.just(1).last(2);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithEmpty() {
        NbpObservable<Integer> o = NbpObservable.<Integer> empty()
                .last(1);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicate() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2, 3, 4, 5, 6)
                .filter(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last(8);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(6);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicateAndOneElement() {
        NbpObservable<Integer> o = NbpObservable.just(1, 2)
                .filter(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last(4);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicateAndEmpty() {
        NbpObservable<Integer> o = NbpObservable.just(1)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last(2);

        NbpSubscriber<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }
}