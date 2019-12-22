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

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.testsupport.*;

public class DeferredScalarObserverTest extends RxJavaTest {

    static final class TakeFirst extends DeferredScalarObserver<Integer, Integer> {

        private static final long serialVersionUID = -2793723002312330530L;

        TakeFirst(Observer<? super Integer> downstream) {
            super(downstream);
        }

        @Override
        public void onNext(Integer value) {
            upstream.dispose();
            complete(value);
            complete(value);
        }

    }

    @Test
    public void normal() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Integer> to = new TestObserver<>();

            TakeFirst source = new TakeFirst(to);

            source.onSubscribe(Disposable.empty());

            Disposable d = Disposable.empty();
            source.onSubscribe(d);

            assertTrue(d.isDisposed());

            source.onNext(1);

            to.assertResult(1);

            TestHelper.assertError(errors, 0, ProtocolViolationException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void error() {
        TestObserver<Integer> to = new TestObserver<>();

        TakeFirst source = new TakeFirst(to);

        source.onSubscribe(Disposable.empty());
        source.onError(new TestException());

        to.assertFailure(TestException.class);
    }

    @Test
    public void complete() {
        TestObserver<Integer> to = new TestObserver<>();

        TakeFirst source = new TakeFirst(to);

        source.onSubscribe(Disposable.empty());
        source.onComplete();

        to.assertResult();
    }

    @Test
    public void dispose() {
        TestObserver<Integer> to = new TestObserver<>();

        TakeFirst source = new TakeFirst(to);

        Disposable d = Disposable.empty();

        source.onSubscribe(d);

        assertFalse(d.isDisposed());

        to.dispose();

        assertTrue(d.isDisposed());

        assertTrue(source.isDisposed());
    }

    @Test
    public void fused() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

            TakeFirst source = new TakeFirst(to);

            Disposable d = Disposable.empty();

            source.onSubscribe(d);

            to.assertFuseable();
            to.assertFusionMode(QueueFuseable.ASYNC);

            source.onNext(1);
            source.onNext(1);
            source.onError(new TestException());
            source.onComplete();

            assertTrue(d.isDisposed());

            to.assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void fusedReject() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.SYNC);

            TakeFirst source = new TakeFirst(to);

            Disposable d = Disposable.empty();

            source.onSubscribe(d);

            to.assertFuseable();
            to.assertFusionMode(QueueFuseable.NONE);

            source.onNext(1);
            source.onNext(1);
            source.onError(new TestException());
            source.onComplete();

            assertTrue(d.isDisposed());

            to.assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    static final class TakeLast extends DeferredScalarObserver<Integer, Integer> {

        private static final long serialVersionUID = -2793723002312330530L;

        TakeLast(Observer<? super Integer> downstream) {
            super(downstream);
        }

        @Override
        public void onNext(Integer value) {
            this.value = value;
        }

    }

