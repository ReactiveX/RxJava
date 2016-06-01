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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Single.OnSubscribe;
import rx.exceptions.*;
import rx.functions.*;
import rx.observers.SafeSubscriber;
import rx.observers.TestSubscriber;
import rx.plugins.RxJavaPluginsTest;
import rx.plugins.RxJavaSingleExecutionHook;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;
import rx.singles.BlockingSingle;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

public class SingleTest {

    private static RxJavaSingleExecutionHook hookSpy;

    @Before
    public void setUp() throws Exception {
        hookSpy = spy(
                new RxJavaPluginsTest.RxJavaSingleExecutionHookTestImpl());
        Single.hook = hookSpy;
    }

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
    public void zip2Singles() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<Integer> a = Single.just(1);
        Single<Integer> b = Single.just(2);

        Single.zip(a, b, new Func2<Integer, Integer, String>() {

            @Override
            public String call(Integer a, Integer b) {
                return "" + a + b;
            }

        })
                .subscribe(ts);

        ts.assertValue("12");
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void zip3Singles() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<Integer> a = Single.just(1);
        Single<Integer> b = Single.just(2);
        Single<Integer> c = Single.just(3);

        Single.zip(a, b, c, new Func3<Integer, Integer, Integer, String>() {

            @Override
            public String call(Integer a, Integer b, Integer c) {
                return "" + a + b + c;
            }

        })
                .subscribe(ts);

        ts.assertValue("123");
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void zip4Singles() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<Integer> a = Single.just(1);
        Single<Integer> b = Single.just(2);
        Single<Integer> c = Single.just(3);
        Single<Integer> d = Single.just(4);

        Single.zip(a, b, c, d, new Func4<Integer, Integer, Integer, Integer, String>() {

            @Override
            public String call(Integer a, Integer b, Integer c, Integer d) {
                return "" + a + b + c + d;
            }

        })
                .subscribe(ts);

        ts.assertValue("1234");
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void zip5Singles() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<Integer> a = Single.just(1);
        Single<Integer> b = Single.just(2);
        Single<Integer> c = Single.just(3);
        Single<Integer> d = Single.just(4);
        Single<Integer> e = Single.just(5);

        Single.zip(a, b, c, d, e, new Func5<Integer, Integer, Integer, Integer, Integer, String>() {

            @Override
            public String call(Integer a, Integer b, Integer c, Integer d, Integer e) {
                return "" + a + b + c + d + e;
            }

        })
                .subscribe(ts);

        ts.assertValue("12345");
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void zip6Singles() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<Integer> a = Single.just(1);
        Single<Integer> b = Single.just(2);
        Single<Integer> c = Single.just(3);
        Single<Integer> d = Single.just(4);
        Single<Integer> e = Single.just(5);
        Single<Integer> f = Single.just(6);

        Single.zip(a, b, c, d, e, f, new Func6<Integer, Integer, Integer, Integer, Integer, Integer, String>() {

            @Override
            public String call(Integer a, Integer b, Integer c, Integer d, Integer e, Integer f) {
                return "" + a + b + c + d + e + f;
            }

        })
                .subscribe(ts);

        ts.assertValue("123456");
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void zip7Singles() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<Integer> a = Single.just(1);
        Single<Integer> b = Single.just(2);
        Single<Integer> c = Single.just(3);
        Single<Integer> d = Single.just(4);
        Single<Integer> e = Single.just(5);
        Single<Integer> f = Single.just(6);
        Single<Integer> g = Single.just(7);

        Single.zip(a, b, c, d, e, f, g, new Func7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, String>() {

            @Override
            public String call(Integer a, Integer b, Integer c, Integer d, Integer e, Integer f, Integer g) {
                return "" + a + b + c + d + e + f + g;
            }

        })
                .subscribe(ts);

        ts.assertValue("1234567");
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void zip8Singles() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<Integer> a = Single.just(1);
        Single<Integer> b = Single.just(2);
        Single<Integer> c = Single.just(3);
        Single<Integer> d = Single.just(4);
        Single<Integer> e = Single.just(5);
        Single<Integer> f = Single.just(6);
        Single<Integer> g = Single.just(7);
        Single<Integer> h = Single.just(8);

        Single.zip(a, b, c, d, e, f, g, h, new Func8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, String>() {

            @Override
            public String call(Integer a, Integer b, Integer c, Integer d, Integer e, Integer f, Integer g, Integer h) {
                return "" + a + b + c + d + e + f + g + h;
            }

        })
                .subscribe(ts);

