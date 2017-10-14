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

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CyclicBarrier;

import org.junit.*;
import org.reactivestreams.Publisher;

import io.reactivex.exceptions.TestException;
import io.reactivex.functions.Function;
import io.reactivex.observers.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;

public class XFlatMapTest {

    @Rule
    public Retry retry = new Retry(5, 1000, true);

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

    void beforeCancelSleep(BaseTestConsumer<?, ?> ts) throws Exception {
        long before = System.currentTimeMillis();
        Thread.sleep(50);
        if (System.currentTimeMillis() - before > 100) {
            ts.dispose();
            throw new IllegalStateException("Overslept?" + (System.currentTimeMillis() - before));
        }
    }

    @Test
    public void flowableFlowable() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestSubscriber<Integer> ts = Flowable.just(1)
            .subscribeOn(Schedulers.io())
            .flatMap(new Function<Integer, Publisher<Integer>>() {
                @Override
                public Publisher<Integer> apply(Integer v) throws Exception {
                    sleep();
                    return Flowable.<Integer>error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void flowableSingle() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestSubscriber<Integer> ts = Flowable.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapSingle(new Function<Integer, Single<Integer>>() {
                @Override
                public Single<Integer> apply(Integer v) throws Exception {
                    sleep();
                    return Single.<Integer>error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void flowableMaybe() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestSubscriber<Integer> ts = Flowable.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapMaybe(new Function<Integer, Maybe<Integer>>() {
                @Override
                public Maybe<Integer> apply(Integer v) throws Exception {
                    sleep();
                    return Maybe.<Integer>error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void flowableCompletable() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Void> ts = Flowable.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(new Function<Integer, Completable>() {
                @Override
                public Completable apply(Integer v) throws Exception {
                    sleep();
                    return Completable.error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void flowableCompletable2() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestSubscriber<Void> ts = Flowable.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(new Function<Integer, Completable>() {
                @Override
                public Completable apply(Integer v) throws Exception {
                    sleep();
                    return Completable.error(new TestException());
                }
            })
            .<Void>toFlowable()
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void observableFlowable() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Integer> ts = Observable.just(1)
            .subscribeOn(Schedulers.io())
            .flatMap(new Function<Integer, Observable<Integer>>() {
                @Override
                public Observable<Integer> apply(Integer v) throws Exception {
                    sleep();
                    return Observable.<Integer>error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void observerSingle() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Integer> ts = Observable.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapSingle(new Function<Integer, Single<Integer>>() {
                @Override
                public Single<Integer> apply(Integer v) throws Exception {
                    sleep();
                    return Single.<Integer>error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void observerMaybe() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Integer> ts = Observable.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapMaybe(new Function<Integer, Maybe<Integer>>() {
                @Override
                public Maybe<Integer> apply(Integer v) throws Exception {
                    sleep();
                    return Maybe.<Integer>error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void observerCompletable() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Void> ts = Observable.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(new Function<Integer, Completable>() {
                @Override
                public Completable apply(Integer v) throws Exception {
                    sleep();
                    return Completable.error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void observerCompletable2() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Void> ts = Observable.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(new Function<Integer, Completable>() {
                @Override
                public Completable apply(Integer v) throws Exception {
                    sleep();
                    return Completable.error(new TestException());
                }
            })
            .<Void>toObservable()
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void singleSingle() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Integer> ts = Single.just(1)
            .subscribeOn(Schedulers.io())
            .flatMap(new Function<Integer, Single<Integer>>() {
                @Override
                public Single<Integer> apply(Integer v) throws Exception {
                    sleep();
                    return Single.<Integer>error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void singleMaybe() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Integer> ts = Single.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapMaybe(new Function<Integer, Maybe<Integer>>() {
                @Override
                public Maybe<Integer> apply(Integer v) throws Exception {
                    sleep();
                    return Maybe.<Integer>error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void singleCompletable() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Void> ts = Single.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(new Function<Integer, Completable>() {
                @Override
                public Completable apply(Integer v) throws Exception {
                    sleep();
                    return Completable.error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void singleCompletable2() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Integer> ts = Single.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(new Function<Integer, Completable>() {
                @Override
                public Completable apply(Integer v) throws Exception {
                    sleep();
                    return Completable.error(new TestException());
                }
            })
            .toSingleDefault(0)
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void maybeSingle() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Integer> ts = Maybe.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapSingle(new Function<Integer, Single<Integer>>() {
                @Override
                public Single<Integer> apply(Integer v) throws Exception {
                    sleep();
                    return Single.<Integer>error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void maybeMaybe() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Integer> ts = Maybe.just(1)
            .subscribeOn(Schedulers.io())
            .flatMap(new Function<Integer, Maybe<Integer>>() {
                @Override
                public Maybe<Integer> apply(Integer v) throws Exception {
                    sleep();
                    return Maybe.<Integer>error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void maybeCompletable() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Void> ts = Maybe.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(new Function<Integer, Completable>() {
                @Override
                public Completable apply(Integer v) throws Exception {
                    sleep();
                    return Completable.error(new TestException());
                }
            })
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void maybeCompletable2() throws Exception {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            TestObserver<Void> ts = Maybe.just(1)
            .subscribeOn(Schedulers.io())
            .flatMapCompletable(new Function<Integer, Completable>() {
                @Override
                public Completable apply(Integer v) throws Exception {
                    sleep();
                    return Completable.error(new TestException());
                }
            })
            .<Void>toMaybe()
            .test();

            cb.await();

            beforeCancelSleep(ts);

            ts.cancel();

            Thread.sleep(SLEEP_AFTER_CANCEL);

            ts.assertEmpty();

            assertTrue(errors.toString(), errors.isEmpty());
        } finally {
            RxJavaPlugins.reset();
        }
    }
}
