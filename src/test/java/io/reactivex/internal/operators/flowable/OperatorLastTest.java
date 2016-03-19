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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.Predicate;

public class OperatorLastTest {

    @Test
    public void testLastWithElements() {
        Flowable<Integer> last = Flowable.just(1, 2, 3).last();
        assertEquals(3, last.toBlocking().single().intValue());
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastWithNoElements() {
        Flowable<?> last = Flowable.empty().last();
        last.toBlocking().single();
    }

    @Test
    public void testLastMultiSubscribe() {
        Flowable<Integer> last = Flowable.just(1, 2, 3).last();
        assertEquals(3, last.toBlocking().single().intValue());
        assertEquals(3, last.toBlocking().single().intValue());
    }

    @Test
    public void testLastViaObservable() {
        Flowable.just(1, 2, 3).last();
    }

    @Test
    public void testLast() {
        Flowable<Integer> observable = Flowable.just(1, 2, 3).last();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(3);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithOneElement() {
        Flowable<Integer> observable = Flowable.just(1).last();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithEmpty() {
        Flowable<Integer> observable = Flowable.<Integer> empty().last();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicate() {
        Flowable<Integer> observable = Flowable.just(1, 2, 3, 4, 5, 6)
                .filter(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(6);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicateAndOneElement() {
        Flowable<Integer> observable = Flowable.just(1, 2)
            .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
            .last();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastWithPredicateAndEmpty() {
        Flowable<Integer> observable = Flowable.just(1)
            .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                }).last();
        
        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefault() {
        Flowable<Integer> observable = Flowable.just(1, 2, 3)
                .last(4);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(3);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithOneElement() {
        Flowable<Integer> observable = Flowable.just(1).last(2);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithEmpty() {
        Flowable<Integer> observable = Flowable.<Integer> empty()
                .last(1);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicate() {
        Flowable<Integer> observable = Flowable.just(1, 2, 3, 4, 5, 6)
                .filter(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last(8);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(6);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicateAndOneElement() {
        Flowable<Integer> observable = Flowable.just(1, 2)
                .filter(new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last(4);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testLastOrDefaultWithPredicateAndEmpty() {
        Flowable<Integer> observable = Flowable.just(1)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .last(2);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }
}