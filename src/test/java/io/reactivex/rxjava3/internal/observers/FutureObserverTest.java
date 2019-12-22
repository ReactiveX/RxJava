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

package io.reactivex.rxjava3.internal.observers;

import static io.reactivex.rxjava3.internal.util.ExceptionHelper.timeoutMessage;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.subscribers.FutureSubscriber;
import io.reactivex.rxjava3.internal.subscriptions.BooleanSubscription;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.testsupport.TestHelper;

public class FutureObserverTest extends RxJavaTest {
    FutureObserver<Integer> fo;

    @Before
    public void before() {
        fo = new FutureObserver<>();
    }

    @Test
    public void cancel2() {

        fo.dispose();

        assertFalse(fo.isCancelled());
        assertFalse(fo.isDisposed());
        assertFalse(fo.isDone());

        for (int i = 0; i < 2; i++) {
            fo.cancel(i == 0);

            assertTrue(fo.isCancelled());
            assertTrue(fo.isDisposed());
            assertTrue(fo.isDone());
        }

        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            fo.onNext(1);
            fo.onError(new TestException("First"));
            fo.onError(new TestException("Second"));
            fo.onComplete();

            assertTrue(fo.isCancelled());
            assertTrue(fo.isDisposed());
            assertTrue(fo.isDone());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
            TestHelper.assertUndeliverable(errors, 1, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancel() throws Exception {
        assertFalse(fo.isDone());

        assertFalse(fo.isCancelled());

        fo.cancel(false);

        assertTrue(fo.isDone());

        assertTrue(fo.isCancelled());

        try {
            fo.get();
            fail("Should have thrown");
        } catch (CancellationException ex) {
            // expected
        }

        try {
            fo.get(1, TimeUnit.MILLISECONDS);
            fail("Should have thrown");
        } catch (CancellationException ex) {
            // expected
        }
    }

    @Test
    public void onError() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            fo.onError(new TestException("One"));

            fo.onError(new TestException("Two"));

            try {
                fo.get(5, TimeUnit.MILLISECONDS);
            } catch (ExecutionException ex) {
                assertTrue(ex.toString(), ex.getCause() instanceof TestException);
                assertEquals("One", ex.getCause().getMessage());
            }

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Two");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNext() throws Exception {
        fo.onNext(1);
        fo.onComplete();

        assertEquals(1, fo.get(5, TimeUnit.MILLISECONDS).intValue());
    }

    @Test
    public void onSubscribe() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {

            Disposable d1 = Disposable.empty();

            fo.onSubscribe(d1);

            Disposable d2 = Disposable.empty();

            fo.onSubscribe(d2);

            assertFalse(d1.isDisposed());
            assertTrue(d2.isDisposed());

            TestHelper.assertError(errors, 0, IllegalStateException.class, "Disposable already set!");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final FutureSubscriber<Integer> fo = new FutureSubscriber<>();

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    fo.cancel(false);
                }
            };

            TestHelper.race(r, r);
        }
    }

    @Test
    public void await() throws Exception {
        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                fo.onNext(1);
                fo.onComplete();
            }
        }, 100, TimeUnit.MILLISECONDS);

        assertEquals(1, fo.get(5, TimeUnit.SECONDS).intValue());
    }

    @Test
    public void onErrorCancelRace() {
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer());
        try {
            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
                final FutureSubscriber<Integer> fo = new FutureSubscriber<>();

                final TestException ex = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        fo.cancel(false);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        fo.onError(ex);
                    }
                };

                TestHelper.race(r1, r2);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteCancelRace() {
        RxJavaPlugins.setErrorHandler(Functions.emptyConsumer());
        try {
            for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
                final FutureSubscriber<Integer> fo = new FutureSubscriber<>();

                if (i % 3 == 0) {
                    fo.onSubscribe(new BooleanSubscription());
                }

                if (i % 2 == 0) {
                    fo.onNext(1);
                }

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        fo.cancel(false);
                    }
                };

                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        fo.onComplete();
                    }
                };

                TestHelper.race(r1, r2);
            }
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onErrorOnComplete() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            fo.onError(new TestException("One"));
            fo.onComplete();

            try {
                fo.get(5, TimeUnit.MILLISECONDS);
            } catch (ExecutionException ex) {
                assertTrue(ex.toString(), ex.getCause() instanceof TestException);
                assertEquals("One", ex.getCause().getMessage());
            }

            TestHelper.assertUndeliverable(errors, 0, NoSuchElementException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onCompleteOnError() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            fo.onComplete();
            fo.onError(new TestException("One"));

            try {
                assertNull(fo.get(5, TimeUnit.MILLISECONDS));
            } catch (ExecutionException ex) {
                assertTrue(ex.toString(), ex.getCause() instanceof NoSuchElementException);
            }

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelOnError() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            fo.cancel(true);
            fo.onError(new TestException("One"));

            try {
                fo.get(5, TimeUnit.MILLISECONDS);
                fail("Should have thrown");
            } catch (CancellationException ex) {
                // expected
            }
            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void cancelOnComplete() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            fo.cancel(true);
            fo.onComplete();

            try {
                fo.get(5, TimeUnit.MILLISECONDS);
                fail("Should have thrown");
            } catch (CancellationException ex) {
                // expected
            }

            TestHelper.assertUndeliverable(errors, 0, NoSuchElementException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void onNextThenOnCompleteTwice() throws Exception {
        fo.onNext(1);
        fo.onComplete();
        fo.onComplete();

        assertEquals(1, fo.get(5, TimeUnit.MILLISECONDS).intValue());
    }

    @Test(expected = InterruptedException.class)
    public void getInterrupted() throws Exception {
        Thread.currentThread().interrupt();
        fo.get();
    }

    @Test
    public void completeAsync() throws Exception {
        Schedulers.single().scheduleDirect(new Runnable() {
            @Override
            public void run() {
                fo.onNext(1);
                fo.onComplete();
            }
        }, 500, TimeUnit.MILLISECONDS);

        assertEquals(1, fo.get().intValue());
    }

    @Test
    public void getTimedOut() throws Exception {
        try {
            fo.get(1, TimeUnit.NANOSECONDS);
            fail("Should have thrown");
        } catch (TimeoutException expected) {
            assertEquals(timeoutMessage(1, TimeUnit.NANOSECONDS), expected.getMessage());
        }
    }
}
