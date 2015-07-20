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
package rx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Single.OnSubscribe;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class SingleTest {

    @Test
    public void testHelloWorld() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single.just("Hello World!").subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("Hello World!"));
    }

    @Test
    public void testHelloWorld2() {
        final AtomicReference<String> v = new AtomicReference<String>();
        Single.just("Hello World!").subscribe(new SingleSubscriber<String>() {

            @Override
            public void onSuccess(String value) {
                v.set(value);
            }

            @Override
            public void onError(Throwable error) {

            }

        });
        assertEquals("Hello World!", v.get());
    }

    @Test
    public void testMap() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single.just("A")
                .map(new Func1<String, String>() {

                    @Override
                    public String call(String s) {
                        return s + "B";
                    }

                })
                .subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("AB"));
    }

    @Test
    public void testZip() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<String> a = Single.just("A");
        Single<String> b = Single.just("B");

        Single.zip(a, b, new Func2<String, String, String>() {

            @Override
            public String call(String a, String b) {
                return a + b;
            }

        })
                .subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("AB"));
    }

    @Test
    public void testZipWith() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Single.just("A").zipWith(Single.just("B"), new Func2<String, String, String>() {

            @Override
            public String call(String a, String b) {
                return a + b;
            }

        })
                .subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("AB"));
    }

    @Test
    public void testMerge() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<String> a = Single.just("A");
        Single<String> b = Single.just("B");

        Single.merge(a, b).subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("A", "B"));
    }

    @Test
    public void testMergeWith() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Single.just("A").mergeWith(Single.just("B")).subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("A", "B"));
    }

    @Test
    public void testCreateSuccess() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single.create(new OnSubscribe<String>() {

            @Override
            public void call(SingleSubscriber<? super String> s) {
                s.onSuccess("Hello");
            }

        }).subscribe(ts);
        ts.assertReceivedOnNext(Arrays.asList("Hello"));
    }

    @Test
    public void testCreateError() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single.create(new OnSubscribe<String>() {

            @Override
            public void call(SingleSubscriber<? super String> s) {
                s.onError(new RuntimeException("fail"));
            }

        }).subscribe(ts);
        assertEquals(1, ts.getOnErrorEvents().size());
    }

    @Test
    public void testAsync() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single.just("Hello")
                .subscribeOn(Schedulers.io())
                .map(new Func1<String, String>() {

                    @Override
                    public String call(String v) {
                        System.out.println("SubscribeOn Thread: " + Thread.currentThread());
                        return v;
                    }

                })
                .observeOn(Schedulers.computation())
                .map(new Func1<String, String>() {

                    @Override
                    public String call(String v) {
                        System.out.println("ObserveOn Thread: " + Thread.currentThread());
                        return v;
                    }

                })
                .subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList("Hello"));
    }

    @Test
    public void testFlatMap() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single.just("Hello").flatMap(new Func1<String, Single<String>>() {

            @Override
            public Single<String> call(String s) {
                return Single.just(s + " World!").subscribeOn(Schedulers.computation());
            }

        }).subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertReceivedOnNext(Arrays.asList("Hello World!"));
    }

    @Test
    public void testTimeout() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<String> s = Single.create(new OnSubscribe<String>() {

            @Override
            public void call(SingleSubscriber<? super String> s) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // ignore as we expect this for the test
                }
                s.onSuccess("success");
            }

        }).subscribeOn(Schedulers.io());

        s.timeout(100, TimeUnit.MILLISECONDS).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertError(TimeoutException.class);
    }

    @Test
    public void testTimeoutWithFallback() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<String> s = Single.create(new OnSubscribe<String>() {

            @Override
            public void call(SingleSubscriber<? super String> s) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // ignore as we expect this for the test
                }
                s.onSuccess("success");
            }

        }).subscribeOn(Schedulers.io());

        s.timeout(100, TimeUnit.MILLISECONDS, Single.just("hello")).subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertNoErrors();
        ts.assertValue("hello");
    }

    @Test
    public void testUnsubscribe() throws InterruptedException {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        final AtomicBoolean unsubscribed = new AtomicBoolean();
        final AtomicBoolean interrupted = new AtomicBoolean();
        final CountDownLatch latch = new CountDownLatch(2);

        Single<String> s = Single.create(new OnSubscribe<String>() {

            @Override
            public void call(final SingleSubscriber<? super String> s) {
                final Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            Thread.sleep(5000);
                            s.onSuccess("success");
                        } catch (InterruptedException e) {
                            interrupted.set(true);
                            latch.countDown();
                        }
                    }

                });
                s.add(Subscriptions.create(new Action0() {

                    @Override
                    public void call() {
                        unsubscribed.set(true);
                        t.interrupt();
                        latch.countDown();
                    }

                }));
                t.start();
            }

        });

        s.subscribe(ts);

        Thread.sleep(100);

        ts.unsubscribe();

        if (latch.await(1000, TimeUnit.MILLISECONDS)) {
            assertTrue(unsubscribed.get());
            assertTrue(interrupted.get());
        } else {
            fail("timed out waiting for latch");
        }
    }

    /**
     * Assert that unsubscribe propagates when passing in a SingleSubscriber and not a Subscriber
     */
    @Test
    public void testUnsubscribe2() throws InterruptedException {
        SingleSubscriber<String> ts = new SingleSubscriber<String>() {

            @Override
            public void onSuccess(String value) {
                // not interested in value
            }

            @Override
            public void onError(Throwable error) {
                // not interested in value
            }

        };
        final AtomicBoolean unsubscribed = new AtomicBoolean();
        final AtomicBoolean interrupted = new AtomicBoolean();
        final CountDownLatch latch = new CountDownLatch(2);

        Single<String> s = Single.create(new OnSubscribe<String>() {

            @Override
            public void call(final SingleSubscriber<? super String> s) {
                final Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            Thread.sleep(5000);
                            s.onSuccess("success");
                        } catch (InterruptedException e) {
                            interrupted.set(true);
                            latch.countDown();
                        }
                    }

                });
                s.add(Subscriptions.create(new Action0() {

                    @Override
                    public void call() {
                        unsubscribed.set(true);
                        t.interrupt();
                        latch.countDown();
                    }

                }));
                t.start();
            }

        });

        s.subscribe(ts);

        Thread.sleep(100);

        ts.unsubscribe();

        if (latch.await(1000, TimeUnit.MILLISECONDS)) {
            assertTrue(unsubscribed.get());
            assertTrue(interrupted.get());
        } else {
            fail("timed out waiting for latch");
        }
    }

    /**
     * Assert that unsubscribe propagates when passing in a SingleSubscriber and not a Subscriber
     */
    @Test
    public void testUnsubscribeViaReturnedSubscription() throws InterruptedException {
        final AtomicBoolean unsubscribed = new AtomicBoolean();
        final AtomicBoolean interrupted = new AtomicBoolean();
        final CountDownLatch latch = new CountDownLatch(2);

        Single<String> s = Single.create(new OnSubscribe<String>() {

            @Override
            public void call(final SingleSubscriber<? super String> s) {
                final Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            Thread.sleep(5000);
                            s.onSuccess("success");
                        } catch (InterruptedException e) {
                            interrupted.set(true);
                            latch.countDown();
                        }
                    }

                });
                s.add(Subscriptions.create(new Action0() {

                    @Override
                    public void call() {
                        unsubscribed.set(true);
                        t.interrupt();
                        latch.countDown();
                    }

                }));
                t.start();
            }

        });

        Subscription subscription = s.subscribe();

        Thread.sleep(100);

        subscription.unsubscribe();

        if (latch.await(1000, TimeUnit.MILLISECONDS)) {
            assertTrue(unsubscribed.get());
            assertTrue(interrupted.get());
        } else {
            fail("timed out waiting for latch");
        }
    }
    
    @Test
    public void testBackpressureAsObservable() {
        Single<String> s = Single.create(new OnSubscribe<String>() {

            @Override
            public void call(SingleSubscriber<? super String> t) {
                t.onSuccess("hello");
            }
        });

        TestSubscriber<String> ts = new TestSubscriber<String>() {
            @Override
            public void onStart() {
                request(0);
            }
        };

        s.subscribe(ts);

        ts.assertNoValues();

        ts.requestMore(1);

        ts.assertValue("hello");
    }
    
    @Test
    public void testToObservable() {
    	Observable<String> a = Single.just("a").toObservable();
    	TestSubscriber<String> ts = TestSubscriber.create();
    	a.subscribe(ts);
    	ts.assertValue("a");
    	ts.assertCompleted();
    }
}
