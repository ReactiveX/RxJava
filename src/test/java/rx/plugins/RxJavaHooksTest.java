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

import java.lang.reflect.Method;

import org.junit.*;

import rx.*;
import rx.Completable.*;
import rx.Observable.OnSubscribe;
import rx.exceptions.*;
import rx.functions.*;
import rx.internal.operators.OnSubscribeRange;
import rx.internal.producers.SingleProducer;
import rx.internal.util.UtilityFunctions;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class RxJavaHooksTest {

    static Observable<Integer> createObservable() {
        return Observable.range(1, 5).map(new Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer t) {
                throw new TestException();
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
            
            ts.assertError(AssemblyStackTraceException.class);
            
            Throwable ex = ts.getOnErrorEvents().get(0);
            
            Assert.assertTrue("" + ex.getCause(), ex.getCause() instanceof TestException);
            
            Assert.assertTrue("" + ex, ex instanceof AssemblyStackTraceException);
            
            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("createObservable"));
        } finally {
            RxJavaHooks.resetAssemblyTracking();
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
            
            ts.assertError(AssemblyStackTraceException.class);
            
            Throwable ex = ts.getOnErrorEvents().get(0);

            Assert.assertTrue("" + ex, ex instanceof AssemblyStackTraceException);

            Assert.assertTrue("" + ex.getCause(), ex.getCause() instanceof TestException);

            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("createSingle"));
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
            
            ts.assertError(AssemblyStackTraceException.class);
            
            Throwable ex = ts.getOnErrorEvents().get(0);

            Assert.assertTrue("" + ex, ex instanceof AssemblyStackTraceException);

            Assert.assertTrue("" + ex.getCause(), ex.getCause() instanceof TestException);

            Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("createCompletable"));
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
            Action1 a1 = Actions.empty();
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
                    
                    if (m.getParameterTypes()[0].isAssignableFrom(Func1.class)) {
                        m.invoke(null, f1);
                    } else
                    if (m.getParameterTypes()[0].isAssignableFrom(Action1.class)) {
                        m.invoke(null, a1);
                    } else {
                        m.invoke(null, f2);
                    }
                    
                    Object after = getter.invoke(null);
                    
                    Assert.assertSame(m.toString(), before, after);
                }
            }
            
        } finally {
            RxJavaHooks.lockdown = false;
            RxJavaHooks.reset();
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
            
            Assert.assertSame(Schedulers.immediate(), Schedulers.computation());
        } finally {
            RxJavaHooks.reset();
        }
        // make sure the reset worked
        Assert.assertNotSame(Schedulers.immediate(), Schedulers.computation());
    }

    @Test
    public void overrideIoScheduler() {
        try {
            RxJavaHooks.setOnIOScheduler(replaceWithImmediate);
            
            Assert.assertSame(Schedulers.immediate(), Schedulers.io());
        } finally {
            RxJavaHooks.reset();
        }
        // make sure the reset worked
        Assert.assertNotSame(Schedulers.immediate(), Schedulers.io());
    }

    @Test
    public void overrideNewThreadScheduler() {
        try {
            RxJavaHooks.setOnNewThreadScheduler(replaceWithImmediate);
            
            Assert.assertSame(Schedulers.immediate(), Schedulers.newThread());
        } finally {
            RxJavaHooks.reset();
        }
        // make sure the reset worked
        Assert.assertNotSame(Schedulers.immediate(), Schedulers.newThread());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void observableCreate() {
        try {
            RxJavaHooks.setOnObservableCreate(new Func1<OnSubscribe, OnSubscribe>() {
                @Override
                public OnSubscribe call(OnSubscribe t) {
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
            RxJavaHooks.setOnObservableStart(new Func2<Observable, OnSubscribe, OnSubscribe>() {
                @Override
                public OnSubscribe call(Observable o, OnSubscribe t) {
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
            
            Assert.assertSame(s, u);
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
                        public void call(SingleSubscriber<Object> t) {
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
            RxJavaHooks.setOnSingleStart(new Func2<Single, OnSubscribe, OnSubscribe>() {
                @Override
                public OnSubscribe call(Single o, OnSubscribe t) {
                    return new OnSubscribe<Object>() {
                        @Override
                        public void call(Subscriber<Object> t) {
                            t.setProducer(new SingleProducer<Integer>(t, 10));
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
            
            Assert.assertSame(s, u);
        } finally {
            RxJavaHooks.reset();
        }
    }

    @Test
    public void completableCreate() {
        try {
            RxJavaHooks.setOnCompletableCreate(new Func1<CompletableOnSubscribe, CompletableOnSubscribe>() {
                @Override
                public CompletableOnSubscribe call(CompletableOnSubscribe t) {
                    return new CompletableOnSubscribe() {
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
            RxJavaHooks.setOnCompletableStart(new Func2<Completable, CompletableOnSubscribe, CompletableOnSubscribe>() {
                @Override
                public CompletableOnSubscribe call(Completable o, CompletableOnSubscribe t) {
                    return new CompletableOnSubscribe() {
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

}
