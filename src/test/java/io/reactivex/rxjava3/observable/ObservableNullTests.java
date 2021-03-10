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

package io.reactivex.rxjava3.observable;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

import org.junit.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Verifies the operators handle null values properly by emitting/throwing NullPointerExceptions.
 */
public class ObservableNullTests extends RxJavaTest {

    Observable<Integer> just1 = Observable.just(1);

    //***********************************************************
    // Static methods
    //***********************************************************

    @Test
    public void ambIterableIteratorNull() {
        Observable.amb((Iterable<Observable<Object>>) () -> null).test().assertError(NullPointerException.class);
    }

    @Test
    public void ambIterableOneIsNull() {
        Observable.amb(Arrays.asList(Observable.never(), null))
                .test()
                .assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestIterableIteratorNull() {
        Observable.combineLatest((Iterable<Observable<Object>>) () -> null, (Function<Object[], Object>) v -> 1, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestIterableOneIsNull() {
        Observable.combineLatest(Arrays.asList(Observable.never(), null), (Function<Object[], Object>) v -> 1, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestIterableFunctionReturnsNull() {
        Observable.combineLatest(Collections.singletonList(just1), v -> null, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableIteratorNull() {
        Observable.combineLatestDelayError((Iterable<Observable<Object>>) () -> null, (Function<Object[], Object>) v -> 1, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableOneIsNull() {
        Observable.combineLatestDelayError(Arrays.asList(Observable.never(), null), (Function<Object[], Object>) v -> 1, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void combineLatestDelayErrorIterableFunctionReturnsNull() {
        Observable.combineLatestDelayError(Collections.singletonList(just1), v -> null, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableIteratorNull() {
        Observable.concat((Iterable<Observable<Object>>) () -> null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatIterableOneIsNull() {
        Observable.concat(Arrays.asList(just1, null)).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void concatArrayOneIsNull() {
        Observable.concatArray(just1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void deferFunctionReturnsNull() {
        Observable.defer((Supplier<Observable<Object>>) () -> null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void errorFunctionReturnsNull() {
        Observable.error(() -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void fromArrayOneIsNull() {
        Observable.fromArray(1, null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromCallableReturnsNull() {
        Observable.fromCallable(() -> null).blockingLast();
    }

    @Test
    public void fromFutureReturnsNull() {
        FutureTask<Object> f = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
        f.run();

        TestObserver<Object> to = new TestObserver<>();
        Observable.fromFuture(f).subscribe(to);
        to.assertNoValues();
        to.assertNotComplete();
        to.assertError(NullPointerException.class);
    }

    @Test(expected = NullPointerException.class)
    public void fromFutureTimedReturnsNull() {
        FutureTask<Object> f = new FutureTask<>(Functions.EMPTY_RUNNABLE, null);
        f.run();
        Observable.fromFuture(f, 1, TimeUnit.SECONDS).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromIterableIteratorNull() {
        Observable.fromIterable(() -> null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void fromIterableValueNull() {
        Observable.fromIterable(Arrays.asList(1, null)).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void generateConsumerEmitsNull() {
        Observable.generate(s -> s.onNext(null)).blockingLast();
    }

    @Test
    public void generateConsumerStateNullAllowed() {
        BiConsumer<Integer, Emitter<Integer>> generator = (s, o) -> o.onComplete();
        Observable.generate(() -> null, generator).blockingSubscribe();
    }

    @Test
    public void generateFunctionStateNullAllowed() {
        Observable.generate(() -> null, (s, o) -> { o.onComplete(); return s; }).blockingSubscribe();
    }

    public void intervalSchedulerNull() {
        Observable.interval(1, TimeUnit.SECONDS, null);
    }

    @Test
    public void justNull() throws Exception {
        @SuppressWarnings("rawtypes")
        Class<Observable> clazz = Observable.class;
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
        Observable.merge((Iterable<Observable<Object>>) () -> null, 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeIterableOneIsNull() {
        Observable.merge(Arrays.asList(just1, null), 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableIteratorNull() {
        Observable.mergeDelayError((Iterable<Observable<Object>>) () -> null, 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void mergeDelayErrorIterableOneIsNull() {
        Observable.mergeDelayError(Arrays.asList(just1, null), 128, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void usingObservableSupplierReturnsNull() {
        Observable.using((Supplier<Object>) () -> 1, (Function<Object, Observable<Object>>) d -> null, d -> { }).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableIteratorNull() {
        Observable.zip((Iterable<Observable<Object>>) () -> null, (Function<Object[], Object>) v -> 1).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterableFunctionReturnsNull() {
        Observable.zip(Arrays.asList(just1, just1), a -> null).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterable2IteratorNull() {
        Observable.zip((Iterable<Observable<Object>>) () -> null, (Function<Object[], Object>) a -> 1, true, 128).blockingLast();
    }

    @Test(expected = NullPointerException.class)
    public void zipIterable2FunctionReturnsNull() {
        Observable.zip(Arrays.asList(just1, just1), a -> null, true, 128).blockingLast();
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
        just1.buffer(just1, (Function<Integer, Observable<Object>>) v -> null).blockingSubscribe();
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
    public void collectInitialCollectorNull() {
        just1.collect((Supplier<Object>) () -> 1, null);
    }

    @Test(expected = NullPointerException.class)
    public void concatMapReturnsNull() {
        just1.concatMap((Function<Integer, Observable<Object>>) v -> null).blockingSubscribe();
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
        just1.debounce((Function<Integer, Observable<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void delayWithFunctionReturnsNull() {
        just1.delay((Function<Integer, Observable<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void delayBothItemSupplierReturnsNull() {
        just1.delay(just1
        , (Function<Integer, Observable<Object>>) v -> null).blockingSubscribe();
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
        Observable.range(1, 2).distinctUntilChanged(v -> null).test().assertResult(1);
    }

    @Test(expected = NullPointerException.class)
    public void flatMapFunctionReturnsNull() {
        just1.flatMap((Function<Integer, Observable<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnNextReturnsNull() {
        just1.flatMap((Function<Integer, Observable<Integer>>) v -> null, (Function<Throwable, Observable<Integer>>) e -> just1, (Supplier<Observable<Integer>>) () -> just1).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapNotificationOnCompleteReturnsNull() {
        just1.flatMap((Function<Integer, Observable<Integer>>) v -> just1, (Function<Throwable, Observable<Integer>>) e -> just1, (Supplier<Observable<Integer>>) () -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapCombinerMapperReturnsNull() {
        just1.flatMap((Function<Integer, Observable<Object>>) v -> null, (BiFunction<Integer, Object, Object>) (a, b) -> 1).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapCombinerCombinerReturnsNull() {
        just1.flatMap((Function<Integer, Observable<Integer>>) v -> just1, (a, b) -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void flatMapIterableMapperReturnsNull() {
        just1.flatMapIterable((Function<Integer, Iterable<Integer>>) v -> null).blockingSubscribe();
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
        just1.flatMapIterable((Function<Integer, Iterable<Integer>>) v -> Arrays.asList(1), (a, b) -> null).blockingSubscribe();
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
        just1.lift(observer -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void mapReturnsNull() {
        just1.map(v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void onErrorResumeNextFunctionReturnsNull() {
        Observable.error(new TestException()).onErrorResumeNext((Function<Throwable, Observable<Object>>) e -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void onErrorReturnFunctionReturnsNull() {
        Observable.error(new TestException()).onErrorReturn(e -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void publishFunctionReturnsNull() {
        just1.publish((Function<Observable<Integer>, Observable<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void reduceFunctionReturnsNull() {
        Observable.just(1, 1).reduce((a, b) -> null).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void reduceSeedFunctionReturnsNull() {
        just1.reduce(1, (a, b) -> null).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void reduceWithSeedReturnsNull() {
        just1.reduceWith(() -> null, (a, b) -> 1).blockingGet();
    }

    @Test(expected = NullPointerException.class)
    public void repeatWhenFunctionReturnsNull() {
        just1.repeatWhen((Function<Observable<Object>, Observable<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replaySelectorReturnsNull() {
        just1.replay((Function<Observable<Integer>, Observable<Object>>) o -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replayBoundedSelectorReturnsNull() {
        just1.replay((Function<Observable<Integer>, Observable<Object>>) v -> null, 1, 1, TimeUnit.SECONDS).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void replayTimeBoundedSelectorReturnsNull() {
        just1.replay((Function<Observable<Integer>, Observable<Object>>) v -> null, 1, TimeUnit.SECONDS, Schedulers.single()).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void retryWhenFunctionReturnsNull() {
        Observable.error(new TestException()).retryWhen((Function<Observable<? extends Throwable>, Observable<Object>>) f -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void scanFunctionReturnsNull() {
        Observable.just(1, 1).scan((a, b) -> null).blockingSubscribe();
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
    public void switchMapFunctionReturnsNull() {
        just1.switchMap((Function<Integer, Observable<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void timeoutSelectorReturnsNull() {
        just1.timeout((Function<Integer, Observable<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void timeoutFirstItemReturnsNull() {
        Observable.just(1, 1).timeout(Observable.never(), (Function<Integer, Observable<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void toListSupplierReturnsNull() {
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
    public void windowOpenCloseCloseReturnsNull() {
        Observable.never().window(just1, (Function<Integer, Observable<Object>>) v -> null).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void withLatestFromCombinerReturnsNull() {
        just1.withLatestFrom(just1, (a, b) -> null).blockingSubscribe();
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
        Observable.just(1, 2).zipWith(Arrays.asList(1, null), (BiFunction<Integer, Integer, Object>) (a, b) -> 1).blockingSubscribe();
    }

    @Test(expected = NullPointerException.class)
    public void zipWithCombinerReturnsNull() {
        just1.zipWith(just1, (a, b) -> null).blockingSubscribe();
    }

}
