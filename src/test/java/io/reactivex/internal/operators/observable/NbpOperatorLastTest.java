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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;

import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.*;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.Predicate;

public class NbpOperatorLastTest {

    @Test
    public void testLastWithElements() {
        Observable<Integer> last = Observable.just(1, 2, 3).last();
        assertEquals(3, last.toBlocking().single().intValue());
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastWithNoElements() {
        Observable<?> last = Observable.empty().last();
        last.toBlocking().single();
    }

    @Test
    public void testLastMultiSubscribe() {
        Observable<Integer> last = Observable.just(1, 2, 3).last();
        assertEquals(3, last.toBlocking().single().intValue());
        assertEquals(3, last.toBlocking().single().intValue());
    }

    @Test
    public void testLastViaObservable() {
        Observable.just(1, 2, 3).last();
    }

    @Test
    public void testLast() {
        Observable<Integer> o = Observable.just(1, 2, 3).last();

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(3);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithOneElement() {
        Observable<Integer> o = Observable.just(1).last();

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithEmpty() {
        Observable<Integer> o = Observable.<Integer> empty().last();

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicate() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4, 5, 6)
                .filter(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last();

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(6);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicateAndOneElement() {
        Observable<Integer> o = Observable.just(1, 2)
            .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
            .last();

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicateAndEmpty() {
        Observable<Integer> o = Observable.just(1)
            .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                }).last();
        
        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefault() {
        Observable<Integer> o = Observable.just(1, 2, 3)
                .last(4);

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(3);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithOneElement() {
        Observable<Integer> o = Observable.just(1).last(2);

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithEmpty() {
        Observable<Integer> o = Observable.<Integer> empty()
                .last(1);

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicate() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4, 5, 6)
                .filter(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last(8);

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(6);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicateAndOneElement() {
        Observable<Integer> o = Observable.just(1, 2)
                .filter(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last(4);

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicateAndEmpty() {
        Observable<Integer> o = Observable.just(1)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last(2);

        Observer<Integer> NbpObserver = TestHelper.mockNbpSubscriber();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }
}