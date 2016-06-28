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
import rx.exceptions.*;
import rx.functions.*;
import rx.internal.util.UtilityFunctions;
import rx.observers.TestSubscriber;

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
}
