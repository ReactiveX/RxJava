/**
 * Copyright 2016 Netflix, Inc.
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
package rx.plugins;

import static org.junit.Assert.*;

import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.*;

import org.junit.Test;

import rx.*;
import rx.Observable;
import rx.Scheduler.Worker;
import rx.exceptions.*;
import rx.functions.*;
import rx.internal.operators.OnSubscribeRange;
import rx.internal.util.UtilityFunctions;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;


public class RxJavaHooksTest {

    public static class TestExceptionWithUnknownCause extends RuntimeException {
        private static final long serialVersionUID = 6771158999860253299L;

        TestExceptionWithUnknownCause() {
            super((Throwable) null);
        }
    }

    static Observable<Integer> createObservable() {
        return Observable.range(1, 5).map(new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer t) {
                throw new TestException();
            }
        });
    }

    static Observable<Integer> createObservableThrowingUnknownCause() {
        return Observable.range(1, 5).map(new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer t) {
                throw new TestExceptionWithUnknownCause();
            }
        });
    }

    @Test
    public void constructorShouldBePrivate() {
        TestUtil.checkUtilityClass(RxJavaHooks.class);
    }

    @Test
    public void assemblyTrackingObservable() {
        RxJavaHooks.enableAssemblyTracking();
        try {
            TestSubscriber<Integer> ts = TestSubscriber.create();

            createObservable().subscribe(ts);

            ts.assertError(TestException.class);

            Throwable ex = ts.getOnErrorEvents().get(0);

            AssemblyStackTraceException aste = AssemblyStackTraceException.find(ex);

            assertNotNull(aste);

            assertTrue(aste.getMessage(), aste.getMessage().contains("createObservable"));

            RxJavaHooks.clearAssemblyTracking();

            ts = TestSubscriber.create();

            createObservable().subscribe(ts);

            ts.assertError(TestException.class);

            ex = ts.getOnErrorEvents().get(0);

            aste = AssemblyStackTraceException.find(ex);

            assertNull(aste);
        } finally {
            RxJavaHooks.resetAssemblyTracking();
        }
    }

    @Test
    public void assemblyTrackingObservableUnknownCause() {
        RxJavaHooks.enableAssemblyTracking();
        try {
            final AtomicReference<Throwable> onErrorThrowableRef = new AtomicReference<Throwable>();
            RxJavaHooks.setOnError(new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    onErrorThrowableRef.set(throwable);
                }
            });
            TestSubscriber<Integer> ts = TestSubscriber.create();

            createObservableThrowingUnknownCause().subscribe(ts);

            ts.assertError(TestExceptionWithUnknownCause.class);

            Throwable receivedError = onErrorThrowableRef.get();
            assertNotNull(receivedError);
            assertTrue(receivedError.getMessage().contains("cause set to null"));
        } finally {
            RxJavaHooks.reset();
        }
    }

    static Single<Integer> createSingle() {
        return Single.just(1).map(new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer t) {
                throw new TestException();
            }
        });
    }

    @Test
    public void assemblyTrackingSingle() {
        RxJavaHooks.enableAssemblyTracking();
        try {
            TestSubscriber<Integer> ts = TestSubscriber.create();

            createSingle().subscribe(ts);

            ts.assertError(TestException.class);

            Throwable ex = ts.getOnErrorEvents().get(0);

            AssemblyStackTraceException aste = AssemblyStackTraceException.find(ex);

            assertNotNull(aste);

            assertTrue(aste.getMessage(), aste.getMessage().contains("createSingle"));

            RxJavaHooks.clearAssemblyTracking();

            ts = TestSubscriber.create();

            createSingle().subscribe(ts);

            ts.assertError(TestException.class);

            ex = ts.getOnErrorEvents().get(0);

            aste = AssemblyStackTraceException.find(ex);

            assertNull(aste);
        } finally {
            RxJavaHooks.resetAssemblyTracking();
        }
    }

    static Completable createCompletable() {
        return Completable.error(new Func0<Throwable>() {
            @Override
            public Throwable call() {
                return new TestException();
            }
        });
    }

    @Test
    public void assemblyTrackingCompletable() {
        RxJavaHooks.enableAssemblyTracking();
        try {
            TestSubscriber<Integer> ts = TestSubscriber.create();

            createCompletable().subscribe(ts);

            ts.assertError(TestException.class);

            Throwable ex = ts.getOnErrorEvents().get(0);

            AssemblyStackTraceException aste = AssemblyStackTraceException.find(ex);

            assertNotNull(aste);

            assertTrue(aste.getMessage(), aste.getMessage().contains("createCompletable"));

            RxJavaHooks.clearAssemblyTracking();

            ts = TestSubscriber.create();

            createCompletable().subscribe(ts);

            ts.assertError(TestException.class);

            ex = ts.getOnErrorEvents().get(0);

            aste = AssemblyStackTraceException.find(ex);

            assertNull(aste);

        } finally {
            RxJavaHooks.resetAssemblyTracking();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void lockdown() throws Exception {
        RxJavaHooks.reset();
        RxJavaHooks.lockdown();
        try {
            assertTrue(RxJavaHooks.isLockdown());
            Action1 a1 = Actions.empty();
            Func0 f0 = new Func0() {
                @Override
                public Object call() {
                    return null;
                }
            };
            Func1 f1 = UtilityFunctions.identity();
            Func2 f2 = new Func2() {
                @Override
                public Object call(Object t1, Object t2) {
                    return t2;
                }
            };

            for (Method m : RxJavaHooks.class.getMethods()) {
                if (m.getName().startsWith("setOn")) {

                    Method getter = RxJavaHooks.class.getMethod("get" + m.getName().substring(3));

                    Object before = getter.invoke(null);

                    if (m.getParameterTypes()[0].isAssignableFrom(Func0.class)) {
                        m.invoke(null, f0);
                    } else
                    if (m.getParameterTypes()[0].isAssignableFrom(Func1.class)) {
                        m.invoke(null, f1);
                    } else
                    if (m.getParameterTypes()[0].isAssignableFrom(Action1.class)) {
                        m.invoke(null, a1);
                    } else {
                        m.invoke(null, f2);
                    }

                    Object after = getter.invoke(null);

                    assertSame(m.toString(), before, after);

                    if (before != null) {
                        RxJavaHooks.clear();
                        RxJavaHooks.reset();
                        assertSame(m.toString(), before, getter.invoke(null));
                    }
                }
            }


            Object o1 = RxJavaHooks.getOnObservableCreate();
            Object o2 = RxJavaHooks.getOnSingleCreate();
            Object o3 = RxJavaHooks.getOnCompletableCreate();

            RxJavaHooks.enableAssemblyTracking();
            RxJavaHooks.clearAssemblyTracking();
            RxJavaHooks.resetAssemblyTracking();


            assertSame(o1, RxJavaHooks.getOnObservableCreate());
            assertSame(o2, RxJavaHooks.getOnSingleCreate());
            assertSame(o3, RxJavaHooks.getOnCompletableCreate());

        } finally {
            RxJavaHooks.lockdown = false;
            RxJavaHooks.reset();
            assertFalse(RxJavaHooks.isLockdown());
        }
    }

    Func1<Scheduler, Scheduler> replaceWithImmediate = new Func1<Scheduler, Scheduler>() {
        @Override
        public Scheduler call(Scheduler t) {
            return Schedulers.immediate();
        }
    };

    @Test
    public void overrideComputationScheduler() {
        try {
            RxJavaHooks.setOnComputationScheduler(replaceWithImmediate);

            assertSame(Schedulers.immediate(), Schedulers.computation());
        } finally {
            RxJavaHooks.reset();
        }
        // make sure the reset worked
        assertNotSame(Schedulers.immediate(), Schedulers.computation());
    }

    @Test
    public void overrideIoScheduler() {
        try {
            RxJavaHooks.setOnIOScheduler(replaceWithImmediate);

            assertSame(Schedulers.immediate(), Schedulers.io());
        } finally {
            RxJavaHooks.reset();
        }
        // make sure the reset worked
        assertNotSame(Schedulers.immediate(), Schedulers.io());
    }

    @Test
    public void overrideNewThreadScheduler() {
        try {
            RxJavaHooks.setOnNewThreadScheduler(replaceWithImmediate);

            assertSame(Schedulers.immediate(), Schedulers.newThread());
        } finally {
            RxJavaHooks.reset();
        }
        // make sure the reset worked
        assertNotSame(Schedulers.immediate(), Schedulers.newThread());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void observableCreate() {
        try {
            RxJavaHooks.setOnObservableCreate(new Func1<Observable.OnSubscribe, Observable.OnSubscribe>() {
                @Override
                public Observable.OnSubscribe call(Observable.OnSubscribe t) {
                    return new OnSubscribeRange(1, 2);
                }
            });

            TestSubscriber<Integer> ts = TestSubscriber.create();

            Observable.range(10, 3).subscribe(ts);

            ts.assertValues(1, 2);
            ts.assertNoErrors();
            ts.assertCompleted();
        } finally {
            RxJavaHooks.reset();
        }
        // make sure the reset worked
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.range(10, 3).subscribe(ts);

        ts.assertValues(10, 11, 12);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void observableStart() {
        try {
            RxJavaHooks.setOnObservableStart(new Func2<Observable, Observable.OnSubscribe, Observable.OnSubscribe>() {
                @Override
                public Observable.OnSubscribe call(Observable o, Observable.OnSubscribe t) {
                    return new OnSubscribeRange(1, 2);
                }
            });

            TestSubscriber<Integer> ts = TestSubscriber.create();

            Observable.range(10, 3).subscribe(ts);

            ts.assertValues(1, 2);
            ts.assertNoErrors();
            ts.assertCompleted();
        } finally {
            RxJavaHooks.reset();
        }
        // make sure the reset worked
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Observable.range(10, 3).subscribe(ts);

        ts.assertValues(10, 11, 12);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void observableReturn() {
        try {
            final Subscription s = Subscriptions.empty();

            RxJavaHooks.setOnObservableReturn(new Func1<Subscription, Subscription>() {
                @Override
                public Subscription call(Subscription t) {
                    return s;
                }
            });

            TestSubscriber<Integer> ts = TestSubscriber.create();

            Subscription u = Observable.range(10, 3).subscribe(ts);

            ts.assertValues(10, 11, 12);
            ts.assertNoErrors();
            ts.assertCompleted();

            assertSame(s, u);
        } finally {
            RxJavaHooks.reset();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void singleCreate() {
        try {
            RxJavaHooks.setOnSingleCreate(new Func1<Single.OnSubscribe, Single.OnSubscribe>() {
                @Override
                public Single.OnSubscribe call(Single.OnSubscribe t) {
                    return new Single.OnSubscribe<Object>() {
                        @Override
                        public void call(SingleSubscriber<? super Object> t) {
                            t.onSuccess(10);
                        }
                    };
                }
            });

            TestSubscriber<Integer> ts = TestSubscriber.create();

            Single.just(1).subscribe(ts);

            ts.assertValue(10);
            ts.assertNoErrors();
            ts.assertCompleted();
        } finally {
            RxJavaHooks.reset();
        }
        // make sure the reset worked
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Single.just(1).subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void singleStart() {
        try {
            RxJavaHooks.setOnSingleStart(new Func2<Single, Single.OnSubscribe, Single.OnSubscribe>() {
                @Override
                public Single.OnSubscribe call(Single o, Single.OnSubscribe t) {
                    return new Single.OnSubscribe<Object>() {
                        @Override
                        public void call(SingleSubscriber<? super Object> t) {
                            t.onSuccess(10);
                        }
                    };
                }
            });

            TestSubscriber<Integer> ts = TestSubscriber.create();

            Single.just(1).subscribe(ts);

            ts.assertValue(10);
            ts.assertNoErrors();
            ts.assertCompleted();
        } finally {
            RxJavaHooks.reset();
        }
        // make sure the reset worked
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Single.just(1).subscribe(ts);

        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void singleReturn() {
        try {
            final Subscription s = Subscriptions.empty();

            RxJavaHooks.setOnSingleReturn(new Func1<Subscription, Subscription>() {
                @Override
                public Subscription call(Subscription t) {
                    return s;
                }
            });

            TestSubscriber<Integer> ts = TestSubscriber.create();

            Subscription u = Single.just(1).subscribe(ts);

            ts.assertValue(1);
            ts.assertNoErrors();
            ts.assertCompleted();

            assertSame(s, u);
        } finally {
            RxJavaHooks.reset();
        }
    }

    @Test
    public void completableCreate() {
        try {
            RxJavaHooks.setOnCompletableCreate(new Func1<Completable.OnSubscribe, Completable.OnSubscribe>() {
                @Override
                public Completable.OnSubscribe call(Completable.OnSubscribe t) {
                    return new Completable.OnSubscribe() {
                        @Override
                        public void call(CompletableSubscriber t) {
                            t.onError(new TestException());
                        }
                    };
                }
            });

            TestSubscriber<Integer> ts = TestSubscriber.create();

            Completable.complete().subscribe(ts);

            ts.assertNoValues();
            ts.assertNotCompleted();
            ts.assertError(TestException.class);
        } finally {
            RxJavaHooks.reset();
        }
        // make sure the reset worked
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Completable.complete().subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void completableStart() {
        try {
            RxJavaHooks.setOnCompletableStart(new Func2<Completable, Completable.OnSubscribe, Completable.OnSubscribe>() {
                @Override
                public Completable.OnSubscribe call(Completable o, Completable.OnSubscribe t) {
                    return new Completable.OnSubscribe() {
                        @Override
                        public void call(CompletableSubscriber t) {
                            t.onError(new TestException());
                        }
                    };
                }
            });

            TestSubscriber<Integer> ts = TestSubscriber.create();

            Completable.complete().subscribe(ts);

            ts.assertNoValues();
            ts.assertNotCompleted();
            ts.assertError(TestException.class);
        } finally {
            RxJavaHooks.reset();
        }
        // make sure the reset worked
        TestSubscriber<Integer> ts = TestSubscriber.create();

        Completable.complete().subscribe(ts);

        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    void onSchedule(Worker w) throws InterruptedException {
        try {
            try {
                final AtomicInteger value = new AtomicInteger();
                final CountDownLatch cdl = new CountDownLatch(1);

                RxJavaHooks.setOnScheduleAction(new Func1<Action0, Action0>() {
                    @Override
                    public Action0 call(Action0 t) {
                        return new Action0() {
                            @Override
                            public void call() {
                                value.set(10);
                                cdl.countDown();
                            }
                        };
                    }
                });

                w.schedule(new Action0() {
                    @Override
                    public void call() {
                        value.set(1);
                        cdl.countDown();
                    }
                });

                cdl.await();

                assertEquals(10, value.get());

            } finally {

                RxJavaHooks.reset();
            }

            // make sure the reset worked
            final AtomicInteger value = new AtomicInteger();
            final CountDownLatch cdl = new CountDownLatch(1);

            w.schedule(new Action0() {
                @Override
                public void call() {
                    value.set(1);
                    cdl.countDown();
                }
            });

            cdl.await();

            assertEquals(1, value.get());
        } finally {
            w.unsubscribe();
        }
    }

    @Test
    public void onScheduleComputation() throws InterruptedException {
        onSchedule(Schedulers.computation().createWorker());
    }

    @Test
    public void onScheduleIO() throws InterruptedException {
        onSchedule(Schedulers.io().createWorker());
    }

    @Test
    public void onScheduleNewThread() throws InterruptedException {
        onSchedule(Schedulers.newThread().createWorker());
    }

    @Test
    public void onError() {
        try {
            final List<Throwable> list = new ArrayList<Throwable>();

            RxJavaHooks.setOnError(new Action1<Throwable>() {
                @Override
                public void call(Throwable t) {
                    list.add(t);
                }
            });

            RxJavaHooks.onError(new TestException("Forced failure"));

            assertEquals(1, list.size());
            assertTestException(list, 0, "Forced failure");
        } finally {
            RxJavaHooks.reset();
        }
    }

    @Test
    public void clear() throws Exception {
        RxJavaHooks.reset();
        try {
            RxJavaHooks.clear();
            for (Method m : RxJavaHooks.class.getMethods()) {
                if (m.getName().startsWith("getOn")) {
                    assertNull(m.toString(), m.invoke(null));
                }
            }

        } finally {
            RxJavaHooks.reset();
        }

        for (Method m : RxJavaHooks.class.getMethods()) {
            if (m.getName().startsWith("getOn")
                    && !m.getName().endsWith("Scheduler")
                    && !m.getName().contains("GenericScheduledExecutorService")) {
                assertNotNull(m.toString(), m.invoke(null));
            }
        }

    }

    @Test
    public void onErrorNoHandler() {
        try {
            final List<Throwable> list = new ArrayList<Throwable>();

            RxJavaHooks.setOnError(null);

            Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    list.add(e);

                }
            });

            RxJavaHooks.onError(new TestException("Forced failure"));

            Thread.currentThread().setUncaughtExceptionHandler(null);

            // this will be printed on the console and should not crash
            RxJavaHooks.onError(new TestException("Forced failure 3"));

            assertEquals(1, list.size());
            assertTestException(list, 0, "Forced failure");
        } finally {
            RxJavaHooks.reset();
            Thread.currentThread().setUncaughtExceptionHandler(null);
        }
    }

    @Test
    public void onErrorCrashes() {
        try {
            final List<Throwable> list = new ArrayList<Throwable>();

            RxJavaHooks.setOnError(new Action1<Throwable>() {
                @Override
                public void call(Throwable t) {
                    throw new TestException("Forced failure 2");
                }
            });

            Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    list.add(e);

                }
            });

            RxJavaHooks.onError(new TestException("Forced failure"));

            assertEquals(2, list.size());
            assertTestException(list, 0, "Forced failure 2");
            assertTestException(list, 1, "Forced failure");

            Thread.currentThread().setUncaughtExceptionHandler(null);

        } finally {
            RxJavaHooks.reset();
            Thread.currentThread().setUncaughtExceptionHandler(null);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void clearIsPassthrough() {
        try {
            RxJavaHooks.clear();

            assertNull(RxJavaHooks.onCreate((Observable.OnSubscribe)null));

            Observable.OnSubscribe oos = new Observable.OnSubscribe() {
                @Override
                public void call(Object t) {

                }
            };

            assertSame(oos, RxJavaHooks.onCreate(oos));

            assertNull(RxJavaHooks.onCreate((Single.OnSubscribe)null));

            Single.OnSubscribe sos = new Single.OnSubscribe() {
                @Override
                public void call(Object t) {

                }
            };

            assertSame(sos, RxJavaHooks.onCreate(sos));

            assertNull(RxJavaHooks.onCreate((Single.OnSubscribe)null));

            Completable.OnSubscribe cos = new Completable.OnSubscribe() {
                @Override
                public void call(CompletableSubscriber t) {

                }
            };

            assertSame(cos, RxJavaHooks.onCreate(cos));

            assertNull(RxJavaHooks.onScheduledAction(null));

            Action0 action = Actions.empty();

            assertSame(action, RxJavaHooks.onScheduledAction(action));


            assertNull(RxJavaHooks.onObservableStart(Observable.never(), null));

            assertSame(oos, RxJavaHooks.onObservableStart(Observable.never(), oos));

            assertNull(RxJavaHooks.onSingleStart(Single.just(1), null));

            assertSame(sos, RxJavaHooks.onSingleStart(Single.just(1), sos));

            assertNull(RxJavaHooks.onCompletableStart(Completable.never(), null));

            assertSame(cos, RxJavaHooks.onCompletableStart(Completable.never(), cos));

            Subscription subscription = Subscriptions.empty();

            assertNull(RxJavaHooks.onObservableReturn(null));

            assertSame(subscription, RxJavaHooks.onObservableReturn(subscription));

            assertNull(RxJavaHooks.onSingleReturn(null));

            assertSame(subscription, RxJavaHooks.onSingleReturn(subscription));

            TestException ex = new TestException();

            assertNull(RxJavaHooks.onObservableError(null));

            assertSame(ex, RxJavaHooks.onObservableError(ex));

            assertNull(RxJavaHooks.onSingleError(null));

            assertSame(ex, RxJavaHooks.onSingleError(ex));

            assertNull(RxJavaHooks.onCompletableError(null));

            assertSame(ex, RxJavaHooks.onCompletableError(ex));

            Observable.Operator oop = new Observable.Operator() {
                @Override
                public Object call(Object t) {
                    return t;
                }
            };

            assertNull(RxJavaHooks.onObservableLift(null));

            assertSame(oop, RxJavaHooks.onObservableLift(oop));

            assertNull(RxJavaHooks.onSingleLift(null));

            assertSame(oop, RxJavaHooks.onSingleLift(oop));

            Completable.Operator cop = new Completable.Operator() {
                @Override
                public CompletableSubscriber call(CompletableSubscriber t) {
                    return t;
                }
            };

            assertNull(RxJavaHooks.onCompletableLift(null));

            assertSame(cop, RxJavaHooks.onCompletableLift(cop));

        } finally {
            RxJavaHooks.reset();
        }
    }

    static void assertTestException(List<Throwable> list, int index, String message) {
        assertTrue(list.get(index).toString(), list.get(index) instanceof TestException);
        assertEquals(message, list.get(index).getMessage());
    }

    @Test
    public void onXError() {
        try {
            final List<Throwable> list = new ArrayList<Throwable>();

            final TestException ex = new TestException();

            Func1<Throwable, Throwable> errorHandler = new Func1<Throwable, Throwable>() {
                @Override
                public Throwable call(Throwable t) {
                    list.add(t);
                    return ex;
                }
            };

            RxJavaHooks.setOnObservableSubscribeError(errorHandler);

            RxJavaHooks.setOnSingleSubscribeError(errorHandler);

            RxJavaHooks.setOnCompletableSubscribeError(errorHandler);

            assertSame(ex, RxJavaHooks.onObservableError(new TestException("Forced failure 1")));

            assertSame(ex, RxJavaHooks.onSingleError(new TestException("Forced failure 2")));

            assertSame(ex, RxJavaHooks.onCompletableError(new TestException("Forced failure 3")));

            assertTestException(list, 0, "Forced failure 1");

            assertTestException(list, 1, "Forced failure 2");

            assertTestException(list, 2, "Forced failure 3");
        } finally {
            RxJavaHooks.reset();
        }
    }

    @SuppressWarnings("deprecation")
    @Test
    public void onPluginsXError() {
        try {
            RxJavaPlugins.getInstance().reset();
            RxJavaHooks.reset();

            final List<Throwable> list = new ArrayList<Throwable>();

            final TestException ex = new TestException();

            final Func1<Throwable, Throwable> errorHandler = new Func1<Throwable, Throwable>() {
                @Override
                public Throwable call(Throwable t) {
                    list.add(t);
                    return ex;
                }
            };

            RxJavaPlugins.getInstance().registerObservableExecutionHook(new RxJavaObservableExecutionHook() {
                @Override
                public <T> Throwable onSubscribeError(Throwable e) {
                    return errorHandler.call(e);
                }
            });

            RxJavaPlugins.getInstance().registerSingleExecutionHook(new RxJavaSingleExecutionHook() {
                @Override
                public <T> Throwable onSubscribeError(Throwable e) {
                    return errorHandler.call(e);
                }
            });

            RxJavaPlugins.getInstance().registerCompletableExecutionHook(new RxJavaCompletableExecutionHook() {
                @Override
                public Throwable onSubscribeError(Throwable e) {
                    return errorHandler.call(e);
                }
            });

            assertSame(ex, RxJavaHooks.onObservableError(new TestException("Forced failure 1")));

            assertSame(ex, RxJavaHooks.onSingleError(new TestException("Forced failure 2")));

            assertSame(ex, RxJavaHooks.onCompletableError(new TestException("Forced failure 3")));

            assertTestException(list, 0, "Forced failure 1");

            assertTestException(list, 1, "Forced failure 2");

            assertTestException(list, 2, "Forced failure 3");
        } finally {
            RxJavaPlugins.getInstance().reset();
            RxJavaHooks.reset();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void onXLift() {
        try {
            Completable.Operator cop = new Completable.Operator() {
                @Override
                public CompletableSubscriber call(CompletableSubscriber t) {
                    return t;
                }
            };

            Observable.Operator oop = new Observable.Operator() {
                @Override
                public Object call(Object t) {
                    return t;
                }
            };

            final int[] counter = { 0 };

            RxJavaHooks.setOnObservableLift(new Func1<Observable.Operator, Observable.Operator>() {
                @Override
                public Observable.Operator call(Observable.Operator t) {
                    counter[0]++;
                    return t;
                }
            });

            RxJavaHooks.setOnSingleLift(new Func1<Observable.Operator, Observable.Operator>() {
                @Override
                public Observable.Operator call(Observable.Operator t) {
                    counter[0]++;
                    return t;
                }
            });

            RxJavaHooks.setOnCompletableLift(new Func1<Completable.Operator, Completable.Operator>() {
                @Override
                public Completable.Operator call(Completable.Operator t) {
                    counter[0]++;
                    return t;
                }
            });

            assertSame(oop, RxJavaHooks.onObservableLift(oop));

            assertSame(oop, RxJavaHooks.onSingleLift(oop));

            assertSame(cop, RxJavaHooks.onCompletableLift(cop));

            assertEquals(3, counter[0]);

        } finally {
            RxJavaHooks.reset();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
    @Test
    public void onPluginsXLift() {
        try {

            RxJavaPlugins.getInstance().reset();
            RxJavaHooks.reset();

            Completable.Operator cop = new Completable.Operator() {
                @Override
                public CompletableSubscriber call(CompletableSubscriber t) {
                    return t;
                }
            };

            Observable.Operator oop = new Observable.Operator() {
                @Override
                public Object call(Object t) {
                    return t;
                }
            };

            final int[] counter = { 0 };

            final Func1<Observable.Operator, Observable.Operator> onObservableLift = new Func1<Observable.Operator, Observable.Operator>() {
                @Override
                public Observable.Operator call(Observable.Operator t) {
                    counter[0]++;
                    return t;
                }
            };

            final Func1<Completable.Operator, Completable.Operator> onCompletableLift = new Func1<Completable.Operator, Completable.Operator>() {
                @Override
                public Completable.Operator call(Completable.Operator t) {
                    counter[0]++;
                    return t;
                }
            };

            RxJavaPlugins.getInstance().registerObservableExecutionHook(new RxJavaObservableExecutionHook() {
                @Override
                public <T, R> Observable.Operator<? extends R, ? super T> onLift(Observable.Operator<? extends R, ? super T> lift) {
                    return onObservableLift.call(lift);
                }
            });

            RxJavaPlugins.getInstance().registerSingleExecutionHook(new RxJavaSingleExecutionHook() {
                @Override
                public <T, R> Observable.Operator<? extends R, ? super T> onLift(Observable.Operator<? extends R, ? super T> lift) {
                    return onObservableLift.call(lift);
                }
            });

            RxJavaPlugins.getInstance().registerCompletableExecutionHook(new RxJavaCompletableExecutionHook() {
                @Override
                public Completable.Operator onLift(Completable.Operator lift) {
                    return onCompletableLift.call(lift);
                }
            });

            assertSame(oop, RxJavaHooks.onObservableLift(oop));

            assertSame(oop, RxJavaHooks.onSingleLift(oop));

            assertSame(cop, RxJavaHooks.onCompletableLift(cop));

            assertEquals(3, counter[0]);

        } finally {
            RxJavaPlugins.getInstance().reset();
            RxJavaHooks.reset();
        }
    }

    @Test
    public void noCallToHooksOnPlainError() {

        final boolean[] called = { false };

        RxJavaHooks.setOnError(new Action1<Throwable>() {
            @Override
            public void call(Throwable t) {
                called[0] = true;
            }
        });

        try {
            Observable.error(new TestException())
            .subscribe(new TestSubscriber<Object>());

            assertFalse(called[0]);
        } finally {
            RxJavaHooks.reset();
        }
    }
}