    @Test
    public void nonfusedTerminateMore() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.NONE);

            TakeLast source = new TakeLast(to);

            Disposable d = Disposable.empty();

            source.onSubscribe(d);

            source.onNext(1);
            source.onComplete();
            source.onComplete();
            source.onError(new TestException());

            to.assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void nonfusedError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.NONE);

            TakeLast source = new TakeLast(to);

            Disposable d = Disposable.empty();

            source.onSubscribe(d);

            source.onNext(1);
            source.onError(new TestException());
            source.onError(new TestException("second"));
            source.onComplete();

            to.assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void fusedTerminateMore() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

            TakeLast source = new TakeLast(to);

            Disposable d = Disposable.empty();

            source.onSubscribe(d);

            source.onNext(1);
            source.onComplete();
            source.onComplete();
            source.onError(new TestException());

            to.assertResult(1);

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void fusedError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

            TakeLast source = new TakeLast(to);

            Disposable d = Disposable.empty();

            source.onSubscribe(d);

            source.onNext(1);
            source.onError(new TestException());
            source.onError(new TestException("second"));
            source.onComplete();

            to.assertFailure(TestException.class);

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "second");
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void disposed() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.NONE);

        TakeLast source = new TakeLast(to);

        Disposable d = Disposable.empty();

        source.onSubscribe(d);

        to.dispose();

        source.onNext(1);
        source.onComplete();

        to.assertNoValues().assertNoErrors().assertNotComplete();
    }

    @Test
    public void disposedAfterOnNext() {
        final TestObserver<Integer> to = new TestObserver<>();

        TakeLast source = new TakeLast(new Observer<Integer>() {
            Disposable upstream;

            @Override
            public void onSubscribe(Disposable d) {
                this.upstream = d;
                to.onSubscribe(d);
            }

            @Override
            public void onNext(Integer value) {
                to.onNext(value);
                upstream.dispose();
            }

            @Override
            public void onError(Throwable e) {
                to.onError(e);
            }

            @Override
            public void onComplete() {
                to.onComplete();
            }
        });

        source.onSubscribe(Disposable.empty());
        source.onNext(1);
        source.onComplete();

        to.assertValue(1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void fusedEmpty() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.ANY);

        TakeLast source = new TakeLast(to);

        Disposable d = Disposable.empty();

        source.onSubscribe(d);

        source.onComplete();

        to.assertResult();
    }

    @Test
    public void nonfusedEmpty() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.NONE);

        TakeLast source = new TakeLast(to);

        Disposable d = Disposable.empty();

        source.onSubscribe(d);

        source.onComplete();

        to.assertResult();
    }

    @Test
    public void customFusion() {
        final TestObserver<Integer> to = new TestObserver<>();

        TakeLast source = new TakeLast(new Observer<Integer>() {
            QueueDisposable<Integer> d;

            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(Disposable d) {
                this.d = (QueueDisposable<Integer>)d;
                to.onSubscribe(d);
                this.d.requestFusion(QueueFuseable.ANY);
            }

            @Override
            public void onNext(Integer value) {
                if (!d.isEmpty()) {
                    Integer v = null;
                    try {
                        to.onNext(d.poll());

                        v = d.poll();
                    } catch (Throwable ex) {
                        to.onError(ex);
                    }

                    assertNull(v);
                    assertTrue(d.isEmpty());
                }
            }

            @Override
            public void onError(Throwable e) {
                to.onError(e);
            }

            @Override
            public void onComplete() {
                to.onComplete();
            }
        });

        source.onSubscribe(Disposable.empty());
        source.onNext(1);
        source.onComplete();

        to.assertResult(1);
    }

    @Test
    public void customFusionClear() {
        final TestObserver<Integer> to = new TestObserver<>();

        TakeLast source = new TakeLast(new Observer<Integer>() {
            QueueDisposable<Integer> d;

            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(Disposable d) {
                this.d = (QueueDisposable<Integer>)d;
                to.onSubscribe(d);
                this.d.requestFusion(QueueFuseable.ANY);
            }

            @Override
            public void onNext(Integer value) {
                d.clear();
                assertTrue(d.isEmpty());
            }

            @Override
            public void onError(Throwable e) {
                to.onError(e);
            }

            @Override
            public void onComplete() {
                to.onComplete();
            }
        });

        source.onSubscribe(Disposable.empty());
        source.onNext(1);
        source.onComplete();

        to.assertNoValues().assertNoErrors().assertComplete();
    }

    @Test
    public void offerThrow() {
        TestObserverEx<Integer> to = new TestObserverEx<>(QueueFuseable.NONE);

        TakeLast source = new TakeLast(to);

        TestHelper.assertNoOffer(source);
    }

    @Test
    public void customFusionDontConsume() {
        final TestObserver<Integer> to = new TestObserver<>();

        TakeFirst source = new TakeFirst(new Observer<Integer>() {
            QueueDisposable<Integer> d;

            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(Disposable d) {
                this.d = (QueueDisposable<Integer>)d;
                to.onSubscribe(d);
                this.d.requestFusion(QueueFuseable.ANY);
            }

            @Override
            public void onNext(Integer value) {
                // not consuming
            }

            @Override
            public void onError(Throwable e) {
                to.onError(e);
            }

            @Override
            public void onComplete() {
                to.onComplete();
            }
        });

        source.onSubscribe(Disposable.empty());
        source.onNext(1);

        to.assertNoValues().assertNoErrors().assertComplete();
    }

}
