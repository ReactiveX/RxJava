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

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.mockito.InOrder;
import org.reactivestreams.Subscriber;

import io.reactivex.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.*;
import io.reactivex.subscribers.DefaultObserver;

public class OperatorSingleTest {

    @Test
    public void testSingle() {
        Flowable<Integer> observable = Flowable.just(1).single();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithTooManyElements() {
        Flowable<Integer> observable = Flowable.just(1, 2).single();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithEmpty() {
        Flowable<Integer> observable = Flowable.<Integer> empty().single();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }
    
    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsToEmitItem() {
        final AtomicLong request = new AtomicLong();
        Flowable.just(1).doOnRequest(new LongConsumer() {
            @Override
            public void accept(long n) {
                request.addAndGet(n);
            }
        }).toBlocking().single();
        // FIXME single now triggers fast-path
        assertEquals(Long.MAX_VALUE, request.get());
    }

    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsToEmitErrorFromEmpty() {
        final AtomicLong request = new AtomicLong();
        try {
            Flowable.empty().doOnRequest(new LongConsumer() {
                @Override
                public void accept(long n) {
                    request.addAndGet(n);
                }
            }).toBlocking().single();
        } catch (NoSuchElementException e) {
            // FIXME single now triggers fast-path
            assertEquals(Long.MAX_VALUE, request.get());
        }
    }

    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsToEmitErrorFromMoreThanOne() {
        final AtomicLong request = new AtomicLong();
        try {
            Flowable.just(1, 2).doOnRequest(new LongConsumer() {
                @Override
                public void accept(long n) {
                    request.addAndGet(n);
                }
            }).toBlocking().single();
        } catch (IllegalArgumentException e) {
            // FIXME single now triggers fast-path
            assertEquals(Long.MAX_VALUE, request.get());
        }
    }
    
    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsIf1Then2Requested() {
        final List<Long> requests = new ArrayList<Long>();
        Flowable.just(1)
        //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        requests.add(n);
                    }
                })
                //
                .single()
                //
                .subscribe(new DefaultObserver<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                        request(2);
                    }
                });
        // FIXME single now triggers fast-path
        assertEquals(Arrays.asList(Long.MAX_VALUE, Long.MAX_VALUE), requests);
    }
    
    @Test
    public void testSingleDoesNotRequestMoreThanItNeedsIf3Requested() {
        final List<Long> requests = new ArrayList<Long>();
        Flowable.just(1)
        //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        requests.add(n);
                    }
                })
                //
                .single()
                //
                .subscribe(new DefaultObserver<Integer>() {

                    @Override
                    public void onStart() {
                        request(3);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                    }
                });
        // FIXME single now triggers fast-path
        assertEquals(Arrays.asList(Long.MAX_VALUE), requests);
    }
    
    @Test
    public void testSingleRequestsExactlyWhatItNeedsIf1Requested() {
        final List<Long> requests = new ArrayList<Long>();
        Flowable.just(1)
        //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        requests.add(n);
                    }
                })
                //
                .single()
                //
                .subscribe(new DefaultObserver<Integer>() {

                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                    }
                });
        // FIXME single now triggers fast-path
        assertEquals(Arrays.asList(Long.MAX_VALUE), requests);
    }


    @Test
    public void testSingleWithPredicate() {
        Flowable<Integer> observable = Flowable.just(1, 2)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndTooManyElements() {
        Flowable<Integer> observable = Flowable.just(1, 2, 3, 4)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithPredicateAndEmpty() {
        Flowable<Integer> observable = Flowable.just(1)
                .filter(
                new Predicate<Integer>() {

                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single();
        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(NoSuchElementException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefault() {
        Flowable<Integer> observable = Flowable.just(1).single(2);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithTooManyElements() {
        Flowable<Integer> observable = Flowable.just(1, 2).single(3);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithEmpty() {
        Flowable<Integer> observable = Flowable.<Integer> empty()
                .single(1);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(1);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicate() {
        Flowable<Integer> observable = Flowable.just(1, 2)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(4);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndTooManyElements() {
        Flowable<Integer> observable = Flowable.just(1, 2, 3, 4)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(6);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onError(
                isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleOrDefaultWithPredicateAndEmpty() {
        Flowable<Integer> observable = Flowable.just(1)
                .filter(new Predicate<Integer>() {
                    @Override
                    public boolean test(Integer t1) {
                        return t1 % 2 == 0;
                    }
                })
                .single(2);

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        observable.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext(2);
        inOrder.verify(observer, times(1)).onComplete();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testSingleWithBackpressure() {
        Flowable<Integer> observable = Flowable.just(1, 2).single();

        Subscriber<Integer> subscriber = spy(new DefaultObserver<Integer>() {

            @Override
            public void onStart() {
                request(1);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer integer) {
                request(1);
            }
        });
        observable.subscribe(subscriber);

        InOrder inOrder = inOrder(subscriber);
        inOrder.verify(subscriber, times(1)).onError(isA(IllegalArgumentException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test(timeout = 30000)
    public void testIssue1527() throws InterruptedException {
        //https://github.com/ReactiveX/RxJava/pull/1527
        Flowable<Integer> source = Flowable.just(1, 2, 3, 4, 5, 6);
        Flowable<Integer> reduced = source.reduce(new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(Integer i1, Integer i2) {
                return i1 + i2;
            }
        });

        Integer r = reduced.toBlocking().first();
        assertEquals(21, r.intValue());
    }
}