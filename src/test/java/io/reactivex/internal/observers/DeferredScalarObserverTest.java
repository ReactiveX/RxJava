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

package io.reactivex.internal.observers;

import static org.junit.Assert.*;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.internal.fuseable.QueueDisposable;
import io.reactivex.internal.observers.DeferredScalarObserver;
import io.reactivex.observers.*;

public class DeferredScalarObserverTest {

    static final class TakeFirst extends DeferredScalarObserver<Integer, Integer> {

        private static final long serialVersionUID = -2793723002312330530L;

        TakeFirst(Observer<? super Integer> actual) {
            super(actual);
        }

        @Override
        public void onNext(Integer value) {
            s.dispose();
            complete(value);
            complete(value);
        }

    }

    @Test
    public void normal() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        TakeFirst source = new TakeFirst(to);

        source.onSubscribe(Disposables.empty());

        Disposable d = Disposables.empty();
        source.onSubscribe(d);

        assertTrue(d.isDisposed());

        source.onNext(1);

        to.assertResult(1);
    }

    @Test
    public void error() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        TakeFirst source = new TakeFirst(to);

        source.onSubscribe(Disposables.empty());
        source.onError(new TestException());

        to.assertFailure(TestException.class);
    }

    @Test
    public void complete() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        TakeFirst source = new TakeFirst(to);

        source.onSubscribe(Disposables.empty());
        source.onComplete();

        to.assertResult();
    }

    @Test
    public void dispose() {
        TestObserver<Integer> to = new TestObserver<Integer>();

        TakeFirst source = new TakeFirst(to);

        Disposable d = Disposables.empty();

        source.onSubscribe(d);

        assertFalse(d.isDisposed());

        to.cancel();

        assertTrue(d.isDisposed());

        assertTrue(source.isDisposed());
    }

    @Test
    public void fused() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        TakeFirst source = new TakeFirst(to);

        Disposable d = Disposables.empty();

        source.onSubscribe(d);

        to.assertOf(ObserverFusion.<Integer>assertFuseable());
        to.assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC));

        source.onNext(1);
        source.onNext(1);
        source.onError(new TestException());
        source.onComplete();

        assertTrue(d.isDisposed());

        to.assertResult(1);
    }

    @Test
    public void fusedReject() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.SYNC);

        TakeFirst source = new TakeFirst(to);

        Disposable d = Disposables.empty();

        source.onSubscribe(d);

        to.assertOf(ObserverFusion.<Integer>assertFuseable());
        to.assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.NONE));

        source.onNext(1);
        source.onNext(1);
        source.onError(new TestException());
        source.onComplete();

        assertTrue(d.isDisposed());

        to.assertResult(1);
    }

    static final class TakeLast extends DeferredScalarObserver<Integer, Integer> {

        private static final long serialVersionUID = -2793723002312330530L;

        TakeLast(Observer<? super Integer> actual) {
            super(actual);
        }


        @Override
        public void onNext(Integer value) {
            this.value = value;
        }

    }

    @Test
    public void nonfusedTerminateMore() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.NONE);

        TakeLast source = new TakeLast(to);

        Disposable d = Disposables.empty();

        source.onSubscribe(d);

        source.onNext(1);
        source.onComplete();
        source.onComplete();
        source.onError(new TestException());

        to.assertResult(1);
    }

    @Test
    public void nonfusedError() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.NONE);

        TakeLast source = new TakeLast(to);

        Disposable d = Disposables.empty();

        source.onSubscribe(d);

        source.onNext(1);
        source.onError(new TestException());
        source.onError(new TestException());
        source.onComplete();

        to.assertFailure(TestException.class);
    }

    @Test
    public void fusedTerminateMore() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        TakeLast source = new TakeLast(to);

        Disposable d = Disposables.empty();

        source.onSubscribe(d);

        source.onNext(1);
        source.onComplete();
        source.onComplete();
        source.onError(new TestException());

        to.assertResult(1);
    }

    @Test
    public void fusedError() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        TakeLast source = new TakeLast(to);

        Disposable d = Disposables.empty();

        source.onSubscribe(d);

        source.onNext(1);
        source.onError(new TestException());
        source.onError(new TestException());
        source.onComplete();

        to.assertFailure(TestException.class);
    }

    @Test
    public void disposed() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.NONE);

        TakeLast source = new TakeLast(to);

        Disposable d = Disposables.empty();

        source.onSubscribe(d);

        to.cancel();

        source.onNext(1);
        source.onComplete();

        to.assertNoValues().assertNoErrors().assertNotComplete();
    }

    @Test
    public void disposedAfterOnNext() {
        final TestObserver<Integer> to = new TestObserver<Integer>();

        TakeLast source = new TakeLast(new Observer<Integer>() {
            Disposable d;

            @Override
            public void onSubscribe(Disposable d) {
                this.d = d;
                to.onSubscribe(d);
            }

            @Override
            public void onNext(Integer value) {
                to.onNext(value);
                d.dispose();
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

        source.onSubscribe(Disposables.empty());
        source.onNext(1);
        source.onComplete();

        to.assertValue(1).assertNoErrors().assertNotComplete();
    }

    @Test
    public void fusedEmpty() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        TakeLast source = new TakeLast(to);

        Disposable d = Disposables.empty();

        source.onSubscribe(d);

        source.onComplete();

        to.assertResult();
    }

    @Test
    public void nonfusedEmpty() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.NONE);

        TakeLast source = new TakeLast(to);

        Disposable d = Disposables.empty();

        source.onSubscribe(d);

        source.onComplete();

        to.assertResult();
    }

    @Test
    public void customFusion() {
        final TestObserver<Integer> to = new TestObserver<Integer>();

        TakeLast source = new TakeLast(new Observer<Integer>() {
            QueueDisposable<Integer> d;

            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(Disposable d) {
                this.d = (QueueDisposable<Integer>)d;
                to.onSubscribe(d);
                this.d.requestFusion(QueueDisposable.ANY);
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

        source.onSubscribe(Disposables.empty());
        source.onNext(1);
        source.onComplete();

        to.assertResult(1);
    }

    @Test
    public void customFusionClear() {
        final TestObserver<Integer> to = new TestObserver<Integer>();

        TakeLast source = new TakeLast(new Observer<Integer>() {
            QueueDisposable<Integer> d;

            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(Disposable d) {
                this.d = (QueueDisposable<Integer>)d;
                to.onSubscribe(d);
                this.d.requestFusion(QueueDisposable.ANY);
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

        source.onSubscribe(Disposables.empty());
        source.onNext(1);
        source.onComplete();

        to.assertNoValues().assertNoErrors().assertComplete();
    }

    @Test
    public void offerThrow() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.NONE);

        TakeLast source = new TakeLast(to);

        TestHelper.assertNoOffer(source);
    }

    @Test
    public void customFusionDontConsume() {
        final TestObserver<Integer> to = new TestObserver<Integer>();

        TakeFirst source = new TakeFirst(new Observer<Integer>() {
            QueueDisposable<Integer> d;

            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(Disposable d) {
                this.d = (QueueDisposable<Integer>)d;
                to.onSubscribe(d);
                this.d.requestFusion(QueueDisposable.ANY);
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

        source.onSubscribe(Disposables.empty());
        source.onNext(1);

        to.assertNoValues().assertNoErrors().assertComplete();
    }

}
