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

package io.reactivex;

import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.Observable.Operator;
import io.reactivex.subscribers.*;

public class SubscriberTest {

    /**
     * Should request n for whatever the final Subscriber asks for
     */
    @Test
    public void testRequestFromFinalSubscribeWithRequestValue() {
        TestSubscriber<String> s = new TestSubscriber<>((Long)null);
        s.request(10);
        final AtomicLong r = new AtomicLong();
        s.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                r.set(n);
            }
            
            @Override
            public void cancel() {
                
            }

        });
        assertEquals(10, r.get());
    }

    /**
     * Should request -1 for infinite
     */
    @Test
    public void testRequestFromFinalSubscribeWithoutRequestValue() {
        TestSubscriber<String> s = new TestSubscriber<>();
        final AtomicLong r = new AtomicLong();
        s.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                r.set(n);
            }
            
            @Override
            public void cancel() {
                
            }

        });
        assertEquals(Long.MAX_VALUE, r.get());
    }

    @Test
    public void testRequestFromChainedOperator() {
        TestSubscriber<String> s = new TestSubscriber<>();
        Operator<String, String> o = s1 -> new Subscriber<String>() {

            @Override
            public void onSubscribe(Subscription a) {
                s1.onSubscribe(a);
            }
            
            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String t) {

            }

        };
        s.request(10);
        Subscriber<? super String> ns = o.apply(s);

        final AtomicLong r = new AtomicLong();
        // set set the producer at the top of the chain (ns) and it should flow through the operator to the (s) subscriber
        // and then it should request up with the value set on the final Subscriber (s)
        ns.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                r.set(n);
            }
            
            @Override
            public void cancel() {
                
            }

        });
        assertEquals(10, r.get());
    }

    @Test
    public void testRequestFromDecoupledOperator() {
        TestSubscriber<String> s = new TestSubscriber<>((Long)null);
        Operator<String, String> o = s1 -> new Subscriber<String>() {

            @Override
            public void onSubscribe(Subscription a) {
                s1.onSubscribe(a);
            }
            
            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(String t) {

            }

        };
        s.request(10);
        Subscriber<? super String> ns = o.apply(s);

        final AtomicLong r = new AtomicLong();
        // set set the producer at the top of the chain (ns) and it should flow through the operator to the (s) subscriber
        // and then it should request up with the value set on the final Subscriber (s)
        ns.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                r.set(n);
            }
            
            @Override
            public void cancel() {
                
            }

        });
        assertEquals(10, r.get());
    }

    @Test
    public void testRequestFromDecoupledOperatorThatRequestsN() {
        TestSubscriber<String> s = new TestSubscriber<>();
        final AtomicLong innerR = new AtomicLong();
        Operator<String, String> o = child -> {
            // we want to decouple the chain so set our own Producer on the child instead of it coming from the parent
            child.onSubscribe(new Subscription() {

                @Override
                public void request(long n) {
                    innerR.set(n);
                }
                
                @Override
                public void cancel() {
                    
                }

            });

            AsyncObserver<String> as = new AsyncObserver<String>() {
                
                @Override
                protected void onStart() {
                    // we request 99 up to the parent
                    request(99);
                }
                
                @Override
                public void onComplete() {

                }

                @Override
                public void onError(Throwable e) {

                }

                @Override
                public void onNext(String t) {

                }
                
                
            };
            return as;
        };
        s.request(10);
        Subscriber<? super String> ns = o.apply(s);

        final AtomicLong r = new AtomicLong();
        // set set the producer at the top of the chain (ns) and it should flow through the operator to the (s) subscriber
        // and then it should request up with the value set on the final Subscriber (s)
        ns.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                r.set(n);
            }
            
            @Override
            public void cancel() {
                
            }

        });
        assertEquals(99, r.get());
        assertEquals(10, innerR.get());
    }

    @Test
    public void testRequestToObservable() {
        TestSubscriber<Integer> ts = new TestSubscriber<>();
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Observable.<Integer>create(s -> s.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                requested.set(n);
            }

            @Override
            public void cancel() {
                
            }
        })).subscribe(ts);
        assertEquals(3, requested.get());
    }

    @Test
    public void testRequestThroughMap() {
        TestSubscriber<Integer> ts = new TestSubscriber<>((Long)null);
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Observable.<Integer>create(s -> s.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                requested.set(n);
            }

            @Override
            public void cancel() {
                
            }
        })).map(v -> v).subscribe(ts);
        assertEquals(3, requested.get());
    }

    @Test
    public void testRequestThroughTakeThatReducesRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<>((Long)null);
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Observable.<Integer>create(s -> s.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                requested.set(n);
            }

            @Override
            public void cancel() {
                
            }
            
        })).take(2).subscribe(ts);
        
        // FIXME the take now requests Long.MAX_PATH if downstream requests at least the limit
        assertEquals(Long.MAX_VALUE, requested.get());
    }

    @Test
    public void testRequestThroughTakeWhereRequestIsSmallerThanTake() {
        TestSubscriber<Integer> ts = new TestSubscriber<>((Long)null);
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Observable.<Integer>create(s -> s.onSubscribe(new Subscription() {

            @Override
            public void request(long n) {
                requested.set(n);
            }
            
            @Override
            public void cancel() {
                
            }

        })).take(10).subscribe(ts);
        assertEquals(3, requested.get());
    }

    @Test
    public void testOnStartCalledOnceViaSubscribe() {
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1, 2, 3, 4).take(2).subscribe(new Observer<Integer>() {

            @Override
            public void onStart() {
                c.incrementAndGet();
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
                request(1);
            }

        });

        assertEquals(1, c.get());
    }

    @Test
    public void testOnStartCalledOnceViaUnsafeSubscribe() {
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1, 2, 3, 4).take(2).unsafeSubscribe(new Observer<Integer>() {

            @Override
            public void onStart() {
                c.incrementAndGet();
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
                request(1);
            }

        });

        assertEquals(1, c.get());
    }

    @Test
    public void testOnStartCalledOnceViaLift() {
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1, 2, 3, 4).lift(new Operator<Integer, Integer>() {

            @Override
            public Subscriber<? super Integer> apply(final Subscriber<? super Integer> child) {
                return new Observer<Integer>() {

                    @Override
                    public void onStart() {
                        c.incrementAndGet();
                        request(1);
                    }

                    @Override
                    public void onComplete() {
                        child.onComplete();
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @Override
                    public void onNext(Integer t) {
                        child.onNext(t);
                        request(1);
                    }

                };
            }

        }).subscribe();

        assertEquals(1, c.get());
    }
    
    @Ignore("Non-positive requests are relayed to the plugin and is a no-op otherwise")
    @Test
    public void testNegativeRequestThrowsIllegalArgumentException() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> exception = new AtomicReference<>();
        Observable.just(1,2,3,4).subscribe(new Observer<Integer>() {

            @Override
            public void onStart() {
                request(1);
            }
            
            @Override
            public void onComplete() {
                
            }

            @Override
            public void onError(Throwable e) {
               exception.set(e);
               latch.countDown();
            }

            @Override
            public void onNext(Integer t) {
                request(-1);
                request(1);
            }});
        
        Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
        Assert.assertTrue(exception.get() instanceof IllegalArgumentException);
    }
    
    @Test
    public void testOnStartRequestsAreAdditive() {
        final List<Integer> list = new ArrayList<>();
        Observable.just(1,2,3,4,5).subscribe(new Observer<Integer>() {
            @Override
            public void onStart() {
                request(3);
                request(2);
            }
            
            @Override
            public void onComplete() {
                
            }

            @Override
            public void onError(Throwable e) {
                
            }

            @Override
            public void onNext(Integer t) {
                list.add(t);
            }});
        assertEquals(Arrays.asList(1,2,3,4,5), list);
    }
    
    @Test
    public void testOnStartRequestsAreAdditiveAndOverflowBecomesMaxValue() {
        final List<Integer> list = new ArrayList<>();
        Observable.just(1,2,3,4,5).subscribe(new Observer<Integer>() {
            @Override
            public void onStart() {
                request(2);
                request(Long.MAX_VALUE-1);
            }
            
            @Override
            public void onComplete() {
                
            }

            @Override
            public void onError(Throwable e) {
                
            }

            @Override
            public void onNext(Integer t) {
                list.add(t);
            }});
        assertEquals(Arrays.asList(1,2,3,4,5), list);
    }
}