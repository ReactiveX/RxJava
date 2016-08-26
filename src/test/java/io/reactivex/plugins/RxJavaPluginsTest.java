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

package io.reactivex.plugins;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.operators.completable.CompletableError;
import io.reactivex.internal.operators.flowable.FlowableRange;
import io.reactivex.internal.operators.observable.ObservableRange;
import io.reactivex.internal.operators.single.SingleJust;
import io.reactivex.internal.schedulers.ImmediateThinScheduler;
import io.reactivex.internal.subscriptions.ScalarSubscription;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;

public class RxJavaPluginsTest {

//    static Observable<Integer> createObservable() {
//        return Observable.range(1, 5).map(new Function<Integer, Integer>() {
//            @Override
//            public Integer apply(Integer t) {
//                throw new TestException();
//            }
//        });
//    }
//    
//    static Flowable<Integer> createFlowable() {
//        return Flowable.range(1, 5).map(new Function<Integer, Integer>() {
//            @Override
//            public Integer apply(Integer t) {
//                throw new TestException();
//            }
//        });
//    }
    
    @Test
    public void constructorShouldBePrivate() {
        TestHelper.checkUtilityClass(RxJavaPlugins.class);
    }

    @Test
    @Ignore("Not part of 2.0")
    public void assemblyTrackingObservable() {
//        RxJavaPlugins.enableAssemblyTracking();
//        try {
//            TestSubscriber<Integer> ts = TestSubscriber.create();
//            
//            createObservable().subscribe(ts);
//            
//            ts.assertError(TestException.class);
//            
//            Throwable ex = ts.getOnErrorEvents().get(0);
//            
//            AssemblyStackTraceException aste = AssemblyStackTraceException.find(ex);
//            
//            assertNotNull(aste);
//            
//            assertTrue(aste.getMessage(), aste.getMessage().contains("createObservable"));
//            
//            RxJavaPlugins.clearAssemblyTracking();
//
//            ts = TestSubscriber.create();
//            
//            createObservable().subscribe(ts);
//
//            ts.assertError(TestException.class);
//            
//            ex = ts.getOnErrorEvents().get(0);
//            
//            aste = AssemblyStackTraceException.find(ex);
//
//            assertNull(aste);
//        } finally {
//            RxJavaPlugins.resetAssemblyTracking();
//        }
    }
    
//    static Single<Integer> createSingle() {
//        return Single.just(1).map(new Function<Integer, Integer>() {
//            @Override
//            public Integer apply(Integer t) {
//                throw new TestException();
//            }
//        });
//    }
    
    @Test
    @Ignore("Not part of 2.0")
    public void assemblyTrackingSingle() {
//        RxJavaPlugins.enableAssemblyTracking();
//        try {
//            TestSubscriber<Integer> ts = TestSubscriber.create();
//            
//            createSingle().subscribe(ts);
//            
//            ts.assertError(TestException.class);
//            
//            Throwable ex = ts.getOnErrorEvents().get(0);
//            
//            AssemblyStackTraceException aste = AssemblyStackTraceException.find(ex);
//            
//            assertNotNull(aste);
//            
//            assertTrue(aste.getMessage(), aste.getMessage().contains("createSingle"));
//
//            RxJavaPlugins.clearAssemblyTracking();
//
//            ts = TestSubscriber.create();
//            
//            createSingle().subscribe(ts);
//
//            ts.assertError(TestException.class);
//            
//            ex = ts.getOnErrorEvents().get(0);
//            
//            aste = AssemblyStackTraceException.find(ex);
//
//            assertNull(aste);
//        } finally {
//            RxJavaPlugins.resetAssemblyTracking();
//        }
    }
    
//    static Completable createCompletable() {
//        return Completable.error(new Callable<Throwable>() {
//            @Override
//            public Throwable call() {
//                return new TestException();
//            }
//        });
//    }
    
    @Test
    @Ignore("Not part of 2.0")
    public void assemblyTrackingCompletable() {
//        RxJavaPlugins.enableAssemblyTracking();
//        try {
//            TestSubscriber<Integer> ts = TestSubscriber.create();
//            
//            createCompletable().subscribe(ts);
//            
//            ts.assertError(TestException.class);
//            
//            Throwable ex = ts.getOnErrorEvents().get(0);
//            
//            AssemblyStackTraceException aste = AssemblyStackTraceException.find(ex);
//            
//            assertNotNull(aste);
//            
//            assertTrue(aste.getMessage(), aste.getMessage().contains("createCompletable"));
//
//            RxJavaPlugins.clearAssemblyTracking();
//
//            ts = TestSubscriber.create();
//            
//            createCompletable().subscribe(ts);
//
//            ts.assertError(TestException.class);
//
//            ex = ts.getOnErrorEvents().get(0);
//            
//            aste = AssemblyStackTraceException.find(ex);
//
//            assertNull(aste);
//            
//        } finally {
//            RxJavaPlugins.resetAssemblyTracking();
//        }
    }
    