        ts.assertValue("12345678");
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void zip9Singles() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<Integer> a = Single.just(1);
        Single<Integer> b = Single.just(2);
        Single<Integer> c = Single.just(3);
        Single<Integer> d = Single.just(4);
        Single<Integer> e = Single.just(5);
        Single<Integer> f = Single.just(6);
        Single<Integer> g = Single.just(7);
        Single<Integer> h = Single.just(8);
        Single<Integer> i = Single.just(9);

        Single.zip(a, b, c, d, e, f, g, h, i, new Func9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, String>() {

            @Override
            public String call(Integer a, Integer b, Integer c, Integer d, Integer e, Integer f, Integer g, Integer h, Integer i) {
                return "" + a + b + c + d + e + f + g + h + i;
            }

        })
                .subscribe(ts);

        ts.assertValue("123456789");
        ts.assertCompleted();
        ts.assertNoErrors();
    }

    @Test
    public void zipIterableShouldZipListOfSingles() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        @SuppressWarnings("unchecked")
        Iterable<Single<Integer>> singles = Arrays.asList(Single.just(1), Single.just(2), Single.just(3));

        Single
                .zip(singles, new FuncN<String>() {
                    @Override
                    public String call(Object... args) {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (Object arg : args) {
                            stringBuilder.append(arg);
                        }
                        return stringBuilder.toString();
                    }
                }).subscribe(ts);

        ts.assertValue("123");
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void zipIterableShouldZipSetOfSingles() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Set<Single<String>> singlesSet = Collections.newSetFromMap(new LinkedHashMap<Single<String>, Boolean>(2));
        Single<String> s1 = Single.just("1");
        Single<String> s2 = Single.just("2");
        Single<String> s3 = Single.just("3");

        singlesSet.add(s1);
        singlesSet.add(s2);
        singlesSet.add(s3);

        Single
                .zip(singlesSet, new FuncN<String>() {
                    @Override
                    public String call(Object... args) {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (Object arg : args) {
                            stringBuilder.append(arg);
                        }
                        return stringBuilder.toString();
                    }
                }).subscribe(ts);

        ts.assertValue("123");
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void zipEmptyIterableShouldThrow() {
        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();
        Iterable<Single<Object>> singles = Collections.emptyList();

        Single
                .zip(singles, new FuncN<Object>() {
                    @Override
                    public Object call(Object... args) {
                        throw new IllegalStateException("Should not be called");
                    }
                })
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertError(NoSuchElementException.class);
        assertEquals("Can't zip 0 Singles.", testSubscriber.getOnErrorEvents().get(0).getMessage());
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
    public void testHookCreate() {
        @SuppressWarnings("unchecked")
        OnSubscribe<Object> subscriber = mock(OnSubscribe.class);
        Single.create(subscriber);

        verify(hookSpy, times(1)).onCreate(subscriber);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHookSubscribeStart() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Single<String> single = Single.create(new OnSubscribe<String>() {
            @Override public void call(SingleSubscriber<? super String> s) {
                s.onSuccess("Hello");
            }
        });
        single.subscribe(ts);

        verify(hookSpy, times(1)).onSubscribeStart(eq(single), any(Observable.OnSubscribe.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHookUnsafeSubscribeStart() {
        TestSubscriber<String> ts = new TestSubscriber<String>();
        Single<String> single = Single.create(new OnSubscribe<String>() {
            @Override public void call(SingleSubscriber<? super String> s) {
                s.onSuccess("Hello");
            }
        });
        single.unsafeSubscribe(ts);

        verify(hookSpy, times(1)).onSubscribeStart(eq(single), any(Observable.OnSubscribe.class));
    }

    @Test
    public void testHookSubscribeReturn() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Single<String> single = Single.create(new OnSubscribe<String>() {
            @Override public void call(SingleSubscriber<? super String> s) {
                s.onSuccess("Hello");
            }
        });
        single.subscribe(ts);

        verify(hookSpy, times(1)).onSubscribeReturn(any(SafeSubscriber.class));
    }

    @Test
    public void testHookUnsafeSubscribeReturn() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Single<String> single = Single.create(new OnSubscribe<String>() {
            @Override public void call(SingleSubscriber<? super String> s) {
                s.onSuccess("Hello");
            }
        });
        single.unsafeSubscribe(ts);

        verify(hookSpy, times(1)).onSubscribeReturn(ts);
    }

    @Test
    public void testReturnUnsubscribedWhenHookThrowsError() {
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Single<String> single = Single.create(new OnSubscribe<String>() {
            @Override public void call(SingleSubscriber<? super String> s) {
                throw new RuntimeException("Exception");
            }
        });
        Subscription subscription = single.unsafeSubscribe(ts);

        assertTrue(subscription.isUnsubscribed());
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
    public void testToBlocking() {
        Single<String> s = Single.just("one");
        BlockingSingle<String> blocking = s.toBlocking();
        assertNotNull(blocking);
        assertEquals("one", blocking.value());
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
     * @throws InterruptedException on interrupt
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
     * @throws InterruptedException on interrupt
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

    @Test
    public void toCompletableSuccess() {
        Completable completable = Single.just("value").toCompletable();
        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();
        completable.unsafeSubscribe(testSubscriber);

        testSubscriber.assertCompleted();
        testSubscriber.assertNoValues();
        testSubscriber.assertNoErrors();
    }

    @Test
    public void toCompletableError() {
        TestException exception = new TestException();
        Completable completable = Single.error(exception).toCompletable();
        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();
        completable.unsafeSubscribe(testSubscriber);

        testSubscriber.assertError(exception);
        testSubscriber.assertNoValues();
        testSubscriber.assertNotCompleted();
    }

    @Test
    public void doOnErrorShouldNotCallActionIfNoErrorHasOccurred() {
        @SuppressWarnings("unchecked")
        Action1<Throwable> action = mock(Action1.class);

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Single
                .just("value")
                .doOnError(action)
                .subscribe(testSubscriber);

        testSubscriber.assertValue("value");
        testSubscriber.assertNoErrors();

        verifyZeroInteractions(action);
    }

    @Test
    public void doOnErrorShouldCallActionIfErrorHasOccurred() {
        @SuppressWarnings("unchecked")
        Action1<Throwable> action = mock(Action1.class);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

        Throwable error = new IllegalStateException();

        Single
                .error(error)
                .doOnError(action)
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertError(error);

        verify(action).call(error);
    }

    @Test
    public void doOnErrorShouldThrowCompositeExceptionIfOnErrorActionThrows() {
        @SuppressWarnings("unchecked")
        Action1<Throwable> action = mock(Action1.class);


        Throwable error = new RuntimeException();
        Throwable exceptionFromOnErrorAction = new IllegalStateException();
        doThrow(exceptionFromOnErrorAction).when(action).call(error);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

        Single
                .error(error)
                .doOnError(action)
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        CompositeException compositeException = (CompositeException) testSubscriber.getOnErrorEvents().get(0);

        assertEquals(2, compositeException.getExceptions().size());
        assertSame(error, compositeException.getExceptions().get(0));
        assertSame(exceptionFromOnErrorAction, compositeException.getExceptions().get(1));

        verify(action).call(error);
    }

    @Test
    public void shouldEmitValueFromCallable() throws Exception {
        @SuppressWarnings("unchecked")
        Callable<String> callable = mock(Callable.class);

        when(callable.call()).thenReturn("value");

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Single
                .fromCallable(callable)
                .subscribe(testSubscriber);

        testSubscriber.assertValue("value");
        testSubscriber.assertNoErrors();

        verify(callable).call();
    }

    @Test
    public void shouldPassErrorFromCallable() throws Exception {
        @SuppressWarnings("unchecked")
        Callable<String> callable = mock(Callable.class);

        Throwable error = new IllegalStateException();

        when(callable.call()).thenThrow(error);

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Single
                .fromCallable(callable)
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertError(error);

        verify(callable).call();
    }

    @Test
    public void doOnSuccessShouldInvokeAction() {
        @SuppressWarnings("unchecked")
        Action1<String> action = mock(Action1.class);

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Single
                .just("value")
                .doOnSuccess(action)
                .subscribe(testSubscriber);

        testSubscriber.assertValue("value");
        testSubscriber.assertNoErrors();

        verify(action).call(eq("value"));
    }

    @Test
    public void doOnSuccessShouldPassErrorFromActionToSubscriber() {
        @SuppressWarnings("unchecked")
        Action1<String> action = mock(Action1.class);

        Throwable error = new IllegalStateException();
        doThrow(error).when(action).call(eq("value"));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Single
                .just("value")
                .doOnSuccess(action)
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertError(error);

        verify(action).call(eq("value"));
    }

    @Test
    public void doOnSuccessShouldNotCallActionIfSingleThrowsError() {
        @SuppressWarnings("unchecked")
        Action1<Object> action = mock(Action1.class);

        Throwable error = new IllegalStateException();

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

        Single
                .error(error)
                .doOnSuccess(action)
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertError(error);

        verifyZeroInteractions(action);
    }

    @Test
    public void doOnSuccessShouldNotSwallowExceptionThrownByAction() {
        @SuppressWarnings("unchecked")
        Action1<String> action = mock(Action1.class);

        Throwable exceptionFromAction = new IllegalStateException();

        doThrow(exceptionFromAction).when(action).call(eq("value"));

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Single
                .just("value")
                .doOnSuccess(action)
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertError(exceptionFromAction);

        verify(action).call(eq("value"));
    }

    @Test
    public void doOnSubscribeShouldInvokeAction() {
        Action0 action = mock(Action0.class);
        Single<Integer> single = Single.just(1).doOnSubscribe(action);

        verifyZeroInteractions(action);

        single.subscribe();
        single.subscribe();

        verify(action, times(2)).call();
    }

    @Test
    public void doOnSubscribeShouldInvokeActionBeforeSubscriberSubscribes() {
        final List<String> callSequence = new ArrayList<String>(2);

        Single<Integer> single = Single.create(new OnSubscribe<Integer>() {
            @Override
            public void call(SingleSubscriber<? super Integer> singleSubscriber) {
                callSequence.add("onSubscribe");
                singleSubscriber.onSuccess(1);
            }
        }).doOnSubscribe(new Action0() {
            @Override
            public void call() {
                callSequence.add("doOnSubscribe");
            }
        });

        single.subscribe();

        assertEquals(2, callSequence.size());
        assertEquals("doOnSubscribe", callSequence.get(0));
        assertEquals("onSubscribe", callSequence.get(1));
    }

    @Test
    public void delayWithSchedulerShouldDelayCompletion() {
        TestScheduler scheduler = new TestScheduler();
        Single<Integer> single = Single.just(1).delay(100, TimeUnit.DAYS, scheduler);

        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>();
        single.subscribe(subscriber);

        subscriber.assertNotCompleted();
        scheduler.advanceTimeBy(99, TimeUnit.DAYS);
        subscriber.assertNotCompleted();
        scheduler.advanceTimeBy(91, TimeUnit.DAYS);
        subscriber.assertCompleted();
        subscriber.assertValue(1);
    }

    @Test
    public void delayWithSchedulerShouldShortCutWithFailure() {
        TestScheduler scheduler = new TestScheduler();
        final RuntimeException expected = new RuntimeException();
        Single<Integer> single = Single.create(new OnSubscribe<Integer>() {
            @Override
            public void call(SingleSubscriber<? super Integer> singleSubscriber) {
                singleSubscriber.onSuccess(1);
                singleSubscriber.onError(expected);
            }
        }).delay(100, TimeUnit.DAYS, scheduler);

        TestSubscriber<Integer> subscriber = new TestSubscriber<Integer>();
        single.subscribe(subscriber);

        subscriber.assertNotCompleted();
        scheduler.advanceTimeBy(99, TimeUnit.DAYS);
        subscriber.assertNotCompleted();
        scheduler.advanceTimeBy(91, TimeUnit.DAYS);
        subscriber.assertNoValues();
        subscriber.assertError(expected);
    }

    @Test
    public void deferShouldNotCallFactoryFuncUntilSubscriberSubscribes() throws Exception {
        @SuppressWarnings("unchecked")
        Callable<Single<Object>> singleFactory = mock(Callable.class);
        Single.defer(singleFactory);
        verifyZeroInteractions(singleFactory);
    }

    @Test
    public void deferShouldSubscribeSubscriberToSingleFromFactoryFuncAndEmitValue() throws Exception {
        @SuppressWarnings("unchecked")
        Callable<Single<Object>> singleFactory = mock(Callable.class);
        Object value = new Object();
        Single<Object> single = Single.just(value);

        when(singleFactory.call()).thenReturn(single);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

        Single
                .defer(singleFactory)
                .subscribe(testSubscriber);

        testSubscriber.assertValue(value);
        testSubscriber.assertNoErrors();

        verify(singleFactory).call();
    }

    @Test
    public void deferShouldSubscribeSubscriberToSingleFromFactoryFuncAndEmitError() throws Exception {
        @SuppressWarnings("unchecked")
        Callable<Single<Object>> singleFactory = mock(Callable.class);
        Throwable error = new IllegalStateException();
        Single<Object> single = Single.error(error);

        when(singleFactory.call()).thenReturn(single);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

        Single
                .defer(singleFactory)
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertError(error);

        verify(singleFactory).call();
    }

    @Test
    public void deferShouldPassErrorFromSingleFactoryToTheSubscriber() throws Exception {
        @SuppressWarnings("unchecked")
        Callable<Single<Object>> singleFactory = mock(Callable.class);
        Throwable errorFromSingleFactory = new IllegalStateException();
        when(singleFactory.call()).thenThrow(errorFromSingleFactory);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

        Single
                .defer(singleFactory)
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertError(errorFromSingleFactory);

        verify(singleFactory).call();
    }

    @Test
    public void deferShouldCallSingleFactoryForEachSubscriber() throws Exception {
        @SuppressWarnings("unchecked")
        Callable<Single<String>> singleFactory = mock(Callable.class);

        String[] values = {"1", "2", "3"};
        @SuppressWarnings("unchecked")
        final Single<String>[] singles = new Single[] {Single.just(values[0]), Single.just(values[1]), Single.just(values[2])};

        final AtomicInteger singleFactoryCallsCounter = new AtomicInteger();

        when(singleFactory.call()).thenAnswer(new Answer<Single<String>>() {
            @Override
            public Single<String> answer(InvocationOnMock invocation) throws Throwable {
                return singles[singleFactoryCallsCounter.getAndIncrement()];
            }
        });

        Single<String> deferredSingle = Single.defer(singleFactory);

        for (int i = 0; i < singles.length; i++) {
            TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

            deferredSingle.subscribe(testSubscriber);

            testSubscriber.assertValue(values[i]);
            testSubscriber.assertNoErrors();
        }

        verify(singleFactory, times(3)).call();
    }

    @Test
    public void deferShouldPassNullPointerExceptionToTheSubscriberIfSingleFactoryIsNull() {
        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

        Single
                .defer(null)
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertError(NullPointerException.class);
    }


    @Test
    public void deferShouldPassNullPointerExceptionToTheSubscriberIfSingleFactoryReturnsNull() throws Exception {
        @SuppressWarnings("unchecked")
        Callable<Single<Object>> singleFactory = mock(Callable.class);
        when(singleFactory.call()).thenReturn(null);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

        Single
                .defer(singleFactory)
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertError(NullPointerException.class);

        verify(singleFactory).call();
    }

    @Test
    public void doOnUnsubscribeShouldInvokeActionAfterSuccess() {
        Action0 action = mock(Action0.class);

        Single<String> single = Single
                .just("test")
                .doOnUnsubscribe(action);

        verifyZeroInteractions(action);

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        single.subscribe(testSubscriber);

        testSubscriber.assertValue("test");
        testSubscriber.assertCompleted();

        verify(action).call();
    }

    @Test
    public void doOnUnsubscribeShouldInvokeActionAfterError() {
        Action0 action = mock(Action0.class);

        Single<Object> single = Single
                .error(new RuntimeException("test"))
                .doOnUnsubscribe(action);

        verifyZeroInteractions(action);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();
        single.subscribe(testSubscriber);

        testSubscriber.assertError(RuntimeException.class);
        assertEquals("test", testSubscriber.getOnErrorEvents().get(0).getMessage());

        verify(action).call();
    }

    @Test
    public void doOnUnsubscribeShouldInvokeActionAfterExplicitUnsubscription() {
        Action0 action = mock(Action0.class);

        Single<Object> single = Single
                .create(new OnSubscribe<Object>() {
                    @Override
                    public void call(SingleSubscriber<? super Object> singleSubscriber) {
                        // Broken Single that never ends itself (simulates long computation in one thread).
                    }
                })
                .doOnUnsubscribe(action);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();
        Subscription subscription = single.subscribe(testSubscriber);

        verifyZeroInteractions(action);

        subscription.unsubscribe();
        verify(action).call();
        testSubscriber.assertNoValues();
        testSubscriber.assertNoTerminalEvent();
    }

    @Test
    public void doAfterTerminateActionShouldBeInvokedAfterOnSuccess() {
        Action0 action = mock(Action0.class);

        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Single
                .just("value")
                .doAfterTerminate(action)
                .subscribe(testSubscriber);

        testSubscriber.assertValue("value");
        testSubscriber.assertNoErrors();

        verify(action).call();
    }

    @Test
    public void doAfterTerminateActionShouldBeInvokedAfterOnError() {
        Action0 action = mock(Action0.class);

        TestSubscriber<Object> testSubscriber = new TestSubscriber<Object>();

        Throwable error = new IllegalStateException();

        Single
                .error(error)
                .doAfterTerminate(action)
                .subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        testSubscriber.assertError(error);

        verify(action).call();
    }

    @Test
    public void doAfterTerminateActionShouldNotBeInvokedUntilSubscriberSubscribes() {
        Action0 action = mock(Action0.class);

        Single
                .just("value")
                .doAfterTerminate(action);

        Single
                .error(new IllegalStateException())
                .doAfterTerminate(action);

        verifyZeroInteractions(action);
    }

    @Test
    public void onErrorResumeNextViaSingleShouldNotInterruptSuccessfulSingle() {
        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Single
                .just("success")
                .onErrorResumeNext(Single.just("fail"))
                .subscribe(testSubscriber);

        testSubscriber.assertValue("success");
    }

    @Test
    public void onErrorResumeNextViaSingleShouldResumeWithPassedSingleInCaseOfError() {
        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Single
                .<String> error(new RuntimeException("test exception"))
                .onErrorResumeNext(Single.just("fallback"))
                .subscribe(testSubscriber);

        testSubscriber.assertValue("fallback");
    }

    @Test
    public void onErrorResumeNextViaSingleShouldPreventNullSingle() {
        try {
            Single
                    .just("value")
                    .onErrorResumeNext((Single<String>) null);
            fail();
        } catch (NullPointerException expected) {
            assertEquals("resumeSingleInCaseOfError must not be null", expected.getMessage());
        }
    }

    @Test
    public void onErrorResumeNextViaFunctionShouldNotInterruptSuccessfulSingle() {
        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Single
                .just("success")
                .onErrorResumeNext(new Func1<Throwable, Single<? extends String>>() {
                    @Override
                    public Single<? extends String> call(Throwable throwable) {
                        return Single.just("fail");
                    }
                })
                .subscribe(testSubscriber);

        testSubscriber.assertValue("success");
    }

    @Test
    public void onErrorResumeNextViaFunctionShouldResumeWithPassedSingleInCaseOfError() {
        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();

        Single
                .<String> error(new RuntimeException("test exception"))
                .onErrorResumeNext(new Func1<Throwable, Single<? extends String>>() {
                    @Override
                    public Single<? extends String> call(Throwable throwable) {
                        return Single.just("fallback");
                    }
                })
                .subscribe(testSubscriber);

        testSubscriber.assertValue("fallback");
    }

    @Test
    public void onErrorResumeNextViaFunctionShouldPreventNullFunction() {
        try {
            Single
                    .just("value")
                    .onErrorResumeNext((Func1<Throwable, ? extends Single<? extends String>>) null);
            fail();
        } catch (NullPointerException expected) {
            assertEquals("resumeFunctionInCaseOfError must not be null", expected.getMessage());
        }
    }

    @Test
    public void onErrorResumeNextViaFunctionShouldFailIfFunctionReturnsNull() {
        try {
            Single
                    .error(new TestException())
                    .onErrorResumeNext(new Func1<Throwable, Single<? extends String>>() {
                        @Override
                        public Single<? extends String> call(Throwable throwable) {
                            return null;
                        }
                    })
                    .subscribe();

            fail();
        } catch (OnErrorNotImplementedException expected) {
            assertTrue(expected.getCause() instanceof NullPointerException);
        }
    }

    @Test(expected = NullPointerException.class)
    public void iterableToArrayShouldThrowNullPointerExceptionIfIterableNull() {
        Single.iterableToArray(null);
    }

    @Test
    public void iterableToArrayShouldConvertList() {
        @SuppressWarnings("unchecked")
        List<Single<String>> singlesList = Arrays.asList(Single.just("1"), Single.just("2"));

        Single<? extends String>[] singlesArray = Single.iterableToArray(singlesList);
        assertEquals(2, singlesArray.length);
        assertSame(singlesList.get(0), singlesArray[0]);
        assertSame(singlesList.get(1), singlesArray[1]);
    }

    @Test
    public void iterableToArrayShouldConvertSet() {
        // Just to trigger different path of the code that handles non-list iterables.
        Set<Single<String>> singlesSet = Collections.newSetFromMap(new LinkedHashMap<Single<String>, Boolean>(2));
        Single<String> s1 = Single.just("1");
        Single<String> s2 = Single.just("2");

        singlesSet.add(s1);
        singlesSet.add(s2);

        Single<? extends String>[] singlesArray = Single.iterableToArray(singlesSet);
        assertEquals(2, singlesArray.length);
        assertSame(s1, singlesArray[0]);
        assertSame(s2, singlesArray[1]);
    }

    @Test(timeout = 2000)
    public void testRetry() {
        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        final TestSubscriber<Integer> retryCounter = new TestSubscriber<Integer>();

        final int retryCount = 100;
        Callable<String> callable = new Callable<String>() {

            @Override
            public String call() throws Exception {
                int errors = retryCounter.getOnErrorEvents().size();
                if (errors < retryCount) {
                    Exception exception = new Exception();
                    retryCounter.onError(exception);
                    throw exception;
                }
                return null;
            }

        };

        Single.fromCallable(callable)
                .retry()
                .subscribe(testSubscriber);

        testSubscriber.assertCompleted();
        int numberOfErrors = retryCounter.getOnErrorEvents().size();
        assertEquals(retryCount, numberOfErrors);
    }

    @Test(timeout = 2000)
    public void testRetryWithCount() {
        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        final TestSubscriber<Integer> retryCounter = new TestSubscriber<Integer>();

        final int retryCount = 100;
        Callable<String> callable = new Callable<String>() {

            @Override
            public String call() throws Exception {
                int errors = retryCounter.getOnErrorEvents().size();
                if (errors < retryCount) {
                    Exception exception = new Exception();
                    retryCounter.onError(exception);
                    throw exception;
                }

                return null;
            }
        };

        Single.fromCallable(callable)
                .retry(retryCount)
                .subscribe(testSubscriber);

        testSubscriber.assertCompleted();
        int numberOfErrors = retryCounter.getOnErrorEvents().size();
        assertEquals(retryCount, numberOfErrors);
    }

    @Test
    public void testRetryWithPredicate() {
        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        final TestSubscriber<Integer> retryCounter = new TestSubscriber<Integer>();

        final int retryCount = 100;
        Callable<String> callable = new Callable<String>() {

            @Override
            public String call() throws Exception {
                int errors = retryCounter.getOnErrorEvents().size();
                if (errors < retryCount) {
                    IOException exception = new IOException();
                    retryCounter.onError(exception);
                    throw exception;
                }
                return null;
            }
        };

        Single.fromCallable(callable)
                .retry(new Func2<Integer, Throwable, Boolean>() {
                    @Override
                    public Boolean call(Integer integer, Throwable throwable) {
                        return throwable instanceof IOException;
                    }
                })
                .subscribe(testSubscriber);

        testSubscriber.assertCompleted();
        int numberOfErrors = retryCounter.getOnErrorEvents().size();
        assertEquals(retryCount, numberOfErrors);
    }

    @Test
    public void testRetryWhen() {
        TestSubscriber<String> testSubscriber = new TestSubscriber<String>();
        final TestSubscriber<Integer> retryCounter = new TestSubscriber<Integer>();

        final int retryCount = 100;

        Callable<String> callable = new Callable<String>() {

            @Override
            public String call() throws Exception {
                int errors = retryCounter.getOnErrorEvents().size();
                if (errors < retryCount) {
                    IOException exception = new IOException();
                    retryCounter.onError(exception);
                    throw exception;
                }
                return null;
            }
        };

        Single.fromCallable(callable)
                .retryWhen(new Func1<Observable<? extends Throwable>, Observable<?>>() {
                    @Override
                    public Observable<?> call(Observable<? extends Throwable> observable) {

                        return observable.flatMap(new Func1<Throwable, Observable<?>>() {
                            @Override
                            public Observable<?> call(Throwable throwable) {
                                return throwable instanceof IOException ? Observable.just(null) : Observable.error(throwable);
                            }
                        });
                    }
                })
                .subscribe(testSubscriber);

        int numberOfErrors = retryCounter.getOnErrorEvents().size();
        assertEquals(retryCount, numberOfErrors);
    }

    @Test
    public void takeUntilCompletableFires() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until.toCompletable()).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        until.onCompleted();

        ts.assertError(CancellationException.class);

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void takeUntilObservableFires() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until.take(1)).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        until.onNext(1);

        ts.assertError(CancellationException.class);

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void takeUntilSingleFires() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until.take(1).toSingle()).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        until.onNext(1);

        ts.assertError(CancellationException.class);

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void takeUntilObservableCompletes() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until.take(1)).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        until.onCompleted();

        ts.assertError(CancellationException.class);

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void takeUntilSourceUnsubscribes_withCompletable() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until.toCompletable()).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onNext(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertTerminalEvent();

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void takeUntilSourceUnsubscribes_withObservable() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onNext(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertTerminalEvent();

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void takeUntilSourceUnsubscribes_withSingle() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until.take(1).toSingle()).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onNext(1);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertTerminalEvent();

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void takeUntilSourceErrorUnsubscribes_withCompletable() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until.toCompletable()).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        Exception e = new Exception();
        source.onError(e);

        ts.assertNoValues();
        ts.assertError(e);

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void takeUntilSourceErrorUnsubscribes_withObservable() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onError(new Throwable());

        ts.assertNoValues();
        ts.assertError(Throwable.class);

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void takeUntilSourceErrorUnsubscribes_withSingle() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until.take(1).toSingle()).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        source.onError(new Throwable());

        ts.assertNoValues();
        ts.assertError(Throwable.class);

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void takeUntilError_withCompletable_shouldMatch() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until.toCompletable()).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        Exception e = new Exception();
        until.onError(e);

        ts.assertNoValues();
        ts.assertError(e);

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void takeUntilError_withObservable_shouldMatch() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until.asObservable()).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        Exception e = new Exception();
        until.onError(e);

        ts.assertNoValues();
        ts.assertError(e);

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void takeUntilError_withSingle_shouldMatch() {
        PublishSubject<Integer> source = PublishSubject.create();
        PublishSubject<Integer> until = PublishSubject.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        source.take(1).toSingle().takeUntil(until.take(1).toSingle()).unsafeSubscribe(ts);

        assertTrue(source.hasObservers());
        assertTrue(until.hasObservers());

        Exception e = new Exception();
        until.onError(e);

        ts.assertNoValues();
        ts.assertError(e);

        assertFalse(source.hasObservers());
        assertFalse(until.hasObservers());
        assertFalse(ts.isUnsubscribed());
    }

    @Test
    public void subscribeWithObserver() {
        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);

        Single.just(1).subscribe(o);

        verify(o).onNext(1);
        verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void subscribeWithObserverAndGetError() {
        @SuppressWarnings("unchecked")
        Observer<Integer> o = mock(Observer.class);

        Single.<Integer>error(new TestException()).subscribe(o);

        verify(o, never()).onNext(anyInt());
        verify(o, never()).onCompleted();
        verify(o).onError(any(TestException.class));
    }

    @Test
    public void subscribeWithNullObserver() {
        try {
            Single.just(1).subscribe((Observer<Integer>)null);
            fail("Failed to throw NullPointerException");
        } catch (NullPointerException ex) {
            assertEquals("observer is null", ex.getMessage());
        }
    }

    @Test
    public void unsubscribeComposesThrough() {
        PublishSubject<Integer> ps = PublishSubject.create();
        
        Subscription s = ps.toSingle()
        .flatMap(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(1);
            }
        })
        .subscribe();
        
        s.unsubscribe();
        
        assertFalse("Observers present?!", ps.hasObservers());
    }

    @Test(timeout = 1000)
    public void unsubscribeComposesThroughAsync() {
        PublishSubject<Integer> ps = PublishSubject.create();
        
        Subscription s = ps.toSingle()
        .subscribeOn(Schedulers.io())
        .flatMap(new Func1<Integer, Single<Integer>>() {
            @Override
            public Single<Integer> call(Integer v) {
                return Single.just(1);
            }
        })
        .subscribe();
        
        while (!ps.hasObservers() && !Thread.currentThread().isInterrupted()) ;
        
        s.unsubscribe();
        
        assertFalse("Observers present?!", ps.hasObservers());
    }

}
