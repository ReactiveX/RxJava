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
package rx.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.Subscriber;

public class RxJavaPluginsTest {

    @Before
    public void resetBefore() {
        RxJavaPlugins.getInstance().reset();
    }

    @After
    public void resetAfter() {
        RxJavaPlugins.getInstance().reset();
    }

    @Test
    public void testErrorHandlerDefaultImpl() {
        RxJavaErrorHandler impl = new RxJavaPlugins().getErrorHandler();
        assertTrue(impl instanceof RxJavaErrorHandlerDefault);
    }

    @Test
    public void testErrorHandlerViaRegisterMethod() {
        RxJavaPlugins p = new RxJavaPlugins();
        p.registerErrorHandler(new RxJavaErrorHandlerTestImpl());
        RxJavaErrorHandler impl = p.getErrorHandler();
        assertTrue(impl instanceof RxJavaErrorHandlerTestImpl);
    }

    @Test
    public void testErrorHandlerViaProperty() {
        try {
            RxJavaPlugins p = new RxJavaPlugins();
            String fullClass = getFullClassNameForTestClass(RxJavaErrorHandlerTestImpl.class);
            System.setProperty("rxjava.plugin.RxJavaErrorHandler.implementation", fullClass);
            RxJavaErrorHandler impl = p.getErrorHandler();
            assertTrue(impl instanceof RxJavaErrorHandlerTestImpl);
        } finally {
            System.clearProperty("rxjava.plugin.RxJavaErrorHandler.implementation");
        }
    }

    // inside test so it is stripped from Javadocs
    public static class RxJavaErrorHandlerTestImpl extends RxJavaErrorHandler {

        private volatile Throwable e;
        private volatile int count = 0;

        @Override
        public void handleError(Throwable e) {
            e.printStackTrace();
            this.e = e;
            count++;
        }

    }

    @Test
    public void testObservableExecutionHookDefaultImpl() {
        RxJavaPlugins p = new RxJavaPlugins();
        RxJavaObservableExecutionHook impl = p.getObservableExecutionHook();
        assertTrue(impl instanceof RxJavaObservableExecutionHookDefault);
    }

    @Test
    public void testObservableExecutionHookViaRegisterMethod() {
        RxJavaPlugins p = new RxJavaPlugins();
        p.registerObservableExecutionHook(new RxJavaObservableExecutionHookTestImpl());
        RxJavaObservableExecutionHook impl = p.getObservableExecutionHook();
        assertTrue(impl instanceof RxJavaObservableExecutionHookTestImpl);
    }

    @Test
    public void testObservableExecutionHookViaProperty() {
        try {
            RxJavaPlugins p = new RxJavaPlugins();
            String fullClass = getFullClassNameForTestClass(RxJavaObservableExecutionHookTestImpl.class);
            System.setProperty("rxjava.plugin.RxJavaObservableExecutionHook.implementation", fullClass);
            RxJavaObservableExecutionHook impl = p.getObservableExecutionHook();
            assertTrue(impl instanceof RxJavaObservableExecutionHookTestImpl);
        } finally {
            System.clearProperty("rxjava.plugin.RxJavaObservableExecutionHook.implementation");
        }
    }

    @Test
    public void testOnErrorWhenImplementedViaSubscribe() {
        RxJavaErrorHandlerTestImpl errorHandler = new RxJavaErrorHandlerTestImpl();
        RxJavaPlugins.getInstance().registerErrorHandler(errorHandler);

        RuntimeException re = new RuntimeException("test onError");
        Observable.error(re).subscribe(new Subscriber<Object>() {

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onNext(Object args) {
            }
        });
        assertEquals(re, errorHandler.e);
        assertEquals(1, errorHandler.count);
    }

    @Test
    public void testOnErrorWhenNotImplemented() {
        RxJavaErrorHandlerTestImpl errorHandler = new RxJavaErrorHandlerTestImpl();
        RxJavaPlugins.getInstance().registerErrorHandler(errorHandler);

        RuntimeException re = new RuntimeException("test onError");
        try {
            Observable.error(re).subscribe();
            fail("should fail");
        } catch (Throwable e) {
            // ignore as we expect it to throw
        }
        assertEquals(re, errorHandler.e);
        assertEquals(1, errorHandler.count);
    }

    // inside test so it is stripped from Javadocs
    public static class RxJavaObservableExecutionHookTestImpl extends RxJavaObservableExecutionHook {
        // just use defaults
    }

    private static String getFullClassNameForTestClass(Class<?> cls) {
        return RxJavaPlugins.class.getPackage().getName() + "." + RxJavaPluginsTest.class.getSimpleName() + "$" + cls.getSimpleName();
    }
}