    @SuppressWarnings({ "rawtypes" })
    @Test
    public void lockdown() throws Exception {
        RxJavaPlugins.reset();
        RxJavaPlugins.lockdown();
        try {
            assertTrue(RxJavaPlugins.isLockdown());
            Consumer a1 = Functions.emptyConsumer();
            Callable f0 = new Callable() {
                @Override
                public Object call() {
                    return null;
                }
            };
            Function f1 = Functions.identity();
            BiFunction f2 = new BiFunction() {
                @Override
                public Object apply(Object t1, Object t2) {
                    return t2;
                }
            };
            
            for (Method m : RxJavaPlugins.class.getMethods()) {
                if (m.getName().startsWith("set")) {
                    
                    Method getter = RxJavaPlugins.class.getMethod("get" + m.getName().substring(3));
                    
                    Object before = getter.invoke(null);
                    
                    try {
                        if (m.getParameterTypes()[0].isAssignableFrom(Callable.class)) {
                            m.invoke(null, f0);
                        } else
                        if (m.getParameterTypes()[0].isAssignableFrom(Function.class)) {
                            m.invoke(null, f1);
                        } else
                        if (m.getParameterTypes()[0].isAssignableFrom(Consumer.class)) {
                            m.invoke(null, a1);
                        } else {
                            m.invoke(null, f2);
                        }
                        fail("Should have thrown InvocationTargetException(IllegalStateException)");
                    } catch (InvocationTargetException ex) {
                        if (ex.getCause() instanceof IllegalStateException) {
                            assertEquals("Plugins can't be changed anymore",ex.getCause().getMessage());
                        } else {
                            fail("Should have thrown InvocationTargetException(IllegalStateException)");
                        }
                    }
                    
                    Object after = getter.invoke(null);
                    
                    assertSame(m.toString(), before, after);
                }
            }
            
//            Object o1 = RxJavaPlugins.getOnObservableCreate();
//            Object o2 = RxJavaPlugins.getOnSingleCreate();
//            Object o3 = RxJavaPlugins.getOnCompletableCreate();
//            
//            RxJavaPlugins.enableAssemblyTracking();
//            RxJavaPlugins.clearAssemblyTracking();
//            RxJavaPlugins.resetAssemblyTracking();
//
//            
//            assertSame(o1, RxJavaPlugins.getOnObservableCreate());
//            assertSame(o2, RxJavaPlugins.getOnSingleCreate());
//            assertSame(o3, RxJavaPlugins.getOnCompletableCreate());
            
        } finally {
            RxJavaPlugins.unlock();
            RxJavaPlugins.reset();
            assertFalse(RxJavaPlugins.isLockdown());
        }
    }
    
    Function<Scheduler, Scheduler> replaceWithImmediate = new Function<Scheduler, Scheduler>() {
        @Override
        public Scheduler apply(Scheduler t) {
            return ImmediateThinScheduler.INSTANCE;
        }
    };

