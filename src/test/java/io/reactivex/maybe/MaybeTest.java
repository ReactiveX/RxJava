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

package io.reactivex.maybe;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.*;
import io.reactivex.Observable;
import io.reactivex.disposables.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.fuseable.QueueSubscription;
import io.reactivex.internal.operators.flowable.FlowableZipTest.ArgsToString;
import io.reactivex.internal.operators.maybe.*;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subscribers.*;

public class MaybeTest {
    @Test
    public void fromFlowableEmpty() {

        Flowable.empty()
        .singleElement()
        .test()
        .assertResult();
    }

    @Test
    public void fromFlowableJust() {

        Flowable.just(1)
        .singleElement()
        .test()
        .assertResult(1);
    }

    @Test
    public void fromFlowableError() {

        Flowable.error(new TestException())
        .singleElement()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fromFlowableValueAndError() {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .singleElement()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fromFlowableMany() {

        Flowable.range(1, 2)
        .singleElement()
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void fromFlowableDisposeComposesThrough() {
        PublishProcessor<Integer> pp = PublishProcessor.create();

        TestObserver<Integer> ts = pp.singleElement().test();

        assertTrue(pp.hasSubscribers());

        ts.cancel();

        assertFalse(pp.hasSubscribers());
    }

    @Test
    public void fromObservableEmpty() {

        Observable.empty()
        .singleElement()
        .test()
        .assertResult();
    }

    @Test
    public void fromObservableJust() {

        Observable.just(1)
        .singleElement()
        .test()
        .assertResult(1);
    }

    @Test
    public void fromObservableError() {

        Observable.error(new TestException())
        .singleElement()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fromObservableValueAndError() {
        Flowable.just(1).concatWith(Flowable.<Integer>error(new TestException()))
        .singleElement()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fromObservableMany() {

        Observable.range(1, 2)
        .singleElement()
        .test()
        .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void fromObservableDisposeComposesThrough() {
        PublishSubject<Integer> pp = PublishSubject.create();

        TestObserver<Integer> ts = pp.singleElement().test(false);

        assertTrue(pp.hasObservers());

        ts.cancel();

        assertFalse(pp.hasObservers());
    }

    @Test
    public void fromObservableDisposeComposesThroughImmediatelyCancelled() {
        PublishSubject<Integer> pp = PublishSubject.create();

        pp.singleElement().test(true);

        assertFalse(pp.hasObservers());
    }

    @Test
    public void just() {
        Maybe.just(1)
        .test()
        .assertResult(1);
    }

    @Test(expected = NullPointerException.class)
    public void justNull() {
        Maybe.just(null);
    }

    @Test
    public void empty() {
        Maybe.empty()
        .test()
        .assertResult();
    }

    @Test
    public void never() {
        Maybe.never()
        .test()
        .assertSubscribed()
        .assertNoValues()
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test(expected = NullPointerException.class)
    public void errorNull() {
        Maybe.error((Throwable)null);
    }

    @Test(expected = NullPointerException.class)
    public void errorCallableNull() {
        Maybe.error((Callable<Throwable>)null);
    }

    @Test
    public void error() {
        Maybe.error(new TestException())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorCallable() {
        Maybe.error(Functions.justCallable(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorCallableReturnsNull() {
        Maybe.error(Functions.justCallable((Throwable)null))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void wrapCustom() {
        Maybe.wrap(new MaybeSource<Integer>() {
            @Override
            public void subscribe(MaybeObserver<? super Integer> observer) {
                observer.onSubscribe(Disposables.empty());
                observer.onSuccess(1);
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void wrapMaybe() {
        assertSame(Maybe.empty(), Maybe.wrap(Maybe.empty()));
    }

    @Test(expected = NullPointerException.class)
    public void wrapNull() {
        Maybe.wrap(null);
    }

    @Test
    public void emptySingleton() {
        assertSame(Maybe.empty(), Maybe.empty());
    }

    @Test
    public void neverSingleton() {
        assertSame(Maybe.never(), Maybe.never());
    }

    @Test(expected = NullPointerException.class)
    public void liftNull() {
        Maybe.just(1).lift(null);
    }

    @Test
    public void liftJust() {
        Maybe.just(1).lift(new MaybeOperator<Integer, Integer>() {
            @Override
            public MaybeObserver<? super Integer> apply(MaybeObserver<? super Integer> t) throws Exception {
                return t;
            }
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void liftThrows() {
        Maybe.just(1).lift(new MaybeOperator<Integer, Integer>() {
            @Override
            public MaybeObserver<? super Integer> apply(MaybeObserver<? super Integer> t) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test(expected = NullPointerException.class)
    public void deferNull() {
        Maybe.defer(null);
    }

    @Test
    public void deferThrows() {
        Maybe.defer(new Callable<Maybe<Integer>>() {
            @Override
            public Maybe<Integer> call() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void deferReturnsNull() {
        Maybe.defer(new Callable<Maybe<Integer>>() {
            @Override
            public Maybe<Integer> call() throws Exception {
                return null;
            }
        })
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void defer() {
        Maybe<Integer> source = Maybe.defer(new Callable<Maybe<Integer>>() {
            int count;
            @Override
            public Maybe<Integer> call() throws Exception {
                return Maybe.just(count++);
            }
        });

        for (int i = 0; i < 128; i++) {
            source.test().assertResult(i);
        }
    }

    @Test
    public void flowableMaybeFlowable() {
        Flowable.just(1).singleElement().toFlowable().test().assertResult(1);
    }

    @Test
    public void obervableMaybeobervable() {
        Observable.just(1).singleElement().toObservable().test().assertResult(1);
    }

    @Test
    public void singleMaybeSingle() {
        Single.just(1).toMaybe().toSingle().test().assertResult(1);
    }

    @Test
    public void completableMaybeCompletable() {
        Completable.complete().toMaybe().ignoreElement().test().assertResult();
    }


    @Test
    public void unsafeCreate() {
        Maybe.unsafeCreate(new MaybeSource<Integer>() {
            @Override
            public void subscribe(MaybeObserver<? super Integer> observer) {
                observer.onSubscribe(Disposables.empty());
                observer.onSuccess(1);
            }
        })
        .test()
        .assertResult(1);
    }

    @Test(expected = NullPointerException.class)
    public void unsafeCreateNull() {
        Maybe.unsafeCreate(null);
    }

    @Test
    public void to() {
        Maybe.just(1).to(new Function<Maybe<Integer>, Flowable<Integer>>() {
            @Override
            public Flowable<Integer> apply(Maybe<Integer> v) throws Exception {
                return v.toFlowable();
            }
        })
        .test()
        .assertResult(1);
    }

    @Test(expected = NullPointerException.class)
    public void toNull() {
        Maybe.just(1).to(null);
    }

    @Test
    public void compose() {
        Maybe.just(1).compose(new MaybeTransformer<Integer, Integer>() {
            @Override
            public MaybeSource<Integer> apply(Maybe<Integer> m) {
                return m.map(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(Integer w) throws Exception {
                        return w + 1;
                    }
                });
            }
        })
        .test()
        .assertResult(2);
    }

    @Test(expected = NullPointerException.class)
    public void composeNull() {
        Maybe.just(1).compose(null);
    }

    @Test(expected = NullPointerException.class)
    public void mapNull() {
        Maybe.just(1).map(null);
    }

    @Test
    public void mapReturnNull() {
        Maybe.just(1).map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                return null;
            }
        }).test().assertFailure(NullPointerException.class);
    }

    @Test
    public void mapThrows() {
        Maybe.just(1).map(new Function<Integer, Object>() {
            @Override
            public Object apply(Integer v) throws Exception {
                throw new IOException();
            }
        }).test().assertFailure(IOException.class);
    }

    @Test
    public void map() {
        Maybe.just(1).map(new Function<Integer, String>() {
            @Override
            public String apply(Integer v) throws Exception {
                return v.toString();
            }
        }).test().assertResult("1");
    }

    @Test(expected = NullPointerException.class)
    public void filterNull() {
        Maybe.just(1).filter(null);
    }

    @Test
    public void filterThrows() {
        Maybe.just(1).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                throw new IOException();
            }
        }).test().assertFailure(IOException.class);
    }

    @Test
    public void filterTrue() {
        Maybe.just(1).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 1;
            }
        }).test().assertResult(1);
    }

    @Test
    public void filterFalse() {
        Maybe.just(2).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 1;
            }
        }).test().assertResult();
    }

    @Test
    public void filterEmpty() {
        Maybe.<Integer>empty().filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 1;
            }
        }).test().assertResult();
    }

    @Test(expected = NullPointerException.class)
    public void singleFilterNull() {
        Single.just(1).filter(null);
    }

    @Test
    public void singleFilterThrows() {
        Single.just(1).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                throw new IOException();
            }
        }).test().assertFailure(IOException.class);
    }

    @Test
    public void singleFilterTrue() {
        Single.just(1).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 1;
            }
        }).test().assertResult(1);
    }

    @Test
    public void singleFilterFalse() {
        Single.just(2).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer v) throws Exception {
                return v == 1;
            }
        }).test().assertResult();
    }

    @Test
    public void cast() {
        TestObserver<Number> ts = Maybe.just(1).cast(Number.class).test();
        // don'n inline this due to the generic type
        ts.assertResult((Number)1);
    }

    @Test(expected = NullPointerException.class)
    public void observeOnNull() {
        Maybe.just(1).observeOn(null);
    }

    @Test(expected = NullPointerException.class)
    public void subscribeOnNull() {
        Maybe.just(1).observeOn(null);
    }

    @Test
    public void observeOnSuccess() {
        String main = Thread.currentThread().getName();
        Maybe.just(1)
        .observeOn(Schedulers.single())
        .map(new Function<Integer, String>() {
            @Override
            public String apply(Integer v) throws Exception {
                return v + ": " + Thread.currentThread().getName();
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertOf(TestHelper.observerSingleNot("1: " + main))
        ;
    }

    @Test
    public void observeOnError() {
        Maybe.error(new TestException())
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        ;
    }

    @Test
    public void observeOnComplete() {
        Maybe.empty()
        .observeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult()
        ;
    }

    @Test
    public void observeOnDispose2() {
        TestHelper.checkDisposed(Maybe.empty().observeOn(Schedulers.single()));
    }

    @Test
    public void observeOnDoubleSubscribe() {
        TestHelper.checkDoubleOnSubscribeMaybe(new Function<Maybe<Object>, MaybeSource<Object>>() {
            @Override
            public MaybeSource<Object> apply(Maybe<Object> m) throws Exception {
                return m.observeOn(Schedulers.single());
            }
        });
    }

    @Test
    public void subscribeOnSuccess() {
        String main = Thread.currentThread().getName();
        Maybe.fromCallable(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return Thread.currentThread().getName();
            }
        })
        .subscribeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertOf(TestHelper.observerSingleNot(main))
        ;
    }

    @Test
    public void observeOnErrorThread() {
        String main = Thread.currentThread().getName();

        final String[] name = { null };

        Maybe.error(new TestException()).observeOn(Schedulers.single())
        .doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                name[0] = Thread.currentThread().getName();
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        ;

        assertNotEquals(main, name[0]);
    }


    @Test
    public void observeOnCompleteThread() {
        String main = Thread.currentThread().getName();

        final String[] name = { null };

        Maybe.empty().observeOn(Schedulers.single())
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                name[0] = Thread.currentThread().getName();
            }
        })
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult()
        ;

