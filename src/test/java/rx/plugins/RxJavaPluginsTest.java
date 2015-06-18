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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.internal.util.ScalarSynchronousObservable.ScalarSynchronousOnSubscribe;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unchecked")
public class RxJavaPluginsTest {

    @Spy
    private RxJavaObservableExecutionHook execHook = new RxJavaObservableExecutionHookDefault();
    @Mock
    private RxJavaErrorHandler errHook;
    @Mock
    private RxJavaSchedulersHook schedHook;

    @Before
    public void resetBefore() {
        RxJavaPlugins.getInstance().reset();
        MockitoAnnotations.initMocks(this);
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
        p.registerErrorHandler(errHook);
        RxJavaErrorHandler impl = p.getErrorHandler();
        assertSame(errHook, impl);
    }

    @Test
    public void testErrorHandlerViaProperty() {
        try {
            RxJavaPlugins p = new RxJavaPlugins();
            String fullClass = errHook.getClass().getName();
            System.setProperty("rxjava.plugin.RxJavaErrorHandler.implementation", fullClass);
            RxJavaErrorHandler impl = p.getErrorHandler();
            assertTrue(errHook.getClass().isAssignableFrom(impl.getClass()));
        } finally {
            System.clearProperty("rxjava.plugin.RxJavaErrorHandler.implementation");
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
        p.registerObservableExecutionHook(execHook);
        RxJavaObservableExecutionHook impl = p.getObservableExecutionHook();
        assertSame(execHook, impl);
    }

    @Test
    public void testObservableExecutionHookViaProperty() {
        try {
            RxJavaPlugins p = new RxJavaPlugins();
            String fullClass = execHook.getClass().getName();
            System.setProperty("rxjava.plugin.RxJavaObservableExecutionHook.implementation", fullClass);
            RxJavaObservableExecutionHook impl = p.getObservableExecutionHook();
            assertTrue(execHook.getClass().isAssignableFrom(impl.getClass()));
        } finally {
            System.clearProperty("rxjava.plugin.RxJavaObservableExecutionHook.implementation");
        }
    }

    @Test
    public void testOnErrorWhenImplementedViaSubscribe() {
        RxJavaPlugins.getInstance().registerErrorHandler(errHook);

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
        verify(errHook).handleError(re);
        verifyNoMoreInteractions(errHook);
    }

    @Test
    public void testOnErrorWhenNotImplemented() {
        RxJavaPlugins.getInstance().registerErrorHandler(errHook);

        RuntimeException re = new RuntimeException("test onError");
        try {
            Observable.error(re).subscribe();
            fail("should fail");
        } catch (Throwable e) {
            // ignore as we expect it to throw
        }
        verify(errHook).handleError(re);
        verifyNoMoreInteractions(errHook);
    }

    @Test
    public void testOnNextValueRenderingWhenNotImplemented() {
        RxJavaPlugins.getInstance().registerErrorHandler(errHook);

        String rendering = RxJavaPlugins.getInstance().getErrorHandler().handleOnNextValueRendering(new Date());

        assertNull(rendering);
    }

    @Test
    public void testOnNextValueRenderingWhenImplementedAndNotManaged() {
        RxJavaPlugins.getInstance().registerErrorHandler(errHook);

        String rendering = RxJavaPlugins.getInstance().getErrorHandler().handleOnNextValueRendering(
                Collections.emptyList());

        assertNull(rendering);
    }

    @Test
    public void testOnNextValueRenderingWhenImplementedAndManaged() {
        RxJavaPlugins.getInstance().registerErrorHandler(errHook);
        long time = 1234L;
        Date date = new Date(time);
        String dateRender = String.valueOf(time);
        when(errHook.handleOnNextValueRendering(date)).thenReturn(dateRender);

        String rendering = RxJavaPlugins.getInstance().getErrorHandler().handleOnNextValueRendering(date);

        assertNotNull(rendering);
        assertSame(dateRender, rendering);
    }

    @Test
    public void testOnNextValueRenderingWhenImplementedAndThrows() {
        RxJavaPlugins.getInstance().registerErrorHandler(errHook);
        Calendar cal = Calendar.getInstance();
        when(errHook.handleOnNextValueRendering(any(Calendar.class))).thenThrow(new IllegalArgumentException());

        String rendering = RxJavaPlugins.getInstance().getErrorHandler().handleOnNextValueRendering(cal);

        assertNotNull(rendering);
        assertEquals(cal.getClass().getName() + RxJavaErrorHandler.ERROR_IN_RENDERING_SUFFIX, rendering);
    }

    @Test
    public void testOnNextValueCallsPlugin() {
        RxJavaPlugins.getInstance().registerErrorHandler(errHook);
        long time = 456L;
        Date date = new Date(time);
        String dateRender = String.valueOf(((Date) date).getTime());
        when(errHook.handleOnNextValueRendering(date)).thenReturn(dateRender);

        try {
            Date notExpected = Observable.just(date).map(new Func1<Date, Date>() {
                @Override
                public Date call(Date date) {
                    throw new IllegalStateException("Trigger OnNextValue");
                }
            }).timeout(500, TimeUnit.MILLISECONDS).toBlocking().first();
            fail("Did not expect onNext/onCompleted, got " + notExpected);
        } catch (IllegalStateException e) {
            assertEquals("Trigger OnNextValue", e.getMessage());
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof OnErrorThrowable.OnNextValue);
            assertEquals("OnError while emitting onNext value: " + time, e.getCause().getMessage());
        }

    }

