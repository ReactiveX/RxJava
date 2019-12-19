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

package io.reactivex.rxjava3.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class FlowableFlatMapCompletableTest extends RxJavaTest {

    @Test
    public void normalFlowable() {
        Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        }).toFlowable()
        .test()
        .assertResult();
    }

    @Test
    public void mapperThrowsFlowable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                throw new TestException();
            }
        }).<Integer>toFlowable()
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts.assertFailure(TestException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void mapperReturnsNullFlowable() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestSubscriber<Integer> ts = pp
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return null;
            }
        }).<Integer>toFlowable()
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        ts.assertFailure(NullPointerException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void normalDelayErrorFlowable() {
        Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        }, true, Integer.MAX_VALUE).toFlowable()
        .test()
        .assertResult();
    }

    @Test
    public void normalAsyncFlowable() {
        Flowable.range(1, 1000)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Flowable.range(1, 100).subscribeOn(Schedulers.computation()).ignoreElements();
            }
        }).toFlowable()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void normalAsyncFlowableMaxConcurrency() {
        Flowable.range(1, 1000)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Flowable.range(1, 100).subscribeOn(Schedulers.computation()).ignoreElements();
            }
        }, false, 3).toFlowable()
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void normalDelayErrorAllFlowable() {
        TestSubscriberEx<Integer> ts = Flowable.range(1, 10).concatWith(Flowable.<Integer>error(new TestException()))
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.error(new TestException());
            }
        }, true, Integer.MAX_VALUE).<Integer>toFlowable()
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(ts.errors().get(0));

        for (int i = 0; i < 11; i++) {
            TestHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalDelayInnerErrorAllFlowable() {
        TestSubscriberEx<Integer> ts = Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.error(new TestException());
            }
        }, true, Integer.MAX_VALUE).<Integer>toFlowable()
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(ts.errors().get(0));

        for (int i = 0; i < 10; i++) {
            TestHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalNonDelayErrorOuterFlowable() {
        Flowable.range(1, 10).concatWith(Flowable.<Integer>error(new TestException()))
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        }, false, Integer.MAX_VALUE).toFlowable()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedFlowable() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        }).<Integer>toFlowable()
        .subscribe(ts);

        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();
    }

    @Test
    public void normal() {
        Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void mapperThrows() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Void> to = pp
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        to.assertFailure(TestException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void mapperReturnsNull() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Void> to = pp
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return null;
            }
        })
        .test();

        assertTrue(pp.hasSubscribers());

        pp.onNext(1);

        to.assertFailure(NullPointerException.class);

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void normalDelayError() {
        Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        }, true, Integer.MAX_VALUE)
        .test()
        .assertResult();
    }

    @Test
    public void normalAsync() {
        Flowable.range(1, 1000)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Flowable.range(1, 100).subscribeOn(Schedulers.computation()).ignoreElements();
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void normalDelayErrorAll() {
        TestObserverEx<Void> to = Flowable.range(1, 10).concatWith(Flowable.<Integer>error(new TestException()))
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.error(new TestException());
            }
        }, true, Integer.MAX_VALUE)
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        for (int i = 0; i < 11; i++) {
            TestHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalDelayInnerErrorAll() {
        TestObserverEx<Void> to = Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.error(new TestException());
            }
        }, true, Integer.MAX_VALUE)
        .to(TestHelper.<Integer>testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        for (int i = 0; i < 10; i++) {
            TestHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalNonDelayErrorOuter() {
        Flowable.range(1, 10).concatWith(Flowable.<Integer>error(new TestException()))
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        }, false, Integer.MAX_VALUE)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        })
        .<Integer>toFlowable()
        .subscribe(ts);

        ts
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult();
    }

    @Test
    public void disposed() {
        TestHelper.checkDisposed(Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        }));
    }

    @Test
    public void normalAsyncMaxConcurrency() {
        Flowable.range(1, 1000)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Flowable.range(1, 100).subscribeOn(Schedulers.computation()).ignoreElements();
            }
        }, false, 3)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult();
    }

    @Test
    public void disposedFlowable() {
        TestHelper.checkDisposed(Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        }).toFlowable());
    }

    @Test
    public void badSource() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.flatMapCompletable(new Function<Integer, CompletableSource>() {
                    @Override
                    public CompletableSource apply(Integer v) throws Exception {
                        return Completable.complete();
                    }
                });
            }
        }, false, 1, null);
    }

    @Test
    public void fusedInternalsFlowable() {
        Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        })
        .toFlowable()
        .subscribe(new FlowableSubscriber<Object>() {
            @Override
            public void onSubscribe(Subscription s) {
                QueueSubscription<?> qs = (QueueSubscription<?>)s;
                try {
                    assertNull(qs.poll());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
                assertTrue(qs.isEmpty());
                qs.clear();
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
    }

    @Test
    public void innerObserverFlowable() {
        Flowable.range(1, 3)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return new Completable() {
                    @Override
                    protected void subscribeActual(CompletableObserver observer) {
                        observer.onSubscribe(Disposable.empty());

                        assertFalse(((Disposable)observer).isDisposed());

                        ((Disposable)observer).dispose();

                        assertTrue(((Disposable)observer).isDisposed());
                    }
                };
            }
        })
        .toFlowable()
        .test();
    }

    @Test
    public void badSourceFlowable() {
        TestHelper.checkBadSourceFlowable(new Function<Flowable<Integer>, Object>() {
            @Override
            public Object apply(Flowable<Integer> f) throws Exception {
                return f.flatMapCompletable(new Function<Integer, CompletableSource>() {
                    @Override
                    public CompletableSource apply(Integer v) throws Exception {
                        return Completable.complete();
                    }
                }).toFlowable();
            }
        }, false, 1, null);
    }

    @Test
    public void innerObserver() {
        Flowable.range(1, 3)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return new Completable() {
                    @Override
                    protected void subscribeActual(CompletableObserver observer) {
                        observer.onSubscribe(Disposable.empty());

                        assertFalse(((Disposable)observer).isDisposed());

                        ((Disposable)observer).dispose();

                        assertTrue(((Disposable)observer).isDisposed());
                    }
                };
            }
        })
        .test();
    }

    @Test
    public void delayErrorMaxConcurrency() {
        Flowable.range(1, 3)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                if (v == 2) {
                    return Completable.error(new TestException());
                }
                return Completable.complete();
            }
        }, true, 1)
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void asyncMaxConcurrency() {
        for (int itemCount = 1; itemCount <= 100000; itemCount *= 10) {
            for (int concurrency = 1; concurrency <= 256; concurrency *= 2) {
                Flowable.range(1, itemCount)
                .flatMapCompletable(
                        Functions.justFunction(Completable.complete()
                        .subscribeOn(Schedulers.computation()))
                        , false, concurrency)
                .test()
                .withTag("itemCount=" + itemCount + ", concurrency=" + concurrency)
                .awaitDone(5, TimeUnit.SECONDS)
                .assertResult();
            }
        }
    }

    @Test
    public void undeliverableUponCancel() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Completable>() {
            @Override
            public Completable apply(Flowable<Integer> upstream) {
                return upstream.flatMapCompletable(new Function<Integer, Completable>() {
                    @Override
                    public Completable apply(Integer v) throws Throwable {
                        return Completable.complete().hide();
                    }
                });
            }
        });
    }

    @Test
    public void undeliverableUponCancelDelayError() {
        TestHelper.checkUndeliverableUponCancel(new FlowableConverter<Integer, Completable>() {
            @Override
            public Completable apply(Flowable<Integer> upstream) {
                return upstream.flatMapCompletable(new Function<Integer, Completable>() {
                    @Override
                    public Completable apply(Integer v) throws Throwable {
                        return Completable.complete().hide();
                    }
                }, true, 2);
            }
        });
    }
}