        assertNotEquals(main, name[0]);
    }

    @Test
    public void subscribeOnError() {
        Maybe.error(new TestException())
        .subscribeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        ;
    }

    @Test
    public void subscribeOnComplete() {
        Maybe.empty()
        .subscribeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult()
        ;
    }


    @Test
    public void fromAction() {
        final int[] call = { 0 };

        Maybe.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                call[0]++;
            }
        })
        .test()
        .assertResult();

        assertEquals(1, call[0]);
    }

    @Test
    public void fromActionThrows() {
        Maybe.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fromRunnable() {
        final int[] call = { 0 };

        Maybe.fromRunnable(new Runnable() {
            @Override
            public void run() {
                call[0]++;
            }
        })
        .test()
        .assertResult();

        assertEquals(1, call[0]);
    }

    @Test
    public void fromRunnableThrows() {
        Maybe.fromRunnable(new Runnable() {
            @Override
            public void run() {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fromCallableThrows() {
        Maybe.fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnSuccess() {
        final Integer[] value = { null };

        Maybe.just(1).doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                value[0] = v;
            }
        })
        .test()
        .assertResult(1);

        assertEquals(1, value[0].intValue());
    }

    @Test
    public void doOnSuccessEmpty() {
        final Integer[] value = { null };

        Maybe.<Integer>empty().doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                value[0] = v;
            }
        })
        .test()
        .assertResult();

        assertNull(value[0]);
    }

    @Test
    public void doOnSuccessThrows() {
        Maybe.just(1).doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }


    @Test
    public void doOnSubscribe() {
        final Disposable[] value = { null };

        Maybe.just(1).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable v) throws Exception {
                value[0] = v;
            }
        })
        .test()
        .assertResult(1);

        assertNotNull(value[0]);
    }

    @Test
    public void doOnSubscribeThrows() {
        Maybe.just(1).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable v) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }


    @Test
    public void doOnCompleteThrows() {
        Maybe.empty().doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnDispose() {
        final int[] call = { 0 };

        Maybe.just(1).doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                call[0]++;
            }
        })
        .test(true)
        .assertSubscribed()
        .assertNoValues()
        .assertNoErrors()
        .assertNotComplete();

        assertEquals(1, call[0]);
    }


    @Test
    public void doOnDisposeThrows() {
        List<Throwable> list = TestHelper.trackPluginErrors();

        try {
            PublishProcessor<Integer> pp = PublishProcessor.create();

            TestObserver<Integer> ts = pp.singleElement().doOnDispose(new Action() {
                @Override
                public void run() throws Exception {
                    throw new TestException();
                }
            })
            .test();

            assertTrue(pp.hasSubscribers());

            ts.cancel();

            assertFalse(pp.hasSubscribers());

            ts.assertSubscribed()
            .assertNoValues()
            .assertNoErrors()
            .assertNotComplete();

            TestHelper.assertUndeliverable(list, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void observeOnDispose() throws Exception {
        final TestSubscriber<Integer> ts = new TestSubscriber<Integer>();

        final CountDownLatch cdl = new CountDownLatch(1);

        Maybe.just(1)
        .observeOn(Schedulers.single())
        .doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                if (!cdl.await(5, TimeUnit.SECONDS)) {
                    throw new TimeoutException();
                }
            }
        })
        .toFlowable().subscribe(ts);

        Thread.sleep(250);

        ts.cancel();

        ts.awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(InterruptedException.class);
    }

    @Test
    public void doAfterTerminateSuccess() {
        final int[] call = { 0 };

        Maybe.just(1)
        .doOnSuccess(new Consumer<Integer>() {
            @Override
            public void accept(Integer v) throws Exception {
                call[0]++;
            }
        })
        .doAfterTerminate(new Action() {
            @Override
            public void run() throws Exception {
                if (call[0] == 1) {
                    call[0] = -1;
                }
            }
        })
        .test()
        .assertResult(1);

        assertEquals(-1, call[0]);
    }

    @Test
    public void doAfterTerminateError() {
        final int[] call = { 0 };

        Maybe.error(new TestException())
        .doOnError(new Consumer<Object>() {
            @Override
            public void accept(Object v) throws Exception {
                call[0]++;
            }
        })
        .doAfterTerminate(new Action() {
            @Override
            public void run() throws Exception {
                if (call[0] == 1) {
                    call[0] = -1;
                }
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertEquals(-1, call[0]);
    }


    @Test
    public void doAfterTerminateComplete() {
        final int[] call = { 0 };

        Maybe.empty()
        .doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                call[0]++;
            }
        })
        .doAfterTerminate(new Action() {
            @Override
            public void run() throws Exception {
                if (call[0] == 1) {
                    call[0] = -1;
                }
            }
        })
        .test()
        .assertResult();

        assertEquals(-1, call[0]);
    }

    @Test
    public void sourceThrowsNPE() {
        try {
            Maybe.unsafeCreate(new MaybeSource<Object>() {
                @Override
                public void subscribe(MaybeObserver<? super Object> s) {
                    throw new NullPointerException("Forced failure");
                }
            }).test();

            fail("Should have thrown!");
        } catch (NullPointerException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }


    @Test
    public void sourceThrowsIAE() {
        try {
            Maybe.unsafeCreate(new MaybeSource<Object>() {
                @Override
                public void subscribe(MaybeObserver<? super Object> s) {
                    throw new IllegalArgumentException("Forced failure");
                }
            }).test();

            fail("Should have thrown!");
        } catch (NullPointerException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof IllegalArgumentException);
            assertEquals("Forced failure", ex.getCause().getMessage());
        }
    }

    @Test
    public void flatMap() {
        Maybe.just(1).flatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v * 10);
            }
        })
        .test()
        .assertResult(10);
    }

    @Test
    public void concatMap() {
        Maybe.just(1).concatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v * 10);
            }
        })
        .test()
        .assertResult(10);
    }

    @Test
    public void flatMapEmpty() {
        Maybe.just(1).flatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.empty();
            }
        })
        .test()
        .assertResult();
    }

    @Test
    public void flatMapError() {
        Maybe.just(1).flatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.error(new TestException());
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void flatMapNotifySuccess() {
        Maybe.just(1)
        .flatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v * 10);
            }
        },
        new Function<Throwable, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Throwable v) throws Exception {
                return Maybe.just(100);
            }
        },

        new Callable<MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> call() throws Exception {
                return Maybe.just(200);
            }
        })
        .test()
        .assertResult(10);
    }

    @Test
    public void flatMapNotifyError() {
        Maybe.<Integer>error(new TestException())
        .flatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v * 10);
            }
        },
        new Function<Throwable, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Throwable v) throws Exception {
                return Maybe.just(100);
            }
        },

        new Callable<MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> call() throws Exception {
                return Maybe.just(200);
            }
        })
        .test()
        .assertResult(100);
    }

    @Test
    public void flatMapNotifyComplete() {
        Maybe.<Integer>empty()
        .flatMap(new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v * 10);
            }
        },
        new Function<Throwable, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Throwable v) throws Exception {
                return Maybe.just(100);
            }
        },

        new Callable<MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> call() throws Exception {
                return Maybe.just(200);
            }
        })
        .test()
        .assertResult(200);
    }

    @Test
    public void ignoreElementSuccess() {
        Maybe.just(1)
        .ignoreElement()
        .test()
        .assertResult();
    }

    @Test
    public void ignoreElementError() {
        Maybe.error(new TestException())
        .ignoreElement()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void ignoreElementComplete() {
        Maybe.empty()
        .ignoreElement()
        .test()
        .assertResult();
    }
    @Test
    public void ignoreElementSuccessMaybe() {
        Maybe.just(1)
        .ignoreElement()
        .toMaybe()
        .test()
        .assertResult();
    }

    @Test
    public void ignoreElementErrorMaybe() {
        Maybe.error(new TestException())
        .ignoreElement()
        .toMaybe()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void ignoreElementCompleteMaybe() {
        Maybe.empty()
        .ignoreElement()
        .toMaybe()
        .test()
        .assertResult();
    }

    @Test
    public void singleToMaybe() {
        Single.just(1)
        .toMaybe()
        .test()
        .assertResult(1);
    }

    @Test
    public void singleToMaybeError() {
        Single.error(new TestException())
        .toMaybe()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void completableToMaybe() {
        Completable.complete()
        .toMaybe()
        .test()
        .assertResult();
    }

    @Test
    public void completableToMaybeError() {
        Completable.error(new TestException())
        .toMaybe()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyToSingle() {
        Maybe.empty()
        .toSingle()
        .test()
        .assertFailure(NoSuchElementException.class);
    }

    @Test
    public void errorToSingle() {
        Maybe.error(new TestException())
        .toSingle()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void emptyToCompletable() {
        Maybe.empty()
        .ignoreElement()
        .test()
        .assertResult();
    }

    @Test
    public void errorToCompletable() {
        Maybe.error(new TestException())
        .ignoreElement()
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void concat2() {
        Maybe.concat(Maybe.just(1), Maybe.just(2))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concat2Empty() {
        Maybe.concat(Maybe.empty(), Maybe.empty())
        .test()
        .assertResult();
    }

    @Test
    public void concat2Backpressured() {
        TestSubscriber<Integer> ts = Maybe.concat(Maybe.just(1), Maybe.just(2))
        .test(0L);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(1);

        ts.request(1);

        ts.assertResult(1, 2);
    }

    @Test
    public void concat2BackpressuredNonEager() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = Maybe.concat(pp1.singleElement(), pp2.singleElement())
        .test(0L);

        assertTrue(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertEmpty();

        ts.request(1);

        assertTrue(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        pp1.onNext(1);
        pp1.onComplete();

        ts.assertValue(1);

        assertFalse(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        ts.request(1);

        ts.assertValue(1);

        pp2.onNext(2);
        pp2.onComplete();

        ts.assertResult(1, 2);

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());
    }

    @Test
    public void concat3() {
        Maybe.concat(Maybe.just(1), Maybe.just(2), Maybe.just(3))
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void concat3Empty() {
        Maybe.concat(Maybe.empty(), Maybe.empty(), Maybe.empty())
        .test()
        .assertResult();
    }

    @Test
    public void concat3Mixed1() {
        Maybe.concat(Maybe.just(1), Maybe.empty(), Maybe.just(3))
        .test()
        .assertResult(1, 3);
    }

    @Test
    public void concat3Mixed2() {
        Maybe.concat(Maybe.just(1), Maybe.just(2), Maybe.empty())
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concat3Backpressured() {
        TestSubscriber<Integer> ts = Maybe.concat(Maybe.just(1), Maybe.just(2), Maybe.just(3))
        .test(0L);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(1);

        ts.request(2);

        ts.assertResult(1, 2, 3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayZero() {
        assertSame(Flowable.empty(), Maybe.concatArray());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayOne() {
        Maybe.concatArray(Maybe.just(1)).test().assertResult(1);
    }

    @Test
    public void concat4() {
        Maybe.concat(Maybe.just(1), Maybe.just(2), Maybe.just(3), Maybe.just(4))
        .test()
        .assertResult(1, 2, 3, 4);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterable() {
        Maybe.concat(Arrays.asList(Maybe.just(1), Maybe.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableEmpty() {
        Maybe.concat(Arrays.asList(Maybe.empty(), Maybe.empty()))
        .test()
        .assertResult();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableBackpressured() {
        TestSubscriber<Integer> ts = Maybe.concat(Arrays.asList(Maybe.just(1), Maybe.just(2)))
        .test(0L);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(1);

        ts.request(1);

        ts.assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableBackpressuredNonEager() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = Maybe.concat(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test(0L);

        assertTrue(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertEmpty();

        ts.request(1);

        assertTrue(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        pp1.onNext(1);
        pp1.onComplete();

        ts.assertValue(1);

        assertFalse(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        ts.request(1);

        ts.assertValue(1);

        pp2.onNext(2);
        pp2.onComplete();

        ts.assertResult(1, 2);

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());
    }

    @Test
    public void concatIterableZero() {
        Maybe.concat(Collections.<Maybe<Integer>>emptyList()).test().assertResult();
    }

    @Test
    public void concatIterableOne() {
        Maybe.concat(Collections.<Maybe<Integer>>singleton(Maybe.just(1))).test().assertResult(1);
    }

    @Test
    public void concatPublisher() {
        Maybe.concat(Flowable.just(Maybe.just(1), Maybe.just(2))).test().assertResult(1, 2);
    }

    @Test
    public void concatPublisherPrefetch() {
        Maybe.concat(Flowable.just(Maybe.just(1), Maybe.just(2)), 1).test().assertResult(1, 2);
    }

    @Test(expected = NullPointerException.class)
    public void nullArgument() {
        Maybe.create(null);
    }

    @Test
    public void basic() {
        final Disposable d = Disposables.empty();

        Maybe.<Integer>create(new MaybeOnSubscribe<Integer>() {
            @Override
            public void subscribe(MaybeEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onSuccess(1);
                e.onError(new TestException());
                e.onSuccess(2);
                e.onError(new TestException());
                e.onComplete();
            }
        })
        .test()
        .assertResult(1);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicWithError() {
        final Disposable d = Disposables.empty();

        Maybe.<Integer>create(new MaybeOnSubscribe<Integer>() {
            @Override
            public void subscribe(MaybeEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onError(new TestException());
                e.onSuccess(2);
                e.onError(new TestException());
                e.onComplete();
            }
        })
        .test()
        .assertFailure(TestException.class);

        assertTrue(d.isDisposed());
    }

    @Test
    public void basicWithComplete() {
        final Disposable d = Disposables.empty();

        Maybe.<Integer>create(new MaybeOnSubscribe<Integer>() {
            @Override
            public void subscribe(MaybeEmitter<Integer> e) throws Exception {
                e.setDisposable(d);

                e.onComplete();
                e.onSuccess(1);
                e.onError(new TestException());
                e.onComplete();
                e.onSuccess(2);
                e.onError(new TestException());
            }
        })
        .test()
        .assertResult();

        assertTrue(d.isDisposed());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsafeCreateWithMaybe() {
        Maybe.unsafeCreate(Maybe.just(1));
    }

    @Test
    public void maybeToPublisherEnum() {
        TestHelper.checkEnum(MaybeToPublisher.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArrayOneIsNull() {
        Maybe.ambArray(null, Maybe.just(1))
                .test()
                .assertError(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArrayEmpty() {
        assertSame(Maybe.empty(), Maybe.ambArray());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArrayOne() {
        assertSame(Maybe.never(), Maybe.ambArray(Maybe.never()));
    }

    @Test
    public void ambWithOrder() {
        Maybe<Integer> error = Maybe.error(new RuntimeException());
        Maybe.just(1).ambWith(error).test().assertValue(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterableOrder() {
        Maybe<Integer> error = Maybe.error(new RuntimeException());
        Maybe.amb(Arrays.asList(Maybe.just(1), error)).test().assertValue(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArrayOrder() {
        Maybe<Integer> error = Maybe.error(new RuntimeException());
        Maybe.ambArray(Maybe.just(1), error).test().assertValue(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArray1SignalsSuccess() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.ambArray(pp1.singleElement(), pp2.singleElement())
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onNext(1);
        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArray2SignalsSuccess() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.ambArray(pp1.singleElement(), pp2.singleElement())
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(2);
        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertResult(2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArray1SignalsError() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.ambArray(pp1.singleElement(), pp2.singleElement())
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onError(new TestException());

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArray2SignalsError() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.ambArray(pp1.singleElement(), pp2.singleElement())
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onError(new TestException());

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArray1SignalsComplete() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.ambArray(pp1.singleElement(), pp2.singleElement())
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertResult();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambArray2SignalsComplete() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.ambArray(pp1.singleElement(), pp2.singleElement())
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertResult();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable1SignalsSuccess() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onNext(1);
        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable2SignalsSuccess() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(2);
        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertResult(2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable2SignalsSuccessWithOverlap() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
                .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(2);
        pp1.onNext(1);
        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertResult(2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable1SignalsError() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onError(new TestException());

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable2SignalsError() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onError(new TestException());

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable2SignalsErrorWithOverlap() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
                .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onError(new TestException("2"));
        pp1.onError(new TestException("1"));

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertFailureAndMessage(TestException.class, "2");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable1SignalsComplete() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertResult();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterable2SignalsComplete() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertResult();
    }

    @Test(expected = NullPointerException.class)
    public void ambIterableNull() {
        Maybe.amb((Iterable<Maybe<Integer>>)null);
    }

    @Test
    public void ambIterableIteratorNull() {
        Maybe.amb(new Iterable<Maybe<Object>>() {
            @Override
            public Iterator<Maybe<Object>> iterator() {
                return null;
            }
        }).test().assertError(NullPointerException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void ambIterableOneIsNull() {
        Maybe.amb(Arrays.asList(null, Maybe.just(1)))
                .test()
                .assertError(NullPointerException.class);
    }

    @Test
    public void ambIterableEmpty() {
        Maybe.amb(Collections.<Maybe<Integer>>emptyList()).test().assertResult();
    }

    @Test
    public void ambIterableOne() {
        Maybe.amb(Collections.singleton(Maybe.just(1))).test().assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArray() {
        Maybe.mergeArray(Maybe.just(1), Maybe.just(2), Maybe.just(3))
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void merge2() {
        Maybe.merge(Maybe.just(1), Maybe.just(2))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void merge3() {
        Maybe.merge(Maybe.just(1), Maybe.just(2), Maybe.just(3))
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void merge4() {
        Maybe.merge(Maybe.just(1), Maybe.just(2), Maybe.just(3), Maybe.just(4))
        .test()
        .assertResult(1, 2, 3, 4);
    }

    @Test
    public void merge4Take2() {
        Maybe.merge(Maybe.just(1), Maybe.just(2), Maybe.just(3), Maybe.just(4))
        .take(2)
        .test()
        .assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayBackpressured() {
        TestSubscriber<Integer> ts = Maybe.mergeArray(Maybe.just(1), Maybe.just(2), Maybe.just(3))
        .test(0L);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(1);

        ts.request(1);

        ts.assertValues(1, 2);

        ts.request(1);
        ts.assertResult(1, 2, 3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayBackpressuredMixed1() {
        TestSubscriber<Integer> ts = Maybe.mergeArray(Maybe.just(1), Maybe.<Integer>empty(), Maybe.just(3))
        .test(0L);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(1);

        ts.request(1);

        ts.assertResult(1, 3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayBackpressuredMixed2() {
        TestSubscriber<Integer> ts = Maybe.mergeArray(Maybe.just(1), Maybe.just(2), Maybe.<Integer>empty())
        .test(0L);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(1);

        ts.request(1);

        ts.assertResult(1, 2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayBackpressuredMixed3() {
        TestSubscriber<Integer> ts = Maybe.mergeArray(Maybe.<Integer>empty(), Maybe.just(2), Maybe.just(3))
        .test(0L);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(2);

        ts.request(1);

        ts.assertResult(2, 3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayFused() {
        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

        Maybe.mergeArray(Maybe.just(1), Maybe.just(2), Maybe.just(3)).subscribe(ts);

        ts.assertSubscribed()
        .assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC))
        .assertResult(1, 2, 3);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayFusedRace() {
        for (int i = 0; i < 500; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

            Maybe.mergeArray(pp1.singleElement(), pp2.singleElement()).subscribe(ts);

            ts.assertSubscribed()
            .assertOf(SubscriberFusion.<Integer>assertFuseable())
            .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC))
            ;

            TestHelper.race(new Runnable() {
                @Override
                public void run() {
                    pp1.onNext(1);
                    pp1.onComplete();
                }
            }, new Runnable() {
                @Override
                public void run() {
                    pp2.onNext(1);
                    pp2.onComplete();
                }
            }, Schedulers.single());

            ts
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 1);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayZero() {
        assertSame(Flowable.empty(), Maybe.mergeArray());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayOne() {
        Maybe.mergeArray(Maybe.just(1)).test().assertResult(1);
    }

    @Test
    public void mergePublisher() {
        Maybe.merge(Flowable.just(Maybe.just(1), Maybe.just(2), Maybe.just(3)))
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void mergePublisherMaxConcurrent() {
        final PublishProcessor<Integer> pp1 = PublishProcessor.create();
        final PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = Maybe.merge(Flowable.just(pp1.singleElement(), pp2.singleElement()), 1).test(0L);

        assertTrue(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        pp1.onNext(1);
        pp1.onComplete();

        ts.request(1);

        assertFalse(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());
    }

    @Test
    public void mergeMaybe() {
        Maybe.merge(Maybe.just(Maybe.just(1)))
        .test()
        .assertResult(1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterable() {
        Maybe.merge(Arrays.asList(Maybe.just(1), Maybe.just(2), Maybe.just(3)))
        .test()
        .assertResult(1, 2, 3);
    }

    @Test
    public void mergeALot() {
        @SuppressWarnings("unchecked")
        Maybe<Integer>[] sources = new Maybe[Flowable.bufferSize() * 2];
        Arrays.fill(sources, Maybe.just(1));

        Maybe.mergeArray(sources)
        .test()
        .assertSubscribed()
        .assertValueCount(sources.length)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void mergeALotLastEmpty() {
        @SuppressWarnings("unchecked")
        Maybe<Integer>[] sources = new Maybe[Flowable.bufferSize() * 2];
        Arrays.fill(sources, Maybe.just(1));
        sources[sources.length - 1] = Maybe.empty();

        Maybe.mergeArray(sources)
        .test()
        .assertSubscribed()
        .assertValueCount(sources.length - 1)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void mergeALotFused() {
        @SuppressWarnings("unchecked")
        Maybe<Integer>[] sources = new Maybe[Flowable.bufferSize() * 2];
        Arrays.fill(sources, Maybe.just(1));

        TestSubscriber<Integer> ts = SubscriberFusion.newTest(QueueSubscription.ANY);

        Maybe.mergeArray(sources).subscribe(ts);

        ts
        .assertSubscribed()
        .assertOf(SubscriberFusion.<Integer>assertFuseable())
        .assertOf(SubscriberFusion.<Integer>assertFusionMode(QueueSubscription.ASYNC))
        .assertValueCount(sources.length)
        .assertNoErrors()
        .assertComplete();
    }

    @Test
    public void mergeErrorSuccess() {
        Maybe.merge(Maybe.error(new TestException()), Maybe.just(1))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void mergeSuccessError() {
        Maybe.merge(Maybe.just(1), Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void subscribeZero() {
        assertTrue(Maybe.just(1)
        .subscribe().isDisposed());
    }

    @Test
    public void subscribeZeroError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            assertTrue(Maybe.error(new TestException())
            .subscribe().isDisposed());

            TestHelper.assertError(errors, 0, OnErrorNotImplementedException.class);
            Throwable c = errors.get(0).getCause();
            assertTrue("" + c, c instanceof TestException);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void subscribeToOnSuccess() {
        final List<Integer> values = new ArrayList<Integer>();

        Consumer<Integer> onSuccess = new Consumer<Integer>() {
            @Override
            public void accept(Integer e) throws Exception {
                values.add(e);
            }
        };

        Maybe<Integer> source = Maybe.just(1);

        source.subscribe(onSuccess);
        source.subscribe(onSuccess, Functions.emptyConsumer());
        source.subscribe(onSuccess, Functions.emptyConsumer(), Functions.EMPTY_ACTION);

        assertEquals(Arrays.asList(1, 1, 1), values);
    }

    @Test
    public void subscribeToOnError() {
        final List<Throwable> values = new ArrayList<Throwable>();

        Consumer<Throwable> onError = new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) throws Exception {
                values.add(e);
            }
        };

        TestException ex = new TestException();

        Maybe<Integer> source = Maybe.error(ex);

        source.subscribe(Functions.emptyConsumer(), onError);
        source.subscribe(Functions.emptyConsumer(), onError, Functions.EMPTY_ACTION);

        assertEquals(Arrays.asList(ex, ex), values);
    }

    @Test
    public void subscribeToOnComplete() {
        final List<Integer> values = new ArrayList<Integer>();

        Action onComplete = new Action() {
            @Override
            public void run() throws Exception {
                values.add(100);
            }
        };

        Maybe<Integer> source = Maybe.empty();

        source.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer(), onComplete);

        assertEquals(Arrays.asList(100), values);
    }

    @Test
    public void subscribeWith() {
        MaybeObserver<Integer> mo = new MaybeObserver<Integer>() {

            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(Integer value) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }

        };

        assertSame(mo, Maybe.just(1).subscribeWith(mo));
    }

    @Test
    public void doOnEventSuccess() {
        final List<Object> list = new ArrayList<Object>();

        assertTrue(Maybe.just(1)
        .doOnEvent(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer v, Throwable e) throws Exception {
                list.add(v);
                list.add(e);
            }
        })
        .subscribe().isDisposed());

        assertEquals(Arrays.asList(1, null), list);
    }

    @Test
    public void doOnEventError() {
        final List<Object> list = new ArrayList<Object>();

        TestException ex = new TestException();

        assertTrue(Maybe.<Integer>error(ex)
        .doOnEvent(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer v, Throwable e) throws Exception {
                list.add(v);
                list.add(e);
            }
        })
        .subscribe().isDisposed());

        assertEquals(Arrays.asList(null, ex), list);
    }

    @Test
    public void doOnEventComplete() {
        final List<Object> list = new ArrayList<Object>();

        assertTrue(Maybe.<Integer>empty()
        .doOnEvent(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer v, Throwable e) throws Exception {
                list.add(v);
                list.add(e);
            }
        })
        .subscribe().isDisposed());

        assertEquals(Arrays.asList(null, null), list);
    }

    @Test(expected = NullPointerException.class)
    public void doOnEventNull() {
        Maybe.just(1).doOnEvent(null);
    }

    @Test
    public void doOnEventSuccessThrows() {
        Maybe.just(1)
        .doOnEvent(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer v, Throwable e) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnEventErrorThrows() {
        TestObserver<Integer> ts = Maybe.<Integer>error(new TestException("Outer"))
        .doOnEvent(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer v, Throwable e) throws Exception {
                throw new TestException("Inner");
            }
        })
        .test()
        .assertFailure(CompositeException.class);

        List<Throwable> list = TestHelper.compositeList(ts.errors().get(0));
        TestHelper.assertError(list, 0, TestException.class, "Outer");
        TestHelper.assertError(list, 1, TestException.class, "Inner");
        assertEquals(2, list.size());
    }


    @Test
    public void doOnEventCompleteThrows() {
        Maybe.<Integer>empty()
        .doOnEvent(new BiConsumer<Integer, Throwable>() {
            @Override
            public void accept(Integer v, Throwable e) throws Exception {
                throw new TestException();
            }
        })
        .test()
        .assertFailure(TestException.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatArrayDelayError() {
        Maybe.concatArrayDelayError(Maybe.empty(), Maybe.just(1), Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class, 1);

        Maybe.concatArrayDelayError(Maybe.error(new TestException()), Maybe.empty(), Maybe.just(1))
        .test()
        .assertFailure(TestException.class, 1);

        assertSame(Flowable.empty(), Maybe.concatArrayDelayError());

        assertFalse(Maybe.concatArrayDelayError(Maybe.never()) instanceof MaybeConcatArrayDelayError);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatIterableDelayError() {
        Maybe.concatDelayError(Arrays.asList(Maybe.empty(), Maybe.just(1), Maybe.error(new TestException())))
        .test()
        .assertFailure(TestException.class, 1);

        Maybe.concatDelayError(Arrays.asList(Maybe.error(new TestException()), Maybe.empty(), Maybe.just(1)))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void concatPublisherDelayError() {
        Maybe.concatDelayError(Flowable.just(Maybe.empty(), Maybe.just(1), Maybe.error(new TestException())))
        .test()
        .assertFailure(TestException.class, 1);

        Maybe.concatDelayError(Flowable.just(Maybe.error(new TestException()), Maybe.empty(), Maybe.just(1)))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatEagerArray() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = Maybe.concatArrayEager(pp1.singleElement(), pp2.singleElement()).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(2);
        pp2.onComplete();

        ts.assertEmpty();

        pp1.onNext(1);
        pp1.onComplete();

        ts.assertResult(1, 2);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void concatEagerIterable() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = Maybe.concatEager(Arrays.asList(pp1.singleElement(), pp2.singleElement())).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(2);
        pp2.onComplete();

        ts.assertEmpty();

        pp1.onNext(1);
        pp1.onComplete();

        ts.assertResult(1, 2);

    }

    @Test
    public void concatEagerPublisher() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestSubscriber<Integer> ts = Maybe.concatEager(Flowable.just(pp1.singleElement(), pp2.singleElement())).test();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(2);
        pp2.onComplete();

        ts.assertEmpty();

        pp1.onNext(1);
        pp1.onComplete();

        ts.assertResult(1, 2);

    }

    static Future<Integer> emptyFuture() {
        final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

        return exec.schedule(new Callable<Integer>() {

            @Override
            public Integer call() throws Exception {
                exec.shutdown();
                return null;
            }
        }, 200, TimeUnit.MILLISECONDS);
    }

    @Test
    public void fromFuture() {
        Maybe.fromFuture(Flowable.just(1).delay(200, TimeUnit.MILLISECONDS).toFuture())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(1)
        ;

        Maybe.fromFuture(emptyFuture())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult()
        ;

        Maybe.fromFuture(Flowable.error(new TestException()).delay(200, TimeUnit.MILLISECONDS, true).toFuture())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TestException.class)
        ;

        Maybe.fromFuture(Flowable.empty().delay(10, TimeUnit.SECONDS).toFuture(), 100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertFailure(TimeoutException.class)
        ;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeArrayDelayError() {
        Maybe.mergeArrayDelayError(Maybe.empty(), Maybe.just(1), Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class, 1);

        Maybe.mergeArrayDelayError(Maybe.error(new TestException()), Maybe.empty(), Maybe.just(1))
        .test()
        .assertFailure(TestException.class, 1);

        assertSame(Flowable.empty(), Maybe.mergeArrayDelayError());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void mergeIterableDelayError() {
        Maybe.mergeDelayError(Arrays.asList(Maybe.empty(), Maybe.just(1), Maybe.error(new TestException())))
        .test()
        .assertFailure(TestException.class, 1);

        Maybe.mergeDelayError(Arrays.asList(Maybe.error(new TestException()), Maybe.empty(), Maybe.just(1)))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void mergePublisherDelayError() {
        Maybe.mergeDelayError(Flowable.just(Maybe.empty(), Maybe.just(1), Maybe.error(new TestException())))
        .test()
        .assertFailure(TestException.class, 1);

        Maybe.mergeDelayError(Flowable.just(Maybe.error(new TestException()), Maybe.empty(), Maybe.just(1)))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void mergeDelayError2() {
        Maybe.mergeDelayError(Maybe.just(1), Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class, 1);

        Maybe.mergeDelayError(Maybe.error(new TestException()), Maybe.just(1))
        .test()
        .assertFailure(TestException.class, 1);
    }

    @Test
    public void mergeDelayError3() {
        Maybe.mergeDelayError(Maybe.just(1), Maybe.error(new TestException()), Maybe.just(2))
        .test()
        .assertFailure(TestException.class, 1, 2);

        Maybe.mergeDelayError(Maybe.error(new TestException()), Maybe.just(1), Maybe.just(2))
        .test()
        .assertFailure(TestException.class, 1, 2);

        Maybe.mergeDelayError(Maybe.just(1), Maybe.just(2), Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class, 1, 2);
    }

    @Test
    public void mergeDelayError4() {
        Maybe.mergeDelayError(Maybe.just(1), Maybe.error(new TestException()), Maybe.just(2), Maybe.just(3))
        .test()
        .assertFailure(TestException.class, 1, 2, 3);

        Maybe.mergeDelayError(Maybe.error(new TestException()), Maybe.just(1), Maybe.just(2), Maybe.just(3))
        .test()
        .assertFailure(TestException.class, 1, 2, 3);

        Maybe.mergeDelayError(Maybe.just(1), Maybe.just(2), Maybe.just(3), Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class, 1, 2, 3);
    }

    @Test
    public void sequenceEqual() {
        Maybe.sequenceEqual(Maybe.just(1), Maybe.just(new Integer(1))).test().assertResult(true);

        Maybe.sequenceEqual(Maybe.just(1), Maybe.just(2)).test().assertResult(false);

        Maybe.sequenceEqual(Maybe.just(1), Maybe.empty()).test().assertResult(false);

        Maybe.sequenceEqual(Maybe.empty(), Maybe.just(2)).test().assertResult(false);

        Maybe.sequenceEqual(Maybe.empty(), Maybe.empty()).test().assertResult(true);

        Maybe.sequenceEqual(Maybe.just(1), Maybe.error(new TestException())).test().assertFailure(TestException.class);

        Maybe.sequenceEqual(Maybe.error(new TestException()), Maybe.just(1)).test().assertFailure(TestException.class);

        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Maybe.sequenceEqual(Maybe.error(new TestException("One")), Maybe.error(new TestException("Two"))).test().assertFailureAndMessage(TestException.class, "One");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Two");
        } finally {
            RxJavaPlugins.reset();
        }

        Maybe.sequenceEqual(Maybe.just(1), Maybe.error(new TestException()), new BiPredicate<Object, Object>() {
            @Override
            public boolean test(Object t1, Object t2) throws Exception {
                throw new TestException();
            }
        })
        .test().assertFailure(TestException.class);
    }

    @Test
    public void timer() {
        Maybe.timer(100, TimeUnit.MILLISECONDS)
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertResult(0L);
    }

    @Test
    public void blockingGet() {
        assertEquals(1, Maybe.just(1).blockingGet().intValue());

        assertEquals(100, Maybe.empty().blockingGet(100));

        try {
            Maybe.error(new TestException()).blockingGet();
            fail("Should have thrown!");
        } catch (TestException ex) {
            // expected
        }

        try {
            Maybe.error(new TestException()).blockingGet(100);
            fail("Should have thrown!");
        } catch (TestException ex) {
            // expected
        }
    }

    @Test
    public void toSingleDefault() {
        Maybe.just(1).toSingle(100)
        .test().assertResult(1);

        Maybe.empty().toSingle(100)
        .test().assertResult(100);
    }

    @Test
    public void flatMapContinuation() {
        Maybe.just(1).flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer v) throws Exception {
                return Completable.complete();
            }
        })
        .test().assertResult();

        Maybe.just(1).flatMapCompletable(new Function<Integer, Completable>() {
            @Override
            public Completable apply(Integer v) throws Exception {
                return Completable.error(new TestException());
            }
        })
        .test().assertFailure(TestException.class);

        Maybe.just(1).flatMapPublisher(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.range(1, 5);
            }
        })
        .test().assertResult(1, 2, 3, 4, 5);

        Maybe.just(1).flatMapPublisher(new Function<Integer, Publisher<Integer>>() {
            @Override
            public Publisher<Integer> apply(Integer v) throws Exception {
                return Flowable.error(new TestException());
            }
        })
        .test().assertFailure(TestException.class);

        Maybe.just(1).flatMapObservable(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Observable.range(1, 5);
            }
        })
        .test().assertResult(1, 2, 3, 4, 5);

        Maybe.just(1).flatMapObservable(new Function<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> apply(Integer v) throws Exception {
                return Observable.error(new TestException());
            }
        })
        .test().assertFailure(TestException.class);
    }

    @Test
    public void using() {
        final AtomicInteger disposeCount = new AtomicInteger();

        Maybe.using(Functions.justCallable(1), new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v);
            }
        }, new Consumer<Integer>() {
            @Override
            public void accept(Integer d) throws Exception {
                disposeCount.set(d);
            }
        })
        .map(new Function<Integer, Object>() {
            @Override
            public String apply(Integer v) throws Exception {
                return "" + disposeCount.get() + v * 10;
            }
        })
        .test()
        .assertResult("110");
    }

    @Test
    public void usingNonEager() {
        final AtomicInteger disposeCount = new AtomicInteger();

        Maybe.using(Functions.justCallable(1), new Function<Integer, MaybeSource<Integer>>() {
            @Override
            public MaybeSource<Integer> apply(Integer v) throws Exception {
                return Maybe.just(v);
            }
        }, new Consumer<Integer>() {
            @Override
            public void accept(Integer d) throws Exception {
                disposeCount.set(d);
            }
        }, false)
        .map(new Function<Integer, Object>() {
            @Override
            public String apply(Integer v) throws Exception {
                return "" + disposeCount.get() + v * 10;
            }
        })
        .test()
        .assertResult("010");

        assertEquals(1, disposeCount.get());
    }

    Function<Object[], String> arrayToString = new Function<Object[], String>() {
        @Override
        public String apply(Object[] a) throws Exception {
            return Arrays.toString(a);
        }
    };

    @SuppressWarnings("unchecked")
    @Test
    public void zipArray() {
        Maybe.zipArray(arrayToString, Maybe.just(1), Maybe.just(2))
        .test()
        .assertResult("[1, 2]");

        Maybe.zipArray(arrayToString, Maybe.just(1), Maybe.empty())
        .test()
        .assertResult();

        Maybe.zipArray(arrayToString, Maybe.just(1), Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class);

        assertSame(Maybe.empty(), Maybe.zipArray(ArgsToString.INSTANCE));

        Maybe.zipArray(arrayToString, Maybe.just(1))
        .test()
        .assertResult("[1]");
    }


    @SuppressWarnings("unchecked")
    @Test
    public void zipIterable() {
        Maybe.zip(
        Arrays.asList(Maybe.just(1), Maybe.just(2)),
        arrayToString)
        .test()
        .assertResult("[1, 2]");

        Maybe.zip(Collections.<Maybe<Integer>>emptyList(), arrayToString)
        .test()
        .assertResult();

        Maybe.zip(Collections.singletonList(Maybe.just(1)), arrayToString)
        .test()
        .assertResult("[1]");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zip2() {
        Maybe.zip(Maybe.just(1), Maybe.just(2), ArgsToString.INSTANCE)
        .test()
        .assertResult("12");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zipWith() {
        Maybe.just(1).zipWith(Maybe.just(2), ArgsToString.INSTANCE)
        .test()
        .assertResult("12");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zip3() {
        Maybe.zip(Maybe.just(1), Maybe.just(2), Maybe.just(3),
            ArgsToString.INSTANCE)
        .test()
        .assertResult("123");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zip4() {
        Maybe.zip(Maybe.just(1), Maybe.just(2), Maybe.just(3), Maybe.just(4),
            ArgsToString.INSTANCE)
        .test()
        .assertResult("1234");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zip5() {
        Maybe.zip(Maybe.just(1), Maybe.just(2), Maybe.just(3), Maybe.just(4),
                Maybe.just(5),
            ArgsToString.INSTANCE)
        .test()
        .assertResult("12345");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zip6() {
        Maybe.zip(Maybe.just(1), Maybe.just(2), Maybe.just(3), Maybe.just(4),
                Maybe.just(5), Maybe.just(6),
            ArgsToString.INSTANCE)
        .test()
        .assertResult("123456");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zip7() {
        Maybe.zip(Maybe.just(1), Maybe.just(2), Maybe.just(3), Maybe.just(4),
                Maybe.just(5), Maybe.just(6), Maybe.just(7),
            ArgsToString.INSTANCE)
        .test()
        .assertResult("1234567");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zip8() {
        Maybe.zip(Maybe.just(1), Maybe.just(2), Maybe.just(3), Maybe.just(4),
                Maybe.just(5), Maybe.just(6), Maybe.just(7), Maybe.just(8),
            ArgsToString.INSTANCE)
        .test()
        .assertResult("12345678");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void zip9() {
        Maybe.zip(Maybe.just(1), Maybe.just(2), Maybe.just(3), Maybe.just(4),
                Maybe.just(5), Maybe.just(6), Maybe.just(7), Maybe.just(8), Maybe.just(9),
            ArgsToString.INSTANCE)
        .test()
        .assertResult("123456789");
    }


    @Test
    public void ambWith1SignalsSuccess() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = pp1.singleElement().ambWith(pp2.singleElement())
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onNext(1);
        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertResult(1);
    }

    @Test
    public void ambWith2SignalsSuccess() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> ts = pp1.singleElement().ambWith(pp2.singleElement())
        .test();

        ts.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(2);
        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        ts.assertResult(2);
    }

    @Test
    public void zipIterableObject() {
        @SuppressWarnings("unchecked")
        final List<Maybe<Integer>> maybes = Arrays.asList(Maybe.just(1), Maybe.just(4));
        Maybe.zip(maybes, new Function<Object[], Object>() {
            @Override
            public Object apply(final Object[] o) throws Exception {
                int sum = 0;
                for (Object i : o) {
                    sum += (Integer) i;
                }
                return sum;
            }
        }).test().assertResult(5);
    }

    static long usedMemoryNow() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

        return heapMemoryUsage.getUsed();
    }

    @Test
    public void onTerminateDetach() throws Exception {
        System.gc();

        Thread.sleep(150);

        long before = usedMemoryNow();

        Maybe<Object> source = Flowable.just((Object)new Object[10000000]).singleElement();

        long middle = usedMemoryNow();

        MaybeObserver<Object> observer = new MaybeObserver<Object>() {
            @SuppressWarnings("unused")
            Disposable u;

            @Override
            public void onSubscribe(Disposable d) {
                this.u = d;
            }

            @Override
            public void onSuccess(Object value) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }

        };
        source.onTerminateDetach().subscribe(observer);

        source = null;

        System.gc();

        Thread.sleep(250);

        long after = usedMemoryNow();

        String log = String.format("%.2f MB -> %.2f MB -> %.2f MB%n",
                before / 1024.0 / 1024.0,
                middle / 1024.0 / 1024.0,
                after / 1024.0 / 1024.0);

        System.out.printf(log);

        if (before * 1.3 < after) {
            fail("There seems to be a memory leak: " + log);
        }

        assertNotNull(observer); // hold onto the reference to prevent premature GC
    }

    @Test
    public void repeat() {
        Maybe.just(1).repeat().take(5).test().assertResult(1, 1, 1, 1, 1);

        Maybe.just(1).repeat(5).test().assertResult(1, 1, 1, 1, 1);

        Maybe.just(1).repeatUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return false;
            }
        }).take(5).test().assertResult(1, 1, 1, 1, 1);

        Maybe.just(1).repeatWhen(new Function<Flowable<Object>, Publisher<Object>>() {
            @Override
            public Publisher<Object> apply(Flowable<Object> v) throws Exception {
                return v;
            }
        }).take(5).test().assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void retry() {
        Maybe.just(1).retry().test().assertResult(1);

        Maybe.just(1).retry(5).test().assertResult(1);

        Maybe.just(1).retry(Functions.alwaysTrue()).test().assertResult(1);

        Maybe.just(1).retry(5, Functions.alwaysTrue()).test().assertResult(1);

        Maybe.just(1).retry(new BiPredicate<Integer, Throwable>() {
            @Override
            public boolean test(Integer a, Throwable e) throws Exception {
                return true;
            }
        }).test().assertResult(1);

        Maybe.just(1).retryUntil(new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() throws Exception {
                return false;
            }
        }).test().assertResult(1);

        Maybe.just(1).retryWhen(new Function<Flowable<? extends Throwable>, Publisher<Object>>() {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public Publisher<Object> apply(Flowable<? extends Throwable> v) throws Exception {
                return (Publisher)v;
            }
        }).test().assertResult(1);
    }

    @Test
    public void onErrorResumeNextEmpty() {
        Maybe.empty()
            .onErrorResumeNext(Maybe.just(1))
            .test()
            .assertNoValues()
            .assertNoErrors()
            .assertComplete();
    }

    @Test
    public void onErrorResumeNextValue() {
        Maybe.just(1)
            .onErrorResumeNext(Maybe.<Integer>empty())
            .test()
            .assertNoErrors()
            .assertValue(1);
    }

    @Test
    public void onErrorResumeNextError() {
        Maybe.error(new RuntimeException("some error"))
            .onErrorResumeNext(Maybe.empty())
            .test()
            .assertNoValues()
            .assertNoErrors()
            .assertComplete();
    }

    @Test
    public void valueConcatWithValue() {
        Maybe.just(1)
            .concatWith(Maybe.just(2))
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValues(1, 2);
    }

    @Test
    public void errorConcatWithValue() {
        Maybe.<Integer>error(new RuntimeException("error"))
            .concatWith(Maybe.just(2))
            .test()
            .assertError(RuntimeException.class)
            .assertErrorMessage("error")
            .assertNoValues();
    }

    @Test
    public void valueConcatWithError() {
        Maybe.just(1)
            .concatWith(Maybe.<Integer>error(new RuntimeException("error")))
            .test()
            .assertValue(1)
            .assertError(RuntimeException.class)
            .assertErrorMessage("error");
    }

    @Test
    public void emptyConcatWithValue() {
        Maybe.<Integer>empty()
            .concatWith(Maybe.just(2))
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValues(2);
    }

    @Test
    public void emptyConcatWithError() {
        Maybe.<Integer>empty()
            .concatWith(Maybe.<Integer>error(new RuntimeException("error")))
            .test()
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("error");
    }
}
