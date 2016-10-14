/**
 * Copyright 2016 Netflix, Inc.
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

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Test;

import io.reactivex.*;
import io.reactivex.disposables.Disposables;
import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.*;

public class ObservableConcatMapTest {

    @Test
    public void asyncFused() {
        UnicastSubject<Integer> us = UnicastSubject.create();

        TestObserver<Integer> to = us.concatMap(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.range(v, 2);
            }
        })
        .test();

        us.onNext(1);
        us.onComplete();

        to.assertResult(1, 2);
    }

    @Test
    public void dispose() {
        TestHelper.checkDisposed(Observable.<Integer>just(1).hide()
        .concatMap(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.error(new TestException());
            }
        }));
    }

    @Test
    public void dispose2() {
        TestHelper.checkDisposed(Observable.<Integer>just(1).hide()
        .concatMapDelayError(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.error(new TestException());
            }
        }));
    }

    @Test
    public void mainError() {
        Observable.<Integer>error(new TestException())
        .concatMap(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.range(v, 2);
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerError() {
        Observable.<Integer>just(1).hide()
        .concatMap(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mainErrorDelayed() {
        Observable.<Integer>error(new TestException())
        .concatMapDelayError(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.range(v, 2);
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerErrorDelayError() {
        Observable.<Integer>just(1).hide()
        .concatMapDelayError(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void innerErrorDelayError2() {
        Observable.<Integer>just(1).hide()
        .concatMapDelayError(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.fromCallable(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        throw new TestException();
                    }
                });
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badSource() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());

                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .concatMap(new Function<Integer, ObservableSource<Integer>>() {
                @Override
                public ObservableSource<Integer> apply(Integer v) throws Exception {
                    return Observable.range(v, 2);
                }
            })
            .test()
            .assertResult(1, 2);

            TestHelper.assertError(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void badSourceDelayError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            new Observable<Integer>() {
                @Override
                protected void subscribeActual(Observer<? super Integer> observer) {
                    observer.onSubscribe(Disposables.empty());

                    observer.onNext(1);
                    observer.onComplete();
                    observer.onNext(2);
                    observer.onError(new TestException());
                    observer.onComplete();
                }
            }
            .concatMapDelayError(new Function<Integer, ObservableSource<Integer>>() {
                @Override
                public ObservableSource<Integer> apply(Integer v) throws Exception {
                    return Observable.range(v, 2);
                }
            })
            .test()
            .assertResult(1, 2);

            TestHelper.assertError(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void normalDelayErrors() {
        Observable.just(1).hide()
        .concatMapDelayError(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.range(v, 2);
            }
        })
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void normalDelayErrorsTillTheEnd() {
        Observable.just(1).hide()
        .concatMapDelayError(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.range(v, 2);
            }
        }, 16, true)
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void onErrorRace() {
        for (int i = 0; i < 500; i++) {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                final PublishSubject<Integer> ps1 = PublishSubject.create();
                final PublishSubject<Integer> ps2 = PublishSubject.create();

                TestObserver<Integer> to = ps1.concatMap(new Function<Integer, ObservableSource<Integer>>() {
                    @Override
                    public ObservableSource<Integer> apply(Integer v) throws Exception {
                        return ps2;
                    }
                }).test();

                final TestException ex1 = new TestException();
                final TestException ex2 = new TestException();

                Runnable r1 = new Runnable() {
                    @Override
                    public void run() {
                        ps1.onError(ex1);
                    }
                };
                Runnable r2 = new Runnable() {
                    @Override
                    public void run() {
                        ps2.onError(ex2);
                    }
                };

                TestHelper.race(r1, r2, Schedulers.single());

                to.assertFailure(TestException.class);

                if (!errors.isEmpty()) {
                    TestHelper.assertError(errors, 0, TestException.class);
                }
            } finally {
                RxJavaPlugins.reset();
            }
        }
    }

    @Test
    public void mapperThrows() {
        Observable.just(1).hide()
        .concatMap(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedPollThrows() {
        Observable.just(1)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .concatMap(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.range(v, 2);
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fusedPollThrowsDelayError() {
        Observable.just(1)
        .map(new Function<Integer, Integer>() {
            @Override
            public Integer apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .concatMapDelayError(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                return Observable.range(v, 2);
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mapperThrowsDelayError() {
        Observable.just(1).hide()
        .concatMapDelayError(new Function<Integer, ObservableSource<Integer>>() {
            @Override
            public ObservableSource<Integer> apply(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void badInnerDelayError() {
        @SuppressWarnings("rawtypes")
        final Observer[] o = { null };

        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Observable.just(1).hide()
            .concatMapDelayError(new Function<Integer, ObservableSource<Integer>>() {
                @Override
                public ObservableSource<Integer> apply(Integer v) throws Exception {
                    return new Observable<Integer>() {
                        @Override
                        protected void subscribeActual(Observer<? super Integer> observer) {
                            o[0] = observer;
                            observer.onSubscribe(Disposables.empty());
                            observer.onComplete();
                        }
                    };
                }
            })
            .test()
            .assertResult();

            o[0].onError(new TestException());

            TestHelper.assertError(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
