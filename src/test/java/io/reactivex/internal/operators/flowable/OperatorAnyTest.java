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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.Flowable;
import io.reactivex.flowable.TestHelper;
import io.reactivex.functions.*;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorAnyTest {

    @Test
    public void testAnyWithTwoItems() {
        Flowable<Integer> w = Flowable.just(1, 2);
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();
        
        observable.subscribe(observer);
        
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithTwoItems() {
        Flowable<Integer> w = Flowable.just(1, 2);
        Flowable<Boolean> observable = w.isEmpty();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithOneItem() {
        Flowable<Integer> w = Flowable.just(1);
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithOneItem() {
        Flowable<Integer> w = Flowable.just(1);
        Flowable<Boolean> observable = w.isEmpty();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, never()).onNext(true);
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithEmpty() {
        Flowable<Integer> w = Flowable.empty();
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        });

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testIsEmptyWithEmpty() {
        Flowable<Integer> w = Flowable.empty();
        Flowable<Boolean> observable = w.isEmpty();

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onNext(false);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithPredicate1() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testExists1() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, never()).onNext(false);
        verify(observer, times(1)).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithPredicate2() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 1;
            }
        });

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testAnyWithEmptyAndPredicate() {
        // If the source is empty, always output false.
        Flowable<Integer> w = Flowable.empty();
        Flowable<Boolean> observable = w.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t) {
                return true;
            }
        });

        Subscriber<Boolean> observer = TestHelper.mockSubscriber();

        observable.subscribe(observer);
        
        verify(observer, times(1)).onNext(false);
        verify(observer, never()).onNext(true);
        verify(observer, never()).onError(org.mockito.Matchers.any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testWithFollowingFirst() {
        Flowable<Integer> o = Flowable.fromArray(1, 3, 5, 6);
        Flowable<Boolean> anyEven = o.any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer i) {
                return i % 2 == 0;
            }
        });
        
        assertTrue(anyEven.toBlocking().first());
    }
    @Test(timeout = 5000)
    public void testIssue1935NoUnsubscribeDownstream() {
        Flowable<Integer> source = Flowable.just(1).isEmpty()
            .flatMap(new Function<Boolean, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Boolean t1) {
                    return Flowable.just(2).delay(500, TimeUnit.MILLISECONDS);
                }
            });
        
        assertEquals((Object)2, source.toBlocking().first());
    }
    
    @Test
    public void testBackpressureIfNoneRequestedNoneShouldBeDelivered() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>((Long)null);
        
        Flowable.just(1).any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t) {
                return true;
            }
        })
        .subscribe(ts);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotComplete();
    }
    
    @Test
    public void testBackpressureIfOneRequestedOneShouldBeDelivered() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>(1L);
        Flowable.just(1).any(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) {
                return true;
            }
        }).subscribe(ts);
        
        ts.assertTerminated();
        ts.assertNoErrors();
        ts.assertComplete();
        ts.assertValue(true);
    }
    
    @Test
    public void testPredicateThrowsExceptionAndValueInCauseMessage() {
        TestSubscriber<Boolean> ts = new TestSubscriber<Boolean>();
        final IllegalArgumentException ex = new IllegalArgumentException();
        
        Flowable.just("Boo!").any(new Predicate<String>() {
            @Override
            public boolean test(String v) {
                throw ex;
            }
        }).subscribe(ts);
        
        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(ex);
        // FIXME value as last cause?
//        assertTrue(ex.getCause().getMessage().contains("Boo!"));
    }
}