    @Test
    public void testJustWithoutHook() {
        RxJavaPlugins.getInstance().registerObservableExecutionHook(execHook);
        InOrder order = inOrder(execHook);

        Observable<String> o = Observable.just("a");
        o.subscribe();

        order.verify(execHook).onCreate(any(ScalarSynchronousOnSubscribe.class));
        order.verify(execHook).onSubscribeStart(eq(o), any(ScalarSynchronousOnSubscribe.class));
        order.verify(execHook).onSubscribeReturn(any(Subscription.class));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testJustWithHook() {
        RxJavaPlugins.getInstance().registerObservableExecutionHook(execHook);
        InOrder order = inOrder(execHook);

        OnSubscribe onSub = new OnSubscribe() {
            @Override
            public void call(Object t) {
                Subscriber<String> subscriber = (Subscriber<String>) t;
                subscriber.onNext("b");
                subscriber.onCompleted();
            }
        };
        when(execHook.onCreate(any(ScalarSynchronousOnSubscribe.class))).thenReturn(onSub);

        Observable<String> o = Observable.just("a");
        o.subscribe();

        order.verify(execHook, times(2)).onCreate(any(OnSubscribe.class));
        order.verify(execHook).onSubscribeStart(o, onSub);
        order.verify(execHook).onSubscribeReturn(any(Subscription.class));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testMergeJust() {
        RxJavaPlugins.getInstance().registerObservableExecutionHook(execHook);
        InOrder order = inOrder(execHook);

        OnSubscribe onSub = new OnSubscribe() {
            @Override
            public void call(Object t) {
                Subscriber<String> subscriber = (Subscriber<String>) t;
                subscriber.onNext("b");
                subscriber.onCompleted();
            }
        };
        when(execHook.onCreate(any(ScalarSynchronousOnSubscribe.class))).thenReturn(onSub);
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Observable<String> o = Observable.merge(Observable.just(Observable.just("a")));
        o.subscribe(ts);

        ts.assertTerminalEvent();
        ts.assertValue("b");

        order.verify(execHook, times(5)).onCreate(any(OnSubscribe.class));
        order.verify(execHook).onSubscribeStart(o, onSub);
        order.verify(execHook).onSubscribeReturn(any(Subscription.class));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testSubscribeOnJust() {
        RxJavaPlugins.getInstance().registerObservableExecutionHook(execHook);
        InOrder order = inOrder(execHook);

        OnSubscribe onSub = new OnSubscribe() {
            @Override
            public void call(Object t) {
                Subscriber<String> subscriber = (Subscriber<String>) t;
                subscriber.onNext("b");
                subscriber.onCompleted();
            }
        };
        when(execHook.onCreate(any(ScalarSynchronousOnSubscribe.class))).thenReturn(onSub);
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Observable<String> o = Observable.just("a").subscribeOn(Schedulers.computation());
        o.subscribe(ts);

        ts.assertTerminalEvent();
        ts.assertValue("b");

        order.verify(execHook, times(5)).onCreate(any(OnSubscribe.class));
        order.verify(execHook).onSubscribeStart(o, onSub);
        order.verify(execHook).onSubscribeReturn(any(Subscription.class));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void testObserveOnJust() {
        RxJavaPlugins.getInstance().registerObservableExecutionHook(execHook);
        InOrder order = inOrder(execHook);

        OnSubscribe onSub = new OnSubscribe() {
            @Override
            public void call(Object t) {
                Subscriber<String> subscriber = (Subscriber<String>) t;
                subscriber.onNext("b");
                subscriber.onCompleted();
            }
        };
        when(execHook.onCreate(any(ScalarSynchronousOnSubscribe.class))).thenReturn(onSub);
        TestSubscriber<String> ts = new TestSubscriber<String>();

        Observable<String> o = Observable.just("a").observeOn(Schedulers.computation());
        o.subscribe(ts);

        ts.assertTerminalEvent();
        ts.assertValue("b");

        order.verify(execHook, times(3)).onCreate(any(OnSubscribe.class));
        order.verify(execHook).onSubscribeStart(o, onSub);
        order.verify(execHook).onSubscribeReturn(any(Subscription.class));
        order.verifyNoMoreInteractions();
    }

}
