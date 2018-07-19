/**
 * Copyright (c) 2016-present, RxJava Contributors.
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

package io.reactivex;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.reactivestreams.*;

import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.internal.fuseable.*;
import io.reactivex.internal.operators.completable.CompletableToFlowable;
import io.reactivex.internal.operators.maybe.MaybeToFlowable;
import io.reactivex.internal.operators.single.SingleToFlowable;
import io.reactivex.internal.subscriptions.BooleanSubscription;
import io.reactivex.internal.util.ExceptionHelper;
import io.reactivex.observers.TestObserver;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.Subject;
import io.reactivex.subscribers.TestSubscriber;

/**
 * Common methods for helping with tests from 1.x mostly.
 */
public enum TestHelper {
    ;

    /**
     * Number of times to loop a {@link #race(Runnable, Runnable)} invocation
     * by default.
     */
    public static final int RACE_DEFAULT_LOOPS = 2500;

    /**
     * Number of times to loop a {@link #race(Runnable, Runnable)} invocation
     * in tests with race conditions requiring more runs to check.
     */
    public static final int RACE_LONG_LOOPS = 10000;

    /**
     * Mocks a subscriber and prepares it to request Long.MAX_VALUE.
     * @param <T> the value type
     * @return the mocked subscriber
     */
    @SuppressWarnings("unchecked")
    public static <T> FlowableSubscriber<T> mockSubscriber() {
        FlowableSubscriber<T> w = mock(FlowableSubscriber.class);

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock a) throws Throwable {
                Subscription s = a.getArgument(0);
                s.request(Long.MAX_VALUE);
                return null;
            }
        }).when(w).onSubscribe((Subscription)any());

        return w;
    }

    /**
     * Mocks an Observer with the proper receiver type.
     * @param <T> the value type
     * @return the mocked observer
     */
    @SuppressWarnings("unchecked")
    public static <T> Observer<T> mockObserver() {
        return mock(Observer.class);
    }

    /**
     * Mocks an MaybeObserver with the proper receiver type.
     * @param <T> the value type
     * @return the mocked observer
     */
    @SuppressWarnings("unchecked")
    public static <T> MaybeObserver<T> mockMaybeObserver() {
        return mock(MaybeObserver.class);
    }

    /**
     * Mocks an SingleObserver with the proper receiver type.
     * @param <T> the value type
     * @return the mocked observer
     */
    @SuppressWarnings("unchecked")
    public static <T> SingleObserver<T> mockSingleObserver() {
        return mock(SingleObserver.class);
    }

    /**
     * Mocks an CompletableObserver.
     * @return the mocked observer
     */
    public static CompletableObserver mockCompletableObserver() {
        return mock(CompletableObserver.class);
    }

    /**
     * Validates that the given class, when forcefully instantiated throws
     * an IllegalArgumentException("No instances!") exception.
     * @param clazz the class to test, not null
     */
    public static void checkUtilityClass(Class<?> clazz) {
        try {
            Constructor<?> c = clazz.getDeclaredConstructor();

            c.setAccessible(true);

            try {
                c.newInstance();
                fail("Should have thrown InvocationTargetException(IllegalStateException)");
            } catch (InvocationTargetException ex) {
                assertEquals("No instances!", ex.getCause().getMessage());
            }
        } catch (Exception ex) {
            AssertionError ae = new AssertionError(ex.toString());
            ae.initCause(ex);
            throw ae;
        }
    }

    public static List<Throwable> trackPluginErrors() {
        final List<Throwable> list = Collections.synchronizedList(new ArrayList<Throwable>());

        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable t) {
                list.add(t);
            }
        });

        return list;
    }

    public static void assertError(List<Throwable> list, int index, Class<? extends Throwable> clazz) {
        Throwable ex = list.get(index);
        if (!clazz.isInstance(ex)) {
            AssertionError err = new AssertionError(clazz + " expected but got " + list.get(index));
            err.initCause(list.get(index));
            throw err;
        }
    }

    public static void assertUndeliverable(List<Throwable> list, int index, Class<? extends Throwable> clazz) {
        Throwable ex = list.get(index);
        if (!(ex instanceof UndeliverableException)) {
            AssertionError err = new AssertionError("Outer exception UndeliverableException expected but got " + list.get(index));
            err.initCause(list.get(index));
            throw err;
        }
        ex = ex.getCause();
        if (!clazz.isInstance(ex)) {
            AssertionError err = new AssertionError("Inner exception " + clazz + " expected but got " + list.get(index));
            err.initCause(list.get(index));
            throw err;
        }
    }

    public static void assertError(List<Throwable> list, int index, Class<? extends Throwable> clazz, String message) {
        Throwable ex = list.get(index);
        if (!clazz.isInstance(ex)) {
            AssertionError err = new AssertionError("Type " + clazz + " expected but got " + ex);
            err.initCause(ex);
            throw err;
        }
        if (!ObjectHelper.equals(message, ex.getMessage())) {
            AssertionError err = new AssertionError("Message " + message + " expected but got " + ex.getMessage());
            err.initCause(ex);
            throw err;
        }
    }

    public static void assertUndeliverable(List<Throwable> list, int index, Class<? extends Throwable> clazz, String message) {
        Throwable ex = list.get(index);
        if (!(ex instanceof UndeliverableException)) {
            AssertionError err = new AssertionError("Outer exception UndeliverableException expected but got " + list.get(index));
            err.initCause(list.get(index));
            throw err;
        }
        ex = ex.getCause();
        if (!clazz.isInstance(ex)) {
            AssertionError err = new AssertionError("Inner exception " + clazz + " expected but got " + list.get(index));
            err.initCause(list.get(index));
            throw err;
        }
        if (!ObjectHelper.equals(message, ex.getMessage())) {
            AssertionError err = new AssertionError("Message " + message + " expected but got " + ex.getMessage());
            err.initCause(ex);
            throw err;
        }
    }

    public static void assertError(TestObserver<?> to, int index, Class<? extends Throwable> clazz) {
        Throwable ex = to.errors().get(0);
        try {
            if (ex instanceof CompositeException) {
                CompositeException ce = (CompositeException) ex;
                List<Throwable> cel = ce.getExceptions();
                assertTrue(cel.get(index).toString(), clazz.isInstance(cel.get(index)));
            } else {
                fail(ex.toString() + ": not a CompositeException");
            }
        } catch (AssertionError e) {
            ex.printStackTrace();
            throw e;
        }
    }

    public static void assertError(TestSubscriber<?> ts, int index, Class<? extends Throwable> clazz) {
        Throwable ex = ts.errors().get(0);
        if (ex instanceof CompositeException) {
            CompositeException ce = (CompositeException) ex;
            List<Throwable> cel = ce.getExceptions();
            assertTrue(cel.get(index).toString(), clazz.isInstance(cel.get(index)));
        } else {
            fail(ex.toString() + ": not a CompositeException");
        }
    }

    public static void assertError(TestObserver<?> to, int index, Class<? extends Throwable> clazz, String message) {
        Throwable ex = to.errors().get(0);
        if (ex instanceof CompositeException) {
            CompositeException ce = (CompositeException) ex;
            List<Throwable> cel = ce.getExceptions();
            assertTrue(cel.get(index).toString(), clazz.isInstance(cel.get(index)));
            assertEquals(message, cel.get(index).getMessage());
        } else {
            fail(ex.toString() + ": not a CompositeException");
        }
    }

    public static void assertError(TestSubscriber<?> ts, int index, Class<? extends Throwable> clazz, String message) {
        Throwable ex = ts.errors().get(0);
        if (ex instanceof CompositeException) {
            CompositeException ce = (CompositeException) ex;
            List<Throwable> cel = ce.getExceptions();
            assertTrue(cel.get(index).toString(), clazz.isInstance(cel.get(index)));
            assertEquals(message, cel.get(index).getMessage());
        } else {
            fail(ex.toString() + ": not a CompositeException");
        }
    }

    /**
     * Verify that a specific enum type has no enum constants.
     * @param <E> the enum type
     * @param e the enum class instance
     */
    public static <E extends Enum<E>> void assertEmptyEnum(Class<E> e) {
        assertEquals(0, e.getEnumConstants().length);

        try {
            try {
                Method m0 = e.getDeclaredMethod("values");

                Object[] a = (Object[])m0.invoke(null);
                assertEquals(0, a.length);

                Method m = e.getDeclaredMethod("valueOf", String.class);

                m.invoke("INSTANCE");
                fail("Should have thrown!");
            } catch (InvocationTargetException ex) {
                fail(ex.toString());
            } catch (IllegalAccessException ex) {
                fail(ex.toString());
            } catch (IllegalArgumentException ex) {
                // we expected this
            }
        } catch (NoSuchMethodException ex) {
            fail(ex.toString());
        }
    }

    /**
     * Assert that by consuming the Publisher with a bad request amount, it is
     * reported to the plugin error handler promptly.
     * @param source the source to consume
     */
    public static void assertBadRequestReported(Publisher<?> source) {
        List<Throwable> list = trackPluginErrors();
        try {
            final CountDownLatch cdl = new CountDownLatch(1);

            source.subscribe(new FlowableSubscriber<Object>() {

                @Override
                public void onSubscribe(Subscription s) {
                    try {
                        s.request(-99);
                        s.cancel();
                        s.cancel();
                    } finally {
                        cdl.countDown();
                    }
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

            });

            try {
                assertTrue(cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw new AssertionError(ex.getMessage());
            }

            assertTrue(list.toString(), list.get(0) instanceof IllegalArgumentException);
            assertEquals("n > 0 required but it was -99", list.get(0).getMessage());
        } finally {
            RxJavaPlugins.setErrorHandler(null);
        }
    }
    /**
     * Synchronizes the execution of two runnables (as much as possible)
     * to test race conditions.
     * <p>The method blocks until both have run to completion.
     * @param r1 the first runnable
     * @param r2 the second runnable
     * @see #RACE_DEFAULT_LOOPS
     * @see #RACE_LONG_LOOPS
     */
    public static void race(final Runnable r1, final Runnable r2) {
        race(r1, r2, Schedulers.single());
    }
    /**
     * Synchronizes the execution of two runnables (as much as possible)
     * to test race conditions.
     * <p>The method blocks until both have run to completion.
     * @param r1 the first runnable
     * @param r2 the second runnable
     * @param s the scheduler to use
     * @see #RACE_DEFAULT_LOOPS
     * @see #RACE_LONG_LOOPS
     */
    public static void race(final Runnable r1, final Runnable r2, Scheduler s) {
        final AtomicInteger count = new AtomicInteger(2);
        final CountDownLatch cdl = new CountDownLatch(2);

        final Throwable[] errors = { null, null };

        s.scheduleDirect(new Runnable() {
            @Override
            public void run() {
                if (count.decrementAndGet() != 0) {
                    while (count.get() != 0) { }
                }

                try {
                    try {
                        r1.run();
                    } catch (Throwable ex) {
                        errors[0] = ex;
                    }
                } finally {
                    cdl.countDown();
                }
            }
        });

        if (count.decrementAndGet() != 0) {
            while (count.get() != 0) { }
        }

        try {
            try {
                r2.run();
            } catch (Throwable ex) {
                errors[1] = ex;
            }
        } finally {
            cdl.countDown();
        }

        try {
            if (!cdl.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("The wait timed out!");
            }
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        if (errors[0] != null && errors[1] == null) {
            throw ExceptionHelper.wrapOrThrow(errors[0]);
        }

        if (errors[0] == null && errors[1] != null) {
            throw ExceptionHelper.wrapOrThrow(errors[1]);
        }

        if (errors[0] != null && errors[1] != null) {
            throw new CompositeException(errors);
        }
    }

    /**
     * Cast the given Throwable to CompositeException and returns its inner
     * Throwable list.
     * @param ex the target Throwable
     * @return the list of Throwables
     */
    public static List<Throwable> compositeList(Throwable ex) {
        if (ex instanceof UndeliverableException) {
            ex = ex.getCause();
        }
        return ((CompositeException)ex).getExceptions();
    }

    /**
     * Assert that the offer methods throw UnsupportedOperationExcetpion.
     * @param q the queue implementation
     */
    public static void assertNoOffer(SimpleQueue<?> q) {
        try {
            q.offer(null);
            fail("Should have thrown!");
        } catch (UnsupportedOperationException ex) {
            // expected
        }
        try {
            q.offer(null, null);
            fail("Should have thrown!");
        } catch (UnsupportedOperationException ex) {
            // expected
        }
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> void checkEnum(Class<E> enumClass) {
        try {
            Method m = enumClass.getMethod("values");
            m.setAccessible(true);
            Method e = enumClass.getMethod("valueOf", String.class);
            m.setAccessible(true);

            for (Enum<E> o : (Enum<E>[])m.invoke(null)) {
                assertSame(o, e.invoke(null, o.name()));
            }

        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }
    }

    /**
     * Returns an Consumer that asserts the TestSubscriber has exactly one value + completed
     * normally and that single value is not the value specified.
     * @param <T> the value type
     * @param value the value not expected
     * @return the consumer
     */
    public static <T> Consumer<TestSubscriber<T>> subscriberSingleNot(final T value) {
        return new Consumer<TestSubscriber<T>>() {
            @Override
            public void accept(TestSubscriber<T> ts) throws Exception {
                ts
                .assertSubscribed()
                .assertValueCount(1)
                .assertNoErrors()
                .assertComplete();

                T v = ts.values().get(0);
                assertNotEquals(value, v);
            }
        };
    }

    /**
     * Returns an Consumer that asserts the TestObserver has exactly one value + completed
     * normally and that single value is not the value specified.
     * @param <T> the value type
     * @param value the value not expected
     * @return the consumer
     */
    public static <T> Consumer<TestObserver<T>> observerSingleNot(final T value) {
        return new Consumer<TestObserver<T>>() {
            @Override
            public void accept(TestObserver<T> to) throws Exception {
                to
                .assertSubscribed()
                .assertValueCount(1)
                .assertNoErrors()
                .assertComplete();

                T v = to.values().get(0);
                assertNotEquals(value, v);
            }
        };
    }

    /**
     * Calls onSubscribe twice and checks if it doesn't affect the first Subscription while
     * reporting it to plugin error handler.
     * @param subscriber the target
     */
    public static void doubleOnSubscribe(Subscriber<?> subscriber) {
        List<Throwable> errors = trackPluginErrors();
        try {
            BooleanSubscription s1 = new BooleanSubscription();

            subscriber.onSubscribe(s1);

            BooleanSubscription s2 = new BooleanSubscription();

            subscriber.onSubscribe(s2);

            assertFalse(s1.isCancelled());

            assertTrue(s2.isCancelled());

            assertError(errors, 0, IllegalStateException.class, "Subscription already set!");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Calls onSubscribe twice and checks if it doesn't affect the first Disposable while
     * reporting it to plugin error handler.
     * @param subscriber the target
     */
    public static void doubleOnSubscribe(Observer<?> subscriber) {
        List<Throwable> errors = trackPluginErrors();
        try {
            Disposable d1 = Disposables.empty();

            subscriber.onSubscribe(d1);

            Disposable d2 = Disposables.empty();

            subscriber.onSubscribe(d2);

            assertFalse(d1.isDisposed());

            assertTrue(d2.isDisposed());

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Calls onSubscribe twice and checks if it doesn't affect the first Disposable while
     * reporting it to plugin error handler.
     * @param subscriber the target
     */
    public static void doubleOnSubscribe(SingleObserver<?> subscriber) {
        List<Throwable> errors = trackPluginErrors();
        try {
            Disposable d1 = Disposables.empty();

            subscriber.onSubscribe(d1);

            Disposable d2 = Disposables.empty();

            subscriber.onSubscribe(d2);

            assertFalse(d1.isDisposed());

            assertTrue(d2.isDisposed());

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Calls onSubscribe twice and checks if it doesn't affect the first Disposable while
     * reporting it to plugin error handler.
     * @param subscriber the target
     */
    public static void doubleOnSubscribe(CompletableObserver subscriber) {
        List<Throwable> errors = trackPluginErrors();
        try {
            Disposable d1 = Disposables.empty();

            subscriber.onSubscribe(d1);

            Disposable d2 = Disposables.empty();

            subscriber.onSubscribe(d2);

            assertFalse(d1.isDisposed());

            assertTrue(d2.isDisposed());

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Calls onSubscribe twice and checks if it doesn't affect the first Disposable while
     * reporting it to plugin error handler.
     * @param subscriber the target
     */
    public static void doubleOnSubscribe(MaybeObserver<?> subscriber) {
        List<Throwable> errors = trackPluginErrors();
        try {
            Disposable d1 = Disposables.empty();

            subscriber.onSubscribe(d1);

            Disposable d2 = Disposables.empty();

            subscriber.onSubscribe(d2);

            assertFalse(d1.isDisposed());

            assertTrue(d2.isDisposed());

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Checks if the upstream's Subscription sent through the onSubscribe reports
     * isCancelled properly before and after calling dispose.
     * @param <T> the input value type
     * @param source the source to test
     */
    public static <T> void checkDisposed(Flowable<T> source) {
        final TestSubscriber<Object> ts = new TestSubscriber<Object>(0L);
        source.subscribe(new FlowableSubscriber<Object>() {
            @Override
            public void onSubscribe(Subscription s) {
                ts.onSubscribe(new BooleanSubscription());

                s.cancel();

                s.cancel();
            }

            @Override
            public void onNext(Object t) {
                ts.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                ts.onError(t);
            }

            @Override
            public void onComplete() {
                ts.onComplete();
            }
        });
        ts.assertEmpty();
    }
    /**
     * Checks if the upstream's Disposable sent through the onSubscribe reports
     * isDisposed properly before and after calling dispose.
     * @param source the source to test
     */
    public static void checkDisposed(Maybe<?> source) {
        final Boolean[] b = { null, null };
        final CountDownLatch cdl = new CountDownLatch(1);
        source.subscribe(new MaybeObserver<Object>() {

            @Override
            public void onSubscribe(Disposable d) {
                try {
                    b[0] = d.isDisposed();

                    d.dispose();

                    b[1] = d.isDisposed();

                    d.dispose();
                } finally {
                    cdl.countDown();
                }
            }

            @Override
            public void onSuccess(Object value) {
                // ignored
            }

            @Override
            public void onError(Throwable e) {
                // ignored
            }

            @Override
            public void onComplete() {
                // ignored
            }
        });

        try {
            assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }

        assertEquals("Reports disposed upfront?", false, b[0]);
        assertEquals("Didn't report disposed after?", true, b[1]);
    }

    /**
     * Checks if the upstream's Disposable sent through the onSubscribe reports
     * isDisposed properly before and after calling dispose.
     * @param source the source to test
     */
    public static void checkDisposed(Observable<?> source) {
        final Boolean[] b = { null, null };
        final CountDownLatch cdl = new CountDownLatch(1);
        source.subscribe(new Observer<Object>() {

            @Override
            public void onSubscribe(Disposable d) {
                try {
                    b[0] = d.isDisposed();

                    d.dispose();

                    b[1] = d.isDisposed();

                    d.dispose();
                } finally {
                    cdl.countDown();
                }
            }

            @Override
            public void onNext(Object value) {
                // ignored
            }

            @Override
            public void onError(Throwable e) {
                // ignored
            }

            @Override
            public void onComplete() {
                // ignored
            }
        });

        try {
            assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }

        assertEquals("Reports disposed upfront?", false, b[0]);
        assertEquals("Didn't report disposed after?", true, b[1]);
    }

    /**
     * Checks if the upstream's Disposable sent through the onSubscribe reports
     * isDisposed properly before and after calling dispose.
     * @param source the source to test
     */
    public static void checkDisposed(Single<?> source) {
        final Boolean[] b = { null, null };
        final CountDownLatch cdl = new CountDownLatch(1);
        source.subscribe(new SingleObserver<Object>() {

            @Override
            public void onSubscribe(Disposable d) {
                try {
                    b[0] = d.isDisposed();

                    d.dispose();

                    b[1] = d.isDisposed();

                    d.dispose();
                } finally {
                    cdl.countDown();
                }
            }

            @Override
            public void onSuccess(Object value) {
                // ignored
            }

            @Override
            public void onError(Throwable e) {
                // ignored
            }
        });

        try {
            assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }

        assertEquals("Reports disposed upfront?", false, b[0]);
        assertEquals("Didn't report disposed after?", true, b[1]);
    }

    /**
     * Checks if the upstream's Disposable sent through the onSubscribe reports
     * isDisposed properly before and after calling dispose.
     * @param source the source to test
     */
    public static void checkDisposed(Completable source) {
        final Boolean[] b = { null, null };
        final CountDownLatch cdl = new CountDownLatch(1);
        source.subscribe(new CompletableObserver() {

            @Override
            public void onSubscribe(Disposable d) {
                try {
                    b[0] = d.isDisposed();

                    d.dispose();

                    b[1] = d.isDisposed();

                    d.dispose();
                } finally {
                    cdl.countDown();
                }
            }

            @Override
            public void onError(Throwable e) {
                // ignored
            }

            @Override
            public void onComplete() {
                // ignored
            }
        });

        try {
            assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }

        assertEquals("Reports disposed upfront?", false, b[0]);
        assertEquals("Didn't report disposed after?", true, b[1]);
    }

    /**
     * Consumer for all base reactive types.
     */
    enum NoOpConsumer implements FlowableSubscriber<Object>, Observer<Object>, MaybeObserver<Object>, SingleObserver<Object>, CompletableObserver {
        INSTANCE;

        @Override
        public void onSubscribe(Disposable d) {
            // deliberately no-op
        }

        @Override
        public void onSuccess(Object value) {
            // deliberately no-op
        }

        @Override
        public void onError(Throwable e) {
            // deliberately no-op
        }

        @Override
        public void onComplete() {
            // deliberately no-op
        }

        @Override
        public void onSubscribe(Subscription s) {
            // deliberately no-op
        }

        @Override
        public void onNext(Object t) {
            // deliberately no-op
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeMaybe(Function<Maybe<T>, ? extends MaybeSource<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Maybe<T> source = new Maybe<T>() {
                @Override
                protected void subscribeActual(MaybeObserver<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            MaybeSource<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeMaybeToSingle(Function<Maybe<T>, ? extends SingleSource<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Maybe<T> source = new Maybe<T>() {
                @Override
                protected void subscribeActual(MaybeObserver<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            SingleSource<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeMaybeToObservable(Function<Maybe<T>, ? extends ObservableSource<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Maybe<T> source = new Maybe<T>() {
                @Override
                protected void subscribeActual(MaybeObserver<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            ObservableSource<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeMaybeToFlowable(Function<Maybe<T>, ? extends Publisher<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Maybe<T> source = new Maybe<T>() {
                @Override
                protected void subscribeActual(MaybeObserver<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            Publisher<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeSingleToMaybe(Function<Single<T>, ? extends MaybeSource<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Single<T> source = new Single<T>() {
                @Override
                protected void subscribeActual(SingleObserver<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            MaybeSource<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeSingleToObservable(Function<Single<T>, ? extends ObservableSource<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Single<T> source = new Single<T>() {
                @Override
                protected void subscribeActual(SingleObserver<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            ObservableSource<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeSingleToFlowable(Function<Single<T>, ? extends Publisher<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Single<T> source = new Single<T>() {
                @Override
                protected void subscribeActual(SingleObserver<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            Publisher<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param transform the transform to drive an operator
     */
    public static <T> void checkDoubleOnSubscribeMaybeToCompletable(Function<Maybe<T>, ? extends CompletableSource> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Maybe<T> source = new Maybe<T>() {
                @Override
                protected void subscribeActual(MaybeObserver<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            CompletableSource out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeSingle(Function<Single<T>, ? extends SingleSource<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Single<T> source = new Single<T>() {
                @Override
                protected void subscribeActual(SingleObserver<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            SingleSource<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeFlowable(Function<Flowable<T>, ? extends Publisher<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Flowable<T> source = new Flowable<T>() {
                @Override
                protected void subscribeActual(Subscriber<? super T> subscriber) {
                    try {
                        BooleanSubscription d1 = new BooleanSubscription();

                        subscriber.onSubscribe(d1);

                        BooleanSubscription d2 = new BooleanSubscription();

                        subscriber.onSubscribe(d2);

                        b[0] = d1.isCancelled();
                        b[1] = d2.isCancelled();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            Publisher<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Subscription already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeObservable(Function<Observable<T>, ? extends ObservableSource<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Observable<T> source = new Observable<T>() {
                @Override
                protected void subscribeActual(Observer<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            ObservableSource<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeObservableToSingle(Function<Observable<T>, ? extends SingleSource<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Observable<T> source = new Observable<T>() {
                @Override
                protected void subscribeActual(Observer<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            SingleSource<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeObservableToMaybe(Function<Observable<T>, ? extends MaybeSource<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Observable<T> source = new Observable<T>() {
                @Override
                protected void subscribeActual(Observer<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            MaybeSource<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param transform the transform to drive an operator
     */
    public static <T> void checkDoubleOnSubscribeObservableToCompletable(Function<Observable<T>, ? extends CompletableSource> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Observable<T> source = new Observable<T>() {
                @Override
                protected void subscribeActual(Observer<? super T> observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            CompletableSource out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeFlowableToObservable(Function<Flowable<T>, ? extends ObservableSource<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Flowable<T> source = new Flowable<T>() {
                @Override
                protected void subscribeActual(Subscriber<? super T> observer) {
                    try {
                        BooleanSubscription d1 = new BooleanSubscription();

                        observer.onSubscribe(d1);

                        BooleanSubscription d2 = new BooleanSubscription();

                        observer.onSubscribe(d2);

                        b[0] = d1.isCancelled();
                        b[1] = d2.isCancelled();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            ObservableSource<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First cancelled?", false, b[0]);
            assertEquals("Second not cancelled?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Subscription already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeFlowableToSingle(Function<Flowable<T>, ? extends SingleSource<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Flowable<T> source = new Flowable<T>() {
                @Override
                protected void subscribeActual(Subscriber<? super T> observer) {
                    try {
                        BooleanSubscription d1 = new BooleanSubscription();

                        observer.onSubscribe(d1);

                        BooleanSubscription d2 = new BooleanSubscription();

                        observer.onSubscribe(d2);

                        b[0] = d1.isCancelled();
                        b[1] = d2.isCancelled();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            SingleSource<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First cancelled?", false, b[0]);
            assertEquals("Second not cancelled?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Subscription already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param <R> the output value type
     * @param transform the transform to drive an operator
     */
    public static <T, R> void checkDoubleOnSubscribeFlowableToMaybe(Function<Flowable<T>, ? extends MaybeSource<R>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Flowable<T> source = new Flowable<T>() {
                @Override
                protected void subscribeActual(Subscriber<? super T> observer) {
                    try {
                        BooleanSubscription d1 = new BooleanSubscription();

                        observer.onSubscribe(d1);

                        BooleanSubscription d2 = new BooleanSubscription();

                        observer.onSubscribe(d2);

                        b[0] = d1.isCancelled();
                        b[1] = d2.isCancelled();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            MaybeSource<R> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First cancelled?", false, b[0]);
            assertEquals("Second not cancelled?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Subscription already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the input value type
     * @param transform the transform to drive an operator
     */
    public static <T> void checkDoubleOnSubscribeFlowableToCompletable(Function<Flowable<T>, ? extends Completable> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Flowable<T> source = new Flowable<T>() {
                @Override
                protected void subscribeActual(Subscriber<? super T> observer) {
                    try {
                        BooleanSubscription d1 = new BooleanSubscription();

                        observer.onSubscribe(d1);

                        BooleanSubscription d2 = new BooleanSubscription();

                        observer.onSubscribe(d2);

                        b[0] = d1.isCancelled();
                        b[1] = d2.isCancelled();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            Completable out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First cancelled?", false, b[0]);
            assertEquals("Second not cancelled?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Subscription already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param transform the transform to drive an operator
     */
    public static void checkDoubleOnSubscribeCompletable(Function<Completable, ? extends CompletableSource> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Completable source = new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            CompletableSource out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the output value tye
     * @param transform the transform to drive an operator
     */
    public static <T> void checkDoubleOnSubscribeCompletableToMaybe(Function<Completable, ? extends MaybeSource<T>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Completable source = new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            MaybeSource<T> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param <T> the output value tye
     * @param transform the transform to drive an operator
     */
    public static <T> void checkDoubleOnSubscribeCompletableToSingle(Function<Completable, ? extends SingleSource<T>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Completable source = new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            SingleSource<T> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param transform the transform to drive an operator
     */
    public static void checkDoubleOnSubscribeCompletableToFlowable(Function<Completable, ? extends Publisher<?>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Completable source = new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            Publisher<?> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the given transformed reactive type reports multiple onSubscribe calls to
     * RxJavaPlugins.
     * @param transform the transform to drive an operator
     */
    public static void checkDoubleOnSubscribeCompletableToObservable(Function<Completable, ? extends ObservableSource<?>> transform) {
        List<Throwable> errors = trackPluginErrors();
        try {
            final Boolean[] b = { null, null };
            final CountDownLatch cdl = new CountDownLatch(1);

            Completable source = new Completable() {
                @Override
                protected void subscribeActual(CompletableObserver observer) {
                    try {
                        Disposable d1 = Disposables.empty();

                        observer.onSubscribe(d1);

                        Disposable d2 = Disposables.empty();

                        observer.onSubscribe(d2);

                        b[0] = d1.isDisposed();
                        b[1] = d2.isDisposed();
                    } finally {
                        cdl.countDown();
                    }
                }
            };

            ObservableSource<?> out = transform.apply(source);

            out.subscribe(NoOpConsumer.INSTANCE);

            try {
                assertTrue("Timed out", cdl.await(5, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                throw ExceptionHelper.wrapOrThrow(ex);
            }

            assertEquals("First disposed?", false, b[0]);
            assertEquals("Second not disposed?", true, b[1]);

            assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Check if the operator applied to a Maybe source propagates dispose properly.
     * @param <T> the source value type
     * @param <U> the output value type
     * @param composer the function to apply an operator to the provided Maybe source
     */
    public static <T, U> void checkDisposedMaybe(Function<Maybe<T>, ? extends MaybeSource<U>> composer) {
        PublishProcessor<T> pp = PublishProcessor.create();

        TestSubscriber<U> ts = new TestSubscriber<U>();

        try {
            new MaybeToFlowable<U>(composer.apply(pp.singleElement())).subscribe(ts);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }

        assertTrue("Not subscribed to source!", pp.hasSubscribers());

        ts.cancel();

        assertFalse("Dispose not propagated!", pp.hasSubscribers());
    }

    /**
     * Check if the operator applied to a Completable source propagates dispose properly.
     * @param composer the function to apply an operator to the provided Completable source
     */
    public static void checkDisposedCompletable(Function<Completable, ? extends CompletableSource> composer) {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        try {
            new CompletableToFlowable<Integer>(composer.apply(pp.ignoreElements())).subscribe(ts);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }

        assertTrue("Not subscribed to source!", pp.hasSubscribers());

        ts.cancel();

        assertFalse("Dispose not propagated!", pp.hasSubscribers());
    }

    /**
     * Check if the operator applied to a Maybe source propagates dispose properly.
     * @param <T> the source value type
     * @param <U> the output value type
     * @param composer the function to apply an operator to the provided Maybe source
     */
    public static <T, U> void checkDisposedMaybeToSingle(Function<Maybe<T>, ? extends SingleSource<U>> composer) {
        PublishProcessor<T> pp = PublishProcessor.create();

        TestSubscriber<U> ts = new TestSubscriber<U>();

        try {
            new SingleToFlowable<U>(composer.apply(pp.singleElement())).subscribe(ts);
        } catch (Throwable ex) {
            throw ExceptionHelper.wrapOrThrow(ex);
        }

        assertTrue(pp.hasSubscribers());

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }

    /**
     * Check if the TestSubscriber has a CompositeException with the specified class
     * of Throwables in the given order.
     * @param ts the TestSubscriber instance
     * @param classes the array of expected Throwables inside the Composite
     */
    public static void assertCompositeExceptions(TestSubscriber<?> ts, Class<? extends Throwable>... classes) {
        ts
        .assertSubscribed()
        .assertError(CompositeException.class)
        .assertNotComplete();

        List<Throwable> list = compositeList(ts.errors().get(0));

        assertEquals(classes.length, list.size());

        for (int i = 0; i < classes.length; i++) {
            assertError(list, i, classes[i]);
        }
    }

    /**
     * Check if the TestSubscriber has a CompositeException with the specified class
     * of Throwables in the given order.
     * @param ts the TestSubscriber instance
     * @param classes the array of subsequent Class and String instances representing the
     * expected Throwable class and the expected error message
     */
    @SuppressWarnings("unchecked")
    public static void assertCompositeExceptions(TestSubscriber<?> ts, Object... classes) {
        ts
        .assertSubscribed()
        .assertError(CompositeException.class)
        .assertNotComplete();

        List<Throwable> list = compositeList(ts.errors().get(0));

        assertEquals(classes.length, list.size());

        for (int i = 0; i < classes.length; i += 2) {
            assertError(list, i, (Class<Throwable>)classes[i], (String)classes[i + 1]);
        }
    }

    /**
     * Check if the TestSubscriber has a CompositeException with the specified class
     * of Throwables in the given order.
     * @param to the TestSubscriber instance
     * @param classes the array of expected Throwables inside the Composite
     */
    public static void assertCompositeExceptions(TestObserver<?> to, Class<? extends Throwable>... classes) {
        to
        .assertSubscribed()
        .assertError(CompositeException.class)
        .assertNotComplete();

        List<Throwable> list = compositeList(to.errors().get(0));

        assertEquals(classes.length, list.size());

        for (int i = 0; i < classes.length; i++) {
            assertError(list, i, classes[i]);
        }
    }

    /**
     * Check if the TestSubscriber has a CompositeException with the specified class
     * of Throwables in the given order.
     * @param to the TestSubscriber instance
     * @param classes the array of subsequent Class and String instances representing the
     * expected Throwable class and the expected error message
     */
    @SuppressWarnings("unchecked")
    public static void assertCompositeExceptions(TestObserver<?> to, Object... classes) {
        to
        .assertSubscribed()
        .assertError(CompositeException.class)
        .assertNotComplete();

        List<Throwable> list = compositeList(to.errors().get(0));

        assertEquals(classes.length, list.size());

        for (int i = 0; i < classes.length; i += 2) {
            assertError(list, i, (Class<Throwable>)classes[i], (String)classes[i + 1]);
        }
    }

    /**
     * Emit the given values and complete the Processor.
     * @param <T> the value type
     * @param p the target processor
     * @param values the values to emit
     */
    public static <T> void emit(Processor<T, ?> p, T... values) {
        for (T v : values) {
            p.onNext(v);
        }
        p.onComplete();
    }

    /**
     * Emit the given values and complete the Subject.
     * @param <T> the value type
     * @param p the target subject
     * @param values the values to emit
     */
    public static <T> void emit(Subject<T> p, T... values) {
        for (T v : values) {
            p.onNext(v);
        }
        p.onComplete();
    }

    /**
     * Checks if the source is fuseable and its isEmpty/clear works properly.
     * @param <T> the value type
     * @param source the source sequence
     */
    public static <T> void checkFusedIsEmptyClear(Observable<T> source) {
        final CountDownLatch cdl = new CountDownLatch(1);

        final Boolean[] state = { null, null, null, null };

        source.subscribe(new Observer<T>() {
            @Override
            public void onSubscribe(Disposable d) {
                try {
                    if (d instanceof QueueDisposable) {
                        @SuppressWarnings("unchecked")
                        QueueDisposable<Object> qd = (QueueDisposable<Object>) d;
                        state[0] = true;

                        int m = qd.requestFusion(QueueFuseable.ANY);

                        if (m != QueueFuseable.NONE) {
                            state[1] = true;

                            state[2] = qd.isEmpty();

                            qd.clear();

                            state[3] = qd.isEmpty();
                        }
                    }
                    cdl.countDown();
                } finally {
                    d.dispose();
                }
            }

            @Override
            public void onNext(T value) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });

        try {
            assertTrue(cdl.await(5, TimeUnit.SECONDS));

            assertTrue("Not fuseable", state[0]);
            assertTrue("Fusion rejected", state[1]);

            assertNotNull(state[2]);
            assertTrue("Did not empty", state[3]);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Checks if the source is fuseable and its isEmpty/clear works properly.
     * @param <T> the value type
     * @param source the source sequence
     */
    public static <T> void checkFusedIsEmptyClear(Flowable<T> source) {
        final CountDownLatch cdl = new CountDownLatch(1);

        final Boolean[] state = { null, null, null, null };

        source.subscribe(new FlowableSubscriber<T>() {
            @Override
            public void onSubscribe(Subscription d) {
                try {
                    if (d instanceof QueueSubscription) {
                        @SuppressWarnings("unchecked")
                        QueueSubscription<Object> qd = (QueueSubscription<Object>) d;
                        state[0] = true;

                        int m = qd.requestFusion(QueueFuseable.ANY);

                        if (m != QueueFuseable.NONE) {
                            state[1] = true;

                            state[2] = qd.isEmpty();

                            qd.clear();

                            state[3] = qd.isEmpty();
                        }
                    }
                    cdl.countDown();
                } finally {
                    d.cancel();
                }
            }

            @Override
            public void onNext(T value) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });

        try {
            assertTrue(cdl.await(5, TimeUnit.SECONDS));

            assertTrue("Not fuseable", state[0]);
            assertTrue("Fusion rejected", state[1]);

            assertNotNull(state[2]);
            assertTrue("Did not empty", state[3]);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns an expanded error list of the given test consumer.
     * @param to the test consumer instance
     * @return the list
     */
    public static List<Throwable> errorList(TestObserver<?> to) {
        return compositeList(to.errors().get(0));
    }

    /**
     * Returns an expanded error list of the given test consumer.
     * @param ts the test consumer instance
     * @return the list
     */
    public static List<Throwable> errorList(TestSubscriber<?> ts) {
        return compositeList(ts.errors().get(0));
    }

    /**
     * Tests the given mapping of a bad Observable by emitting the good values, then an error/completion and then
     * a bad value followed by a TestException and and a completion.
     * @param <T> the value type
     * @param mapper the mapper that receives a bad Observable and returns a reactive base type (detected via reflection).
     * @param error if true, the good value emission is followed by a TestException("error"), if false then onComplete is called
     * @param badValue the bad value to emit if not null
     * @param goodValue the good value to emit before turning bad, if not null
     * @param expected the expected resulting values, null to ignore values received
     */
    public static <T> void checkBadSourceObservable(Function<Observable<T>, Object> mapper,
            final boolean error, final T goodValue, final T badValue, final Object... expected) {
        List<Throwable> errors = trackPluginErrors();
        try {
            Observable<T> bad = new Observable<T>() {
                boolean once;
                @Override
                protected void subscribeActual(Observer<? super T> observer) {
                    observer.onSubscribe(Disposables.empty());

                    if (once) {
                        return;
                    }
                    once = true;

                    if (goodValue != null) {
                        observer.onNext(goodValue);
                    }

                    if (error) {
                        observer.onError(new TestException("error"));
                    } else {
                        observer.onComplete();
                    }

                    if (badValue != null) {
                        observer.onNext(badValue);
                    }
                    observer.onError(new TestException("second"));
                    observer.onComplete();
                }
            };

            Object o = mapper.apply(bad);

            if (o instanceof ObservableSource) {
                ObservableSource<?> os = (ObservableSource<?>) o;
                TestObserver<Object> to = new TestObserver<Object>();

                os.subscribe(to);

                to.awaitDone(5, TimeUnit.SECONDS);

                to.assertSubscribed();

                if (expected != null) {
                    to.assertValues(expected);
                }
                if (error) {
                    to.assertError(TestException.class)
                    .assertErrorMessage("error")
                    .assertNotComplete();
                } else {
                    to.assertNoErrors().assertComplete();
                }
            }

            if (o instanceof Publisher) {
                Publisher<?> os = (Publisher<?>) o;
                TestSubscriber<Object> ts = new TestSubscriber<Object>();

                os.subscribe(ts);

                ts.awaitDone(5, TimeUnit.SECONDS);

                ts.assertSubscribed();

                if (expected != null) {
                    ts.assertValues(expected);
                }
                if (error) {
                    ts.assertError(TestException.class)
                    .assertErrorMessage("error")
                    .assertNotComplete();
                } else {
                    ts.assertNoErrors().assertComplete();
                }
            }

            if (o instanceof SingleSource) {
                SingleSource<?> os = (SingleSource<?>) o;
                TestObserver<Object> to = new TestObserver<Object>();

                os.subscribe(to);

                to.awaitDone(5, TimeUnit.SECONDS);

                to.assertSubscribed();

                if (expected != null) {
                    to.assertValues(expected);
                }
                if (error) {
                    to.assertError(TestException.class)
                    .assertErrorMessage("error")
                    .assertNotComplete();
                } else {
                    to.assertNoErrors().assertComplete();
                }
            }

            if (o instanceof MaybeSource) {
                MaybeSource<?> os = (MaybeSource<?>) o;
                TestObserver<Object> to = new TestObserver<Object>();

                os.subscribe(to);

                to.awaitDone(5, TimeUnit.SECONDS);

                to.assertSubscribed();

                if (expected != null) {
                    to.assertValues(expected);
                }
                if (error) {
                    to.assertError(TestException.class)
                    .assertErrorMessage("error")
                    .assertNotComplete();
                } else {
                    to.assertNoErrors().assertComplete();
                }
            }

            if (o instanceof CompletableSource) {
                CompletableSource os = (CompletableSource) o;
                TestObserver<Object> to = new TestObserver<Object>();

                os.subscribe(to);

                to.awaitDone(5, TimeUnit.SECONDS);

                to.assertSubscribed();

                if (expected != null) {
                    to.assertValues(expected);
                }
                if (error) {
                    to.assertError(TestException.class)
                    .assertErrorMessage("error")
                    .assertNotComplete();
                } else {
                    to.assertNoErrors().assertComplete();
                }
            }

            assertUndeliverable(errors, 0, TestException.class, "second");
        } catch (AssertionError ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    /**
     * Tests the given mapping of a bad Observable by emitting the good values, then an error/completion and then
     * a bad value followed by a TestException and and a completion.
     * @param <T> the value type
     * @param mapper the mapper that receives a bad Observable and returns a reactive base type (detected via reflection).
     * @param error if true, the good value emission is followed by a TestException("error"), if false then onComplete is called
     * @param badValue the bad value to emit if not null
     * @param goodValue the good value to emit before turning bad, if not null
     * @param expected the expected resulting values, null to ignore values received
     */
    public static <T> void checkBadSourceFlowable(Function<Flowable<T>, Object> mapper,
            final boolean error, final T goodValue, final T badValue, final Object... expected) {
        List<Throwable> errors = trackPluginErrors();
        try {
            Flowable<T> bad = new Flowable<T>() {
                @Override
                protected void subscribeActual(Subscriber<? super T> observer) {
                    observer.onSubscribe(new BooleanSubscription());

                    if (goodValue != null) {
                        observer.onNext(goodValue);
                    }

                    if (error) {
                        observer.onError(new TestException("error"));
                    } else {
                        observer.onComplete();
                    }

                    if (badValue != null) {
                        observer.onNext(badValue);
                    }
                    observer.onError(new TestException("second"));
                    observer.onComplete();
                }
            };

            Object o = mapper.apply(bad);

            if (o instanceof ObservableSource) {
                ObservableSource<?> os = (ObservableSource<?>) o;
                TestObserver<Object> to = new TestObserver<Object>();

                os.subscribe(to);

                to.awaitDone(5, TimeUnit.SECONDS);

                to.assertSubscribed();

                if (expected != null) {
                    to.assertValues(expected);
                }
                if (error) {
                    to.assertError(TestException.class)
                    .assertErrorMessage("error")
                    .assertNotComplete();
                } else {
                    to.assertNoErrors().assertComplete();
                }
            }

            if (o instanceof Publisher) {
                Publisher<?> os = (Publisher<?>) o;
                TestSubscriber<Object> ts = new TestSubscriber<Object>();

                os.subscribe(ts);

                ts.awaitDone(5, TimeUnit.SECONDS);

                ts.assertSubscribed();

                if (expected != null) {
                    ts.assertValues(expected);
                }
                if (error) {
                    ts.assertError(TestException.class)
                    .assertErrorMessage("error")
                    .assertNotComplete();
                } else {
                    ts.assertNoErrors().assertComplete();
                }
            }

            if (o instanceof SingleSource) {
                SingleSource<?> os = (SingleSource<?>) o;
                TestObserver<Object> to = new TestObserver<Object>();

                os.subscribe(to);

                to.awaitDone(5, TimeUnit.SECONDS);

                to.assertSubscribed();

                if (expected != null) {
                    to.assertValues(expected);
                }
                if (error) {
                    to.assertError(TestException.class)
                    .assertErrorMessage("error")
                    .assertNotComplete();
                } else {
                    to.assertNoErrors().assertComplete();
                }
            }

            if (o instanceof MaybeSource) {
                MaybeSource<?> os = (MaybeSource<?>) o;
                TestObserver<Object> to = new TestObserver<Object>();

                os.subscribe(to);

                to.awaitDone(5, TimeUnit.SECONDS);

                to.assertSubscribed();

                if (expected != null) {
                    to.assertValues(expected);
                }
                if (error) {
                    to.assertError(TestException.class)
                    .assertErrorMessage("error")
                    .assertNotComplete();
                } else {
                    to.assertNoErrors().assertComplete();
                }
            }

            if (o instanceof CompletableSource) {
                CompletableSource os = (CompletableSource) o;
                TestObserver<Object> to = new TestObserver<Object>();

                os.subscribe(to);

                to.awaitDone(5, TimeUnit.SECONDS);

                to.assertSubscribed();

                if (expected != null) {
                    to.assertValues(expected);
                }
                if (error) {
                    to.assertError(TestException.class)
                    .assertErrorMessage("error")
                    .assertNotComplete();
                } else {
                    to.assertNoErrors().assertComplete();
                }
            }

            assertUndeliverable(errors, 0, TestException.class, "second");
        } catch (AssertionError ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    public static <T> void checkInvalidParallelSubscribers(ParallelFlowable<T> source) {
        int n = source.parallelism();

        @SuppressWarnings("unchecked")
        TestSubscriber<Object>[] tss = new TestSubscriber[n + 1];
        for (int i = 0; i <= n; i++) {
            tss[i] = new TestSubscriber<Object>().withTag("" + i);
        }

        source.subscribe(tss);

        for (int i = 0; i <= n; i++) {
            tss[i].assertFailure(IllegalArgumentException.class);
        }
    }

    public static <T> Observable<T> rejectObservableFusion() {
        return new Observable<T>() {
            @Override
            protected void subscribeActual(Observer<? super T> observer) {
                observer.onSubscribe(new QueueDisposable<T>() {

                    @Override
                    public int requestFusion(int mode) {
                        return 0;
                    }

                    @Override
                    public boolean offer(T value) {
                        throw new IllegalStateException();
                    }

                    @Override
                    public boolean offer(T v1, T v2) {
                        throw new IllegalStateException();
                    }

                    @Override
                    public T poll() throws Exception {
                        return null;
                    }

                    @Override
                    public boolean isEmpty() {
                        return true;
                    }

                    @Override
                    public void clear() {
                    }

                    @Override
                    public void dispose() {
                    }

                    @Override
                    public boolean isDisposed() {
                        return false;
                    }
                });
            }
        };
    }

    public static <T> Flowable<T> rejectFlowableFusion() {
        return new Flowable<T>() {
            @Override
            protected void subscribeActual(Subscriber<? super T> observer) {
                observer.onSubscribe(new QueueSubscription<T>() {

                    @Override
                    public int requestFusion(int mode) {
                        return 0;
                    }

                    @Override
                    public boolean offer(T value) {
                        throw new IllegalStateException();
                    }

                    @Override
                    public boolean offer(T v1, T v2) {
                        throw new IllegalStateException();
                    }

                    @Override
                    public T poll() throws Exception {
                        return null;
                    }

                    @Override
                    public boolean isEmpty() {
                        return true;
                    }

                    @Override
                    public void clear() {
                    }

                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void request(long n) {
                    }
                });
            }
        };
    }

    static final class FlowableStripBoundary<T> extends Flowable<T> implements FlowableTransformer<T, T> {

        final Flowable<T> source;

        FlowableStripBoundary(Flowable<T> source) {
            this.source = source;
        }

        @Override
        public Flowable<T> apply(Flowable<T> upstream) {
            return new FlowableStripBoundary<T>(upstream);
        }

        @Override
        protected void subscribeActual(Subscriber<? super T> s) {
            source.subscribe(new StripBoundarySubscriber<T>(s));
        }

        static final class StripBoundarySubscriber<T> implements FlowableSubscriber<T>, QueueSubscription<T> {

            final Subscriber<? super T> actual;

            Subscription upstream;

            QueueSubscription<T> qs;

            StripBoundarySubscriber(Subscriber<? super T> actual) {
                this.actual = actual;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(Subscription subscription) {
                this.upstream = subscription;
                if (subscription instanceof QueueSubscription) {
                    qs = (QueueSubscription<T>)subscription;
                }
                actual.onSubscribe(this);
            }

            @Override
            public void onNext(T t) {
                actual.onNext(t);
            }

            @Override
            public void onError(Throwable throwable) {
                actual.onError(throwable);
            }

            @Override
            public void onComplete() {
                actual.onComplete();
            }

            @Override
            public int requestFusion(int mode) {
                QueueSubscription<T> fs = qs;
                if (fs != null) {
                    return fs.requestFusion(mode & ~BOUNDARY);
                }
                return NONE;
            }

            @Override
            public boolean offer(T value) {
                throw new UnsupportedOperationException("Should not be called");
            }

            @Override
            public boolean offer(T v1, T v2) {
                throw new UnsupportedOperationException("Should not be called");
            }

            @Override
            public T poll() throws Exception {
                return qs.poll();
            }

            @Override
            public void clear() {
                qs.clear();
            }

            @Override
            public boolean isEmpty() {
                return qs.isEmpty();
            }

            @Override
            public void request(long n) {
                upstream.request(n);
            }

            @Override
            public void cancel() {
                upstream.cancel();
            }
        }
    }

    public static <T> FlowableTransformer<T, T> flowableStripBoundary() {
        return new FlowableStripBoundary<T>(null);
    }

    static final class ObservableStripBoundary<T> extends Observable<T> implements ObservableTransformer<T, T> {

        final Observable<T> source;

        ObservableStripBoundary(Observable<T> source) {
            this.source = source;
        }

        @Override
        public Observable<T> apply(Observable<T> upstream) {
            return new ObservableStripBoundary<T>(upstream);
        }

        @Override
        protected void subscribeActual(Observer<? super T> s) {
            source.subscribe(new StripBoundaryObserver<T>(s));
        }

        static final class StripBoundaryObserver<T> implements Observer<T>, QueueDisposable<T> {

            final Observer<? super T> actual;

            Disposable upstream;

            QueueDisposable<T> qd;

            StripBoundaryObserver(Observer<? super T> actual) {
                this.actual = actual;
            }

            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(Disposable d) {
                this.upstream = d;
                if (d instanceof QueueDisposable) {
                    qd = (QueueDisposable<T>)d;
                }
                actual.onSubscribe(this);
            }

            @Override
            public void onNext(T t) {
                actual.onNext(t);
            }

            @Override
            public void onError(Throwable throwable) {
                actual.onError(throwable);
            }

            @Override
            public void onComplete() {
                actual.onComplete();
            }

            @Override
            public int requestFusion(int mode) {
                QueueDisposable<T> fs = qd;
                if (fs != null) {
                    return fs.requestFusion(mode & ~BOUNDARY);
                }
                return NONE;
            }

            @Override
            public boolean offer(T value) {
                throw new UnsupportedOperationException("Should not be called");
            }

            @Override
            public boolean offer(T v1, T v2) {
                throw new UnsupportedOperationException("Should not be called");
            }

            @Override
            public T poll() throws Exception {
                return qd.poll();
            }

            @Override
            public void clear() {
                qd.clear();
            }

            @Override
            public boolean isEmpty() {
                return qd.isEmpty();
            }

            @Override
            public void dispose() {
                upstream.dispose();
            }

            @Override
            public boolean isDisposed() {
                return upstream.isDisposed();
            }
        }
    }

    public static <T> ObservableTransformer<T, T> observableStripBoundary() {
        return new ObservableStripBoundary<T>(null);
    }
}
