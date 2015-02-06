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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rx.Observable;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;

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
        assertSame(RxJavaPlugins.DEFAULT_ERROR_HANDLER, impl);
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

    public static class RxJavaErrorHandlerTestImplWithRender extends RxJavaErrorHandler {
        @Override
        protected String render(Object item) {
            if (item instanceof Calendar) {
                throw new IllegalArgumentException("calendar");
            } else if (item instanceof Date) {
                return String.valueOf(((Date) item).getTime());
            }
            return null;
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

    @Test
    public void testOnNextValueRenderingWhenNotImplemented() {
        RxJavaErrorHandlerTestImpl errorHandler = new RxJavaErrorHandlerTestImpl();
        RxJavaPlugins.getInstance().registerErrorHandler(errorHandler);

        String rendering = RxJavaPlugins.getInstance().getErrorHandler().handleOnNextValueRendering(new Date());

        assertNull(rendering);
    }

    @Test
    public void testOnNextValueRenderingWhenImplementedAndNotManaged() {
        RxJavaErrorHandlerTestImplWithRender errorHandler = new RxJavaErrorHandlerTestImplWithRender();
        RxJavaPlugins.getInstance().registerErrorHandler(errorHandler);

        String rendering = RxJavaPlugins.getInstance().getErrorHandler().handleOnNextValueRendering(
                Collections.emptyList());

        assertNull(rendering);
    }

    @Test
    public void testOnNextValueRenderingWhenImplementedAndManaged() {
        RxJavaErrorHandlerTestImplWithRender errorHandler = new RxJavaErrorHandlerTestImplWithRender();
        RxJavaPlugins.getInstance().registerErrorHandler(errorHandler);
        long time = 1234L;
        Date date = new Date(time);

        String rendering = RxJavaPlugins.getInstance().getErrorHandler().handleOnNextValueRendering(date);

        assertNotNull(rendering);
        assertEquals(String.valueOf(time), rendering);
    }

    @Test
    public void testOnNextValueRenderingWhenImplementedAndThrows() {
        RxJavaErrorHandlerTestImplWithRender errorHandler = new RxJavaErrorHandlerTestImplWithRender();
        RxJavaPlugins.getInstance().registerErrorHandler(errorHandler);
        Calendar cal = Calendar.getInstance();

        String rendering = RxJavaPlugins.getInstance().getErrorHandler().handleOnNextValueRendering(cal);

        assertNotNull(rendering);
        assertEquals(cal.getClass().getName() + RxJavaErrorHandler.ERROR_IN_RENDERING_SUFFIX, rendering);
    }

    @Test
    public void testOnNextValueCallsPlugin() {
        RxJavaErrorHandlerTestImplWithRender errorHandler = new RxJavaErrorHandlerTestImplWithRender();
        RxJavaPlugins.getInstance().registerErrorHandler(errorHandler);
        long time = 456L;
        Date date = new Date(time);

        try {
            Date notExpected = Observable.just(date)
                                         .map(new Func1<Date, Date>() {
                                             @Override
                                             public Date call(Date date) {
                                                 throw new IllegalStateException("Trigger OnNextValue");
                                             }
                                         })
                                         .timeout(500, TimeUnit.MILLISECONDS)
                                         .toBlocking().first();
            fail("Did not expect onNext/onCompleted, got " + notExpected);
        } catch (IllegalStateException e) {
            assertEquals("Trigger OnNextValue", e.getMessage());
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof OnErrorThrowable.OnNextValue);
            assertEquals("OnError while emitting onNext value: " + time, e.getCause().getMessage());
        }

    }

    // inside test so it is stripped from Javadocs
    public static class RxJavaObservableExecutionHookTestImpl extends RxJavaObservableExecutionHook {
        // just use defaults
    }

    private static String getFullClassNameForTestClass(Class<?> cls) {
        return RxJavaPlugins.class.getPackage()
                                  .getName() + "." + RxJavaPluginsTest.class.getSimpleName() + "$" + cls.getSimpleName();
    }
}
