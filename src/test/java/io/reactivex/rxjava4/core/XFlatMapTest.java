/*
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

package io.reactivex.rxjava4.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Flow.Publisher;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava4.exceptions.TestException;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.observers.TestObserver;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.schedulers.Schedulers;
import io.reactivex.rxjava4.subscribers.TestSubscriber;
import io.reactivex.rxjava4.testsupport.TestHelper;

public class XFlatMapTest extends RxJavaTest {

    static final int SLEEP_AFTER_CANCEL = 500;

    final CyclicBarrier cb = new CyclicBarrier(2);

    void sleep() throws Exception {
        cb.await();
        try {
            long before = System.currentTimeMillis();
            Thread.sleep(5000);
            throw new IllegalStateException("Was not interrupted in time?! " + (System.currentTimeMillis() - before));
        } catch (InterruptedException ex) {
            // ignored here
        }
    }

    void beforeCancelSleep(TestSubscriber<?> ts) throws Exception {
        long before = System.currentTimeMillis();
        Thread.sleep(50);
        if (System.currentTimeMillis() - before > 100) {
            ts.cancel();
            throw new IllegalStateException("Overslept?" + (System.currentTimeMillis() - before));
        }
    }

    void beforeCancelSleep(TestObserver<?> to) throws Exception {
        long before = System.currentTimeMillis();
        Thread.sleep(50);
        if (System.currentTimeMillis() - before > 100) {
            to.dispose();
            throw new IllegalStateException("Overslept?" + (System.currentTimeMillis() - before));
        }
    }

    @Test
    public void flowableFlowable() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestSubscriber<Integer> ts = Flowable.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMap((Function<Integer, Publisher<Integer>>) _ -> {
                    sleep();
                    return Flowable.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(ts);

                ts.cancel();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                ts.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void flowableSingle() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestSubscriber<Integer> ts = Flowable.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapSingle(_ -> {
                    sleep();
                    return Single.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(ts);

                ts.cancel();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                ts.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void flowableMaybe() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestSubscriber<Integer> ts = Flowable.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapMaybe((Function<Integer, Maybe<Integer>>) _ -> {
                    sleep();
                    return Maybe.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(ts);

                ts.cancel();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                ts.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void flowableCompletable() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Void> to = Flowable.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapCompletable((Function<Integer, Completable>) _ -> {
                    sleep();
                    return Completable.error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void flowableCompletable2() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestSubscriber<Void> ts = Flowable.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapCompletable((Function<Integer, Completable>) _ -> {
                    sleep();
                    return Completable.error(new TestException());
                })
                .<Void>toFlowable()
                .test();

                cb.await();

                beforeCancelSleep(ts);

                ts.cancel();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                ts.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void observableObservable() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Observable.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMap((Function<Integer, Observable<Integer>>) _ -> {
                    sleep();
                    return Observable.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void observerSingle() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Observable.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapSingle((Function<Integer, Single<Integer>>) _ -> {
                    sleep();
                    return Single.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void observerMaybe() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Observable.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapMaybe((Function<Integer, Maybe<Integer>>) _ -> {
                    sleep();
                    return Maybe.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void observerCompletable() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Void> to = Observable.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapCompletable((Function<Integer, Completable>) _ -> {
                    sleep();
                    return Completable.error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void observerCompletable2() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Void> to = Observable.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapCompletable((Function<Integer, Completable>) _ -> {
                    sleep();
                    return Completable.error(new TestException());
                })
                .<Void>toObservable()
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void singleSingle() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Single.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMap((Function<Integer, Single<Integer>>) _ -> {
                    sleep();
                    return Single.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void singleMaybe() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Single.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapMaybe((Function<Integer, Maybe<Integer>>) _ -> {
                    sleep();
                    return Maybe.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void singleCompletable() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Void> to = Single.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapCompletable((Function<Integer, Completable>) _ -> {
                    sleep();
                    return Completable.error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void singleCompletable2() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Single.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapCompletable((Function<Integer, Completable>) _ -> {
                    sleep();
                    return Completable.error(new TestException());
                })
                .toSingleDefault(0)
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void singlePublisher() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestSubscriber<Integer> ts = Single.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapPublisher((Function<Integer, Publisher<Integer>>) _ -> {
                    sleep();
                    return Flowable.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(ts);

                ts.cancel();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                ts.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void singleCombiner() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Single.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMap((Function<Integer, Single<Integer>>) _ -> {
                    sleep();
                    return Single.<Integer>error(new TestException());
                }, Integer::sum)
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void singleObservable() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Single.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapObservable((Function<Integer, Observable<Integer>>) _ -> {
                    sleep();
                    return Observable.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void singleNotificationSuccess() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Single.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMap(
                    (Function<Integer, Single<Integer>>) _ -> {
                        sleep();
                        return Single.<Integer>error(new TestException());
                    },
                    (Function<Throwable, Single<Integer>>) _ -> {
                        sleep();
                        return Single.<Integer>error(new TestException());
                    }
                )
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void singleNotificationError() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Single.<Integer>error(new TestException())
                .subscribeOn(Schedulers.cached())
                .flatMap(
                    (Function<Integer, Single<Integer>>) _ -> {
                        sleep();
                        return Single.<Integer>error(new TestException());
                    },
                    (Function<Throwable, Single<Integer>>) _ -> {
                        sleep();
                        return Single.<Integer>error(new TestException());
                    }
                )
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void maybeSingle() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Maybe.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapSingle((Function<Integer, Single<Integer>>) _ -> {
                    sleep();
                    return Single.<Integer>error(new TestException());
                })
                .toSingle()
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void maybeSingle2() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Maybe.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapSingle((Function<Integer, Single<Integer>>) _ -> {
                    sleep();
                    return Single.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void maybeMaybe() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Maybe.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMap((Function<Integer, Maybe<Integer>>) _ -> {
                    sleep();
                    return Maybe.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void maybePublisher() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestSubscriber<Integer> ts = Maybe.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapPublisher((Function<Integer, Publisher<Integer>>) _ -> {
                    sleep();
                    return Flowable.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(ts);

                ts.cancel();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                ts.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void maybeObservable() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Maybe.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapObservable((Function<Integer, Observable<Integer>>) _ -> {
                    sleep();
                    return Observable.<Integer>error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void maybeNotificationSuccess() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Maybe.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMap(
                    (Function<Integer, Maybe<Integer>>) _ -> {
                        sleep();
                        return Maybe.<Integer>error(new TestException());
                    },
                    (Function<Throwable, Maybe<Integer>>) _ -> {
                        sleep();
                        return Maybe.<Integer>error(new TestException());
                    },
                    (Supplier<Maybe<Integer>>) () -> {
                        sleep();
                        return Maybe.<Integer>error(new TestException());
                    }
                )
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void maybeNotificationError() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Maybe.<Integer>error(new TestException())
                .subscribeOn(Schedulers.cached())
                .flatMap(
                    (Function<Integer, Maybe<Integer>>) _ -> {
                        sleep();
                        return Maybe.<Integer>error(new TestException());
                    },
                    (Function<Throwable, Maybe<Integer>>) _ -> {
                        sleep();
                        return Maybe.<Integer>error(new TestException());
                    },
                    (Supplier<Maybe<Integer>>) () -> {
                        sleep();
                        return Maybe.<Integer>error(new TestException());
                    }
                )
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void maybeNotificationEmpty() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Maybe.<Integer>empty()
                .subscribeOn(Schedulers.cached())
                .flatMap(
                    (Function<Integer, Maybe<Integer>>) _ -> {
                        sleep();
                        return Maybe.<Integer>error(new TestException());
                    },
                    (Function<Throwable, Maybe<Integer>>) _ -> {
                        sleep();
                        return Maybe.<Integer>error(new TestException());
                    },
                    (Supplier<Maybe<Integer>>) () -> {
                        sleep();
                        return Maybe.<Integer>error(new TestException());
                    }
                )
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void maybeCombiner() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Integer> to = Maybe.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMap((Function<Integer, Maybe<Integer>>) _ -> {
                    sleep();
                    return Maybe.<Integer>error(new TestException());
                }, Integer::sum)
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void maybeCompletable() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Void> to = Maybe.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapCompletable((Function<Integer, Completable>) _ -> {
                    sleep();
                    return Completable.error(new TestException());
                })
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }

    @Test
    public void maybeCompletable2() throws Exception {
        withRetry(3, () -> {
            List<Throwable> errors = TestHelper.trackPluginErrors();
            try {
                TestObserver<Void> to = Maybe.just(1)
                .subscribeOn(Schedulers.cached())
                .flatMapCompletable((Function<Integer, Completable>) _ -> {
                    sleep();
                    return Completable.error(new TestException());
                })
                .<Void>toMaybe()
                .test();

                cb.await();

                beforeCancelSleep(to);

                to.dispose();

                Thread.sleep(SLEEP_AFTER_CANCEL);

                to.assertEmpty();

                assertTrue(errors.isEmpty(), errors.toString());
            } finally {
                RxJavaPlugins.reset();
            }
        });
    }
}
