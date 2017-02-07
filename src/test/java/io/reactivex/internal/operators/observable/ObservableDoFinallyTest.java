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

package io.reactivex.internal.operators.observable;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.TestHelper;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.QueueDisposable;
import io.reactivex.observers.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.subjects.UnicastSubject;

public class ObservableDoFinallyTest implements Action {

    int calls;

    @Override
    public void run() throws Exception {
        calls++;
    }

    @Test
    public void normalJust() {
        Observable.just(1)
        .doFinally(this)
        .test()
        .assertResult(1);

        assertEquals(1, calls);
    }

    @Test
    public void normalEmpty() {
        Observable.empty()
        .doFinally(this)
        .test()
        .assertResult();

        assertEquals(1, calls);
    }

    @Test
    public void normalError() {
        Observable.error(new TestException())
        .doFinally(this)
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, calls);
    }

    @Test
    public void normalTake() {
        Observable.range(1, 10)
        .doFinally(this)
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Observable<Object> f) throws Exception {
                return f.doFinally(ObservableDoFinallyTest.this);
            }
        });
        TestHelper.checkDoubleOnSubscribeObservable(new Function<Observable<Object>, Observable<Object>>() {
            @Override
            public Observable<Object> apply(Observable<Object> f) throws Exception {
                return f.doFinally(ObservableDoFinallyTest.this).filter(Functions.alwaysTrue());
            }
        });
    }

    @Test
    public void syncFused() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.SYNC);

        Observable.range(1, 5)
        .doFinally(this)
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void syncFusedBoundary() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.SYNC | QueueDisposable.BOUNDARY);

        Observable.range(1, 5)
        .doFinally(this)
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void asyncFused() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ASYNC);

        UnicastSubject<Integer> up = UnicastSubject.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doFinally(this)
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void asyncFusedBoundary() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ASYNC | QueueDisposable.BOUNDARY);

        UnicastSubject<Integer> up = UnicastSubject.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doFinally(this)
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }


    @Test
    public void normalJustConditional() {
        Observable.just(1)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .test()
        .assertResult(1);

        assertEquals(1, calls);
    }

    @Test
    public void normalEmptyConditional() {
        Observable.empty()
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .test()
        .assertResult();

        assertEquals(1, calls);
    }

    @Test
    public void normalErrorConditional() {
        Observable.error(new TestException())
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .test()
        .assertFailure(TestException.class);

        assertEquals(1, calls);
    }

    @Test
    public void normalTakeConditional() {
        Observable.range(1, 10)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .take(5)
        .test()
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void syncFusedConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.SYNC);

        Observable.range(1, 5)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.SYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void nonFused() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.SYNC);

        Observable.range(1, 5).hide()
        .doFinally(this)
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void nonFusedConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.SYNC);

        Observable.range(1, 5).hide()
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void syncFusedBoundaryConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.SYNC | QueueDisposable.BOUNDARY);

        Observable.range(1, 5)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void asyncFusedConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ASYNC);

        UnicastSubject<Integer> up = UnicastSubject.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.ASYNC)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test
    public void asyncFusedBoundaryConditional() {
        TestObserver<Integer> ts = ObserverFusion.newTest(QueueDisposable.ASYNC | QueueDisposable.BOUNDARY);

        UnicastSubject<Integer> up = UnicastSubject.create();
        TestHelper.emit(up, 1, 2, 3, 4, 5);

        up
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(ts);

        ObserverFusion.assertFusion(ts, QueueDisposable.NONE)
        .assertResult(1, 2, 3, 4, 5);

        assertEquals(1, calls);
    }

    @Test(expected = NullPointerException.class)
    public void nullAction() {
        Observable.just(1).doFinally(null);
    }

    @Test
    public void actionThrows() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Observable.just(1)
            .doFinally(new Action() {
                @Override
                public void run() throws Exception {
                    throw new TestException();
                }
            })
            .test()
            .assertResult(1)
            .cancel();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void actionThrowsConditional() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            Observable.just(1)
            .doFinally(new Action() {
                @Override
                public void run() throws Exception {
                    throw new TestException();
                }
            })
            .filter(Functions.alwaysTrue())
            .test()
            .assertResult(1)
            .cancel();

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void clearIsEmpty() {
        Observable.range(1, 5)
        .doFinally(this)
        .subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable s) {
                @SuppressWarnings("unchecked")
                QueueDisposable<Integer> qs = (QueueDisposable<Integer>)s;

                qs.requestFusion(QueueDisposable.ANY);

                assertFalse(qs.isEmpty());

                try {
                    assertEquals(1, qs.poll().intValue());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }

                assertFalse(qs.isEmpty());

                qs.clear();

                assertTrue(qs.isEmpty());

                qs.dispose();
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertEquals(1, calls);
    }

    @Test
    public void clearIsEmptyConditional() {
        Observable.range(1, 5)
        .doFinally(this)
        .filter(Functions.alwaysTrue())
        .subscribe(new Observer<Integer>() {

            @Override
            public void onSubscribe(Disposable s) {
                @SuppressWarnings("unchecked")
                QueueDisposable<Integer> qs = (QueueDisposable<Integer>)s;

                qs.requestFusion(QueueDisposable.ANY);

                assertFalse(qs.isEmpty());

                assertFalse(qs.isDisposed());

                try {
                    assertEquals(1, qs.poll().intValue());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }

                assertFalse(qs.isEmpty());

                qs.clear();

                assertTrue(qs.isEmpty());

                qs.dispose();

                assertTrue(qs.isDisposed());
            }

            @Override
            public void onNext(Integer t) {
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onComplete() {
            }
        });

        assertEquals(1, calls);
    }


    @Test
    public void eventOrdering() {
        final List<String> list = new ArrayList<String>();

        Observable.error(new TestException())
        .doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                list.add("dispose");
            }
        })
        .doFinally(new Action() {
            @Override
            public void run() throws Exception {
                list.add("finally");
            }
        })
        .subscribe(
                new Consumer<Object>() {
                    @Override
                    public void accept(Object v) throws Exception {
                        list.add("onNext");
                    }
                },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable e) throws Exception {
                        list.add("onError");
                    }
                },
                new Action() {
                    @Override
                    public void run() throws Exception {
                        list.add("onComplete");
                    }
                });

        assertEquals(Arrays.asList("onError", "finally"), list);
    }

    @Test
    public void eventOrdering2() {
        final List<String> list = new ArrayList<String>();

        Observable.just(1)
        .doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                list.add("dispose");
            }
        })
        .doFinally(new Action() {
            @Override
            public void run() throws Exception {
                list.add("finally");
            }
        })
        .subscribe(
                new Consumer<Object>() {
                    @Override
                    public void accept(Object v) throws Exception {
                        list.add("onNext");
                    }
                },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable e) throws Exception {
                        list.add("onError");
                    }
                },
                new Action() {
                    @Override
                    public void run() throws Exception {
                        list.add("onComplete");
                    }
                });

        assertEquals(Arrays.asList("onNext", "onComplete", "finally"), list);
    }

}
