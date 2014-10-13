/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

public class SubscriberTest {

    /**
     * Should request n for whatever the final Subscriber asks for
     */
    @Test
    public void testRequestFromFinalSubscribeWithRequestValue() {
        Subscriber<String> s = new TestSubscriber<String>();
        s.request(10);
        final AtomicLong r = new AtomicLong();
        s.setProducer(new Producer() {

            @Override
            public void request(long n) {
                r.set(n);
            }

        });
        assertEquals(10, r.get());
    }

    /**
     * Should request -1 for infinite
     */
    @Test
    public void testRequestFromFinalSubscribeWithoutRequestValue() {
        Subscriber<String> s = new TestSubscriber<String>();
        final AtomicLong r = new AtomicLong();
        s.setProducer(new Producer() {

            @Override
            public void request(long n) {
                r.set(n);
            }

        });
        assertEquals(Long.MAX_VALUE, r.get());
    }

    @Test
    public void testRequestFromChainedOperator() {
        Subscriber<String> s = new TestSubscriber<String>();
        Operator<String, String> o = new Operator<String, String>() {

            @Override
            public Subscriber<? super String> call(Subscriber<? super String> s) {
                return new Subscriber<String>(s) {

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String t) {

                    }

                };
            }

        };
        s.request(10);
        Subscriber<? super String> ns = o.call(s);

        final AtomicLong r = new AtomicLong();
        // set set the producer at the top of the chain (ns) and it should flow through the operator to the (s) subscriber
        // and then it should request up with the value set on the final Subscriber (s)
        ns.setProducer(new Producer() {

            @Override
            public void request(long n) {
                r.set(n);
            }

        });
        assertEquals(10, r.get());
    }

    @Test
    public void testRequestFromDecoupledOperator() {
        Subscriber<String> s = new TestSubscriber<String>();
        Operator<String, String> o = new Operator<String, String>() {

            @Override
            public Subscriber<? super String> call(Subscriber<? super String> s) {
                return new Subscriber<String>() {

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String t) {

                    }

                };
            }

        };
        s.request(10);
        Subscriber<? super String> ns = o.call(s);

        final AtomicLong r = new AtomicLong();
        // set set the producer at the top of the chain (ns) and it should flow through the operator to the (s) subscriber
        // and then it should request up with the value set on the final Subscriber (s)
        ns.setProducer(new Producer() {

            @Override
            public void request(long n) {
                r.set(n);
            }

        });
        // this will be Long.MAX_VALUE because it is decoupled and nothing requsted on the Operator subscriber
        assertEquals(Long.MAX_VALUE, r.get());
    }

    @Test
    public void testRequestFromDecoupledOperatorThatRequestsN() {
        Subscriber<String> s = new TestSubscriber<String>();
        final AtomicLong innerR = new AtomicLong();
        Operator<String, String> o = new Operator<String, String>() {

            @Override
            public Subscriber<? super String> call(Subscriber<? super String> child) {
                // we want to decouple the chain so set our own Producer on the child instead of it coming from the parent
                child.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        innerR.set(n);
                    }

                });

                Subscriber<String> as = new Subscriber<String>() {

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(String t) {

                    }

                };
                // we request 99 up to the parent
                as.request(99);
                return as;
            }

        };
        s.request(10);
        Subscriber<? super String> ns = o.call(s);

        final AtomicLong r = new AtomicLong();
        // set set the producer at the top of the chain (ns) and it should flow through the operator to the (s) subscriber
        // and then it should request up with the value set on the final Subscriber (s)
        ns.setProducer(new Producer() {

            @Override
            public void request(long n) {
                r.set(n);
            }

        });
        assertEquals(99, r.get());
        assertEquals(10, innerR.get());
    }

    @Test
    public void testRequestToObservable() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> s) {
                s.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                });
            }

        }).subscribe(ts);
        assertEquals(3, requested.get());
    }

    @Test
    public void testRequestThroughMap() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> s) {
                s.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                });
            }

        }).map(new Func1<Integer, Integer>() {

            @Override
            public Integer call(Integer t1) {
                return t1;
            }

        }).subscribe(ts);
        assertEquals(3, requested.get());
    }

    @Test
    public void testRequestThroughTakeThatReducesRequest() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> s) {
                s.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                });
            }

        }).take(2).subscribe(ts);
        assertEquals(2, requested.get());
    }

    @Test
    public void testRequestThroughTakeWhereRequestIsSmallerThanTake() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        ts.request(3);
        final AtomicLong requested = new AtomicLong();
        Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> s) {
                s.setProducer(new Producer() {

                    @Override
                    public void request(long n) {
                        requested.set(n);
                    }

                });
            }

        }).take(10).subscribe(ts);
        assertEquals(3, requested.get());
    }

    @Test
    public void testOnStartCalledOnceViaSubscribe() {
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1, 2, 3, 4).take(2).subscribe(new Subscriber<Integer>() {

            @Override
            public void onStart() {
                c.incrementAndGet();
                request(1);
            }

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                System.out.println(t);
                request(1);
            }

        });

        assertEquals(1, c.get());
    }

    @Test
    public void testOnStartCalledOnceViaUnsafeSubscribe() {
        final AtomicInteger c = new AtomicInteger();
        Observable.just(1, 2, 3, 4).take(2).unsafeSubscribe(new Subscriber<Integer>() {

            @Override
            public void onStart() {
                c.incrementAndGet();
                request(1);
            }

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                System.out.println(t);
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
            public Subscriber<? super Integer> call(final Subscriber<? super Integer> child) {
                return new Subscriber<Integer>() {

                    @Override
                    public void onStart() {
                        c.incrementAndGet();
                        request(1);
                    }

                    @Override
                    public void onCompleted() {
                        child.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        child.onError(e);
                    }

                    @Override
                    public void onNext(Integer t) {
                        System.out.println(t);
                        child.onNext(t);
                        request(1);
                    }

                };
            }

        }).subscribe();

        assertEquals(1, c.get());
    }
}
