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

package io.reactivex.internal.operators.flowable;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.Function;
import io.reactivex.internal.fuseable.*;
import io.reactivex.observers.*;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.*;

public class FlowableFlatMapCompletableTest {

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
        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to = ps
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                throw new TestException();
            }
        }).<Integer>toFlowable()
        .test();

        assertTrue(ps.hasSubscribers());

        ps.onNext(1);

        to.assertFailure(TestException.class);

        assertFalse(ps.hasSubscribers());
    }

    @Test
    public void mapperReturnsNullFlowable() {
        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestSubscriber<Integer> to = ps
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return null;
            }
        }).<Integer>toFlowable()
        .test();

        assertTrue(ps.hasSubscribers());

        ps.onNext(1);

        to.assertFailure(NullPointerException.class);

        assertFalse(ps.hasSubscribers());
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
        TestSubscriber<Integer> to = Flowable.range(1, 10).concatWith(Flowable.<Integer>error(new TestException()))
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.error(new TestException());
            }
        }, true, Integer.MAX_VALUE).<Integer>toFlowable()
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        for (int i = 0; i < 11; i++) {
            TestHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalDelayInnerErrorAllFlowable() {
        TestSubscriber<Integer> to = Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.error(new TestException());
            }
        }, true, Integer.MAX_VALUE).<Integer>toFlowable()
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

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
        TestSubscriber<Integer> to = SubscriberFusion.newTest(QueueDisposable.ANY);

        Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.complete();
            }
        }).<Integer>toFlowable()
        .subscribe(to);

        to
        .assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC))
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
        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestObserver<Void> to = ps
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test();

        assertTrue(ps.hasSubscribers());

        ps.onNext(1);

        to.assertFailure(TestException.class);

        assertFalse(ps.hasSubscribers());
    }

    @Test
    public void mapperReturnsNull() {
        PublishProcessor<Integer> ps = PublishProcessor.create();

        TestObserver<Void> to = ps
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return null;
            }
        })
        .test();

        assertTrue(ps.hasSubscribers());

        ps.onNext(1);

        to.assertFailure(NullPointerException.class);

        assertFalse(ps.hasSubscribers());
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
        TestObserver<Void> to = Flowable.range(1, 10).concatWith(Flowable.<Integer>error(new TestException()))
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.error(new TestException());
            }
        }, true, Integer.MAX_VALUE)
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> errors = TestHelper.compositeList(to.errors().get(0));

        for (int i = 0; i < 11; i++) {
            TestHelper.assertError(errors, i, TestException.class);
        }
    }

    @Test
    public void normalDelayInnerErrorAll() {
        TestObserver<Void> to = Flowable.range(1, 10)
        .flatMapCompletable(new Function<Integer, CompletableSource>() {
            @Override
            public CompletableSource apply(Integer v) throws Exception {
                return Completable.error(new TestException());
            }
        }, true, Integer.MAX_VALUE)
        .test()
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
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueDisposable.ANY);

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
        .assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC))
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
            public Object apply(Flowable<Integer> o) throws Exception {
                return o.flatMapCompletable(new Function<Integer, CompletableSource>() {
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
            public void onSubscribe(Subscription d) {
                QueueSubscription<?> qd = (QueueSubscription<?>)d;
                try {
                    assertNull(qd.poll());
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                }
                assertTrue(qd.isEmpty());
                qd.clear();
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
                    protected void subscribeActual(CompletableObserver s) {
                        s.onSubscribe(Disposables.empty());

                        assertFalse(((Disposable)s).isDisposed());

                        ((Disposable)s).dispose();

                        assertTrue(((Disposable)s).isDisposed());
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
            public Object apply(Flowable<Integer> o) throws Exception {
                return o.flatMapCompletable(new Function<Integer, CompletableSource>() {
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
                    protected void subscribeActual(CompletableObserver s) {
                        s.onSubscribe(Disposables.empty());

                        assertFalse(((Disposable)s).isDisposed());

                        ((Disposable)s).dispose();

                        assertTrue(((Disposable)s).isDisposed());
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
}