    @Test
    public void overrideSingleScheduler() {
        try {
            RxJavaPlugins.setSingleSchedulerHandler(replaceWithImmediate);
            
            assertSame(ImmediateThinScheduler.INSTANCE, Schedulers.single());
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertNotSame(ImmediateThinScheduler.INSTANCE, Schedulers.computation());
    }

    @Test
    public void overrideComputationScheduler() {
        try {
            RxJavaPlugins.setComputationSchedulerHandler(replaceWithImmediate);
            
            assertSame(ImmediateThinScheduler.INSTANCE, Schedulers.computation());
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertNotSame(ImmediateThinScheduler.INSTANCE, Schedulers.computation());
    }

    @Test
    public void overrideIoScheduler() {
        try {
            RxJavaPlugins.setIoSchedulerHandler(replaceWithImmediate);
            
            assertSame(ImmediateThinScheduler.INSTANCE, Schedulers.io());
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertNotSame(ImmediateThinScheduler.INSTANCE, Schedulers.io());
    }

    @Test
    public void overrideNewThreadScheduler() {
        try {
            RxJavaPlugins.setNewThreadSchedulerHandler(replaceWithImmediate);
            
            assertSame(ImmediateThinScheduler.INSTANCE, Schedulers.newThread());
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertNotSame(ImmediateThinScheduler.INSTANCE, Schedulers.newThread());
    }

    @Test
    public void overrideInitSingleScheduler() {
        Scheduler s = Schedulers.single(); // make sure the Schedulers is initialized
        try {
            RxJavaPlugins.setInitSingleSchedulerHandler(replaceWithImmediate);
            
            assertSame(ImmediateThinScheduler.INSTANCE, RxJavaPlugins.initSingleScheduler(s));
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertSame(s, RxJavaPlugins.initSingleScheduler(s));
    }

    @Test
    public void overrideInitComputationScheduler() {
        Scheduler s = Schedulers.computation(); // make sure the Schedulers is initialized
        try {
            RxJavaPlugins.setInitComputationSchedulerHandler(replaceWithImmediate);
            
            assertSame(ImmediateThinScheduler.INSTANCE, RxJavaPlugins.initComputationScheduler(s));
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertSame(s, RxJavaPlugins.initComputationScheduler(s));
    }

    @Test
    public void overrideInitIoScheduler() {
        Scheduler s = Schedulers.io(); // make sure the Schedulers is initialized
        try {
            RxJavaPlugins.setInitIoSchedulerHandler(replaceWithImmediate);
            
            assertSame(ImmediateThinScheduler.INSTANCE, RxJavaPlugins.initIoScheduler(s));
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertSame(s, RxJavaPlugins.initIoScheduler(s));
    }

    @Test
    public void overrideInitNewThreadScheduler() {
        Scheduler s = Schedulers.newThread(); // make sure the Schedulers is initialized
        try {
            RxJavaPlugins.setInitNewThreadSchedulerHandler(replaceWithImmediate);
            
            assertSame(ImmediateThinScheduler.INSTANCE, RxJavaPlugins.initNewThreadScheduler(s));
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        assertSame(s, RxJavaPlugins.initNewThreadScheduler(s));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void observableCreate() {
        try {
            RxJavaPlugins.setOnObservableAssembly(new Function<Observable, Observable>() {
                @Override
                public Observable apply(Observable t) {
                    return new ObservableRange(1, 2);
                }
            });
            
            Observable.range(10, 3)
            .test()
            .assertValues(1, 2)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Observable.range(10, 3)
        .test()
        .assertValues(10, 11, 12)
        .assertNoErrors()
        .assertComplete();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void flowableCreate() {
        try {
            RxJavaPlugins.setOnFlowableAssembly(new Function<Flowable, Flowable>() {
                @Override
                public Flowable apply(Flowable t) {
                    return new FlowableRange(1, 2);
                }
            });
            
            Flowable.range(10, 3)
            .test()
            .assertValues(1, 2)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Flowable.range(10, 3)
        .test()
        .assertValues(10, 11, 12)
        .assertNoErrors()
        .assertComplete();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void observableStart() {
        try {
            RxJavaPlugins.setOnObservableSubscribe(new BiFunction<Observable, Observer, Observer>() {
                @Override
                public Observer apply(Observable o, final Observer t) {
                    return new Observer() {

                        @Override
                        public void onSubscribe(Disposable d) {
                            t.onSubscribe(d);
                        }

                        @SuppressWarnings("unchecked")
                        @Override
                        public void onNext(Object value) {
                            t.onNext((Integer)value - 9);
                        }

                        @Override
                        public void onError(Throwable e) {
                            t.onError(e);
                        }

                        @Override
                        public void onComplete() {
                            t.onComplete();
                        }
                        
                    };
                }
            });
            
            Observable.range(10, 3)
            .test()
            .assertValues(1, 2, 3)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Observable.range(10, 3)
        .test()
        .assertValues(10, 11, 12)
        .assertNoErrors()
        .assertComplete();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void flowableStart() {
        try {
            RxJavaPlugins.setOnFlowableSubscribe(new BiFunction<Flowable, Subscriber, Subscriber>() {
                @Override
                public Subscriber apply(Flowable o, final Subscriber t) {
                    return new Subscriber() {

                        @Override
                        public void onSubscribe(Subscription d) {
                            t.onSubscribe(d);
                        }

                        @SuppressWarnings("unchecked")
                        @Override
                        public void onNext(Object value) {
                            t.onNext((Integer)value - 9);
                        }

                        @Override
                        public void onError(Throwable e) {
                            t.onError(e);
                        }

                        @Override
                        public void onComplete() {
                            t.onComplete();
                        }
                        
                    };
                }
            });
            
            Flowable.range(10, 3)
            .test()
            .assertValues(1, 2, 3)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Flowable.range(10, 3)
        .test()
        .assertValues(10, 11, 12)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    @Ignore("Different architecture, no longer supported")
    public void observableReturn() {
//        try {
//            final Subscription s = Subscriptions.empty();
//            
//            RxJavaPlugins.setOnObservableReturn(new Function<Subscription, Subscription>() {
//                @Override
//                public Subscription call(Subscription t) {
//                    return s;
//                }
//            });
//            
//            TestSubscriber<Integer> ts = TestSubscriber.create();
//            
//            Subscription u = Observable.range(10, 3).subscribe(ts);
//            
//            ts.assertValues(10, 11, 12);
//            ts.assertNoErrors();
//            ts.assertComplete();
//            
//            assertSame(s, u);
//        } finally {
//            RxJavaPlugins.reset();
//        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void singleCreate() {
        try {
            RxJavaPlugins.setOnSingleAssembly(new Function<Single, Single>() {
                @Override
                public Single apply(Single t) {
                    return new SingleJust<Integer>(10);
                }
            });
            
            Single.just(1)
            .test()
            .assertValue(10)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Single.just(1)
        .test()
        .assertValue(1)
        .assertNoErrors()
        .assertComplete();
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void singleStart() {
        try {
            RxJavaPlugins.setOnSingleSubscribe(new BiFunction<Single, SingleObserver, SingleObserver>() {
                @Override
                public SingleObserver apply(Single o, final SingleObserver t) {
                    return new SingleObserver<Object>() {

                        @Override
                        public void onSubscribe(Disposable d) {
                            t.onSubscribe(d);
                        }

                        @SuppressWarnings("unchecked")
                        @Override
                        public void onSuccess(Object value) {
                            t.onSuccess(10);
                        }

                        @Override
                        public void onError(Throwable e) {
                            t.onError(e);
                        }
                        
                    };
                }
            });
            
            Single.just(1)
            .test()
            .assertValue(10)
            .assertNoErrors()
            .assertComplete();
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Single.just(1)
        .test()
        .assertValue(1)
        .assertNoErrors()
        .assertComplete();
    }
    
    @Test
    @Ignore("Different architecture, no longer supported")
    public void singleReturn() {
//        try {
//            final Subscription s = Subscriptions.empty();
//            
//            RxJavaPlugins.setOnSingleReturn(new Function<Subscription, Subscription>() {
//                @Override
//                public Subscription call(Subscription t) {
//                    return s;
//                }
//            });
//            
//            TestSubscriber<Integer> ts = TestSubscriber.create();
//            
//            Subscription u = Single.just(1).subscribe(ts);
//            
//            ts.assertValue(1);
//            ts.assertNoErrors();
//            ts.assertComplete();
//            
//            assertSame(s, u);
//        } finally {
//            RxJavaPlugins.reset();
//        }
    }

    @Test
    public void completableCreate() {
        try {
            RxJavaPlugins.setOnCompletableAssembly(new Function<Completable, Completable>() {
                @Override
                public Completable apply(Completable t) {
                    return new CompletableError(new TestException());
                }
            });
            
            Completable.complete()
            .test()
            .assertNoValues()
            .assertNotComplete()
            .assertError(TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        Completable.complete()
        .test()
        .assertNoValues()
        .assertNoErrors()
        .assertComplete();
    }
    
    @Test
    public void completableStart() {
        try {
            RxJavaPlugins.setOnCompletableSubscribe(new BiFunction<Completable, CompletableObserver, CompletableObserver>() {
                @Override
                public CompletableObserver apply(Completable o, final CompletableObserver t) {
                    return new CompletableObserver() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            t.onSubscribe(d);
                        }
                        
                        @Override
                        public void onError(Throwable e) {
                            t.onError(e);
                        }
                        
                        @Override
                        public void onComplete() {
                            t.onError(new TestException());
                        }
                    };
                }
            });
            
            Completable.complete()
            .test()
            .assertNoValues()
            .assertNotComplete()
            .assertError(TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
        // make sure the reset worked
        
        Completable.complete()
        .test()
        .assertNoValues()
        .assertNoErrors()
        .assertComplete();
    }

    void onSchedule(Worker w) throws InterruptedException {
        try {
            try {
                final AtomicInteger value = new AtomicInteger();
                final CountDownLatch cdl = new CountDownLatch(1);
                
                RxJavaPlugins.setScheduleHandler(new Function<Runnable, Runnable>() {
                    @Override
                    public Runnable apply(Runnable t) {
                        return new Runnable() {
                            @Override
                            public void run() {
                                value.set(10);
                                cdl.countDown();
                            }
                        };
                    }
                });
                
                w.schedule(new Runnable() {
                    @Override
                    public void run() {
                        value.set(1);
                        cdl.countDown();
                    }
                });
                
                cdl.await();
                
                assertEquals(10, value.get());
                
            } finally {
                
                RxJavaPlugins.reset();
            }
            
            // make sure the reset worked
            final AtomicInteger value = new AtomicInteger();
            final CountDownLatch cdl = new CountDownLatch(1);
            
            w.schedule(new Runnable() {
                @Override
                public void run() {
                    value.set(1);
                    cdl.countDown();
                }
            });
            
            cdl.await();
            
            assertEquals(1, value.get());
        } finally {
            w.dispose();
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
            
            RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t) {
                    list.add(t);
                }
            });
            
            RxJavaPlugins.onError(new TestException("Forced failure"));
            
            assertEquals(1, list.size());
            assertTestException(list, 0, "Forced failure");
        } finally {
            RxJavaPlugins.reset();
        }
    }
    
    @Test
    @Ignore("No (need for) clear() method in 2.0")
    public void clear() throws Exception {
//        RxJavaPlugins.reset();
//        try {
//            RxJavaPlugins.clear();
//            for (Method m : RxJavaPlugins.class.getMethods()) {
//                if (m.getName().startsWith("getOn")) {
//                    assertNull(m.toString(), m.invoke(null));
//                }
//            }
//
//        } finally {
//            RxJavaPlugins.reset();
//        }
//
//        for (Method m : RxJavaPlugins.class.getMethods()) {
//            if (m.getName().startsWith("getOn") 
//                    && !m.getName().endsWith("Scheduler")
//                    && !m.getName().contains("GenericScheduledExecutorService")) {
//                assertNotNull(m.toString(), m.invoke(null));
//            }
//        }
    }

    @Test
    public void onErrorNoHandler() {
        try {
            final List<Throwable> list = new ArrayList<Throwable>();
            
            RxJavaPlugins.setErrorHandler(null);
            
            Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    list.add(e);
                    
                }
            });
            
            RxJavaPlugins.onError(new TestException("Forced failure"));

            Thread.currentThread().setUncaughtExceptionHandler(null);

            // this will be printed on the console and should not crash
            RxJavaPlugins.onError(new TestException("Forced failure 3"));

            assertEquals(1, list.size());
            assertTestException(list, 0, "Forced failure");
        } finally {
            RxJavaPlugins.reset();
            Thread.currentThread().setUncaughtExceptionHandler(null);
        }
    }

    @Test
    public void onErrorCrashes() {
        try {
            final List<Throwable> list = new ArrayList<Throwable>();
            
            RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t) {
                    throw new TestException("Forced failure 2");
                }
            });

            Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    list.add(e);
                    
                }
            });

            RxJavaPlugins.onError(new TestException("Forced failure"));
            
            assertEquals(2, list.size());
            assertTestException(list, 0, "Forced failure 2");
            assertTestException(list, 1, "Forced failure");

            Thread.currentThread().setUncaughtExceptionHandler(null);
            
        } finally {
            RxJavaPlugins.reset();
            Thread.currentThread().setUncaughtExceptionHandler(null);
        }
    }

    @Test
    public void onErrorWithNull() {
        try {
            final List<Throwable> list = new ArrayList<Throwable>();
            
            RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t) {
                    throw new TestException("Forced failure 2");
                }
            });

            Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    list.add(e);
                    
                }
            });

            RxJavaPlugins.onError(null);
            
            assertEquals(2, list.size());
            assertTestException(list, 0, "Forced failure 2");
            assertNPE(list, 1);

            RxJavaPlugins.reset();
            
            RxJavaPlugins.onError(null);
            
            assertNPE(list, 2);

        } finally {
            RxJavaPlugins.reset();
            
            Thread.currentThread().setUncaughtExceptionHandler(null);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void clearIsPassthrough() {
        try {
            RxJavaPlugins.reset();
            
            assertNull(RxJavaPlugins.onAssembly((Observable)null));

            assertNull(RxJavaPlugins.onAssembly((ConnectableObservable)null));

            assertNull(RxJavaPlugins.onAssembly((Flowable)null));
            
            assertNull(RxJavaPlugins.onAssembly((ConnectableFlowable)null));

            Observable oos = new Observable() {
                @Override
                public void subscribeActual(Observer t) {
                    
                }
            };

            Flowable fos = new Flowable() {
                @Override
                public void subscribeActual(Subscriber t) {
                    
                }
            };

            assertSame(oos, RxJavaPlugins.onAssembly(oos));

            assertSame(fos, RxJavaPlugins.onAssembly(fos));

            assertNull(RxJavaPlugins.onAssembly((Single)null));

            Single sos = new Single() {
                @Override
                public void subscribeActual(SingleObserver t) {
                    
                }
            };

            assertSame(sos, RxJavaPlugins.onAssembly(sos));
            
            assertNull(RxJavaPlugins.onAssembly((Completable)null));

            Completable cos = new Completable() {
                @Override
                public void subscribeActual(CompletableObserver t) {
                    
                }
            };

            assertSame(cos, RxJavaPlugins.onAssembly(cos));
            
            assertNull(RxJavaPlugins.onSchedule(null));
            
            Runnable action = Functions.EMPTY_RUNNABLE;
            
            assertSame(action, RxJavaPlugins.onSchedule(action));
            
            class AllSubscriber implements Subscriber, Observer, SingleObserver, CompletableObserver {

                @Override
                public void onSuccess(Object value) {
                    
                }

                @Override
                public void onSubscribe(Disposable d) {
                    
                }

                @Override
                public void onSubscribe(Subscription s) {
                    
                }

                @Override
                public void onNext(Object t) {
                    
                }

                @Override
                public void onError(Throwable t) {
                    
                }

                @Override
                public void onComplete() {
                    
                }
                
            }
            
            AllSubscriber all = new AllSubscriber();
            
            assertNull(RxJavaPlugins.onSubscribe(Observable.never(), null));
            
            assertSame(all, RxJavaPlugins.onSubscribe(Observable.never(), all));

            assertNull(RxJavaPlugins.onSubscribe(Flowable.never(), null));
            
            assertSame(all, RxJavaPlugins.onSubscribe(Flowable.never(), all));

            assertNull(RxJavaPlugins.onSubscribe(Single.just(1), null));
            
            assertSame(all, RxJavaPlugins.onSubscribe(Single.just(1), all));

            assertNull(RxJavaPlugins.onSubscribe(Completable.never(), null));
            
            assertSame(all, RxJavaPlugins.onSubscribe(Completable.never(), all));

            // These hooks don't exist in 2.0
//            Subscription subscription = Subscriptions.empty();
//            
//            assertNull(RxJavaPlugins.onObservableReturn(null));
//            
//            assertSame(subscription, RxJavaPlugins.onObservableReturn(subscription));
//
//            assertNull(RxJavaPlugins.onSingleReturn(null));
//            
//            assertSame(subscription, RxJavaPlugins.onSingleReturn(subscription));
//
//            TestException ex = new TestException();
//            
//            assertNull(RxJavaPlugins.onObservableError(null));
//            
//            assertSame(ex, RxJavaPlugins.onObservableError(ex));
//
//            assertNull(RxJavaPlugins.onSingleError(null));
//            
//            assertSame(ex, RxJavaPlugins.onSingleError(ex));
//
//            assertNull(RxJavaPlugins.onCompletableError(null));
//            
//            assertSame(ex, RxJavaPlugins.onCompletableError(ex));
//
//            Observable.Operator oop = new Observable.Operator() {
//                @Override
//                public Object call(Object t) {
//                    return t;
//                }
//            };
//            
//            assertNull(RxJavaPlugins.onObservableLift(null));
//            
//            assertSame(oop, RxJavaPlugins.onObservableLift(oop));
//            
//            assertNull(RxJavaPlugins.onSingleLift(null));
//            
//            assertSame(oop, RxJavaPlugins.onSingleLift(oop));
//            
//            Completable.CompletableOperator cop = new Completable.CompletableOperator() {
//                @Override
//                public CompletableSubscriber call(CompletableSubscriber t) {
//                    return t;
//                }
//            };
//            
//            assertNull(RxJavaPlugins.onCompletableLift(null));
//            
//            assertSame(cop, RxJavaPlugins.onCompletableLift(cop));
              
            assertNull(RxJavaPlugins.onComputationScheduler(null));

            assertNull(RxJavaPlugins.onIoScheduler(null));

            assertNull(RxJavaPlugins.onNewThreadScheduler(null));

            assertNull(RxJavaPlugins.onSingleScheduler(null));

            Scheduler s = ImmediateThinScheduler.INSTANCE;

            assertSame(s, RxJavaPlugins.onComputationScheduler(s));

            assertSame(s, RxJavaPlugins.onIoScheduler(s));

            assertSame(s, RxJavaPlugins.onNewThreadScheduler(s));

            assertSame(s, RxJavaPlugins.onSingleScheduler(s));

            
            assertNull(RxJavaPlugins.initComputationScheduler(null));

            assertNull(RxJavaPlugins.initIoScheduler(null));

            assertNull(RxJavaPlugins.initNewThreadScheduler(null));

            assertNull(RxJavaPlugins.initSingleScheduler(null));

            assertSame(s, RxJavaPlugins.initComputationScheduler(s));

            assertSame(s, RxJavaPlugins.initIoScheduler(s));

            assertSame(s, RxJavaPlugins.initNewThreadScheduler(s));

            assertSame(s, RxJavaPlugins.initSingleScheduler(s));

            
        } finally {
            RxJavaPlugins.reset();
        }
    }
    
    static void assertTestException(List<Throwable> list, int index, String message) {
        assertTrue(list.get(index).toString(), list.get(index) instanceof TestException);
        assertEquals(message, list.get(index).getMessage());
    }

    static void assertNPE(List<Throwable> list, int index) {
        assertTrue(list.get(index).toString(), list.get(index) instanceof NullPointerException);
    }

    @Test
    @Ignore("Not present in 2.0")
    public void onXError() {
//        try {
//            final List<Throwable> list = new ArrayList<Throwable>();
//            
//            final TestException ex = new TestException();
//            
//            Function<Throwable, Throwable> errorHandler = new Function<Throwable, Throwable>() {
//                @Override
//                public Throwable a(Throwable t) {
//                    list.add(t);
//                    return ex;
//                }
//            };
//            
//            RxJavaPlugins.setOnObservableSubscribeError(errorHandler);
//
//            RxJavaPlugins.setOnSingleSubscribeError(errorHandler);
//
//            RxJavaPlugins.setOnCompletableSubscribeError(errorHandler);
//
//            assertSame(ex, RxJavaPlugins.onObservableError(new TestException("Forced failure 1")));
//
//            assertSame(ex, RxJavaPlugins.onSingleError(new TestException("Forced failure 2")));
//            
//            assertSame(ex, RxJavaPlugins.onCompletableError(new TestException("Forced failure 3")));
//            
//            assertTestException(list, 0, "Forced failure 1");
//            
//            assertTestException(list, 1, "Forced failure 2");
//
//            assertTestException(list, 2, "Forced failure 3");
//        } finally {
//            RxJavaPlugins.reset();
//        }
    }

//    @SuppressWarnings("deprecation")
    @Test
    @Ignore("Not present in 2.0")
    public void onPluginsXError() {
//        try {
//            RxJavaPlugins.reset();
//            
//            final List<Throwable> list = new ArrayList<Throwable>();
//            
//            final TestException ex = new TestException();
//            
//            final Function<Throwable, Throwable> errorHandler = new Function<Throwable, Throwable>() {
//                @Override
//                public Throwable apply(Throwable t) {
//                    list.add(t);
//                    return ex;
//                }
//            };
//
//            RxJavaPlugins.getInstance().registerObservableExecutionHook(new RxJavaObservableExecutionHook() {
//                @Override
//                public <T> Throwable onSubscribeError(Throwable e) {
//                    return errorHandler.call(e);
//                }
//            });
//
//            RxJavaPlugins.getInstance().registerSingleExecutionHook(new RxJavaSingleExecutionHook() {
//                @Override
//                public <T> Throwable onSubscribeError(Throwable e) {
//                    return errorHandler.call(e);
//                }
//            });
//
//            RxJavaPlugins.getInstance().registerCompletableExecutionHook(new RxJavaCompletableExecutionHook() {
//                @Override
//                public Throwable onSubscribeError(Throwable e) {
//                    return errorHandler.call(e);
//                }
//            });
//
//            assertSame(ex, RxJavaPlugins.onObservableError(new TestException("Forced failure 1")));
//
//            assertSame(ex, RxJavaPlugins.onSingleError(new TestException("Forced failure 2")));
//            
//            assertSame(ex, RxJavaPlugins.onCompletableError(new TestException("Forced failure 3")));
//            
//            assertTestException(list, 0, "Forced failure 1");
//            
//            assertTestException(list, 1, "Forced failure 2");
//
//            assertTestException(list, 2, "Forced failure 3");
//        } finally {
//            RxJavaPlugins.getInstance().reset();
//            RxJavaPlugins.reset();
//        }
    }

//    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    @Ignore("Not present in 2.0")
    public void onXLift() {
//        try {
//            Completable.CompletableOperator cop = new Completable.CompletableOperator() {
//                @Override
//                public CompletableSubscriber call(CompletableSubscriber t) {
//                    return t;
//                }
//            };
//            
//            Observable.Operator oop = new Observable.Operator() {
//                @Override
//                public Object call(Object t) {
//                    return t;
//                }
//            };
//
//            final int[] counter = { 0 };
//
//            RxJavaPlugins.setOnObservableLift(new Function<Operator, Operator>() {
//                @Override
//                public Operator call(Operator t) {
//                    counter[0]++;
//                    return t;
//                }
//            });
//
//            RxJavaPlugins.setOnSingleLift(new Function<Operator, Operator>() {
//                @Override
//                public Operator call(Operator t) {
//                    counter[0]++;
//                    return t;
//                }
//            });
//
//            RxJavaPlugins.setOnCompletableLift(new Function<CompletableOperator, CompletableOperator>() {
//                @Override
//                public CompletableOperator call(CompletableOperator t) {
//                    counter[0]++;
//                    return t;
//                }
//            });
//            
//            assertSame(oop, RxJavaPlugins.onObservableLift(oop));
//
//            assertSame(oop, RxJavaPlugins.onSingleLift(oop));
//
//            assertSame(cop, RxJavaPlugins.onCompletableLift(cop));
//            
//            assertEquals(3, counter[0]);
//
//        } finally {
//            RxJavaPlugins.reset();
//        }
    }

//    @SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
    @Test
    @Ignore("Not present in 2.0")
    public void onPluginsXLift() {
//        try {
//            
//            RxJavaPlugins.getInstance().reset();
//            RxJavaPlugins.reset();
//
//            Completable.CompletableOperator cop = new Completable.CompletableOperator() {
//                @Override
//                public CompletableSubscriber call(CompletableSubscriber t) {
//                    return t;
//                }
//            };
//            
//            Observable.Operator oop = new Observable.Operator() {
//                @Override
//                public Object call(Object t) {
//                    return t;
//                }
//            };
//
//            final int[] counter = { 0 };
//
//            final Function<Operator, Operator> onObservableLift = new Function<Operator, Operator>() {
//                @Override
//                public Operator call(Operator t) {
//                    counter[0]++;
//                    return t;
//                }
//            };
//
//            final Function<CompletableOperator, CompletableOperator> onCompletableLift = new Function<CompletableOperator, CompletableOperator>() {
//                @Override
//                public CompletableOperator call(CompletableOperator t) {
//                    counter[0]++;
//                    return t;
//                }
//            };
//            
//            RxJavaPlugins.getInstance().registerObservableExecutionHook(new RxJavaObservableExecutionHook() {
//                @Override
//                public <T, R> Operator<? extends R, ? super T> onLift(Operator<? extends R, ? super T> lift) {
//                    return onObservableLift.call(lift);
//                }
//            });
//            
//            RxJavaPlugins.getInstance().registerSingleExecutionHook(new RxJavaSingleExecutionHook() {
//                @Override
//                public <T, R> Operator<? extends R, ? super T> onLift(Operator<? extends R, ? super T> lift) {
//                    return onObservableLift.call(lift);
//                }
//            });
//            
//            RxJavaPlugins.getInstance().registerCompletableExecutionHook(new RxJavaCompletableExecutionHook() {
//                @Override
//                public CompletableOperator onLift(CompletableOperator lift) {
//                    return onCompletableLift.call(lift);
//                }
//            });
//            
//            assertSame(oop, RxJavaPlugins.onObservableLift(oop));
//
//            assertSame(oop, RxJavaPlugins.onSingleLift(oop));
//
//            assertSame(cop, RxJavaPlugins.onCompletableLift(cop));
//            
//            assertEquals(3, counter[0]);
//
//        } finally {
//            RxJavaPlugins.reset();
//        }
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void overrideConnectableObservable() {
        try {
            RxJavaPlugins.setOnConnectableObservableAssembly(new Function<ConnectableObservable, ConnectableObservable>() {
                @Override
                public ConnectableObservable apply(ConnectableObservable co) throws Exception {
                    return new ConnectableObservable() {
                        
                        @Override
                        public void connect(Consumer connection) {
                            
                        }
                        
                        @SuppressWarnings("unchecked")
                        @Override
                        protected void subscribeActual(Observer observer) {
                            observer.onSubscribe(Disposables.empty());
                            observer.onNext(10);
                            observer.onComplete();
                        }
                    };
                }
            });
            
            Observable
            .just(1)
            .publish()
            .autoConnect()
            .test()
            .assertResult(10);
            
        } finally {
            RxJavaPlugins.reset();
        }
        
        Observable
        .just(1)
        .publish()
        .autoConnect()
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void overrideConnectableFlowable() {
        try {
            RxJavaPlugins.setOnConnectableFlowableAssembly(new Function<ConnectableFlowable, ConnectableFlowable>() {
                @Override
                public ConnectableFlowable apply(ConnectableFlowable co) throws Exception {
                    return new ConnectableFlowable() {
                        
                        @Override
                        public void connect(Consumer connection) {
                            
                        }
                        
                        @SuppressWarnings("unchecked")
                        @Override
                        protected void subscribeActual(Subscriber subscriber) {
                            subscriber.onSubscribe(new ScalarSubscription(subscriber, 10));
                        }
                    };
                }
            });
            
            Flowable
            .just(1)
            .publish()
            .autoConnect()
            .test()
            .assertResult(10);
            
        } finally {
            RxJavaPlugins.reset();
        }
        
        Flowable
        .just(1)
        .publish()
        .autoConnect()
        .test()
        .assertResult(1);
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void assemblyHookCrashes() {
        try {
            RxJavaPlugins.setOnFlowableAssembly(new Function<Flowable, Flowable>() {
                @Override
                public Flowable apply(Flowable f) throws Exception {
                    throw new IllegalArgumentException();
                }
            });
            
            try {
                Flowable.empty();
                fail("Should have thrown!");
            } catch (IllegalArgumentException ex) {
                // expected
            }
            
            RxJavaPlugins.setOnFlowableAssembly(new Function<Flowable, Flowable>() {
                @Override
                public Flowable apply(Flowable f) throws Exception {
                    throw new InternalError();
                }
            });
            
            try {
                Flowable.empty();
                fail("Should have thrown!");
            } catch (InternalError ex) {
                // expected
            }
            
            RxJavaPlugins.setOnFlowableAssembly(new Function<Flowable, Flowable>() {
                @Override
                public Flowable apply(Flowable f) throws Exception {
                    throw new IOException();
                }
            });
            
            try {
                Flowable.empty();
                fail("Should have thrown!");
            } catch (RuntimeException ex) {
                if (!(ex.getCause() instanceof IOException)) {
                    fail(ex.getCause().toString() + ": Should have thrown RuntimeException(IOException)");
                }
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    public void subscribeHookCrashes() {
        try {
            RxJavaPlugins.setOnFlowableSubscribe(new BiFunction<Flowable, Subscriber, Subscriber>() {
                @Override
                public Subscriber apply(Flowable f, Subscriber s) throws Exception {
                    throw new IllegalArgumentException();
                }
            });
            
            try {
                Flowable.empty().test();
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                if (!(ex.getCause() instanceof IllegalArgumentException)) {
                    fail(ex.getCause().toString() + ": Should have thrown NullPointerException(IllegalArgumentException)");
                }
            }
            
            RxJavaPlugins.setOnFlowableSubscribe(new BiFunction<Flowable, Subscriber, Subscriber>() {
                @Override
                public Subscriber apply(Flowable f, Subscriber s) throws Exception {
                    throw new InternalError();
                }
            });
            
            try {
                Flowable.empty().test();
                fail("Should have thrown!");
            } catch (InternalError ex) {
                // expected
            }
            
            RxJavaPlugins.setOnFlowableSubscribe(new BiFunction<Flowable, Subscriber, Subscriber>() {
                @Override
                public Subscriber apply(Flowable f, Subscriber s) throws Exception {
                    throw new IOException();
                }
            });
            
            try {
                Flowable.empty().test();
                fail("Should have thrown!");
            } catch (NullPointerException ex) {
                if (!(ex.getCause() instanceof RuntimeException)) {
                    fail(ex.getCause().toString() + ": Should have thrown NullPointerException(RuntimeException(IOException))");
                }
                if (!(ex.getCause().getCause() instanceof IOException)) {
                    fail(ex.getCause().toString() + ": Should have thrown NullPointerException(RuntimeException(IOException))");
                }
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

}