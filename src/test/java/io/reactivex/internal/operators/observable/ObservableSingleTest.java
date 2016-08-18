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
import io.reactivex.functions.*;

public class ObservableSingleTest {

    @Test
    public void testSingle() {
        Observable<Integer> o = Observable.just(1).single();

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithTooManyElements() {
        Observable<Integer> o = Observable.just(1, 2).single();

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithEmpty() {
        Observable<Integer> o = Observable.<Integer> empty().single();

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }
    
    @Test
    public void testSingleWithPredicate() {
        Observable<Integer> o = Observable.just(1, 2)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single();

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndTooManyElements() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single();

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndEmpty() {
        Observable<Integer> o = Observable.just(1)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single();
        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefault() {
        Observable<Integer> o = Observable.just(1).single(2);

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithTooManyElements() {
        Observable<Integer> o = Observable.just(1, 2).single(3);

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithEmpty() {
        Observable<Integer> o = Observable.<Integer> empty()
                .single(1);

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(1);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicate() {
        Observable<Integer> o = Observable.just(1, 2)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(4);

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndTooManyElements() {
        Observable<Integer> o = Observable.just(1, 2, 3, 4)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(6);

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndEmpty() {
        Observable<Integer> o = Observable.just(1)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(2);

        Observer<Integer> NbpObserver = TestHelper.mockObserver();
        o.subscribe(NbpObserver);

        InOrder inOrder = inOrder(NbpObserver);
        inOrder.verify(NbpObserver, times(1)).onNext(2);
        inOrder.verify(NbpObserver, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 30000)
    public void testIssue1527() throws InterruptedException {
        //https://github.com/ReactiveX/RxJava/pull/1527
        Observable<Integer> source = Observable.just(1, 2, 3, 4, 5, 6);
        Observable<Integer> reduced = source.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer i1, Integer i2) {
                return i1 + i2;
            }
        });

        Integer r = reduced.blockingFirst();
        assertEquals(21, r.intValue());
    }
}