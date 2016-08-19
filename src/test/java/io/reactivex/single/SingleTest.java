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

package io.reactivex.single;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class SingleTest {

    @Test
    public void testHelloWorld() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single.just("Hello World!").toFlowable().subscribe(ts);
        ts.assertValueSequence(Arrays.asList("Hello World!"));
    }

    @Test
    public void testHelloWorld2() {
        final AtomicReference<String> v = new AtomicReference<String>();
        Single.just("Hello World!").subscribe(new SingleObserver<String>() {

            @Override
            public void onSubscribe(Disposable d) {
                
            }
            
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
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String s) {
                        return s + "B";
                    }
                })
                .toFlowable().subscribe(ts);
        ts.assertValueSequence(Arrays.asList("AB"));
    }

    @Test
    public void testZip() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<String> a = Single.just("A");
        Single<String> b = Single.just("B");

        Single.zip(a, b, new BiFunction<String, String, String>() {
            @Override
            public String apply(String a1, String b1) {
                return a1 + b1;
            }
        })
        .toFlowable().subscribe(ts);
        ts.assertValueSequence(Arrays.asList("AB"));
    }

    @Test
    public void testZipWith() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Single.just("A").zipWith(Single.just("B"), new BiFunction<String, String, String>() {
            @Override
            public String apply(String a1, String b1) {
                return a1 + b1;
            }
        })
        .toFlowable().subscribe(ts);
        ts.assertValueSequence(Arrays.asList("AB"));
    }

    @Test
    public void testMerge() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<String> a = Single.just("A");
        Single<String> b = Single.just("B");

        Single.merge(a, b).subscribe(ts);
        ts.assertValueSequence(Arrays.asList("A", "B"));
    }

    @Test
    public void testMergeWith() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Single.just("A").mergeWith(Single.just("B")).subscribe(ts);
        ts.assertValueSequence(Arrays.asList("A", "B"));
    }

    @Test
    public void testCreateSuccess() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        
        Single.unsafeCreate(new SingleSource<Object>() {
            @Override
            public void subscribe(SingleObserver<? super Object> s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                s.onSuccess("Hello");
            }
        }).toFlowable().subscribe(ts);
        
        ts.assertValueSequence(Arrays.asList("Hello"));
    }

    @Test
    public void testCreateError() {
        TestSubscriber<Object> ts = new TestSubscriber<Object>();
        Single.unsafeCreate(new SingleSource<Object>() {
            @Override
            public void subscribe(SingleObserver<? super Object> s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                s.onError(new RuntimeException("fail"));
            }
        }).toFlowable().subscribe(ts);
        
        ts.assertError(RuntimeException.class);
        ts.assertErrorMessage("fail");
    }

    @Test
    public void testAsync() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single.just("Hello")
                .subscribeOn(Schedulers.io())
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String v) {
                        System.out.println("SubscribeOn Thread: " + Thread.currentThread());
                        return v;
                    }
                })
                .observeOn(Schedulers.computation())
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String v) {
                        System.out.println("ObserveOn Thread: " + Thread.currentThread());
                        return v;
                    }
                })
                .toFlowable().subscribe(ts);
        ts.awaitTerminalEvent();
        ts.assertValueSequence(Arrays.asList("Hello"));
    }

    @Test
    public void testFlatMap() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single.just("Hello").flatMap(new Function<String, Single<String>>() {
            @Override
            public Single<String> apply(String s) {
                return Single.just(s + " World!").subscribeOn(Schedulers.computation());
            }
        }
        ).toFlowable().subscribe(ts);
        if (!ts.awaitTerminalEvent(5, TimeUnit.SECONDS)) {
            ts.cancel();
            Assert.fail("TestSubscriber timed out.");
        }
        ts.assertValueSequence(Arrays.asList("Hello World!"));
    }

    @Test
    public void testTimeout() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<String> s1 = Single.<String>unsafeCreate(new SingleSource<String>() {
            @Override
            public void subscribe(SingleObserver<? super String> s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // ignore as we expect this for the test
                }
                s.onSuccess("success");
            }
        }).subscribeOn(Schedulers.io());

        s1.timeout(100, TimeUnit.MILLISECONDS).toFlowable().subscribe(ts);

        ts.awaitTerminalEvent();
        ts.assertError(TimeoutException.class);
    }

    @Test
    public void testTimeoutWithFallback() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<String> s1 = Single.<String>unsafeCreate(new SingleSource<String>() {
            @Override
            public void subscribe(SingleObserver<? super String> s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        // ignore as we expect this for the test
                    }
                    s.onSuccess("success");
            }
        }).subscribeOn(Schedulers.io());

        s1.timeout(100, TimeUnit.MILLISECONDS, Single.just("hello")).toFlowable().subscribe(ts);

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

        Single<String> s1 = Single.<String>unsafeCreate(new SingleSource<String>() {
            @Override
            public void subscribe(final SingleObserver<? super String> s) {
                SerialDisposable sd = new SerialDisposable();
                s.onSubscribe(sd);
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
                sd.replace(Disposables.from(new Runnable() {
                    @Override
                    public void run() {
                        unsubscribed.set(true);
                        t.interrupt();
                        latch.countDown();
                    }
                }));
                t.start();
            }
        });

        s1.toFlowable().subscribe(ts);

        Thread.sleep(100);

        ts.dispose();

        if (latch.await(1000, TimeUnit.MILLISECONDS)) {
            assertTrue(unsubscribed.get());
            assertTrue(interrupted.get());
        } else {
            fail("timed out waiting for latch");
        }
    }

    /**
     * Assert that unsubscribe propagates when passing in a SingleSubscriber and not a Subscriber
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void testUnsubscribe2() throws InterruptedException {
        final SerialDisposable sd = new SerialDisposable();
        SingleObserver<String> ts = new SingleObserver<String>() {

            @Override
            public void onSubscribe(Disposable d) {
                sd.replace(d);
            }
            
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

        Single<String> s1 = Single.unsafeCreate(new SingleSource<String>() {
            @Override
            public void subscribe(final SingleObserver<? super String> s) {
                SerialDisposable sd = new SerialDisposable();
                s.onSubscribe(sd);
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
                sd.replace(Disposables.from(new Runnable() {
                    @Override
                    public void run() {
                        unsubscribed.set(true);
                        t.interrupt();
                        latch.countDown();
                    }
                }));
                t.start();

            }
        });

        s1.subscribe(ts);

        Thread.sleep(100);

        sd.dispose();

        if (latch.await(1000, TimeUnit.MILLISECONDS)) {
            assertTrue(unsubscribed.get());
            assertTrue(interrupted.get());
        } else {
            fail("timed out waiting for latch");
        }
    }

    /**
     * Assert that unsubscribe propagates when passing in a SingleSubscriber and not a Subscriber
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void testUnsubscribeViaReturnedSubscription() throws InterruptedException {
        final AtomicBoolean unsubscribed = new AtomicBoolean();
        final AtomicBoolean interrupted = new AtomicBoolean();
        final CountDownLatch latch = new CountDownLatch(2);

        Single<String> s1 = Single.unsafeCreate(new SingleSource<String>() {
            @Override
            public void subscribe(final SingleObserver<? super String> s) {
                SerialDisposable sd = new SerialDisposable();
                s.onSubscribe(sd);
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
                sd.replace(Disposables.from(new Runnable() {
                    @Override
                    public void run() {
                        unsubscribed.set(true);
                        t.interrupt();
                        latch.countDown();
                    }
                }));
                t.start();

            }
        });

        Disposable subscription = s1.subscribe();

        Thread.sleep(100);

        subscription.dispose();

        if (latch.await(1000, TimeUnit.MILLISECONDS)) {
            assertTrue(unsubscribed.get());
            assertTrue(interrupted.get());
        } else {
            fail("timed out waiting for latch");
        }
    }
    
    @Test
    public void testBackpressureAsObservable() {
        Single<String> s = Single.unsafeCreate(new SingleSource<String>() {
            @Override
            public void subscribe(SingleObserver<? super String> t) {
                t.onSubscribe(EmptyDisposable.INSTANCE);
                t.onSuccess("hello");
            }
        });

        TestSubscriber<String> ts = new TestSubscriber<String>(0L);

        s.toFlowable().subscribe(ts);

        ts.assertNoValues();

        ts.request(1);

        ts.assertValue("hello");
    }
    
    @Test
    public void testToObservable() {
    	Flowable<String> a = Single.just("a").toFlowable();
    	TestSubscriber<String> ts = new TestSubscriber<String>();
    	a.subscribe(ts);
    	ts.assertValue("a");
    	ts.assertNoErrors();
    	ts.assertComplete();
    }
}

