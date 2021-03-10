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

package io.reactivex.rxjava3.maybe;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.annotations.NonNull;
import org.junit.Test;
import org.reactivestreams.Publisher;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.internal.fuseable.QueueFuseable;
import io.reactivex.rxjava3.internal.operators.flowable.FlowableZipTest.ArgsToString;
import io.reactivex.rxjava3.internal.operators.maybe.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.processors.PublishProcessor;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.*;

public class MaybeTest extends RxJavaTest {
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
        Flowable.just(1).concatWith(Flowable.error(new TestException()))
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

        TestObserver<Integer> to = pp.singleElement().test();

        assertTrue(pp.hasSubscribers());

        to.dispose();

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
        Flowable.just(1).concatWith(Flowable.error(new TestException()))
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
        PublishSubject<Integer> ps = PublishSubject.create();

        TestObserver<Integer> to = ps.singleElement().test(false);

        assertTrue(ps.hasObservers());

        to.dispose();

        assertFalse(ps.hasObservers());
    }

    @Test
    public void fromObservableDisposeComposesThroughImmediatelyCancelled() {
        PublishSubject<Integer> ps = PublishSubject.create();

        ps.singleElement().test(true);

        assertFalse(ps.hasObservers());
    }

    @Test
    public void just() {
        Maybe.just(1)
        .test()
        .assertResult(1);
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
        .to(TestHelper.testConsumer())
        .assertSubscribed()
        .assertNoValues()
        .assertNoErrors()
        .assertNotComplete();
    }

    @Test
    public void error() {
        Maybe.error(new TestException())
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorCallable() {
        Maybe.error(Functions.justSupplier(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void errorCallableReturnsNull() {
        Maybe.error(Functions.justSupplier(null))
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void wrapCustom() {
        Maybe.wrap((MaybeSource<Integer>) observer -> {
            observer.onSubscribe(Disposable.empty());
            observer.onSuccess(1);
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void wrapMaybe() {
        assertSame(Maybe.empty(), Maybe.wrap(Maybe.empty()));
    }

    @Test
    public void emptySingleton() {
        assertSame(Maybe.empty(), Maybe.empty());
    }

    @Test
    public void neverSingleton() {
        assertSame(Maybe.never(), Maybe.never());
    }

    @Test
    public void liftJust() {
        Maybe.just(1).lift((MaybeOperator<Integer, Integer>) t -> t)
        .test()
        .assertResult(1);
    }

    @Test
    public void liftThrows() {
        Maybe.just(1).lift((MaybeOperator<Integer, Integer>) t -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void deferThrows() {
        Maybe.defer((Supplier<Maybe<Integer>>) () -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void deferReturnsNull() {
        Maybe.defer((Supplier<Maybe<Integer>>) () -> null)
        .test()
        .assertFailure(NullPointerException.class);
    }

    @Test
    public void defer() {
        Maybe<Integer> source = Maybe.defer(new Supplier<Maybe<Integer>>() {
            int count;
            @Override
            public Maybe<Integer> get() throws Exception {
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
        Maybe.unsafeCreate((MaybeSource<Integer>) observer -> {
            observer.onSubscribe(Disposable.empty());
            observer.onSuccess(1);
        })
        .test()
        .assertResult(1);
    }

    @Test
    public void to() {
        Maybe.just(1).to(Maybe::toFlowable)
        .test()
        .assertResult(1);
    }

    @Test
    public void as() {
        Maybe.just(1).to(Maybe::toFlowable)
        .test()
        .assertResult(1);
    }

    @Test
    public void compose() {
        Maybe.just(1).compose(m -> m.map(w -> w + 1))
        .test()
        .assertResult(2);
    }

    @Test
    public void mapReturnNull() {
        Maybe.just(1).map(v -> null).test().assertFailure(NullPointerException.class);
    }

    @Test
    public void mapThrows() {
        Maybe.just(1).map(v -> {
            throw new IOException();
        }).test().assertFailure(IOException.class);
    }

    @Test
    public void map() {
        Maybe.just(1).map(Object::toString).test().assertResult("1");
    }

    @Test
    public void filterThrows() {
        Maybe.just(1).filter(v -> {
            throw new IOException();
        }).test().assertFailure(IOException.class);
    }

    @Test
    public void filterTrue() {
        Maybe.just(1).filter(v -> v == 1).test().assertResult(1);
    }

    @Test
    public void filterFalse() {
        Maybe.just(2).filter(v -> v == 1).test().assertResult();
    }

    @Test
    public void filterEmpty() {
        Maybe.<Integer>empty().filter(v -> v == 1).test().assertResult();
    }

    @Test
    public void singleFilterThrows() {
        Single.just(1).filter(v -> {
            throw new IOException();
        }).test().assertFailure(IOException.class);
    }

    @Test
    public void singleFilterTrue() {
        Single.just(1).filter(v -> v == 1).test().assertResult(1);
    }

    @Test
    public void singleFilterFalse() {
        Single.just(2).filter(v -> v == 1).test().assertResult();
    }

    @Test
    public void cast() {
        TestObserver<Number> to = Maybe.just(1).cast(Number.class).test();
        // don'n inline this due to the generic type
        to.assertResult(1);
    }

    @Test
    public void observeOnSuccess() {
        String main = Thread.currentThread().getName();
        TestObserver<String> to = Maybe.just(1)
        .observeOn(Schedulers.single())
        .map(v -> v + ": " + Thread.currentThread().getName())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        ;

        assertNotEquals("1: " + main, to.values().get(0));
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
        TestHelper.checkDoubleOnSubscribeMaybe(m -> m.observeOn(Schedulers.single()));
    }

    @Test
    public void subscribeOnSuccess() {
        String main = Thread.currentThread().getName();
        TestObserver<String> to = Maybe.fromCallable(() -> Thread.currentThread().getName())
        .subscribeOn(Schedulers.single())
        .test()
        .awaitDone(5, TimeUnit.SECONDS)
        .assertValueCount(1)
        ;

        assertNotEquals(main, to.values().get(0));
    }

    @Test
    public void observeOnErrorThread() {
        String main = Thread.currentThread().getName();

        final String[] name = { null };

        Maybe.error(new TestException()).observeOn(Schedulers.single())
        .doOnError(e -> name[0] = Thread.currentThread().getName())
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
        .doOnComplete(() -> name[0] = Thread.currentThread().getName())
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

        Maybe.fromAction(() -> call[0]++)
        .test()
        .assertResult();

        assertEquals(1, call[0]);
    }

    @Test
    public void fromActionThrows() {
        Maybe.fromAction(() -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fromRunnable() {
        final int[] call = { 0 };

        Maybe.fromRunnable(() -> call[0]++)
        .test()
        .assertResult();

        assertEquals(1, call[0]);
    }

    @Test
    public void fromRunnableThrows() {
        Maybe.fromRunnable(() -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void fromCallableThrows() {
        Maybe.fromCallable(() -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnSuccess() {
        final Integer[] value = { null };

        Maybe.just(1).doOnSuccess(v -> value[0] = v)
        .test()
        .assertResult(1);

        assertEquals(1, value[0].intValue());
    }

    @Test
    public void doOnSuccessEmpty() {
        final Integer[] value = { null };

        Maybe.<Integer>empty().doOnSuccess(v -> value[0] = v)
        .test()
        .assertResult();

        assertNull(value[0]);
    }

    @Test
    public void doOnSuccessThrows() {
        Maybe.just(1).doOnSuccess(v -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnSubscribe() {
        final Disposable[] value = { null };

        Maybe.just(1).doOnSubscribe(v -> value[0] = v)
        .test()
        .assertResult(1);

        assertNotNull(value[0]);
    }

    @Test
    public void doOnSubscribeThrows() {
        Maybe.just(1).doOnSubscribe(v -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnCompleteThrows() {
        Maybe.empty().doOnComplete(() -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnDispose() {
        final int[] call = { 0 };

        Maybe.just(1).doOnDispose(() -> call[0]++)
        .to(TestHelper.testConsumer(true))
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

            TestObserverEx<Integer> to = pp.singleElement().doOnDispose(() -> {
                throw new TestException();
            })
            .to(TestHelper.testConsumer());

            assertTrue(pp.hasSubscribers());

            to.dispose();

            assertFalse(pp.hasSubscribers());

            to.assertSubscribed()
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
        final TestSubscriber<Integer> ts = new TestSubscriber<>();

        final CountDownLatch cdl = new CountDownLatch(1);

        Maybe.just(1)
        .observeOn(Schedulers.single())
        .doOnSuccess(v -> {
            if (!cdl.await(5, TimeUnit.SECONDS)) {
                throw new TimeoutException();
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
        .doOnSuccess(v -> call[0]++)
        .doAfterTerminate(() -> {
            if (call[0] == 1) {
                call[0] = -1;
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
        .doOnError((Consumer<Object>) v -> call[0]++)
        .doAfterTerminate(() -> {
            if (call[0] == 1) {
                call[0] = -1;
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
        .doOnComplete(() -> call[0]++)
        .doAfterTerminate(() -> {
            if (call[0] == 1) {
                call[0] = -1;
            }
        })
        .test()
        .assertResult();

        assertEquals(-1, call[0]);
    }

    @Test
    public void sourceThrowsNPE() {
        try {
            Maybe.unsafeCreate(observer -> {
                throw new NullPointerException("Forced failure");
            }).test();

            fail("Should have thrown!");
        } catch (NullPointerException ex) {
            assertEquals("Forced failure", ex.getMessage());
        }
    }

    @Test
    public void sourceThrowsIAE() {
        try {
            Maybe.unsafeCreate(observer -> {
                throw new IllegalArgumentException("Forced failure");
            }).test();

            fail("Should have thrown!");
        } catch (NullPointerException ex) {
            assertTrue(ex.toString(), ex.getCause() instanceof IllegalArgumentException);
            assertEquals("Forced failure", ex.getCause().getMessage());
        }
    }

    @Test
    public void flatMap() {
        Maybe.just(1).flatMap((Function<Integer, MaybeSource<Integer>>) v -> Maybe.just(v * 10))
        .test()
        .assertResult(10);
    }

    @Test
    public void concatMap() {
        Maybe.just(1).concatMap((Function<Integer, MaybeSource<Integer>>) v -> Maybe.just(v * 10))
        .test()
        .assertResult(10);
    }

    @Test
    public void flatMapEmpty() {
        Maybe.just(1).flatMap((Function<Integer, MaybeSource<Integer>>) v -> Maybe.empty())
        .test()
        .assertResult();
    }

    @Test
    public void flatMapError() {
        Maybe.just(1).flatMap((Function<Integer, MaybeSource<Integer>>) v -> Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void flatMapNotifySuccess() {
        Maybe.just(1)
        .flatMap((Function<Integer, MaybeSource<Integer>>) v -> Maybe.just(v * 10),
                (Function<Throwable, MaybeSource<Integer>>) v -> Maybe.just(100),

                (Supplier<MaybeSource<Integer>>) () -> Maybe.just(200))
        .test()
        .assertResult(10);
    }

    @Test
    public void flatMapNotifyError() {
        Maybe.<Integer>error(new TestException())
        .flatMap((Function<Integer, MaybeSource<Integer>>) v -> Maybe.just(v * 10),
                (Function<Throwable, MaybeSource<Integer>>) v -> Maybe.just(100),

                (Supplier<MaybeSource<Integer>>) () -> Maybe.just(200))
        .test()
        .assertResult(100);
    }

    @Test
    public void flatMapNotifyComplete() {
        Maybe.<Integer>empty()
        .flatMap((Function<Integer, MaybeSource<Integer>>) v -> Maybe.just(v * 10),
                (Function<Throwable, MaybeSource<Integer>>) v -> Maybe.just(100),

                (Supplier<MaybeSource<Integer>>) () -> Maybe.just(200))
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

    @Test
    public void concatArrayZero() {
        assertSame(Flowable.empty(), Maybe.concatArray());
    }

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

    @Test
    public void concatIterable() {
        Maybe.concat(Arrays.asList(Maybe.just(1), Maybe.just(2)))
        .test()
        .assertResult(1, 2);
    }

    @Test
    public void concatIterableEmpty() {
        Maybe.concat(Arrays.asList(Maybe.empty(), Maybe.empty()))
        .test()
        .assertResult();
    }

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
        Maybe.concat(Collections.singleton(Maybe.just(1))).test().assertResult(1);
    }

    @Test
    public void concatPublisher() {
        Maybe.concat(Flowable.just(Maybe.just(1), Maybe.just(2))).test().assertResult(1, 2);
    }

    @Test
    public void concatPublisherPrefetch() {
        Maybe.concat(Flowable.just(Maybe.just(1), Maybe.just(2)), 1).test().assertResult(1, 2);
    }

    @Test
    public void basic() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final Disposable d = Disposable.empty();

            Maybe.<Integer>create(e -> {
                e.setDisposable(d);

                e.onSuccess(1);
                e.onError(new TestException());
                e.onSuccess(2);
                e.onError(new TestException());
                e.onComplete();
            })
            .test()
            .assertResult(1);

            assertTrue(d.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
            TestHelper.assertUndeliverable(errors, 1, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void basicWithError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final Disposable d = Disposable.empty();

            Maybe.<Integer>create(e -> {
                e.setDisposable(d);

                e.onError(new TestException());
                e.onSuccess(2);
                e.onError(new TestException());
                e.onComplete();
            })
            .test()
            .assertFailure(TestException.class);

            assertTrue(d.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void basicWithComplete() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final Disposable d = Disposable.empty();

            Maybe.<Integer>create(e -> {
                e.setDisposable(d);

                e.onComplete();
                e.onSuccess(1);
                e.onError(new TestException());
                e.onComplete();
                e.onSuccess(2);
                e.onError(new TestException());
            })
            .test()
            .assertResult();

            assertTrue(d.isDisposed());

            TestHelper.assertUndeliverable(errors, 0, TestException.class);
            TestHelper.assertUndeliverable(errors, 1, TestException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void unsafeCreateWithMaybe() {
        Maybe.unsafeCreate(Maybe.just(1));
    }

    @Test
    public void maybeToPublisherEnum() {
        TestHelper.checkEnum(MaybeToPublisher.class);
    }

    @Test
    public void ambArrayOneIsNull() {
        Maybe.ambArray(null, Maybe.just(1))
                .test()
                .assertError(NullPointerException.class);
    }

    @Test
    public void ambArrayEmpty() {
        assertSame(Maybe.empty(), Maybe.ambArray());
    }

    @Test
    public void ambArrayOne() {
        assertSame(Maybe.never(), Maybe.ambArray(Maybe.never()));
    }

    @Test
    public void ambWithOrder() {
        Maybe<Integer> error = Maybe.error(new RuntimeException());
        Maybe.just(1).ambWith(error).test().assertValue(1);
    }

    @Test
    public void ambIterableOrder() {
        Maybe<Integer> error = Maybe.error(new RuntimeException());
        Maybe.amb(Arrays.asList(Maybe.just(1), error)).test().assertValue(1);
    }

    @Test
    public void ambArrayOrder() {
        Maybe<Integer> error = Maybe.error(new RuntimeException());
        Maybe.ambArray(Maybe.just(1), error).test().assertValue(1);
    }

    @Test
    public void ambArray1SignalsSuccess() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.ambArray(pp1.singleElement(), pp2.singleElement())
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onNext(1);
        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult(1);
    }

    @Test
    public void ambArray2SignalsSuccess() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.ambArray(pp1.singleElement(), pp2.singleElement())
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(2);
        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult(2);
    }

    @Test
    public void ambArray1SignalsError() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.ambArray(pp1.singleElement(), pp2.singleElement())
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onError(new TestException());

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void ambArray2SignalsError() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.ambArray(pp1.singleElement(), pp2.singleElement())
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onError(new TestException());

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void ambArray1SignalsComplete() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.ambArray(pp1.singleElement(), pp2.singleElement())
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult();
    }

    @Test
    public void ambArray2SignalsComplete() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.ambArray(pp1.singleElement(), pp2.singleElement())
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult();
    }

    @Test
    public void ambIterable1SignalsSuccess() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onNext(1);
        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult(1);
    }

    @Test
    public void ambIterable2SignalsSuccess() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(2);
        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult(2);
    }

    @Test
    public void ambIterable2SignalsSuccessWithOverlap() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
                .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(2);
        pp1.onNext(1);
        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult(2);
    }

    @Test
    public void ambIterable1SignalsError() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onError(new TestException());

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void ambIterable2SignalsError() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onError(new TestException());

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertFailure(TestException.class);
    }

    @Test
    public void ambIterable2SignalsErrorWithOverlap() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserverEx<Integer> to = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
                .to(TestHelper.testConsumer());

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onError(new TestException("2"));
        pp1.onError(new TestException("1"));

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertFailureAndMessage(TestException.class, "2");
    }

    @Test
    public void ambIterable1SignalsComplete() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult();
    }

    @Test
    public void ambIterable2SignalsComplete() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = Maybe.amb(Arrays.asList(pp1.singleElement(), pp2.singleElement()))
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult();
    }

    @Test
    public void ambIterableIteratorNull() {
        Maybe.amb((Iterable<Maybe<Object>>) () -> null).test().assertError(NullPointerException.class);
    }

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

    @Test
    public void mergeArrayBackpressuredMixed1() {
        TestSubscriber<Integer> ts = Maybe.mergeArray(Maybe.just(1), Maybe.empty(), Maybe.just(3))
        .test(0L);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(1);

        ts.request(1);

        ts.assertResult(1, 3);
    }

    @Test
    public void mergeArrayBackpressuredMixed2() {
        TestSubscriber<Integer> ts = Maybe.mergeArray(Maybe.just(1), Maybe.just(2), Maybe.empty())
        .test(0L);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(1);

        ts.request(1);

        ts.assertResult(1, 2);
    }

    @Test
    public void mergeArrayBackpressuredMixed3() {
        TestSubscriber<Integer> ts = Maybe.mergeArray(Maybe.empty(), Maybe.just(2), Maybe.just(3))
        .test(0L);

        ts.assertEmpty();

        ts.request(1);

        ts.assertValue(2);

        ts.request(1);

        ts.assertResult(2, 3);
    }

    @Test
    public void mergeArrayFused() {
        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Maybe.mergeArray(Maybe.just(1), Maybe.just(2), Maybe.just(3)).subscribe(ts);

        ts.assertSubscribed()
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
        .assertResult(1, 2, 3);
    }

    @Test
    public void mergeArrayFusedRace() {
        for (int i = 0; i < TestHelper.RACE_DEFAULT_LOOPS; i++) {
            final PublishProcessor<Integer> pp1 = PublishProcessor.create();
            final PublishProcessor<Integer> pp2 = PublishProcessor.create();

            TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

            Maybe.mergeArray(pp1.singleElement(), pp2.singleElement()).subscribe(ts);

            ts.assertSubscribed()
            .assertFuseable()
            .assertFusionMode(QueueFuseable.ASYNC)
            ;

            TestHelper.race(() -> {
                pp1.onNext(1);
                pp1.onComplete();
            }, () -> {
                pp2.onNext(1);
                pp2.onComplete();
            });

            ts
            .awaitDone(5, TimeUnit.SECONDS)
            .assertResult(1, 1);
        }
    }

    @Test
    public void mergeArrayZero() {
        assertSame(Flowable.empty(), Maybe.mergeArray());
    }

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
        .to(TestHelper.testConsumer())
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
        .to(TestHelper.testConsumer())
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

        TestSubscriberEx<Integer> ts = new TestSubscriberEx<Integer>().setInitialFusionMode(QueueFuseable.ANY);

        Maybe.mergeArray(sources).subscribe(ts);

        ts
        .assertSubscribed()
        .assertFuseable()
        .assertFusionMode(QueueFuseable.ASYNC)
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
        final List<Integer> values = new ArrayList<>();

        Consumer<Integer> onSuccess = values::add;

        Maybe<Integer> source = Maybe.just(1);

        source.subscribe(onSuccess);
        source.subscribe(onSuccess, Functions.emptyConsumer());
        source.subscribe(onSuccess, Functions.emptyConsumer(), Functions.EMPTY_ACTION);

        assertEquals(Arrays.asList(1, 1, 1), values);
    }

    @Test
    public void subscribeToOnError() {
        final List<Throwable> values = new ArrayList<>();

        Consumer<Throwable> onError = values::add;

        TestException ex = new TestException();

        Maybe<Integer> source = Maybe.error(ex);

        source.subscribe(Functions.emptyConsumer(), onError);
        source.subscribe(Functions.emptyConsumer(), onError, Functions.EMPTY_ACTION);

        assertEquals(Arrays.asList(ex, ex), values);
    }

    @Test
    public void subscribeToOnComplete() {
        final List<Integer> values = new ArrayList<>();

        Action onComplete = () -> values.add(100);

        Maybe<Integer> source = Maybe.empty();

        source.subscribe(Functions.emptyConsumer(), Functions.emptyConsumer(), onComplete);

        assertEquals(Collections.singletonList(100), values);
    }

    @Test
    public void subscribeWith() {
        MaybeObserver<Integer> mo = new MaybeObserver<Integer>() {

            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onSuccess(@NonNull Integer value) {

            }

            @Override
            public void onError(@NonNull Throwable e) {

            }

            @Override
            public void onComplete() {

            }

        };

        assertSame(mo, Maybe.just(1).subscribeWith(mo));
    }

    @Test
    public void doOnEventSuccess() {
        final List<Object> list = new ArrayList<>();

        assertTrue(Maybe.just(1)
        .doOnEvent((v, e) -> {
            list.add(v);
            list.add(e);
        })
        .subscribe().isDisposed());

        assertEquals(Arrays.asList(1, null), list);
    }

    @Test
    public void doOnEventError() {
        List<Throwable> errors = TestHelper.trackPluginErrors();
        try {
            final List<Object> list = new ArrayList<>();

            TestException ex = new TestException();

            assertTrue(Maybe.<Integer>error(ex)
            .doOnEvent((v, e) -> {
                list.add(v);
                list.add(e);
            })
            .subscribe().isDisposed());

            assertEquals(Arrays.asList(null, ex), list);

            TestHelper.assertError(errors, 0, OnErrorNotImplementedException.class);
        } finally {
            RxJavaPlugins.reset();
        }
    }

    @Test
    public void doOnEventComplete() {
        final List<Object> list = new ArrayList<>();

        assertTrue(Maybe.<Integer>empty()
        .doOnEvent((v, e) -> {
            list.add(v);
            list.add(e);
        })
        .subscribe().isDisposed());

        assertEquals(Arrays.asList(null, null), list);
    }

    @Test
    public void doOnEventSuccessThrows() {
        Maybe.just(1)
        .doOnEvent((v, e) -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

    @Test
    public void doOnEventErrorThrows() {
        TestObserverEx<Integer> to = Maybe.<Integer>error(new TestException("Outer"))
        .doOnEvent((v, e) -> {
            throw new TestException("Inner");
        })
        .to(TestHelper.testConsumer())
        .assertFailure(CompositeException.class);

        List<Throwable> list = TestHelper.compositeList(to.errors().get(0));
        TestHelper.assertError(list, 0, TestException.class, "Outer");
        TestHelper.assertError(list, 1, TestException.class, "Inner");
        assertEquals(2, list.size());
    }

    @Test
    public void doOnEventCompleteThrows() {
        Maybe.<Integer>empty()
        .doOnEvent((v, e) -> {
            throw new TestException();
        })
        .test()
        .assertFailure(TestException.class);
    }

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

    @Test
    public void concatPublisherDelayErrorPrefetch() {
        Maybe.concatDelayError(Flowable.just(Maybe.empty(), Maybe.just(1), Maybe.error(new TestException())), 1)
        .test()
        .assertFailure(TestException.class, 1);

        Maybe.concatDelayError(Flowable.just(Maybe.error(new TestException()), Maybe.empty(), Maybe.just(1)), 1)
        .test()
        .assertFailure(TestException.class, 1);
    }

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

        return exec.schedule(() -> {
            exec.shutdown();
            return null;
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

    @Test
    public void mergeArrayDelayError() {
        Maybe.mergeArrayDelayError(Maybe.empty(), Maybe.just(1), Maybe.error(new TestException()))
        .test()
        .assertFailure(TestException.class, 1);

        Maybe.mergeArrayDelayError(Maybe.error(new TestException()), Maybe.empty(), Maybe.just(1))
        .test()
        .assertFailure(TestException.class, 1);
    }

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
        Maybe.sequenceEqual(Maybe.just(1_000_000), Maybe.just(1_000_000)).test().assertResult(true);

        Maybe.sequenceEqual(Maybe.just(1), Maybe.just(2)).test().assertResult(false);

        Maybe.sequenceEqual(Maybe.just(1), Maybe.empty()).test().assertResult(false);

        Maybe.sequenceEqual(Maybe.empty(), Maybe.just(2)).test().assertResult(false);

        Maybe.sequenceEqual(Maybe.empty(), Maybe.empty()).test().assertResult(true);

        Maybe.sequenceEqual(Maybe.just(1), Maybe.error(new TestException())).test().assertFailure(TestException.class);

        Maybe.sequenceEqual(Maybe.error(new TestException()), Maybe.just(1)).test().assertFailure(TestException.class);

        List<Throwable> errors = TestHelper.trackPluginErrors();

        try {
            Maybe.sequenceEqual(Maybe.error(new TestException("One")), Maybe.error(new TestException("Two")))
            .to(TestHelper.testConsumer())
            .assertFailureAndMessage(TestException.class, "One");

            TestHelper.assertUndeliverable(errors, 0, TestException.class, "Two");
        } finally {
            RxJavaPlugins.reset();
        }

        Maybe.sequenceEqual(Maybe.just(1), Maybe.error(new TestException()), (BiPredicate<Object, Object>) (t1, t2) -> {
            throw new TestException();
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
    public void flatMapContinuation() {
        Maybe.just(1).flatMapCompletable((Function<Integer, Completable>) v -> Completable.complete())
        .test().assertResult();

        Maybe.just(1).flatMapCompletable((Function<Integer, Completable>) v -> Completable.error(new TestException()))
        .test().assertFailure(TestException.class);

        Maybe.just(1).flatMapPublisher((Function<Integer, Publisher<Integer>>) v -> Flowable.range(1, 5))
        .test().assertResult(1, 2, 3, 4, 5);

        Maybe.just(1).flatMapPublisher((Function<Integer, Publisher<Integer>>) v -> Flowable.error(new TestException()))
        .test().assertFailure(TestException.class);

        Maybe.just(1).flatMapObservable((Function<Integer, Observable<Integer>>) v -> Observable.range(1, 5))
        .test().assertResult(1, 2, 3, 4, 5);

        Maybe.just(1).flatMapObservable((Function<Integer, Observable<Integer>>) v -> Observable.error(new TestException()))
        .test().assertFailure(TestException.class);
    }

    @Test
    public void using() {
        final AtomicInteger disposeCount = new AtomicInteger();

        Maybe.using(Functions.justSupplier(1), (Function<Integer, MaybeSource<Integer>>) Maybe::just, disposeCount::set)
        .map((Function<Integer, Object>) v -> "" + disposeCount.get() + v * 10)
        .test()
        .assertResult("110");
    }

    @Test
    public void usingNonEager() {
        final AtomicInteger disposeCount = new AtomicInteger();

        Maybe.using(Functions.justSupplier(1), (Function<Integer, MaybeSource<Integer>>) Maybe::just, disposeCount::set, false)
        .map((Function<Integer, Object>) v -> "" + disposeCount.get() + v * 10)
        .test()
        .assertResult("010");

        assertEquals(1, disposeCount.get());
    }

    Function<Object[], String> arrayToString = Arrays::toString;

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

        TestObserver<Integer> to = pp1.singleElement().ambWith(pp2.singleElement())
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp1.onNext(1);
        pp1.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult(1);
    }

    @Test
    public void ambWith2SignalsSuccess() {
        PublishProcessor<Integer> pp1 = PublishProcessor.create();
        PublishProcessor<Integer> pp2 = PublishProcessor.create();

        TestObserver<Integer> to = pp1.singleElement().ambWith(pp2.singleElement())
        .test();

        to.assertEmpty();

        assertTrue(pp1.hasSubscribers());
        assertTrue(pp2.hasSubscribers());

        pp2.onNext(2);
        pp2.onComplete();

        assertFalse(pp1.hasSubscribers());
        assertFalse(pp2.hasSubscribers());

        to.assertResult(2);
    }

    @Test
    public void zipIterableObject() {
        final List<Maybe<Integer>> maybes = Arrays.asList(Maybe.just(1), Maybe.just(4));
        Maybe.zip(maybes, (Function<Object[], Object>) o -> {
            int sum = 0;
            for (Object i : o) {
                sum += (Integer) i;
            }
            return sum;
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
            public void onSubscribe(@NonNull Disposable d) {
                this.u = d;
            }

            @Override
            public void onSuccess(@NonNull Object value) {

            }

            @Override
            public void onError(@NonNull Throwable e) {

            }

            @Override
            public void onComplete() {

            }

        };
        source.onTerminateDetach().subscribe(observer);

        System.gc();

        Thread.sleep(250);

        long after = usedMemoryNow();

        String log = String.format("%.2f MB -> %.2f MB -> %.2f MB%n",
                before / 1024.0 / 1024.0,
                middle / 1024.0 / 1024.0,
                after / 1024.0 / 1024.0);

        System.out.print(log);

        if (before * 1.3 < after) {
            fail("There seems to be a memory leak: " + log);
        }

        assertNotNull(observer); // hold onto the reference to prevent premature GC
    }

    @Test
    public void repeat() {
        Maybe.just(1).repeat().take(5).test().assertResult(1, 1, 1, 1, 1);

        Maybe.just(1).repeat(5).test().assertResult(1, 1, 1, 1, 1);

        Maybe.just(1).repeatUntil(() -> false).take(5).test().assertResult(1, 1, 1, 1, 1);

        Maybe.just(1).repeatWhen((Function<Flowable<Object>, Publisher<Object>>) v -> v).take(5).test().assertResult(1, 1, 1, 1, 1);
    }

    @Test
    public void retry() {
        Maybe.just(1).retry().test().assertResult(1);

        Maybe.just(1).retry(5).test().assertResult(1);

        Maybe.just(1).retry(Functions.alwaysTrue()).test().assertResult(1);

        Maybe.just(1).retry(5, Functions.alwaysTrue()).test().assertResult(1);

        Maybe.just(1).retry((a, e) -> true).test().assertResult(1);

        Maybe.just(1).retryUntil(() -> false).test().assertResult(1);

        Maybe.just(1).retryWhen((Function<Flowable<? extends Throwable>, Publisher<Object>>) v -> (Publisher)v).test().assertResult(1);

        final AtomicInteger calls = new AtomicInteger();
        try {
            Maybe.error(() -> {
                calls.incrementAndGet();
                return new TestException();
            }).retry(5).test();
        } finally {
            assertEquals(6, calls.get());
        }
    }

    @Test
    public void onErrorResumeWithEmpty() {
        Maybe.empty()
            .onErrorResumeWith(Maybe.just(1))
            .test()
            .assertNoValues()
            .assertNoErrors()
            .assertComplete();
    }

    @Test
    public void onErrorResumeWithValue() {
        Maybe.just(1)
            .onErrorResumeWith(Maybe.empty())
            .test()
            .assertNoErrors()
            .assertValue(1);
    }

    @Test
    public void onErrorResumeWithError() {
        Maybe.error(new RuntimeException("some error"))
            .onErrorResumeWith(Maybe.empty())
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
            .to(TestHelper.testConsumer())
            .assertError(RuntimeException.class)
            .assertErrorMessage("error")
            .assertNoValues();
    }

    @Test
    public void valueConcatWithError() {
        Maybe.just(1)
            .concatWith(Maybe.error(new RuntimeException("error")))
            .to(TestHelper.testConsumer())
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
            .concatWith(Maybe.error(new RuntimeException("error")))
            .to(TestHelper.testConsumer())
            .assertNoValues()
            .assertError(RuntimeException.class)
            .assertErrorMessage("error");
    }
}
