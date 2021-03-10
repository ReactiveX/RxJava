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

package io.reactivex.rxjava3.flowable;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.processors.*;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.reactivex.rxjava3.testsupport.TestHelper;

/**
 * Verifies the operators handle null values properly by emitting/throwing NullPointerExceptions.
 */
public class FlowableNullTests extends RxJavaTest {

    Flowable<Integer> just1 = Flowable.just(1);

    //***********************************************************
    // Static methods
    //***********************************************************

    @Test(expected = NullPointerException.class)
    public void ambVarargsOneIsNull() {
        Flowable.ambArray(Flowable.never(), null).blockingLast();
    }

    @Test
    public void ambIterableIteratorNull() {
        Flowable.amb((Iterable<Publisher<Object>>) () -> null).test().assertError(NullPointerException.class);
    }

    @Test
    public void ambIterableOneIsNull() {
        Flowable.amb(Arrays.asList(Flowable.never(), null))
                .test()
                .assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestIterableIteratorNull() {
        Flowable.combineLatestDelayError((Iterable<Publisher<Object>>) () -> null, (Function<Object[], Object>) v -> 1).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestIterableOneIsNull() {
        Flowable.combineLatestDelayError(Arrays.asList(Flowable.never(), null), (Function<Object[], Object>) v -> 1).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestIterableFunctionReturnsNull() {
        Flowable.combineLatestDelayError(Collections.singletonList(just1), v -> null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableIteratorNull() {
        Flowable.concat((Iterable<Publisher<Object>>) () -> null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableOneIsNull() {
        Flowable.concat(Arrays.asList(just1, null)).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatArrayOneIsNull() {
        Flowable.concatArray(just1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void deferFunctionReturnsNull() {
        Flowable.defer((Supplier<Publisher<Object>>) () -> null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void errorFunctionReturnsNull() {
        Flowable.error(() -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void fromArrayOneIsNull() {
        Flowable.fromArray(1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromCallableReturnsNull() {
        Flowable.fromCallable(() -> null).blockingLast();
    }

    @Test
    public void fromFutureReturnsNull() {
        FutureTask<Object> f = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
        f.run();

        TestSubscriber<Object> ts = new TestSubscriber<>();
        Flowable.fromFuture(f).subscribe(ts);
        ts.assertNoValues();
        ts.assertNotComplete();
        ts.assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedReturnsNull() {
      FutureTask<Object> f = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
        f.run();
        Flowable.fromFuture(f, 1, TimeUnit.SECONDS).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromIterableIteratorNull() {
        Flowable.fromIterable(() -> null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromIterableValueNull() {
        Flowable.fromIterable(Arrays.asList(1, null)).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void generateConsumerEmitsNull() {
        Flowable.generate(s -> s.onNext(null)).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void generateStateConsumerInitialStateNull() {
        BiConsumer<Integer, Emitter<Integer>> generator = (s, o) -> o.onNext(1);
        Flowable.generate(null, generator);
    }

    @Test(expected = NullPointerException.class)
    public void generateStateFunctionInitialStateNull() {
        Flowable.generate(null, (s, o) -> {
            o.onNext(1); return s;
        });
    }

    @Test(expected = NullPointerException.class)
    public void generateStateConsumerNull() {
        Flowable.generate(() -> 1, (BiConsumer<Integer, Emitter<Object>>)null);
    }

    @Test
    public void generateConsumerStateNullAllowed() {
        BiConsumer<Integer, Emitter<Integer>> generator = (s, o) -> o.onComplete();
        Flowable.generate(() -> null, generator).blockingSubscribe();
    }

    @Test
    public void generateFunctionStateNullAllowed() {
        Flowable.generate(() -> null, (s, o) -> {
            o.onComplete(); return s;
        }).blockingSubscribe();
    }

    @Test
    public void justNull() throws Exception {
        @SuppressWarnings("rawtypes")
        Class<Flowable> clazz = Flowable.class;
        for (int argCount = 1; argCount < 10; argCount++) {
            for (int argNull = 1; argNull <= argCount; argNull++) {
                Class<?>[] params = new Class[argCount];
                Arrays.fill(params, Object.class);

                Object[] values = new Object[argCount];
                Arrays.fill(values, 1);
                values[argNull - 1] = null;

                Method m = clazz.getMethod("just", params);

                try {
                    m.invoke(null, values);
                    Assert.fail("No exception for argCount " + argCount + " / argNull " + argNull);
                } catch (InvocationTargetException ex) {
                    if (!(ex.getCause() instanceof NullPointerException)) {
                        Assert.fail("Unexpected exception for argCount " + argCount + " / argNull " + argNull + ": " + ex);
                    }
                }
            }
        }
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableIteratorNull() {
        Flowable.merge((Iterable<Publisher<Object>>) () -> null, 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableOneIsNull() {
        Flowable.merge(Arrays.asList(just1, null), 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeArrayOneIsNull() {
        Flowable.mergeArray(128, 128, just1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableIteratorNull() {
        Flowable.mergeDelayError((Iterable<Publisher<Object>>) () -> null, 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableOneIsNull() {
        Flowable.mergeDelayError(Arrays.asList(just1, null), 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorArrayOneIsNull() {
        Flowable.mergeArrayDelayError(128, 128, just1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void usingFlowableSupplierReturnsNull() {
        Flowable.using((Supplier<Object>) () -> 1, (Function<Object, Publisher<Object>>) d -> null, Functions.emptyConsumer()).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableIteratorNull() {
        Flowable.zip((Iterable<Publisher<Object>>) () -> null, (Function<Object[], Object>) v -> 1).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableFunctionReturnsNull() {
        Flowable.zip(Arrays.asList(just1, just1), a -> null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterable2Null() {
        Flowable.zip(null, (Function<Object[], Object>) a -> 1, true, 128);
    }

    @Test(expected = NullPointerException.class)
    public void zipIterable2IteratorNull() {
        Flowable.zip((Iterable<Publisher<Object>>) () -> null, (Function<Object[], Object>) a -> 1, true, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterable2FunctionReturnsNull() {
        Flowable.zip(Arrays.asList(just1, just1), a -> null, true, 128).blockingLast();
    }

    //*************************************************************
    // Instance methods
    //*************************************************************

    @Test(expected = NullPointerException.class)
    public void bufferSupplierReturnsNull() {
        just1.buffer(1, 1, (Supplier<Collection<Integer>>) () -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferTimedSupplierReturnsNull() {
        just1.buffer(1L, 1L, TimeUnit.SECONDS, Schedulers.single(), (Supplier<Collection<Integer>>) () -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferOpenCloseCloseReturnsNull() {
        just1.buffer(just1, (Function<Integer, Publisher<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void bufferBoundarySupplierReturnsNull() {
        just1.buffer(just1, (Supplier<Collection<Integer>>) () -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void collectInitialSupplierReturnsNull() {
        just1.collect(() -> null, (a, b) -> { }).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void concatMapReturnsNull() {
        just1.concatMap((Function<Integer, Publisher<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void concatMapIterableReturnNull() {
        just1.concatMapIterable((Function<Integer, Iterable<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void concatMapIterableIteratorNull() {
        just1.concatMapIterable((Function<Integer, Iterable<Object>>) v -> () -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void debounceFunctionReturnsNull() {
        just1.debounce(v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void delayWithFunctionReturnsNull() {
        just1.delay(v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void delayBothItemSupplierReturnsNull() {
        just1.delay(just1, v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void distinctSupplierReturnsNull() {
        just1.distinct((Function<Integer, Object>) v -> v, () -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void distinctFunctionReturnsNull() {
        just1.distinct(v -> null).blockingSubscribe();
    }

    @Test
    public void distinctUntilChangedFunctionReturnsNull() {
        Flowable.range(1, 2).distinctUntilChanged(v -> null).test().assertResult(1);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapFunctionReturnsNull() {
        just1.flatMap((Function<Integer, Publisher<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnNextReturnsNull() {
        just1.flatMap((Function<Integer, Publisher<Integer>>) v -> null, (Function<Throwable, Publisher<Integer>>) e -> just1, (Supplier<Publisher<Integer>>) () -> just1).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnCompleteReturnsNull() {
        just1.flatMap((Function<Integer, Publisher<Integer>>) v -> just1, (Function<Throwable, Publisher<Integer>>) e -> just1, (Supplier<Publisher<Integer>>) () -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapCombinerMapperReturnsNull() {
        just1.flatMap((Function<Integer, Publisher<Object>>) v -> null, (BiFunction<Integer, Object, Object>) (a, b) -> 1).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapCombinerCombinerReturnsNull() {
        just1.flatMap((Function<Integer, Publisher<Integer>>) v -> just1, (a, b) -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperReturnsNull() {
        just1.flatMapIterable((Function<Integer, Iterable<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperIteratorNull() {
        just1.flatMapIterable((Function<Integer, Iterable<Object>>) v -> () -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperIterableOneNull() {
        just1.flatMapIterable((Function<Integer, Iterable<Integer>>) v -> Arrays.asList(1, null)).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableCombinerReturnsNull() {
        just1.flatMapIterable((Function<Integer, Iterable<Integer>>) v -> Collections.singletonList(1), (a, b) -> null).blockingSubscribe();
    }

    public void groupByKeyNull() {
        just1.groupBy(v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void groupByValueReturnsNull() {
        just1.groupBy((Function<Integer, Object>) v -> v, v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void liftReturnsNull() {
        just1.lift(s -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void mapReturnsNull() {
        just1.map(v -> null).blockingSubscribe();
    }

    @Test
    public void onErrorResumeNextFunctionReturnsNull() {
        try {
            Flowable.error(new TestException()).onErrorResumeNext((Function<Throwable, Publisher<Object>>) e -> null).blockingSubscribe();
            fail("Should have thrown");
        } catch (CompositeException ex) {
            List<Throwable> errors = ex.getExceptions();
            TestHelper.assertError(errors, 0, TestException.class);
            TestHelper.assertError(errors, 1, NullPointerException.class);
            assertEquals(2, errors.size());
        }
    }

    @Test
    public void onErrorReturnFunctionReturnsNull() {
        try {
            Flowable.error(new TestException()).onErrorReturn(e -> null).blockingSubscribe();
            fail("Should have thrown");
        } catch (CompositeException ex) {
            List<Throwable> errors = TestHelper.compositeList(ex);

            TestHelper.assertError(errors, 0, TestException.class);
            TestHelper.assertError(errors, 1, NullPointerException.class, "The valueSupplier returned a null value");
        }
    }

    @Test(expected = NullPointerException.class)
    public void publishFunctionReturnsNull() {
        just1.publish(v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void reduceFunctionReturnsNull() {
        Flowable.just(1, 1).reduce((a, b) -> null).toFlowable().blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void reduceSeedFunctionReturnsNull() {
        just1.reduce(1, (a, b) -> null).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void reduceWithSeedNull() {
        just1.reduceWith(null, (a, b) -> 1);
    }

    @Test(expected = NullPointerException.class)
    public void reduceWithSeedReturnsNull() {
        just1.reduceWith(() -> null, (a, b) -> 1).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void repeatWhenFunctionReturnsNull() {
        just1.repeatWhen((Function<Flowable<Object>, Publisher<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replaySelectorNull() {
        just1.replay((Function<Flowable<Integer>, Flowable<Integer>>)null);
    }

    @Test(expected = NullPointerException.class)
    public void replaySelectorReturnsNull() {
        just1.replay(f -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replayBoundedSelectorReturnsNull() {
        just1.replay(v -> null, 1, 1, TimeUnit.SECONDS).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeBoundedSelectorReturnsNull() {
        just1.replay(v -> null, 1, TimeUnit.SECONDS, Schedulers.single()).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void retryWhenFunctionReturnsNull() {
        Flowable.error(new TestException()).retryWhen((Function<Flowable<? extends Throwable>, Publisher<Object>>) f -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void scanFunctionReturnsNull() {
        Flowable.just(1, 1).scan((a, b) -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedNull() {
        just1.scan(null, (a, b) -> 1);
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedFunctionReturnsNull() {
        just1.scan(1, (a, b) -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedSupplierReturnsNull() {
        just1.scanWith(() -> null, (a, b) -> 1).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void scanSeedSupplierFunctionReturnsNull() {
        just1.scanWith((Supplier<Object>) () -> 1, (a, b) -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void startWithIterableIteratorNull() {
        just1.startWithIterable(() -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void startWithIterableOneNull() {
        just1.startWithIterable(Arrays.asList(1, null)).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void startWithArrayOneNull() {
        just1.startWithArray(1, null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void switchMapFunctionReturnsNull() {
        just1.switchMap((Function<Integer, Publisher<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void timeoutSelectorReturnsNull() {
        just1.timeout(v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void timeoutSelectorOtherNull() {
        just1.timeout((Function<Integer, Publisher<Integer>>) v -> just1, null);
    }

    @Test(expected = NullPointerException.class)
    public void timeoutFirstItemReturnsNull() {
        just1.timeout(Flowable.never(), v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void timestampUnitNull() {
        just1.timestamp(null, Schedulers.single());
    }

    @Test(expected = NullPointerException.class)
    public void timestampSchedulerNull() {
        just1.timestamp(TimeUnit.SECONDS, null);
    }

    @Test(expected = NullPointerException.class)
    public void toListSupplierReturnsNull() {
        just1.toList((Supplier<Collection<Integer>>) () -> null).toFlowable().blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void toListSupplierReturnsNullSingle() {
        just1.toList((Supplier<Collection<Integer>>) () -> null).blockingGet();
    }

    @Test
    public void toMapValueSelectorReturnsNull() {
        just1.toMap((Function<Integer, Object>) v -> v, v -> null).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void toMapMapSupplierReturnsNull() {
        just1.toMap((Function<Integer, Object>) v -> v, (Function<Integer, Object>) v -> v, () -> null).blockingGet();
    }

    @Test
    public void toMultiMapValueSelectorReturnsNullAllowed() {
        just1.toMap((Function<Integer, Object>) v -> v, v -> null).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapMapSupplierReturnsNull() {
        just1.toMultimap((Function<Integer, Object>) v -> v, (Function<Integer, Object>) v -> v, () -> null).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void toMultimapMapCollectionSupplierReturnsNull() {
        just1.toMultimap(v -> v, v -> v, (Supplier<Map<Integer, Collection<Integer>>>) HashMap::new, (Function<Integer, Collection<Integer>>) v -> null).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void windowOpenCloseOpenNull() {
        just1.window(null, v -> just1);
    }

    @Test(expected = NullPointerException.class)
    public void windowOpenCloseCloseReturnsNull() {
        Flowable.never().window(just1, v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void withLatestFromOtherNull() {
        just1.withLatestFrom(null, (BiFunction<Integer, Object, Object>) (a, b) -> 1);
    }

    @Test(expected = NullPointerException.class)
    public void withLatestFromCombinerReturnsNull() {
        just1.withLatestFrom(just1, (a, b) -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableNull() {
        just1.zipWith((Iterable<Integer>)null, (BiFunction<Integer, Integer, Object>) (a, b) -> 1);
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableCombinerReturnsNull() {
        just1.zipWith(Collections.singletonList(1), (a, b) -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableIteratorNull() {
        just1.zipWith(() -> null, (BiFunction<Integer, Object, Object>) (a, b) -> 1).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithIterableOneIsNull() {
        Flowable.just(1, 2).zipWith(Arrays.asList(1, null), (BiFunction<Integer, Integer, Object>) (a, b) -> 1).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithPublisherNull() {
        just1.zipWith((Publisher<Integer>)null, (BiFunction<Integer, Integer, Object>) (a, b) -> 1);
    }

    @Test(expected = NullPointerException.class)
    public void zipWithCombinerReturnsNull() {
        just1.zipWith(just1, (a, b) -> null).blockingSubscribe();
    }

    //*********************************************
    // Subject null tests
    //*********************************************

    @Test(expected = NullPointerException.class)
    public void asyncSubjectOnNextNull() {
        FlowableProcessor<Integer> processor = AsyncProcessor.create();
        processor.onNext(null);
        processor.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void asyncSubjectOnErrorNull() {
        FlowableProcessor<Integer> processor = AsyncProcessor.create();
        processor.onError(null);
        processor.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void behaviorSubjectOnNextNull() {
        FlowableProcessor<Integer> processor = BehaviorProcessor.create();
        processor.onNext(null);
        processor.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void behaviorSubjectOnErrorNull() {
        FlowableProcessor<Integer> processor = BehaviorProcessor.create();
        processor.onError(null);
        processor.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void publishSubjectOnNextNull() {
        FlowableProcessor<Integer> processor = PublishProcessor.create();
        processor.onNext(null);
        processor.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void publishSubjectOnErrorNull() {
        FlowableProcessor<Integer> processor = PublishProcessor.create();
        processor.onError(null);
        processor.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replaycSubjectOnNextNull() {
        FlowableProcessor<Integer> processor = ReplayProcessor.create();
        processor.onNext(null);
        processor.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replaySubjectOnErrorNull() {
        FlowableProcessor<Integer> processor = ReplayProcessor.create();
        processor.onError(null);
        processor.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void serializedcSubjectOnNextNull() {
        FlowableProcessor<Integer> processor = PublishProcessor.<Integer>create().toSerialized();
        processor.onNext(null);
        processor.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void serializedSubjectOnErrorNull() {
        FlowableProcessor<Integer> processor = PublishProcessor.<Integer>create().toSerialized();
        processor.onError(null);
        processor.blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableFunctionReturnsNull() {
        Flowable.combineLatestDelayError(Collections.singletonList(just1), v -> null, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableIteratorNull() {
        Flowable.combineLatestDelayError((Iterable<Flowable<Object>>) () -> null, (Function<Object[], Object>) v -> 1, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableOneIsNull() {
        Flowable.combineLatestDelayError(Arrays.asList(Flowable.never(), null), (Function<Object[], Object>) v -> 1, 128).blockingLast();
    }
}
