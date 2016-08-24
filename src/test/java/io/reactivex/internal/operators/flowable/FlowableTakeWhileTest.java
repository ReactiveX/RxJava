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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.processors.*;
import io.reactivex.subscribers.TestSubscriber;

public class FlowableTakeWhileTest {

    @Test
    public void testTakeWhile1() {
        Flowable<Integer> w = Flowable.just(1, 2, 3);
        Flowable<Integer> take = w.takeWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer input) {
                return input < 3;
            }
        });

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        take.subscribe(observer);
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, never()).onNext(3);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTakeWhileOnSubject1() {
        FlowableProcessor<Integer> s = PublishProcessor.create();
        Flowable<Integer> take = s.takeWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer input) {
                return input < 3;
            }
        });

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        take.subscribe(observer);

        s.onNext(1);
        s.onNext(2);
        s.onNext(3);
        s.onNext(4);
        s.onNext(5);
        s.onComplete();

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, never()).onNext(3);
        verify(observer, never()).onNext(4);
        verify(observer, never()).onNext(5);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTakeWhile2() {
        Flowable<String> w = Flowable.just("one", "two", "three");
        Flowable<String> take = w.takeWhile(new Predicate<String>() {
            int index = 0;

            @Override
            public boolean test(String input) {
                return index++ < 2;
            }
        });

        Subscriber<String> observer = TestHelper.mockSubscriber();
        take.subscribe(observer);
        verify(observer, times(1)).onNext("one");
        verify(observer, times(1)).onNext("two");
        verify(observer, never()).onNext("three");
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
    }

    @Test
    public void testTakeWhileDoesntLeakErrors() {
        Flowable<String> source = Flowable.unsafeCreate(new Publisher<String>() {
            @Override
            public void subscribe(Subscriber<? super String> observer) {
                observer.onSubscribe(new BooleanSubscription());
                observer.onNext("one");
                observer.onError(new Throwable("test failed"));
            }
        });

        source.takeWhile(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                return false;
            }
        }).blockingLast("");
    }

    @Test
    public void testTakeWhileProtectsPredicateCall() {
        TestObservable source = new TestObservable(mock(Subscription.class), "one");
        final RuntimeException testException = new RuntimeException("test exception");

        Subscriber<String> observer = TestHelper.mockSubscriber();
        Flowable<String> take = Flowable.unsafeCreate(source)
                .takeWhile(new Predicate<String>() {
            @Override
            public boolean test(String s) {
                throw testException;
            }
        });
        take.subscribe(observer);

        // wait for the Observable to complete
        try {
            source.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        verify(observer, never()).onNext(any(String.class));
        verify(observer, times(1)).onError(testException);
    }

    @Test
    public void testUnsubscribeAfterTake() {
        Subscription s = mock(Subscription.class);
        TestObservable w = new TestObservable(s, "one", "two", "three");

        Subscriber<String> observer = TestHelper.mockSubscriber();
        Flowable<String> take = Flowable.unsafeCreate(w)
                .takeWhile(new Predicate<String>() {
            int index = 0;

            @Override
            public boolean test(String s) {
                return index++ < 1;
            }
        });
        take.subscribe(observer);

        // wait for the Observable to complete
        try {
            w.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        System.out.println("TestObservable thread finished");
        verify(observer, times(1)).onNext("one");
        verify(observer, never()).onNext("two");
        verify(observer, never()).onNext("three");
        verify(s, times(1)).cancel();
    }

    private static class TestObservable implements Publisher<String> {

        final Subscription s;
        final String[] values;
        Thread t = null;

        public TestObservable(Subscription s, String... values) {
            this.s = s;
            this.values = values;
        }

        @Override
        public void subscribe(final Subscriber<? super String> observer) {
            System.out.println("TestObservable subscribed to ...");
            observer.onSubscribe(s);
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        observer.onComplete();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
        }
    }
    
    @Test
    public void testBackpressure() {
        Flowable<Integer> source = Flowable.range(1, 1000).takeWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 100;
            }
        });
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(5L);
        
        source.subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertValues(1, 2, 3, 4, 5);
        
        ts.request(5);

        ts.assertNoErrors();
        ts.assertValues(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }
    
    @Test
    public void testNoUnsubscribeDownstream() {
        Flowable<Integer> source = Flowable.range(1, 1000).takeWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer t1) {
                return t1 < 2;
            }
        });
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        
        source.subscribe(ts);
        
        ts.assertNoErrors();
        ts.assertValue(1);
        
        Assert.assertFalse("Unsubscribed!", ts.isCancelled());
    }
    
    @Test
    public void testErrorCauseIncludesLastValue() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Flowable.just("abc").takeWhile(new Predicate<String>() {
            @Override
            public boolean test(String t1) {
                throw new TestException();
            }
        }).subscribe(ts);
        
        ts.assertTerminated();
        ts.assertNoValues();
        ts.assertError(TestException.class);
        // FIXME last cause value not recorded
//        assertTrue(ts.getOnErrorEvents().get(0).getCause().getMessage().contains("abc"));
    }
    
}