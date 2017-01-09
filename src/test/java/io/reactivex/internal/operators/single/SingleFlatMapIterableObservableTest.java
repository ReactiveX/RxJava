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

package io.reactivex.internal.operators.single;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.internal.fuseable.QueueDisposable;
import io.reactivex.internal.util.CrashingIterable;
import io.reactivex.observers.*;
import io.reactivex.schedulers.Schedulers;

public class SingleFlatMapIterableObservableTest {

    @Test
    public void normal() {

        Single.just(1).flattenAsObservable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Arrays.asList(v, v + 1);
            }
        })
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void emptyIterable() {

        Single.just(1).flattenAsObservable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Collections.<Integer>emptyList();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void error() {

        Single.<Integer>error(new TestException()).flattenAsObservable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Arrays.asList(v, v + 1);
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void take() {
        Single.just(1).flattenAsObservable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Arrays.asList(v, v + 1);
            }
        })
        .take(1)
        .test()
        .assertResult(1);
    }

    @Test
    public void fused() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.ANY);

        Single.just(1).flattenAsObservable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Arrays.asList(v, v + 1);
            }
        })
        .subscribe(to);

        to.assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.ASYNC))
        .assertResult(1, 2);
        ;
    }

    @Test
    public void fusedNoSync() {
        TestObserver<Integer> to = ObserverFusion.newTest(QueueDisposable.SYNC);

        Single.just(1).flattenAsObservable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return Arrays.asList(v, v + 1);
            }
        })
        .subscribe(to);

        to.assertOf(ObserverFusion.<Integer>assertFuseable())
        .assertOf(ObserverFusion.<Integer>assertFusionMode(QueueDisposable.NONE))
        .assertResult(1, 2);
        ;
    }

    @Test
    public void iteratorCrash() {

        Single.just(1).flattenAsObservable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return new CrashingIterable(1, 100, 100);
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "iterator()");
    }

    @Test
    public void hasNextCrash() {

        Single.just(1).flattenAsObservable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return new CrashingIterable(100, 1, 100);
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()");
    }

    @Test
    public void nextCrash() {

        Single.just(1).flattenAsObservable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return new CrashingIterable(100, 100, 1);
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "next()");
    }

    @Test
    public void hasNextCrash2() {

        Single.just(1).flattenAsObservable(new Function<Integer, Iterable<Integer>>() {
            @Override
            public Iterable<Integer> apply(Integer v) throws Exception {
                return new CrashingIterable(100, 2, 100);
            }
        })
        .test()
        .assertFailureAndMessage(TestException.class, "hasNext()", 0);
    }

    @Test
    public void doubleOnSubscribe() {
        TestHelper.checkDoubleOnSubscribeSingleToObservable(new Function<Single<Object>, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Single<Object> o) throws Exception {
                return o.flattenAsObservable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        return Collections.singleton(1);
                    }
                });
            }
        });
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Single.just(1).flattenAsObservable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        return Collections.singleton(1);
                    }
                }));
    }

    @Test
    public void async1() {
        Single.just(1)
        .flattenAsObservable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        Integer[] array = new Integer[1000 * 1000];
                        Arrays.fill(array, 1);
                        return Arrays.asList(array);
                    }
                })
        .hide()
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(1000 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void async2() {
        Single.just(1)
        .flattenAsObservable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        Integer[] array = new Integer[1000 * 1000];
                        Arrays.fill(array, 1);
                        return Arrays.asList(array);
                    }
                })
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(1000 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void async3() {
        Single.just(1)
        .flattenAsObservable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        Integer[] array = new Integer[1000 * 1000];
                        Arrays.fill(array, 1);
                        return Arrays.asList(array);
                    }
                })
        .take(500 * 1000)
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(500 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void async4() {
        Single.just(1)
        .flattenAsObservable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        Integer[] array = new Integer[1000 * 1000];
                        Arrays.fill(array, 1);
                        return Arrays.asList(array);
                    }
                })
        .observeOn(Schedulers.single())
        .take(500 * 1000)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertSubscribed()
        .assertValueCount(500 * 1000)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void fusedEmptyCheck() {
        Single.just(1)
        .flattenAsObservable(new Function<Object, Iterable<Integer>>() {
                    @Override
                    public Iterable<Integer> apply(Object v) throws Exception {
                        return Arrays.asList(1, 2, 3);
                    }
        }).subscribe(new Observer<Integer>() {
            QueueDisposable<Integer> qd;
            @SuppressWarnings("unchecked")
            @Override
            public void onSubscribe(Disposable d) {
                qd = (QueueDisposable<Integer>)d;

                assertEquals(QueueDisposable.ASYNC, qd.requestFusion(QueueDisposable.ANY));
            }

            @Override
            public void onNext(Integer value) {
                assertFalse(qd.isEmpty());

                qd.clear();

                assertTrue(qd.isEmpty());

                qd.dispose();
            }

            @Override
            public void onError(Throwable e) {
            }

            @Override
            public void onComplete() {
            }
        });
    }
}
