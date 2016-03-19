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
package io.reactivex;


import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.annotations.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.functions.Objects;
import io.reactivex.internal.operators.flowable.*;
import io.reactivex.internal.subscribers.flowable.*;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.*;
import io.reactivex.subscribers.*;

public class Flowable<T> implements Publisher<T> {
    /**
     * Interface to map/wrap a downstream subscriber to an upstream subscriber.
     *
     * @param <Downstream> the value type of the downstream
     * @param <Upstream> the value type of the upstream
     */
    public interface Operator<Downstream, Upstream> extends Function<Subscriber<? super Downstream>, Subscriber<? super Upstream>> {

    }
    
    /**
     * Interface to compose observables.
     *
     * @param <T> the upstream value type
     * @param <R> the downstream value type
     */
    public interface Transformer<T, R> extends Function<Flowable<T>, Publisher<? extends R>> {
        
    }

    /** The default buffer size. */
    static final int BUFFER_SIZE;
    static {
        BUFFER_SIZE = Math.max(16, Integer.getInteger("rx2.buffer-size", 128));
    }

    /** An empty observable instance as there is no need to instantiate this more than once. */
    static final Flowable<Object> EMPTY = create(PublisherEmptySource.INSTANCE);

    /** A never observable instance as there is no need to instantiate this more than once. */
    static final Flowable<Object> NEVER = create(new Publisher<Object>() {
        @Override
        public void subscribe(Subscriber<? super Object> s) {
            s.onSubscribe(EmptySubscription.INSTANCE);
        }
    });

    public static <T> Flowable<T> amb(Iterable<? extends Publisher<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return create(new PublisherAmb<T>(null, sources));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> amb(Publisher<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        int len = sources.length;
        if (len == 0) {
            return empty();
        } else
        if (len == 1) {
            return fromPublisher(sources[0]);
        }
        return create(new PublisherAmb<T>(sources, null));
    }

    public static int bufferSize() {
        return BUFFER_SIZE;
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, R> Flowable<R> combineLatest(Function<? super Object[], ? extends R> combiner, boolean delayError, int bufferSize, Publisher<? extends T>... sources) {
        return combineLatest(sources, combiner, delayError, bufferSize);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, R> Flowable<R> combineLatest(Iterable<? extends Publisher<? extends T>> sources, Function<? super Object[], ? extends R> combiner) {
        return combineLatest(sources, combiner, false, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, R> Flowable<R> combineLatest(Iterable<? extends Publisher<? extends T>> sources, Function<? super Object[], ? extends R> combiner, boolean delayError) {
        return combineLatest(sources, combiner, delayError, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, R> Flowable<R> combineLatest(Iterable<? extends Publisher<? extends T>> sources, Function<? super Object[], ? extends R> combiner, boolean delayError, int bufferSize) {
        Objects.requireNonNull(sources, "sources is null");
        Objects.requireNonNull(combiner, "combiner is null");
        validateBufferSize(bufferSize);
        
        // the queue holds a pair of values so we need to double the capacity
        int s = bufferSize << 1;
        return create(new PublisherCombineLatest<T, R>(null, sources, combiner, s, delayError));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, R> Flowable<R> combineLatest(Publisher<? extends T>[] sources, Function<? super Object[], ? extends R> combiner) {
        return combineLatest(sources, combiner, false, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, R> Flowable<R> combineLatest(Publisher<? extends T>[] sources, Function<? super Object[], ? extends R> combiner, boolean delayError) {
        return combineLatest(sources, combiner, delayError, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, R> Flowable<R> combineLatest(Publisher<? extends T>[] sources, Function<? super Object[], ? extends R> combiner, boolean delayError, int bufferSize) {
        validateBufferSize(bufferSize);
        Objects.requireNonNull(combiner, "combiner is null");
        if (sources.length == 0) {
            return empty();
        }
        // the queue holds a pair of values so we need to double the capacity
        int s = bufferSize << 1;
        return create(new PublisherCombineLatest<T, R>(sources, null, combiner, s, delayError));
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, R> Flowable<R> combineLatest(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, 
            BiFunction<? super T1, ? super T2, ? extends R> combiner) {
        Function<Object[], R> f = Functions.toFunction(combiner);
        return combineLatest(f, false, bufferSize(), p1, p2);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, R> Flowable<R> combineLatest(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, 
            Publisher<? extends T3> p3, 
            Function3<? super T1, ? super T2, ? super T3, ? extends R> combiner) {
        return combineLatest(Functions.toFunction(combiner), false, bufferSize(), p1, p2, p3);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, T4, R> Flowable<R> combineLatest(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, 
            Publisher<? extends T3> p3, Publisher<? extends T4> p4,
            Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> combiner) {
        return combineLatest(Functions.toFunction(combiner), false, bufferSize(), p1, p2, p3, p4);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, T4, T5, R> Flowable<R> combineLatest(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, 
            Publisher<? extends T3> p3, Publisher<? extends T4> p4,
            Publisher<? extends T5> p5,
            Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> combiner) {
        return combineLatest(Functions.toFunction(combiner), false, bufferSize(), p1, p2, p3, p4, p5);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, T4, T5, T6, R> Flowable<R> combineLatest(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, 
            Publisher<? extends T3> p3, Publisher<? extends T4> p4,
            Publisher<? extends T5> p5, Publisher<? extends T6> p6,
            Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> combiner) {
        return combineLatest(Functions.toFunction(combiner), false, bufferSize(), p1, p2, p3, p4, p5, p6);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, R> Flowable<R> combineLatest(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, 
            Publisher<? extends T3> p3, Publisher<? extends T4> p4,
            Publisher<? extends T5> p5, Publisher<? extends T6> p6,
            Publisher<? extends T7> p7,
            Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> combiner) {
        return combineLatest(Functions.toFunction(combiner), false, bufferSize(), p1, p2, p3, p4, p5, p6, p7);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Flowable<R> combineLatest(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, 
            Publisher<? extends T3> p3, Publisher<? extends T4> p4,
            Publisher<? extends T5> p5, Publisher<? extends T6> p6,
            Publisher<? extends T7> p7, Publisher<? extends T8> p8,
            Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> combiner) {
        return combineLatest(Functions.toFunction(combiner), false, bufferSize(), p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Flowable<R> combineLatest(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, 
            Publisher<? extends T3> p3, Publisher<? extends T4> p4,
            Publisher<? extends T5> p5, Publisher<? extends T6> p6,
            Publisher<? extends T7> p7, Publisher<? extends T8> p8,
            Publisher<? extends T9> p9,
            Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> combiner) {
        return combineLatest(Functions.toFunction(combiner), false, bufferSize(), p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> concat(int prefetch, Iterable<? extends Publisher<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return fromIterable(sources).concatMap((Function)Functions.identity(), prefetch);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> concat(Iterable<? extends Publisher<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return fromIterable(sources).concatMap((Function)Functions.identity());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static final <T> Flowable<T> concat(Publisher<? extends Publisher<? extends T>> sources) {
        return concat(sources, bufferSize());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static final <T> Flowable<T> concat(Publisher<? extends Publisher<? extends T>> sources, int bufferSize) {
        return fromPublisher(sources).concatMap((Function)Functions.identity());
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> concat(Publisher<? extends T> p1, Publisher<? extends T> p2) {
        return concatArray(p1, p2);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> concat(
            Publisher<? extends T> p1, Publisher<? extends T> p2,
            Publisher<? extends T> p3) {
        return concatArray(p1, p2, p3);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> concat(
            Publisher<? extends T> p1, Publisher<? extends T> p2,
            Publisher<? extends T> p3, Publisher<? extends T> p4) {
        return concatArray(p1, p2, p3, p4);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> concat(
            Publisher<? extends T> p1, Publisher<? extends T> p2, 
            Publisher<? extends T> p3, Publisher<? extends T> p4,
            Publisher<? extends T> p5
    ) {
        return concatArray(p1, p2, p3, p4, p5);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> concat(
            Publisher<? extends T> p1, Publisher<? extends T> p2, 
            Publisher<? extends T> p3, Publisher<? extends T> p4,
            Publisher<? extends T> p5, Publisher<? extends T> p6
    ) {
        return concatArray(p1, p2, p3, p4, p5, p6);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> concat(
            Publisher<? extends T> p1, Publisher<? extends T> p2,
            Publisher<? extends T> p3, Publisher<? extends T> p4,
            Publisher<? extends T> p5, Publisher<? extends T> p6,
            Publisher<? extends T> p7
    ) {
        return concatArray(p1, p2, p3, p4, p5, p6, p7);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> concat(
            Publisher<? extends T> p1, Publisher<? extends T> p2, 
            Publisher<? extends T> p3, Publisher<? extends T> p4,
            Publisher<? extends T> p5, Publisher<? extends T> p6,
            Publisher<? extends T> p7, Publisher<? extends T> p8
    ) {
        return concatArray(p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> concat(
            Publisher<? extends T> p1, Publisher<? extends T> p2, 
            Publisher<? extends T> p3, Publisher<? extends T> p4,
            Publisher<? extends T> p5, Publisher<? extends T> p6,
            Publisher<? extends T> p7, Publisher<? extends T> p8,
            Publisher<? extends T> p9
    ) {
        return concatArray(p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> concatArray(int prefetch, Publisher<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        return fromArray(sources).concatMap((Function)Functions.identity(), prefetch);
    }

    /**
     * Concatenates a variable number of Observable sources.
     * <p>
     * Note: named this way because of overload conflict with concat(NbpObservable&lt;NbpObservable&gt)
     * @param sources the array of sources
     * @param <T> the common base value type
     * @return the new NbpObservable instance
     * @throws NullPointerException if sources is null
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> concatArray(Publisher<? extends T>... sources) {
        if (sources.length == 0) {
            return empty();
        } else
        if (sources.length == 1) {
            return fromPublisher(sources[0]);
        }
        return fromArray(sources).concatMap((Function)Functions.identity());
    }

    @BackpressureSupport(BackpressureKind.NONE)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> create(Publisher<T> onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        onSubscribe = RxJavaPlugins.onCreate(onSubscribe);
        return new Flowable<T>(onSubscribe);
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> defer(Supplier<? extends Publisher<? extends T>> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return create(new PublisherDefer<T>(supplier));
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> empty() {
        return (Flowable<T>)EMPTY;
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> error(Supplier<? extends Throwable> errorSupplier) {
        Objects.requireNonNull(errorSupplier, "errorSupplier is null");
        return create(new PublisherErrorSource<T>(errorSupplier));
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> error(final Throwable e) {
        Objects.requireNonNull(e, "e is null");
        return error(new Supplier<Throwable>() {
            @Override
            public Throwable get() {
                return e;
            }
        });
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> fromArray(T... values) {
        Objects.requireNonNull(values, "values is null");
        if (values.length == 0) {
            return empty();
        } else
            if (values.length == 1) {
                return just(values[0]);
            }
        return create(new PublisherArraySource<T>(values));
    }

    // TODO match naming with RxJava 1.x
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> fromCallable(Callable<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        return create(new PublisherScalarAsyncSource<T>(supplier));
    }

    /*
     * It doesn't add cancellation support by default like 1.x
     * if necessary, one can use composition to achieve it:
     * futureObservable.doOnCancel(() -> future.cancel(true));
     */
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> fromFuture(Future<? extends T> future) {
        Objects.requireNonNull(future, "future is null");
        Flowable<T> o = create(new PublisherFutureSource<T>(future, 0L, null));
        
        return o;
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> fromFuture(Future<? extends T> future, long timeout, TimeUnit unit) {
        Objects.requireNonNull(future, "future is null");
        Objects.requireNonNull(unit, "unit is null");
        Flowable<T> o = create(new PublisherFutureSource<T>(future, timeout, unit));
        return o;
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public static <T> Flowable<T> fromFuture(Future<? extends T> future, long timeout, TimeUnit unit, Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        Flowable<T> o = fromFuture(future, timeout, unit); 
        return o.subscribeOn(scheduler);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.IO)
    public static <T> Flowable<T> fromFuture(Future<? extends T> future, Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        Flowable<T> o = fromFuture(future);
        return o.subscribeOn(Schedulers.io());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> fromIterable(Iterable<? extends T> source) {
        Objects.requireNonNull(source, "source is null");
        return create(new PublisherIterableSource<T>(source));
    }
    
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> fromPublisher(final Publisher<? extends T> publisher) {
        if (publisher instanceof Flowable) {
            return (Flowable<T>)publisher;
        }
        Objects.requireNonNull(publisher, "publisher is null");

        return create(new Publisher<T>() {
            @Override
            public void subscribe(Subscriber<? super T> s) {
                publisher.subscribe(s);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> generate(final Consumer<Subscriber<T>> generator) {
        Objects.requireNonNull(generator, "generator is null");
        return generate(Functions.nullSupplier(), 
        new BiFunction<Object, Subscriber<T>, Object>() {
            @Override
            public Object apply(Object s, Subscriber<T> o) {
                generator.accept(o);
                return s;
            }
        }, Functions.emptyConsumer());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, S> Flowable<T> generate(Supplier<S> initialState, final BiConsumer<S, Subscriber<T>> generator) {
        Objects.requireNonNull(generator, "generator is null");
        return generate(initialState, new BiFunction<S, Subscriber<T>, S>() {
            @Override
            public S apply(S s, Subscriber<T> o) {
                generator.accept(s, o);
                return s;
            }
        }, Functions.emptyConsumer());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, S> Flowable<T> generate(Supplier<S> initialState, final BiConsumer<S, Subscriber<T>> generator, Consumer<? super S> disposeState) {
        Objects.requireNonNull(generator, "generator is null");
        return generate(initialState, new BiFunction<S, Subscriber<T>, S>() {
            @Override
            public S apply(S s, Subscriber<T> o) {
                generator.accept(s, o);
                return s;
            }
        }, disposeState);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, S> Flowable<T> generate(Supplier<S> initialState, BiFunction<S, Subscriber<T>, S> generator) {
        return generate(initialState, generator, Functions.emptyConsumer());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, S> Flowable<T> generate(Supplier<S> initialState, BiFunction<S, Subscriber<T>, S> generator, Consumer<? super S> disposeState) {
        Objects.requireNonNull(initialState, "initialState is null");
        Objects.requireNonNull(generator, "generator is null");
        Objects.requireNonNull(disposeState, "disposeState is null");
        return create(new PublisherGenerate<T, S>(initialState, generator, disposeState));
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public static Flowable<Long> interval(long initialDelay, long period, TimeUnit unit) {
        return interval(initialDelay, period, unit, Schedulers.computation());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public static Flowable<Long> interval(long initialDelay, long period, TimeUnit unit, Scheduler scheduler) {
        if (initialDelay < 0) {
            initialDelay = 0L;
        }
        if (period < 0) {
            period = 0L;
        }
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");

        return create(new PublisherIntervalSource(initialDelay, period, unit, scheduler));
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public static Flowable<Long> interval(long period, TimeUnit unit) {
        return interval(period, period, unit, Schedulers.computation());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public static Flowable<Long> interval(long period, TimeUnit unit, Scheduler scheduler) {
        return interval(period, period, unit, scheduler);
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public static Flowable<Long> intervalRange(long start, long count, long initialDelay, long period, TimeUnit unit) {
        return intervalRange(start, count, initialDelay, period, unit, Schedulers.computation());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public static Flowable<Long> intervalRange(long start, long count, long initialDelay, long period, TimeUnit unit, Scheduler scheduler) {

        long end = start + (count - 1);
        if (end < 0) {
            throw new IllegalArgumentException("Overflow! start + count is bigger than Long.MAX_VALUE");
        }

        if (initialDelay < 0) {
            initialDelay = 0L;
        }
        if (period < 0) {
            period = 0L;
        }
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");

        return create(new PublisherIntervalRangeSource(start, end, initialDelay, period, unit, scheduler));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> just(T value) {
        Objects.requireNonNull(value, "value is null");
        return new ObservableScalarSource<T>(value);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static final <T> Flowable<T> just(T v1, T v2) {
        Objects.requireNonNull(v1, "The first value is null");
        Objects.requireNonNull(v2, "The second value is null");
        
        return fromArray(v1, v2);
    }

    
    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static final <T> Flowable<T> just(T v1, T v2, T v3) {
        Objects.requireNonNull(v1, "The first value is null");
        Objects.requireNonNull(v2, "The second value is null");
        Objects.requireNonNull(v3, "The third value is null");
        
        return fromArray(v1, v2, v3);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static final <T> Flowable<T> just(T v1, T v2, T v3, T v4) {
        Objects.requireNonNull(v1, "The first value is null");
        Objects.requireNonNull(v2, "The second value is null");
        Objects.requireNonNull(v3, "The third value is null");
        Objects.requireNonNull(v4, "The fourth value is null");
        
        return fromArray(v1, v2, v3, v4);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static final <T> Flowable<T> just(T v1, T v2, T v3, T v4, T v5) {
        Objects.requireNonNull(v1, "The first value is null");
        Objects.requireNonNull(v2, "The second value is null");
        Objects.requireNonNull(v3, "The third value is null");
        Objects.requireNonNull(v4, "The fourth value is null");
        Objects.requireNonNull(v5, "The fifth value is null");
        
        return fromArray(v1, v2, v3, v4, v5);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static final <T> Flowable<T> just(T v1, T v2, T v3, T v4, T v5, T v6) {
        Objects.requireNonNull(v1, "The first value is null");
        Objects.requireNonNull(v2, "The second value is null");
        Objects.requireNonNull(v3, "The third value is null");
        Objects.requireNonNull(v4, "The fourth value is null");
        Objects.requireNonNull(v5, "The fifth value is null");
        Objects.requireNonNull(v6, "The sixth value is null");
        
        return fromArray(v1, v2, v3, v4, v5, v6);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static final <T> Flowable<T> just(T v1, T v2, T v3, T v4, T v5, T v6, T v7) {
        Objects.requireNonNull(v1, "The first value is null");
        Objects.requireNonNull(v2, "The second value is null");
        Objects.requireNonNull(v3, "The third value is null");
        Objects.requireNonNull(v4, "The fourth value is null");
        Objects.requireNonNull(v5, "The fifth value is null");
        Objects.requireNonNull(v6, "The sixth value is null");
        Objects.requireNonNull(v7, "The seventh value is null");
        
        return fromArray(v1, v2, v3, v4, v5, v6, v7);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static final <T> Flowable<T> just(T v1, T v2, T v3, T v4, T v5, T v6, T v7, T v8) {
        Objects.requireNonNull(v1, "The first value is null");
        Objects.requireNonNull(v2, "The second value is null");
        Objects.requireNonNull(v3, "The third value is null");
        Objects.requireNonNull(v4, "The fourth value is null");
        Objects.requireNonNull(v5, "The fifth value is null");
        Objects.requireNonNull(v6, "The sixth value is null");
        Objects.requireNonNull(v7, "The seventh value is null");
        Objects.requireNonNull(v8, "The eigth value is null");
        
        return fromArray(v1, v2, v3, v4, v5, v6, v7, v8);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static final <T> Flowable<T> just(T v1, T v2, T v3, T v4, T v5, T v6, T v7, T v8, T v9) {
        Objects.requireNonNull(v1, "The first value is null");
        Objects.requireNonNull(v2, "The second value is null");
        Objects.requireNonNull(v3, "The third value is null");
        Objects.requireNonNull(v4, "The fourth value is null");
        Objects.requireNonNull(v5, "The fifth value is null");
        Objects.requireNonNull(v6, "The sixth value is null");
        Objects.requireNonNull(v7, "The seventh value is null");
        Objects.requireNonNull(v8, "The eigth value is null");
        Objects.requireNonNull(v9, "The ninth is null");
        
        return fromArray(v1, v2, v3, v4, v5, v6, v7, v8, v9);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> merge(int maxConcurrency, int bufferSize, Iterable<? extends Publisher<? extends T>> sources) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), false, maxConcurrency, bufferSize);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> merge(int maxConcurrency, int bufferSize, Publisher<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), false, maxConcurrency, bufferSize);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> merge(int maxConcurrency, Publisher<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), maxConcurrency);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> merge(Iterable<? extends Publisher<? extends T>> sources) {
        return fromIterable(sources).flatMap((Function)Functions.identity());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> merge(Iterable<? extends Publisher<? extends T>> sources, int maxConcurrency) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), maxConcurrency);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> merge(Publisher<? extends Publisher<? extends T>> sources) {
        return merge(sources, bufferSize());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> merge(Publisher<? extends Publisher<? extends T>> sources, int maxConcurrency) {
        return fromPublisher(sources).flatMap((Function)Functions.identity(), maxConcurrency);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> merge(Publisher<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), sources.length);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> merge(Publisher<? extends T> p1, Publisher<? extends T> p2) {
        Objects.requireNonNull(p1, "p1 is null");
        Objects.requireNonNull(p2, "p2 is null");
        return fromArray(p1, p2).flatMap((Function)Functions.identity(), false, 2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> merge(Publisher<? extends T> p1, Publisher<? extends T> p2, Publisher<? extends T> p3) {
        Objects.requireNonNull(p1, "p1 is null");
        Objects.requireNonNull(p2, "p2 is null");
        Objects.requireNonNull(p3, "p3 is null");
        return fromArray(p1, p2, p3).flatMap((Function)Functions.identity(), false, 3);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> merge(
            Publisher<? extends T> p1, Publisher<? extends T> p2, 
            Publisher<? extends T> p3, Publisher<? extends T> p4) {
        Objects.requireNonNull(p1, "p1 is null");
        Objects.requireNonNull(p2, "p2 is null");
        Objects.requireNonNull(p3, "p3 is null");
        Objects.requireNonNull(p4, "p4 is null");
        return fromArray(p1, p2, p3, p4).flatMap((Function)Functions.identity(), false, 4);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> mergeDelayError(boolean delayErrors, Iterable<? extends Publisher<? extends T>> sources) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), true);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> mergeDelayError(int maxConcurrency, int bufferSize, Iterable<? extends Publisher<? extends T>> sources) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), true, maxConcurrency, bufferSize);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> mergeDelayError(int maxConcurrency, int bufferSize, Publisher<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), true, maxConcurrency, bufferSize);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> mergeDelayError(int maxConcurrency, Iterable<? extends Publisher<? extends T>> sources) {
        return fromIterable(sources).flatMap((Function)Functions.identity(), true, maxConcurrency);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> mergeDelayError(int maxConcurrency, Publisher<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), true, maxConcurrency);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> mergeDelayError(Publisher<? extends Publisher<? extends T>> sources) {
        return mergeDelayError(sources, bufferSize());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> mergeDelayError(Publisher<? extends Publisher<? extends T>> sources, int maxConcurrency) {
        return fromPublisher(sources).flatMap((Function)Functions.identity(), true, maxConcurrency);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> mergeDelayError(Publisher<? extends T>... sources) {
        return fromArray(sources).flatMap((Function)Functions.identity(), true, sources.length);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> mergeDelayError(Publisher<? extends T> p1, Publisher<? extends T> p2) {
        Objects.requireNonNull(p1, "p1 is null");
        Objects.requireNonNull(p2, "p2 is null");
        return fromArray(p1, p2).flatMap((Function)Functions.identity(), true, 2);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> mergeDelayError(Publisher<? extends T> p1, Publisher<? extends T> p2, Publisher<? extends T> p3) {
        Objects.requireNonNull(p1, "p1 is null");
        Objects.requireNonNull(p2, "p2 is null");
        Objects.requireNonNull(p3, "p3 is null");
        return fromArray(p1, p2, p3).flatMap((Function)Functions.identity(), true, 3);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> mergeDelayError(
            Publisher<? extends T> p1, Publisher<? extends T> p2, 
            Publisher<? extends T> p3, Publisher<? extends T> p4) {
        Objects.requireNonNull(p1, "p1 is null");
        Objects.requireNonNull(p2, "p2 is null");
        Objects.requireNonNull(p3, "p3 is null");
        Objects.requireNonNull(p4, "p4 is null");
        return fromArray(p1, p2, p3, p4).flatMap((Function)Functions.identity(), true, 4);
    }

    
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> never() {
        return (Flowable<T>)NEVER;
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static Flowable<Integer> range(int start, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count >= required but it was " + count);
        } else
        if (count == 0) {
            return empty();
        } else
        if (count == 1) {
            return just(start);
        } else
        if ((long)start + (count - 1) > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Integer overflow");
        }
        return create(new PublisherRangeSource(start, count));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<Boolean> sequenceEqual(Publisher<? extends T> p1, Publisher<? extends T> p2) {
        return sequenceEqual(p1, p2, Objects.equalsPredicate(), bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<Boolean> sequenceEqual(Publisher<? extends T> p1, Publisher<? extends T> p2, BiPredicate<? super T, ? super T> isEqual) {
        return sequenceEqual(p1, p2, isEqual, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<Boolean> sequenceEqual(Publisher<? extends T> p1, Publisher<? extends T> p2, BiPredicate<? super T, ? super T> isEqual, int bufferSize) {
        Objects.requireNonNull(p1, "p1 is null");
        Objects.requireNonNull(p2, "p2 is null");
        Objects.requireNonNull(isEqual, "isEqual is null");
        validateBufferSize(bufferSize);
        return create(new PublisherSequenceEqual<T>(p1, p2, isEqual, bufferSize));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<Boolean> sequenceEqual(Publisher<? extends T> p1, Publisher<? extends T> p2, int bufferSize) {
        return sequenceEqual(p1, p2, Objects.equalsPredicate(), bufferSize);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> switchOnNext(int bufferSize, Publisher<? extends Publisher<? extends T>> sources) {
        return fromPublisher(sources).switchMap((Function)Functions.identity(), bufferSize);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T> Flowable<T> switchOnNext(Publisher<? extends Publisher<? extends T>> sources) {
        return fromPublisher(sources).switchMap((Function)Functions.identity());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public static Flowable<Long> timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public static Flowable<Long> timer(long delay, TimeUnit unit, Scheduler scheduler) {
        if (delay < 0) {
            delay = 0L;
        }
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");

        return create(new PublisherIntervalOnceSource(delay, unit, scheduler));
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, D> Flowable<T> using(Supplier<? extends D> resourceSupplier, Function<? super D, ? extends Publisher<? extends T>> sourceSupplier, Consumer<? super D> disposer) {
        return using(resourceSupplier, sourceSupplier, disposer, true);
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, D> Flowable<T> using(Supplier<? extends D> resourceSupplier, Function<? super D, ? extends Publisher<? extends T>> sourceSupplier, Consumer<? super D> disposer, boolean eager) {
        Objects.requireNonNull(resourceSupplier, "resourceSupplier is null");
        Objects.requireNonNull(sourceSupplier, "sourceSupplier is null");
        Objects.requireNonNull(disposer, "disposer is null");
        return create(new PublisherUsing<T, D>(resourceSupplier, sourceSupplier, disposer, eager));
    }

    private static void validateBufferSize(int bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize > 0 required but it was " + bufferSize);
        }
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, R> Flowable<R> zip(Iterable<? extends Publisher<? extends T>> sources, Function<? super Object[], ? extends R> zipper) {
        Objects.requireNonNull(zipper, "zipper is null");
        Objects.requireNonNull(sources, "sources is null");
        return create(new PublisherZip<T, R>(null, sources, zipper, bufferSize(), false));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, R> Flowable<R> zip(Publisher<? extends Publisher<? extends T>> sources, final Function<? super Object[], ? extends R> zipper) {
        Objects.requireNonNull(zipper, "zipper is null");
        return fromPublisher(sources).toList().flatMap(new Function<List<Publisher<? extends T>>, Publisher<? extends R>>() {
            @Override
            public Publisher<? extends R> apply(List<Publisher<? extends T>> list) {
                return zipIterable(zipper, false, bufferSize(), list);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, R> Flowable<R> zip(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, 
            BiFunction<? super T1, ? super T2, ? extends R> zipper) {
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), p1, p2);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, R> Flowable<R> zip(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, 
            BiFunction<? super T1, ? super T2, ? extends R> zipper, boolean delayError) {
        return zipArray(Functions.toFunction(zipper), delayError, bufferSize(), p1, p2);
    }

    
    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, R> Flowable<R> zip(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, 
            BiFunction<? super T1, ? super T2, ? extends R> zipper, boolean delayError, int bufferSize) {
        return zipArray(Functions.toFunction(zipper), delayError, bufferSize, p1, p2);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, R> Flowable<R> zip(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, Publisher<? extends T3> p3, 
            Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper) {
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), p1, p2, p3);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, T4, R> Flowable<R> zip(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, Publisher<? extends T3> p3,
            Publisher<? extends T4> p4,
            Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper) {
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), p1, p2, p3, p4);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, T4, T5, R> Flowable<R> zip(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, Publisher<? extends T3> p3,
            Publisher<? extends T4> p4, Publisher<? extends T5> p5,
            Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper) {
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), p1, p2, p3, p4, p5);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, T4, T5, T6, R> Flowable<R> zip(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, Publisher<? extends T3> p3,
            Publisher<? extends T4> p4, Publisher<? extends T5> p5, Publisher<? extends T6> p6,
            Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper) {
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), p1, p2, p3, p4, p5, p6);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, R> Flowable<R> zip(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, Publisher<? extends T3> p3,
            Publisher<? extends T4> p4, Publisher<? extends T5> p5, Publisher<? extends T6> p6,
            Publisher<? extends T7> p7,
            Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper) {
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), p1, p2, p3, p4, p5, p6, p7);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Flowable<R> zip(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, Publisher<? extends T3> p3,
            Publisher<? extends T4> p4, Publisher<? extends T5> p5, Publisher<? extends T6> p6,
            Publisher<? extends T7> p7, Publisher<? extends T8> p8,
            Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper) {
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Flowable<R> zip(
            Publisher<? extends T1> p1, Publisher<? extends T2> p2, Publisher<? extends T3> p3,
            Publisher<? extends T4> p4, Publisher<? extends T5> p5, Publisher<? extends T6> p6,
            Publisher<? extends T7> p7, Publisher<? extends T8> p8, Publisher<? extends T9> p9,
            Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper) {
        return zipArray(Functions.toFunction(zipper), false, bufferSize(), p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, R> Flowable<R> zipArray(Function<? super Object[], ? extends R> zipper, 
            boolean delayError, int bufferSize, Publisher<? extends T>... sources) {
        if (sources.length == 0) {
            return empty();
        }
        Objects.requireNonNull(zipper, "zipper is null");
        validateBufferSize(bufferSize);
        return create(new PublisherZip<T, R>(sources, null, zipper, bufferSize, delayError));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public static <T, R> Flowable<R> zipIterable(Function<? super Object[], ? extends R> zipper,
            boolean delayError, int bufferSize, 
            Iterable<? extends Publisher<? extends T>> sources) {
        Objects.requireNonNull(zipper, "zipper is null");
        Objects.requireNonNull(sources, "sources is null");
        validateBufferSize(bufferSize);
        return create(new PublisherZip<T, R>(null, sources, zipper, bufferSize, delayError));
    }

    final Publisher<T> onSubscribe;
    
    protected Flowable(Publisher<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<Boolean> all(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return lift(new OperatorAll<T>(predicate));
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> ambWith(Publisher<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return amb(this, other);
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<Boolean> any(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return lift(new OperatorAny<T>(predicate));
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> asObservable() {
        return create(new Publisher<T>() {
            @Override
            public void subscribe(Subscriber<? super T> s) {
                Flowable.this.subscribe(s);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<List<T>> buffer(int count) {
        return buffer(count, count);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<List<T>> buffer(int count, int skip) {
        return buffer(count, skip, new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>();
            }
        });
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U extends Collection<? super T>> Flowable<U> buffer(int count, int skip, Supplier<U> bufferSupplier) {
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return lift(new OperatorBuffer<T, U>(count, skip, bufferSupplier));
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U extends Collection<? super T>> Flowable<U> buffer(int count, Supplier<U> bufferSupplier) {
        return buffer(count, count, bufferSupplier);
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<List<T>> buffer(long timespan, long timeskip, TimeUnit unit) {
        return buffer(timespan, timeskip, unit, Schedulers.computation(), new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>();
            }
        });
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<List<T>> buffer(long timespan, long timeskip, TimeUnit unit, Scheduler scheduler) {
        return buffer(timespan, timeskip, unit, scheduler, new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>();
            }
        });
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final <U extends Collection<? super T>> Flowable<U> buffer(long timespan, long timeskip, TimeUnit unit, Scheduler scheduler, Supplier<U> bufferSupplier) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return lift(new OperatorBufferTimed<T, U>(timespan, timeskip, unit, scheduler, bufferSupplier, Integer.MAX_VALUE, false));
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<List<T>> buffer(long timespan, TimeUnit unit) {
        return buffer(timespan, unit, Integer.MAX_VALUE, Schedulers.computation());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<List<T>> buffer(long timespan, TimeUnit unit, int count) {
        return buffer(timespan, unit, count, Schedulers.computation());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<List<T>> buffer(long timespan, TimeUnit unit, int count, Scheduler scheduler) {
        return buffer(timespan, unit, count, scheduler, new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>();
            }
        }, false);
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final <U extends Collection<? super T>> Flowable<U> buffer(
            long timespan, TimeUnit unit, 
            int count, Scheduler scheduler, 
            Supplier<U> bufferSupplier, 
            boolean restartTimerOnMaxSize) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        if (count <= 0) {
            throw new IllegalArgumentException("count > 0 required but it was " + count);
        }
        return lift(new OperatorBufferTimed<T, U>(timespan, timespan, unit, scheduler, bufferSupplier, count, restartTimerOnMaxSize));
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<List<T>> buffer(long timespan, TimeUnit unit, Scheduler scheduler) {
        return buffer(timespan, unit, Integer.MAX_VALUE, scheduler, new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>();
            }
        }, false);
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <TOpening, TClosing> Flowable<List<T>> buffer(
            Flowable<? extends TOpening> bufferOpenings, 
            Function<? super TOpening, ? extends Publisher<? extends TClosing>> bufferClosingSelector) {
        return buffer(bufferOpenings, bufferClosingSelector, new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>();
            }
        });
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <TOpening, TClosing, U extends Collection<? super T>> Flowable<U> buffer(
            Flowable<? extends TOpening> bufferOpenings, 
            Function<? super TOpening, ? extends Publisher<? extends TClosing>> bufferClosingSelector,
            Supplier<U> bufferSupplier) {
        Objects.requireNonNull(bufferOpenings, "bufferOpenings is null");
        Objects.requireNonNull(bufferClosingSelector, "bufferClosingSelector is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return lift(new OperatorBufferBoundary<T, U, TOpening, TClosing>(bufferOpenings, bufferClosingSelector, bufferSupplier));
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <B> Flowable<List<T>> buffer(Publisher<B> boundary) {
        /*
         * XXX: javac complains if this is not manually cast, Eclipse is fine
         */
        return buffer(boundary, new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>();
            }
        });
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <B> Flowable<List<T>> buffer(Publisher<B> boundary, final int initialCapacity) {
        return buffer(boundary, new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>(initialCapacity);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <B, U extends Collection<? super T>> Flowable<U> buffer(Publisher<B> boundary, Supplier<U> bufferSupplier) {
        Objects.requireNonNull(boundary, "boundary is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return lift(new OperatorBufferExactBoundary<T, U, B>(boundary, bufferSupplier));
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <B> Flowable<List<T>> buffer(Supplier<? extends Publisher<B>> boundarySupplier) {
        return buffer(boundarySupplier, new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>();
            }
        });
        
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <B, U extends Collection<? super T>> Flowable<U> buffer(Supplier<? extends Publisher<B>> boundarySupplier, Supplier<U> bufferSupplier) {
        Objects.requireNonNull(boundarySupplier, "boundarySupplier is null");
        Objects.requireNonNull(bufferSupplier, "bufferSupplier is null");
        return lift(new OperatorBufferBoundarySupplier<T, U, B>(boundarySupplier, bufferSupplier));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> cache() {
        return CachedObservable.from(this);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> cache(int capacityHint) {
        if (capacityHint <= 0) {
            throw new IllegalArgumentException("capacityHint > 0 required but it was " + capacityHint);
        }
        return CachedObservable.from(this, capacityHint);
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<U> cast(final Class<U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return map(new Function<T, U>() {
            @Override
            public U apply(T v) {
                return clazz.cast(v);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<U> collect(Supplier<? extends U> initialValueSupplier, BiConsumer<? super U, ? super T> collector) {
        Objects.requireNonNull(initialValueSupplier, "initialValueSupplier is null");
        Objects.requireNonNull(collector, "collectior is null");
        return lift(new OperatorCollect<T, U>(initialValueSupplier, collector));
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<U> collectInto(final U initialValue, BiConsumer<? super U, ? super T> collector) {
        Objects.requireNonNull(initialValue, "initialValue is null");
        return collect(new Supplier<U>() {
            @Override
            public U get() {
                return initialValue;
            }
        }, collector);
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    // TODO generics
    public final <R> Flowable<R> compose(Transformer<T, R> composer) {
        return fromPublisher(to(composer));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> concatMap(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return concatMap(mapper, 2);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> concatMap(Function<? super T, ? extends Publisher<? extends R>> mapper, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (prefetch <= 0) {
            throw new IllegalArgumentException("prefetch > 0 required but it was " + prefetch);
        }
        return lift(new OperatorConcatMap<T, R>(mapper, prefetch));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<U> concatMapIterable(Function<? super T, ? extends Iterable<? extends U>> mapper) {
        return concatMapIterable(mapper, 2);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<U> concatMapIterable(final Function<? super T, ? extends Iterable<? extends U>> mapper, int prefetch) {
        Objects.requireNonNull(mapper, "mapper is null");
        return concatMap(new Function<T, Publisher<U>>() {
            @Override
            public Publisher<U> apply(T v) {
                return new PublisherIterableSource<U>(mapper.apply(v));
            }
        }, prefetch);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> concatWith(Publisher<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return concat(this, other);
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<Boolean> contains(final Object o) {
        Objects.requireNonNull(o, "o is null");
        return any(new Predicate<T>() {
            @Override
            public boolean test(T v) {
                return Objects.equals(v, o);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<Long> count() {
        return lift(OperatorCount.instance());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<T> debounce(Function<? super T, ? extends Publisher<U>> debounceSelector) {
        Objects.requireNonNull(debounceSelector, "debounceSelector is null");
        return lift(new OperatorDebounce<T, U>(debounceSelector));
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<T> debounce(long timeout, TimeUnit unit) {
        return debounce(timeout, unit, Schedulers.computation());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> debounce(long timeout, TimeUnit unit, Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return lift(new OperatorDebounceTimed<T>(timeout, unit, scheduler));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> defaultIfEmpty(T value) {
        Objects.requireNonNull(value, "value is null");
        return switchIfEmpty(just(value));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    // TODO a more efficient implementation if necessary
    public final <U> Flowable<T> delay(final Function<? super T, ? extends Publisher<U>> itemDelay) {
        Objects.requireNonNull(itemDelay, "itemDelay is null");
        return flatMap(new Function<T, Publisher<T>>() {
            @Override
            public Publisher<T> apply(final T v) {
                return fromPublisher(itemDelay.apply(v)).take(1).map(new Function<U, T>() {
                    @Override
                    public T apply(U u) {
                        return v;
                    }
                }).defaultIfEmpty(v);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<T> delay(long delay, TimeUnit unit) {
        return delay(delay, unit, Schedulers.computation(), false);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<T> delay(long delay, TimeUnit unit, boolean delayError) {
        return delay(delay, unit, Schedulers.computation(), delayError);
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> delay(long delay, TimeUnit unit, Scheduler scheduler) {
        return delay(delay, unit, scheduler, false);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> delay(long delay, TimeUnit unit, Scheduler scheduler, boolean delayError) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        
        return lift(new OperatorDelay<T>(delay, unit, scheduler, delayError));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, V> Flowable<T> delay(Supplier<? extends Publisher<U>> delaySupplier,
            Function<? super T, ? extends Publisher<V>> itemDelay) {
        return delaySubscription(delaySupplier).delay(itemDelay);
    }
    
    /**
     * Returns an Observable that delays the subscription to this Observable
     * until the other Observable emits an element or completes normally.
     * <p>
     * <dl>
     *  <dt><b>Backpressure:</b></dt>
     *  <dd>The operator forwards the backpressure requests to this Observable once
     *  the subscription happens and requests Long.MAX_VALUE from the other Observable</dd>
     *  <dt><b>Scheduler:</b></dt>
     *  <dd>This method does not operate by default on a particular {@link Scheduler}.</dd>
     * </dl>
     * 
     * @param <U> the value type of the other Observable, irrelevant
     * @param other the other Observable that should trigger the subscription
     *        to this Observable.
     * @return an Observable that delays the subscription to this Observable
     *         until the other Observable emits an element or completes normally.
     */
    @Experimental
    public final <U> Flowable<T> delaySubscription(Publisher<U> other) {
        Objects.requireNonNull(other, "other is null");
        return create(new PublisherDelaySubscriptionOther<T, U>(this, other));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<T> delaySubscription(long delay, TimeUnit unit) {
        return delaySubscription(delay, unit, Schedulers.computation());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    // TODO a more efficient implementation if necessary
    public final Flowable<T> delaySubscription(long delay, TimeUnit unit, Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        
        return timer(delay, unit, scheduler).flatMap(new Function<Long, Publisher<T>>() {
            @Override
            public Publisher<T> apply(Long v) {
                return Flowable.this;
            }
        });
    }

    private static final Object OBJECT = new Object(); 
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<T> delaySubscription(final Supplier<? extends Publisher<U>> delaySupplier) {
        Objects.requireNonNull(delaySupplier, "delaySupplier is null");
        return fromCallable(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return delaySupplier.get();
            }
        })  
        .flatMap((Function)Functions.identity())  
        .take(1)  
        .cast(Object.class) // need a common supertype, the value is not relevant  
        .defaultIfEmpty(OBJECT) // in case the publisher is empty  
        .flatMap(new Function() {
            @Override
            public Object apply(Object v) {
                return Flowable.this;
            }
        });  
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <T2> Flowable<T2> dematerialize() {
        @SuppressWarnings("unchecked")
        Flowable<Try<Optional<T2>>> m = (Flowable<Try<Optional<T2>>>)this;
        return m.lift(OperatorDematerialize.<T2>instance());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> distinct() {
        return distinct((Function)Functions.identity(), new Supplier<Collection<T>>() {
            @Override
            public Collection<T> get() {
                return new HashSet<T>();
            }
        });
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K> Flowable<T> distinct(Function<? super T, K> keySelector) {
        return distinct(keySelector, new Supplier<Collection<K>>() {
            @Override
            public Collection<K> get() {
                return new HashSet<K>();
            }
        });
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K> Flowable<T> distinct(Function<? super T, K> keySelector, Supplier<? extends Collection<? super K>> collectionSupplier) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(collectionSupplier, "collectionSupplier is null");
        return lift(OperatorDistinct.withCollection(keySelector, collectionSupplier));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> distinctUntilChanged() {
        return lift(OperatorDistinct.<T>untilChanged());
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K> Flowable<T> distinctUntilChanged(Function<? super T, K> keySelector) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        return lift(OperatorDistinct.untilChanged(keySelector));
    }
    
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> doOnCancel(Runnable onCancel) {
        return doOnLifecycle(Functions.emptyConsumer(), Functions.emptyLongConsumer(), onCancel);
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> doOnComplete(Runnable onComplete) {
        return doOnEach(Functions.emptyConsumer(), Functions.emptyConsumer(), onComplete, Functions.emptyRunnable());
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    private Flowable<T> doOnEach(Consumer<? super T> onNext, Consumer<? super Throwable> onError, Runnable onComplete, Runnable onAfterTerminate) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onAfterTerminate, "onAfterTerminate is null");
        return lift(new OperatorDoOnEach<T>(onNext, onError, onComplete, onAfterTerminate));
    }
    
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> doOnEach(final Consumer<? super Try<Optional<T>>> consumer) {
        Objects.requireNonNull(consumer, "consumer is null");
        return doOnEach(
                new Consumer<T>() {
                    @Override
                    public void accept(T v) {
                        consumer.accept(Try.ofValue(Optional.of(v)));
                    }
                },
                new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable e) {
                        consumer.accept(Try.<Optional<T>>ofError(e));
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        consumer.accept(Try.ofValue(Optional.<T>empty()));
                    }
                },
                Functions.emptyRunnable()
                );
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> doOnEach(final Subscriber<? super T> observer) {
        Objects.requireNonNull(observer, "observer is null");
        return doOnEach(new Consumer<T>() {
            @Override
            public void accept(T v) {
                observer.onNext(v);
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                observer.onError(e);
            }
        }, new Runnable() {
            @Override
            public void run() {
                observer.onComplete();
            }
        }, Functions.emptyRunnable());
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> doOnError(Consumer<? super Throwable> onError) {
        return doOnEach(Functions.emptyConsumer(), onError, Functions.emptyRunnable(), Functions.emptyRunnable());
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> doOnLifecycle(final Consumer<? super Subscription> onSubscribe, final LongConsumer onRequest, final Runnable onCancel) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        Objects.requireNonNull(onRequest, "onRequest is null");
        Objects.requireNonNull(onCancel, "onCancel is null");
        return lift(new Operator<T, T>() {
            @Override
            public Subscriber<? super T> apply(Subscriber<? super T> s) {
                return new SubscriptionLambdaSubscriber<T>(s, onSubscribe, onRequest, onCancel);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> doOnNext(Consumer<? super T> onNext) {
        return doOnEach(onNext, Functions.emptyConsumer(), Functions.emptyRunnable(), Functions.emptyRunnable());
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> doOnRequest(LongConsumer onRequest) {
        return doOnLifecycle(Functions.emptyConsumer(), onRequest, Functions.emptyRunnable());
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> doOnSubscribe(Consumer<? super Subscription> onSubscribe) {
        return doOnLifecycle(onSubscribe, Functions.emptyLongConsumer(), Functions.emptyRunnable());
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> doOnTerminate(final Runnable onTerminate) {
        return doOnEach(Functions.emptyConsumer(), new Consumer<Throwable>() {
            @Override
            public void accept(Throwable e) {
                onTerminate.run();
            }
        }, onTerminate, Functions.emptyRunnable());
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> elementAt(long index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index >= 0 required but it was " + index);
        }
        return lift(new OperatorElementAt<T>(index, null));
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> elementAt(long index, T defaultValue) {
        if (index < 0) {
            throw new IndexOutOfBoundsException("index >= 0 required but it was " + index);
        }
        Objects.requireNonNull(defaultValue, "defaultValue is null");
        return lift(new OperatorElementAt<T>(index, defaultValue));
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> endWith(Iterable<? extends T> values) {
        return concatArray(this, fromIterable(values));
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> endWith(Publisher<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return concatArray(this, other);
    }


    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> endWith(T value) {
        Objects.requireNonNull(value, "value is null");
        return concatArray(this, just(value));
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> endWithArray(T... values) {
        Flowable<T> fromArray = fromArray(values);
        if (fromArray == empty()) {
            return this;
        }
        return concatArray(this, fromArray);
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return lift(new OperatorFilter<T>(predicate));
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> finallyDo(Runnable onFinally) {
        return doOnEach(Functions.emptyConsumer(), Functions.emptyConsumer(), Functions.emptyRunnable(), onFinally);
    }

    @BackpressureSupport(BackpressureKind.SPECIAL) // take may trigger UNBOUNDED_IN
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> first() {
        return take(1).single();
    }

    @BackpressureSupport(BackpressureKind.SPECIAL) // take may trigger UNBOUNDED_IN
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> first(T defaultValue) {
        return take(1).single(defaultValue);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return flatMap(mapper, false, bufferSize(), bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper, boolean delayErrors) {
        return flatMap(mapper, delayErrors, bufferSize(), bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper, boolean delayErrors, int maxConcurrency) {
        return flatMap(mapper, delayErrors, maxConcurrency, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper, 
            boolean delayErrors, int maxConcurrency, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        if (maxConcurrency <= 0) {
            throw new IllegalArgumentException("maxConcurrency > 0 required but it was " + maxConcurrency);
        }
        validateBufferSize(bufferSize);
        if (this instanceof ObservableScalarSource) {
            ObservableScalarSource<T> scalar = (ObservableScalarSource<T>) this;
            return create(scalar.scalarFlatMap(mapper));
        }
        return lift(new OperatorFlatMap<T, R>(mapper, delayErrors, maxConcurrency, bufferSize));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> flatMap(
            Function<? super T, ? extends Publisher<? extends R>> onNextMapper, 
            Function<? super Throwable, ? extends Publisher<? extends R>> onErrorMapper, 
            Supplier<? extends Publisher<? extends R>> onCompleteSupplier) {
        Objects.requireNonNull(onNextMapper, "onNextMapper is null");
        Objects.requireNonNull(onErrorMapper, "onErrorMapper is null");
        Objects.requireNonNull(onCompleteSupplier, "onCompleteSupplier is null");
        return merge(lift(new OperatorMapNotification<T, R>(onNextMapper, onErrorMapper, onCompleteSupplier)));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> flatMap(
            Function<? super T, ? extends Publisher<? extends R>> onNextMapper, 
            Function<Throwable, ? extends Publisher<? extends R>> onErrorMapper, 
            Supplier<? extends Publisher<? extends R>> onCompleteSupplier, 
            int maxConcurrency) {
        Objects.requireNonNull(onNextMapper, "onNextMapper is null");
        Objects.requireNonNull(onErrorMapper, "onErrorMapper is null");
        Objects.requireNonNull(onCompleteSupplier, "onCompleteSupplier is null");
        return merge(lift(new OperatorMapNotification<T, R>(onNextMapper, onErrorMapper, onCompleteSupplier)), maxConcurrency);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper, int maxConcurrency) {
        return flatMap(mapper, false, maxConcurrency, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, R> Flowable<R> flatMap(Function<? super T, ? extends Publisher<? extends U>> mapper, BiFunction<? super T, ? super U, ? extends R> resultSelector) {
        return flatMap(mapper, resultSelector, false, bufferSize(), bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, R> Flowable<R> flatMap(Function<? super T, ? extends Publisher<? extends U>> mapper, BiFunction<? super T, ? super U, ? extends R> combiner, boolean delayError) {
        return flatMap(mapper, combiner, delayError, bufferSize(), bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, R> Flowable<R> flatMap(Function<? super T, ? extends Publisher<? extends U>> mapper, BiFunction<? super T, ? super U, ? extends R> combiner, boolean delayError, int maxConcurrency) {
        return flatMap(mapper, combiner, delayError, maxConcurrency, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, R> Flowable<R> flatMap(final Function<? super T, ? extends Publisher<? extends U>> mapper, final BiFunction<? super T, ? super U, ? extends R> combiner, boolean delayError, int maxConcurrency, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(combiner, "combiner is null");
        return flatMap(new Function<T, Publisher<R>>() {
            @Override
            public Publisher<R> apply(final T t) {
                Flowable<U> u = fromPublisher(mapper.apply(t));
                return u.map(new Function<U, R>() {
                    @Override
                    public R apply(U w) {
                        return combiner.apply(t, w);
                    }
                });
            }
        }, delayError, maxConcurrency, bufferSize);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, R> Flowable<R> flatMap(Function<? super T, ? extends Publisher<? extends U>> mapper, BiFunction<? super T, ? super U, ? extends R> combiner, int maxConcurrency) {
        return flatMap(mapper, combiner, false, maxConcurrency, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<U> flatMapIterable(final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return flatMap(new Function<T, Publisher<U>>() {
            @Override
            public Publisher<U> apply(T v) {
                return new PublisherIterableSource<U>(mapper.apply(v));
            }
        });
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, V> Flowable<V> flatMapIterable(final Function<? super T, ? extends Iterable<? extends U>> mapper, final BiFunction<? super T, ? super U, ? extends V> resultSelector) {
        Objects.requireNonNull(mapper, "mapper is null");
        Objects.requireNonNull(resultSelector, "resultSelector is null");
        return flatMap(new Function<T, Publisher<U>>() {
            @Override
            public Publisher<U> apply(T t) {
                return new PublisherIterableSource<U>(mapper.apply(t));
            }
        }, resultSelector, false, bufferSize(), bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<U> flatMapIterable(final Function<? super T, ? extends Iterable<? extends U>> mapper, int bufferSize) {
        return flatMap(new Function<T, Publisher<U>>() {
            @Override
            public Publisher<U> apply(T v) {
                return new PublisherIterableSource<U>(mapper.apply(v));
            }
        }, false, bufferSize);
    }

    @BackpressureSupport(BackpressureKind.NONE)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Disposable forEach(Consumer<? super T> onNext) {
        return subscribe(onNext);
    }

    @BackpressureSupport(BackpressureKind.NONE)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Disposable forEachWhile(Predicate<? super T> onNext) {
        return forEachWhile(onNext, RxJavaPlugins.errorConsumer(), Functions.emptyRunnable());
    }

    @BackpressureSupport(BackpressureKind.NONE)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Disposable forEachWhile(Predicate<? super T> onNext, Consumer<? super Throwable> onError) {
        return forEachWhile(onNext, onError, Functions.emptyRunnable());
    }

    @BackpressureSupport(BackpressureKind.NONE)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Disposable forEachWhile(final Predicate<? super T> onNext, final Consumer<? super Throwable> onError,
            final Runnable onComplete) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");

        final AtomicReference<Subscription> subscription = new AtomicReference<Subscription>();
        return subscribe(new Consumer<T>() {
            @Override
            public void accept(T v) {
                if (!onNext.test(v)) {
                    subscription.get().cancel();
                    onComplete.run();
                }
            }
        }, onError, onComplete, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                subscription.lazySet(s);
                s.request(Long.MAX_VALUE);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K> Flowable<GroupedFlowable<K, T>> groupBy(Function<? super T, ? extends K> keySelector) {
        return groupBy(keySelector, Functions.<T>identity(), false, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K> Flowable<GroupedFlowable<K, T>> groupBy(Function<? super T, ? extends K> keySelector, boolean delayError) {
        return groupBy(keySelector, Functions.<T>identity(), delayError, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K, V> Flowable<GroupedFlowable<K, V>> groupBy(Function<? super T, ? extends K> keySelector, 
            Function<? super T, ? extends V> valueSelector) {
        return groupBy(keySelector, valueSelector, false, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K, V> Flowable<GroupedFlowable<K, V>> groupBy(Function<? super T, ? extends K> keySelector, 
            Function<? super T, ? extends V> valueSelector, boolean delayError) {
        return groupBy(keySelector, valueSelector, false, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K, V> Flowable<GroupedFlowable<K, V>> groupBy(Function<? super T, ? extends K> keySelector, 
            Function<? super T, ? extends V> valueSelector, 
            boolean delayError, int bufferSize) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(valueSelector, "valueSelector is null");
        validateBufferSize(bufferSize);

        return lift(new OperatorGroupBy<T, K, V>(keySelector, valueSelector, bufferSize, delayError));
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> ignoreElements() {
        return lift(OperatorIgnoreElements.<T>instance());
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<Boolean> isEmpty() {
        return all(new Predicate<T>() {
            @Override
            public boolean test(T v) {
                return false;
            }
        });
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> last() {
        return takeLast(1).single();
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> last(T defaultValue) {
        return takeLast(1).single(defaultValue);
    }

    @BackpressureSupport(BackpressureKind.SPECIAL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> lift(Operator<? extends R, ? super T> lifter) {
        Objects.requireNonNull(lifter, "lifter is null");
        // using onSubscribe so the fusing has access to the underlying raw Publisher
        return create(new PublisherLift<R, T>(onSubscribe, lifter));
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return lift(new OperatorMap<T, R>(mapper));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<Try<Optional<T>>> materialize() {
        return lift(OperatorMaterialize.<T>instance());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> mergeWith(Publisher<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return merge(this, other);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    @Deprecated
    public final Flowable<Flowable<T>> nest() {
        return just(this);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> observeOn(Scheduler scheduler) {
        return observeOn(scheduler, false, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> observeOn(Scheduler scheduler, boolean delayError) {
        return observeOn(scheduler, delayError, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> observeOn(Scheduler scheduler, boolean delayError, int bufferSize) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        validateBufferSize(bufferSize);
        return lift(new OperatorObserveOn<T>(scheduler, delayError, bufferSize));
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<U> ofType(final Class<U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return filter(new Predicate<T>() {
            @Override
            public boolean test(T c) {
                return clazz.isInstance(c);
            }
        }).cast(clazz);
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onBackpressureBuffer() {
        return onBackpressureBuffer(bufferSize(), false, true);
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onBackpressureBuffer(boolean delayError) {
        return onBackpressureBuffer(bufferSize(), true, true);
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onBackpressureBuffer(int bufferSize) {
        return onBackpressureBuffer(bufferSize, false, false);
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onBackpressureBuffer(int bufferSize, boolean delayError) {
        return onBackpressureBuffer(bufferSize, true, false);
    }

    @BackpressureSupport(BackpressureKind.SPECIAL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onBackpressureBuffer(int bufferSize, boolean delayError, boolean unbounded) {
        validateBufferSize(bufferSize);
        return lift(new OperatorOnBackpressureBuffer<T>(bufferSize, unbounded, delayError, Functions.emptyRunnable()));
    }

    @BackpressureSupport(BackpressureKind.SPECIAL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onBackpressureBuffer(int bufferSize, boolean delayError, boolean unbounded, Runnable onOverflow) {
        Objects.requireNonNull(onOverflow, "onOverflow is null");
        return lift(new OperatorOnBackpressureBuffer<T>(bufferSize, unbounded, delayError, onOverflow));
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onBackpressureBuffer(int bufferSize, Runnable onOverflow) {
        return onBackpressureBuffer(bufferSize, false, false, onOverflow);
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onBackpressureDrop() {
        return lift(OperatorOnBackpressureDrop.<T>instance());
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onBackpressureDrop(Consumer<? super T> onDrop) {
        Objects.requireNonNull(onDrop, "onDrop is null");
        return lift(new OperatorOnBackpressureDrop<T>(onDrop));
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onBackpressureLatest() {
        return lift(OperatorOnBackpressureLatest.<T>instance());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onErrorResumeNext(Function<? super Throwable, ? extends Publisher<? extends T>> resumeFunction) {
        Objects.requireNonNull(resumeFunction, "resumeFunction is null");
        return lift(new OperatorOnErrorNext<T>(resumeFunction, false));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onErrorResumeNext(final Publisher<? extends T> next) {
        Objects.requireNonNull(next, "next is null");
        return onErrorResumeNext(new Function<Throwable, Publisher<? extends T>>() {
            @Override
            public Publisher<? extends T> apply(Throwable e) {
                return next;
            }
        });
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onErrorReturn(Function<? super Throwable, ? extends T> valueSupplier) {
        Objects.requireNonNull(valueSupplier, "valueSupplier is null");
        return lift(new OperatorOnErrorReturn<T>(valueSupplier));
    }

    // TODO would result in ambiguity with onErrorReturn(Function)
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onErrorReturnValue(final T value) {
        Objects.requireNonNull(value, "value is null");
        return onErrorReturn(new Function<Throwable, T>() {
            @Override
            public T apply(Throwable e) {
                return value;
            }
        });
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> onExceptionResumeNext(final Publisher<? extends T> next) {
        Objects.requireNonNull(next, "next is null");
        return lift(new OperatorOnErrorNext<T>(new Function<Throwable, Publisher<? extends T>>() {
            @Override
            public Publisher<? extends T> apply(Throwable e) {
                return next;
            }
        }, true));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final ConnectableFlowable<T> publish() {
        return publish(bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> publish(Function<? super Flowable<T>, ? extends Publisher<R>> selector) {
        return publish(selector, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> publish(Function<? super Flowable<T>, ? extends Publisher<R>> selector, int bufferSize) {
        validateBufferSize(bufferSize);
        Objects.requireNonNull(selector, "selector is null");
        return OperatorPublish.create(this, selector, bufferSize);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final ConnectableFlowable<T> publish(int bufferSize) {
        validateBufferSize(bufferSize);
        return OperatorPublish.create(this, bufferSize);
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> reduce(BiFunction<T, T, T> reducer) {
        return scan(reducer).last();
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> reduce(R seed, BiFunction<R, ? super T, R> reducer) {
        return scan(seed, reducer).last();
    }

    // Naming note, a plain scan would cause ambiguity with the value-seeded version
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> reduceWith(Supplier<R> seedSupplier, BiFunction<R, ? super T, R> reducer) {
        return scanWith(seedSupplier, reducer).last();
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> repeat() {
        return repeat(Long.MAX_VALUE);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> repeat(long times) {
        if (times < 0) {
            throw new IllegalArgumentException("times >= 0 required but it was " + times);
        }
        if (times == 0) {
            return empty();
        }
        return create(new PublisherRepeat<T>(this, times));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> repeatUntil(BooleanSupplier stop) {
        Objects.requireNonNull(stop, "stop is null");
        return create(new PublisherRepeatUntil<T>(this, stop));
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> repeatWhen(final Function<? super Flowable<Object>, ? extends Publisher<?>> handler) {
        Objects.requireNonNull(handler, "handler is null");
        
        Function<Flowable<Try<Optional<Object>>>, Publisher<?>> f = new Function<Flowable<Try<Optional<Object>>>, Publisher<?>>() {
            @Override
            public Publisher<?> apply(Flowable<Try<Optional<Object>>> no) {
                return handler.apply(no.map(new Function<Try<Optional<Object>>, Object>() {
                    @Override
                    public Object apply(Try<Optional<Object>> v) {
                        return 0;
                    }
                }));
            }
        }
        ;
        
        return create(new PublisherRedo<T>(this, f));
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final ConnectableFlowable<T> replay() {
        return OperatorReplay.createFrom(this);
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> replay(Function<? super Flowable<T>, ? extends Publisher<R>> selector) {
        Objects.requireNonNull(selector, "selector is null");
        return OperatorReplay.multicastSelector(new Supplier<ConnectableFlowable<T>>() {
            @Override
            public ConnectableFlowable<T> get() {
                return replay();
            }
        }, selector);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> replay(Function<? super Flowable<T>, ? extends Publisher<R>> selector, final int bufferSize) {
        Objects.requireNonNull(selector, "selector is null");
        return OperatorReplay.multicastSelector(new Supplier<ConnectableFlowable<T>>() {
            @Override
            public ConnectableFlowable<T> get() {
                return replay(bufferSize);
            }
        }, selector);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final <R> Flowable<R> replay(Function<? super Flowable<T>, ? extends Publisher<R>> selector, int bufferSize, long time, TimeUnit unit) {
        return replay(selector, bufferSize, time, unit, Schedulers.computation());
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final <R> Flowable<R> replay(Function<? super Flowable<T>, ? extends Publisher<R>> selector, final int bufferSize, final long time, final TimeUnit unit, final Scheduler scheduler) {
        if (bufferSize < 0) {
            throw new IllegalArgumentException("bufferSize < 0");
        }
        Objects.requireNonNull(selector, "selector is null");
        return OperatorReplay.multicastSelector(new Supplier<ConnectableFlowable<T>>() {
            @Override
            public ConnectableFlowable<T> get() {
                return replay(bufferSize, time, unit, scheduler);
            }
        }, selector);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final <R> Flowable<R> replay(final Function<? super Flowable<T>, ? extends Publisher<R>> selector, final int bufferSize, final Scheduler scheduler) {
        return OperatorReplay.multicastSelector(new Supplier<ConnectableFlowable<T>>() {
            @Override
            public ConnectableFlowable<T> get() {
                return replay(bufferSize);
            }
        }, 
        new Function<Flowable<T>, Publisher<R>>() {
            @Override
            public Publisher<R> apply(Flowable<T> t) {
                return fromPublisher(selector.apply(t)).observeOn(scheduler);
            }
        });
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final <R> Flowable<R> replay(Function<? super Flowable<T>, ? extends Publisher<R>> selector, long time, TimeUnit unit) {
        return replay(selector, time, unit, Schedulers.computation());
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final <R> Flowable<R> replay(Function<? super Flowable<T>, ? extends Publisher<R>> selector, final long time, final TimeUnit unit, final Scheduler scheduler) {
        Objects.requireNonNull(selector, "selector is null");
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return OperatorReplay.multicastSelector(new Supplier<ConnectableFlowable<T>>() {
            @Override
            public ConnectableFlowable<T> get() {
                return replay(time, unit, scheduler);
            }
        }, selector);
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final <R> Flowable<R> replay(final Function<? super Flowable<T>, ? extends Publisher<R>> selector, final Scheduler scheduler) {
        Objects.requireNonNull(selector, "selector is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return OperatorReplay.multicastSelector(new Supplier<ConnectableFlowable<T>>() {
            @Override
            public ConnectableFlowable<T> get() {
                return replay();
            }
        }, 
        new Function<Flowable<T>, Publisher<R>>() {
            @Override
            public Publisher<R> apply(Flowable<T> t) {
                return fromPublisher(selector.apply(t)).observeOn(scheduler);
            }
        });
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final ConnectableFlowable<T> replay(final int bufferSize) {
        return OperatorReplay.create(this, bufferSize);
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final ConnectableFlowable<T> replay(int bufferSize, long time, TimeUnit unit) {
        return replay(bufferSize, time, unit, Schedulers.computation());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final ConnectableFlowable<T> replay(final int bufferSize, final long time, final TimeUnit unit, final Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        if (bufferSize < 0) {
            throw new IllegalArgumentException("bufferSize < 0");
        }
        return OperatorReplay.create(this, time, unit, scheduler, bufferSize);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final ConnectableFlowable<T> replay(final int bufferSize, final Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return OperatorReplay.observeOn(replay(bufferSize), scheduler);
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final ConnectableFlowable<T> replay(long time, TimeUnit unit) {
        return replay(time, unit, Schedulers.computation());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final ConnectableFlowable<T> replay(final long time, final TimeUnit unit, final Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return OperatorReplay.create(this, time, unit, scheduler);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final ConnectableFlowable<T> replay(final Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return OperatorReplay.observeOn(replay(), scheduler);
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> retry() {
        return retry(Long.MAX_VALUE, Functions.alwaysTrue());
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> retry(BiPredicate<? super Integer, ? super Throwable> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        
        return create(new PublisherRetryBiPredicate<T>(this, predicate));
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> retry(long times) {
        return retry(times, Functions.alwaysTrue());
    }
    
    // Retries at most times or until the predicate returns false, whichever happens first
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> retry(long times, Predicate<? super Throwable> predicate) {
        if (times < 0) {
            throw new IllegalArgumentException("times >= 0 required but it was " + times);
        }
        Objects.requireNonNull(predicate, "predicate is null");

        return create(new PublisherRetryPredicate<T>(this, times, predicate));
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> retry(Predicate<? super Throwable> predicate) {
        return retry(Long.MAX_VALUE, predicate);
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> retryUntil(final BooleanSupplier stop) {
        Objects.requireNonNull(stop, "stop is null");
        return retry(Long.MAX_VALUE, new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable e) {
                return !stop.getAsBoolean();
            }
        });
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> retryWhen(
            final Function<? super Flowable<? extends Throwable>, ? extends Publisher<?>> handler) {
        Objects.requireNonNull(handler, "handler is null");
        
        Function<Flowable<Try<Optional<Object>>>, Publisher<?>> f = new Function<Flowable<Try<Optional<Object>>>, Publisher<?>>() {
            @Override
            public Publisher<?> apply(Flowable<Try<Optional<Object>>> no) {
                Flowable<Throwable> map = no.takeWhile(new Predicate<Try<Optional<Object>>>() {
                    @Override
                    public boolean test(Try<Optional<Object>> e) {
                        return e.hasError();
                    }
                }).map(new Function<Try<Optional<Object>>, Throwable>() {
                    @Override
                    public Throwable apply(Try<Optional<Object>> t) {
                        return t.error();
                    }
                });
                return handler.apply(map);
            }
        }
        ;
        
        return create(new PublisherRedo<T>(this, f));
    }
    
    // TODO decide if safe subscription or unsafe should be the default
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final void safeSubscribe(Subscriber<? super T> s) {
        Objects.requireNonNull(s, "s is null");
        if (s instanceof SafeSubscriber) {
            subscribeActual(s);
        } else {
            subscribeActual(new SafeSubscriber<T>(s));
        }
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<T> sample(long period, TimeUnit unit) {
        return sample(period, unit, Schedulers.computation());
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> sample(long period, TimeUnit unit, Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return lift(new OperatorSampleTimed<T>(period, unit, scheduler));
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<T> sample(Publisher<U> sampler) {
        Objects.requireNonNull(sampler, "sampler is null");
        return lift(new OperatorSamplePublisher<T>(sampler));
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> scan(BiFunction<T, T, T> accumulator) {
        Objects.requireNonNull(accumulator, "accumulator is null");
        return lift(new OperatorScan<T>(accumulator));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> scan(final R seed, BiFunction<R, ? super T, R> accumulator) {
        Objects.requireNonNull(seed, "seed is null");
        return scanWith(new Supplier<R>() {
            @Override
            public R get() {
                return seed;
            }
        }, accumulator);
    }
    
    // Naming note, a plain scan would cause ambiguity with the value-seeded version
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> scanWith(Supplier<R> seedSupplier, BiFunction<R, ? super T, R> accumulator) {
        Objects.requireNonNull(seedSupplier, "seedSupplier is null");
        Objects.requireNonNull(accumulator, "accumulator is null");
        return lift(new OperatorScanSeed<T, R>(seedSupplier, accumulator));
    }
    
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> serialize() {
        return lift(new Operator<T, T>() {
            @Override
            public Subscriber<? super T> apply(Subscriber<? super T> s) {
                return new SerializedSubscriber<T>(s);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> share() {
        return publish().refCount();
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> single() {
        return lift(OperatorSingle.<T>instanceNoDefault());
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> single(T defaultValue) {
        Objects.requireNonNull(defaultValue, "defaultValue is null");
        return lift(new OperatorSingle<T>(defaultValue));
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> skip(long n) {
//        if (n < 0) {
//            throw new IllegalArgumentException("n >= 0 required but it was " + n);
//        } else
        // FIXME negative skip allowed?!
        if (n <= 0) {
            return this;
        }
    return lift(new OperatorSkip<T>(n));
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> skip(long time, TimeUnit unit, Scheduler scheduler) {
        // TODO consider inlining this behavior
        return skipUntil(timer(time, unit, scheduler));
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> skipLast(int n) {
        if (n < 0) {
            throw new IndexOutOfBoundsException("n >= 0 required but it was " + n);
        } else
            if (n == 0) {
                return this;
            }
        return lift(new OperatorSkipLast<T>(n));
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.TRAMPOLINE)
    public final Flowable<T> skipLast(long time, TimeUnit unit) {
        return skipLast(time, unit, Schedulers.trampoline(), false, bufferSize());
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.TRAMPOLINE)
    public final Flowable<T> skipLast(long time, TimeUnit unit, boolean delayError) {
        return skipLast(time, unit, Schedulers.trampoline(), delayError, bufferSize());
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> skipLast(long time, TimeUnit unit, Scheduler scheduler) {
        return skipLast(time, unit, scheduler, false, bufferSize());
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> skipLast(long time, TimeUnit unit, Scheduler scheduler, boolean delayError) {
        return skipLast(time, unit, scheduler, delayError, bufferSize());
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> skipLast(long time, TimeUnit unit, Scheduler scheduler, boolean delayError, int bufferSize) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        validateBufferSize(bufferSize);
     // the internal buffer holds pairs of (timestamp, value) so double the default buffer size
        int s = bufferSize << 1; 
        return lift(new OperatorSkipLastTimed<T>(time, unit, scheduler, s, delayError));
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<T> skipUntil(Publisher<U> other) {
        Objects.requireNonNull(other, "other is null");
        return lift(new OperatorSkipUntil<T, U>(other));
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> skipWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return lift(new OperatorSkipWhile<T>(predicate));
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> startWith(Iterable<? extends T> values) {
        return concatArray(fromIterable(values), this);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> startWith(Publisher<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return concatArray(other, this);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> startWith(T value) {
        Objects.requireNonNull(value, "value is null");
        return concatArray(just(value), this);
    }

    @SuppressWarnings("unchecked")
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> startWithArray(T... values) {
        Flowable<T> fromArray = fromArray(values);
        if (fromArray == empty()) {
            return this;
        }
        return concatArray(fromArray, this);
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Disposable subscribe() {
        return subscribe(Functions.emptyConsumer(), RxJavaPlugins.errorConsumer(), Functions.emptyRunnable(), new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                s.request(Long.MAX_VALUE);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Disposable subscribe(Consumer<? super T> onNext) {
        return subscribe(onNext, RxJavaPlugins.errorConsumer(), Functions.emptyRunnable(), new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                s.request(Long.MAX_VALUE);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError) {
        return subscribe(onNext, onError, Functions.emptyRunnable(), new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                s.request(Long.MAX_VALUE);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, 
            Runnable onComplete) {
        return subscribe(onNext, onError, onComplete, new Consumer<Subscription>() {
            @Override
            public void accept(Subscription s) {
                s.request(Long.MAX_VALUE);
            }
        });
    }
    
    @BackpressureSupport(BackpressureKind.SPECIAL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Disposable subscribe(Consumer<? super T> onNext, Consumer<? super Throwable> onError, 
            Runnable onComplete, Consumer<? super Subscription> onSubscribe) {
        Objects.requireNonNull(onNext, "onNext is null");
        Objects.requireNonNull(onError, "onError is null");
        Objects.requireNonNull(onComplete, "onComplete is null");
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");

        LambdaSubscriber<T> ls = new LambdaSubscriber<T>(onNext, onError, onComplete, onSubscribe);

        unsafeSubscribe(ls);

        return ls;
    }

    // TODO decide if safe subscription or unsafe should be the default
    @BackpressureSupport(BackpressureKind.SPECIAL)
    @SchedulerSupport(SchedulerKind.NONE)
    @Override
    public final void subscribe(Subscriber<? super T> s) {
        Objects.requireNonNull(s, "s is null");
        subscribeActual(s);
    }
    
    private void subscribeActual(Subscriber<? super T> s) {
        try {
            s = RxJavaPlugins.onSubscribe(s);

            if (s == null) {
                throw new NullPointerException("Plugin returned null Subscriber");
            }
            
            onSubscribe.subscribe(s);
        } catch (NullPointerException e) {
            throw e;
        } catch (Throwable e) {
            // TODO throw if fatal?
            // can't call onError because no way to know if a Subscription has been set or not
            // can't call onSubscribe because the call might have set a Subscription already
            RxJavaPlugins.onError(e);
            
            NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions due to RS");
            npe.initCause(e);
            throw npe;
        }
    }
    
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> subscribeOn(Scheduler scheduler) {
        return subscribeOn(scheduler, true);
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> subscribeOn(Scheduler scheduler, boolean requestOn) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return create(new PublisherSubscribeOn<T>(this, scheduler, requestOn));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> switchIfEmpty(Publisher<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return lift(new OperatorSwitchIfEmpty<T>(other));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> switchMap(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return switchMap(mapper, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> Flowable<R> switchMap(Function<? super T, ? extends Publisher<? extends R>> mapper, int bufferSize) {
        Objects.requireNonNull(mapper, "mapper is null");
        validateBufferSize(bufferSize);
        return lift(new OperatorSwitchMap<T, R>(mapper, bufferSize));
    }

    @BackpressureSupport(BackpressureKind.SPECIAL) // may trigger UNBOUNDED_IN
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> take(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("n >= required but it was " + n);
        } else
        if (n == 0) {
         // FIXME may want to subscribe an cancel immediately
//            return lift(s -> CancelledSubscriber.INSTANCE);
            return empty(); 
        }
        return lift(new OperatorTake<T>(n));
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> take(long time, TimeUnit unit, Scheduler scheduler) {
        // TODO consider inlining this behavior
        return takeUntil(timer(time, unit, scheduler));
    }
    
    @BackpressureSupport(BackpressureKind.SPECIAL) // may trigger UNBOUNDED_IN
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> takeFirst(Predicate<? super T> predicate) {
        return filter(predicate).take(1);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> takeLast(int n) {
        if (n < 0) {
            throw new IndexOutOfBoundsException("n >= required but it was " + n);
        } else
        if (n == 0) {
            return ignoreElements();
        } else
        if (n == 1) {
            return lift(OperatorTakeLastOne.<T>instance());
        }
        return lift(new OperatorTakeLast<T>(n));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.TRAMPOLINE)
    public final Flowable<T> takeLast(long count, long time, TimeUnit unit) {
        return takeLast(count, time, unit, Schedulers.trampoline(), false, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> takeLast(long count, long time, TimeUnit unit, Scheduler scheduler) {
        return takeLast(count, time, unit, scheduler, false, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> takeLast(long count, long time, TimeUnit unit, Scheduler scheduler, boolean delayError, int bufferSize) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        validateBufferSize(bufferSize);
        if (count < 0) {
            throw new IndexOutOfBoundsException("count >= 0 required but it was " + count);
        }
        return lift(new OperatorTakeLastTimed<T>(count, time, unit, scheduler, bufferSize, delayError));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.TRAMPOLINE)
    public final Flowable<T> takeLast(long time, TimeUnit unit) {
        return takeLast(time, unit, Schedulers.trampoline(), false, bufferSize());
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.TRAMPOLINE)
    public final Flowable<T> takeLast(long time, TimeUnit unit, boolean delayError) {
        return takeLast(time, unit, Schedulers.trampoline(), delayError, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> takeLast(long time, TimeUnit unit, Scheduler scheduler) {
        return takeLast(time, unit, scheduler, false, bufferSize());
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> takeLast(long time, TimeUnit unit, Scheduler scheduler, boolean delayError) {
        return takeLast(time, unit, scheduler, delayError, bufferSize());
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> takeLast(long time, TimeUnit unit, Scheduler scheduler, boolean delayError, int bufferSize) {
        return takeLast(Long.MAX_VALUE, time, unit, scheduler, delayError, bufferSize);
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<List<T>> takeLastBuffer(int count) {
        return takeLast(count).toList();
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.TRAMPOLINE)
    public final Flowable<List<T>> takeLastBuffer(int count, long time, TimeUnit unit) {
        return takeLast(count, time, unit).toList();
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<List<T>> takeLastBuffer(int count, long time, TimeUnit unit, Scheduler scheduler) {
        return takeLast(count, time, unit, scheduler).toList();
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.TRAMPOLINE)
    public final Flowable<List<T>> takeLastBuffer(long time, TimeUnit unit) {
        return takeLast(time, unit).toList();
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<List<T>> takeLastBuffer(long time, TimeUnit unit, Scheduler scheduler) {
        return takeLast(time, unit, scheduler).toList();
    }
    
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return lift(new OperatorTakeUntilPredicate<T>(predicate));
    }
    
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U> Flowable<T> takeUntil(Publisher<U> other) {
        Objects.requireNonNull(other, "other is null");
        return lift(new OperatorTakeUntil<T, U>(other));
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<T> takeWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate, "predicate is null");
        return lift(new OperatorTakeWhile<T>(predicate));
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<T> throttleFirst(long windowDuration, TimeUnit unit) {
        return throttleFirst(windowDuration, unit, Schedulers.computation());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> throttleFirst(long skipDuration, TimeUnit unit, Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return lift(new OperatorThrottleFirstTimed<T>(skipDuration, unit, scheduler));
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<T> throttleLast(long intervalDuration, TimeUnit unit) {
        return sample(intervalDuration, unit);
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> throttleLast(long intervalDuration, TimeUnit unit, Scheduler scheduler) {
        return sample(intervalDuration, unit, scheduler);
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<T> throttleWithTimeout(long timeout, TimeUnit unit) {
        return debounce(timeout, unit);
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> throttleWithTimeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        return debounce(timeout, unit, scheduler);
    }
    
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.TRAMPOLINE)
    public final Flowable<Timed<T>> timeInterval() {
        return timeInterval(TimeUnit.MILLISECONDS, Schedulers.trampoline());
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<Timed<T>> timeInterval(Scheduler scheduler) {
        return timeInterval(TimeUnit.MILLISECONDS, scheduler);
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.TRAMPOLINE)
    public final Flowable<Timed<T>> timeInterval(TimeUnit unit) {
        return timeInterval(unit, Schedulers.trampoline());
    }
    
    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<Timed<T>> timeInterval(TimeUnit unit, Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return lift(new OperatorTimeInterval<T>(unit, scheduler));
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <V> Flowable<T> timeout(Function<? super T, ? extends Publisher<V>> timeoutSelector) {
        return timeout0(null, timeoutSelector, null);
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <V> Flowable<T> timeout(Function<? super T, ? extends Publisher<V>> timeoutSelector, Flowable<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(null, timeoutSelector, other);
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<T> timeout(long timeout, TimeUnit timeUnit) {
        return timeout0(timeout, timeUnit, null, Schedulers.computation());
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<T> timeout(long timeout, TimeUnit timeUnit, Flowable<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(timeout, timeUnit, other, Schedulers.computation());
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> timeout(long timeout, TimeUnit timeUnit, Flowable<? extends T> other, Scheduler scheduler) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(timeout, timeUnit, other, scheduler);
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> timeout(long timeout, TimeUnit timeUnit, Scheduler scheduler) {
        return timeout0(timeout, timeUnit, null, scheduler);
    }
    
    public final <U, V> Flowable<T> timeout(Supplier<? extends Publisher<U>> firstTimeoutSelector, 
            Function<? super T, ? extends Publisher<V>> timeoutSelector) {
        Objects.requireNonNull(firstTimeoutSelector, "firstTimeoutSelector is null");
        return timeout0(firstTimeoutSelector, timeoutSelector, null);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, V> Flowable<T> timeout(
            Supplier<? extends Publisher<U>> firstTimeoutSelector, 
            Function<? super T, ? extends Publisher<V>> timeoutSelector, 
                    Publisher<? extends T> other) {
        Objects.requireNonNull(firstTimeoutSelector, "firstTimeoutSelector is null");
        Objects.requireNonNull(other, "other is null");
        return timeout0(firstTimeoutSelector, timeoutSelector, other);
    }

    private Flowable<T> timeout0(long timeout, TimeUnit timeUnit, Flowable<? extends T> other, 
            Scheduler scheduler) {
        Objects.requireNonNull(timeUnit, "timeUnit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return lift(new OperatorTimeoutTimed<T>(timeout, timeUnit, scheduler, other));
    }

    private <U, V> Flowable<T> timeout0(
            Supplier<? extends Publisher<U>> firstTimeoutSelector, 
            Function<? super T, ? extends Publisher<V>> timeoutSelector, 
                    Publisher<? extends T> other) {
        Objects.requireNonNull(timeoutSelector, "timeoutSelector is null");
        return lift(new OperatorTimeout<T, U, V>(firstTimeoutSelector, timeoutSelector, other));
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.TRAMPOLINE)
    public final Flowable<Timed<T>> timestamp() {
        return timestamp(TimeUnit.MILLISECONDS, Schedulers.trampoline());
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<Timed<T>> timestamp(Scheduler scheduler) {
        return timestamp(TimeUnit.MILLISECONDS, scheduler);
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.TRAMPOLINE)
    public final Flowable<Timed<T>> timestamp(TimeUnit unit) {
        return timestamp(unit, Schedulers.trampoline());
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<Timed<T>> timestamp(final TimeUnit unit, final Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return map(new Function<T, Timed<T>>() {
            @Override
            public Timed<T> apply(T v) {
                return new Timed<T>(v, scheduler.now(unit), unit);
            }
        });
    }

    // TODO generics
    @BackpressureSupport(BackpressureKind.SPECIAL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <R> R to(Function<? super Flowable<T>, R> converter) {
        return converter.apply(this);
    }

    @BackpressureSupport(BackpressureKind.SPECIAL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final BlockingFlowable<T> toBlocking() {
        return BlockingFlowable.from(this);
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<List<T>> toList() {
        return lift(OperatorToList.<T>defaultInstance());
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<List<T>> toList(final int capacityHint) {
        if (capacityHint <= 0) {
            throw new IllegalArgumentException("capacityHint > 0 required but it was " + capacityHint);
        }
        return lift(new OperatorToList<T, List<T>>(new Supplier<List<T>>() {
            @Override
            public List<T> get() {
                return new ArrayList<T>(capacityHint);
            }
        }));
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U extends Collection<? super T>> Flowable<U> toList(Supplier<U> collectionSupplier) {
        Objects.requireNonNull(collectionSupplier, "collectionSupplier is null");
        return lift(new OperatorToList<T, U>(collectionSupplier));
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K> Flowable<Map<K, T>> toMap(final Function<? super T, ? extends K> keySelector) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        return collect(new Supplier<Map<K, T>>() {
            @Override
            public Map<K, T> get() {
                return new HashMap<K, T>();
            }
        }, new BiConsumer<Map<K, T>, T>() {
            @Override
            public void accept(Map<K, T> m, T t) {
                K key = keySelector.apply(t);
                m.put(key, t);
            }
        });
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K, V> Flowable<Map<K, V>> toMap(final Function<? super T, ? extends K> keySelector, final Function<? super T, ? extends V> valueSelector) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(valueSelector, "valueSelector is null");
        return collect(new Supplier<Map<K, V>>() {
            @Override
            public Map<K, V> get() {
                return new HashMap<K, V>();
            }
        }, new BiConsumer<Map<K, V>, T>() {
            @Override
            public void accept(Map<K, V> m, T t) {
                K key = keySelector.apply(t);
                V value = valueSelector.apply(t);
                m.put(key, value);
            }
        });
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K, V> Flowable<Map<K, V>> toMap(final Function<? super T, ? extends K> keySelector, 
            final Function<? super T, ? extends V> valueSelector,
            final Supplier<? extends Map<K, V>> mapSupplier) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(valueSelector, "valueSelector is null");
        return collect(mapSupplier, new BiConsumer<Map<K, V>, T>() {
            @Override
            public void accept(Map<K, V> m, T t) {
                K key = keySelector.apply(t);
                V value = valueSelector.apply(t);
                m.put(key, value);
            }
        });
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K> Flowable<Map<K, Collection<T>>> toMultimap(Function<? super T, ? extends K> keySelector) {
        Function<? super T, ? extends T> valueSelector = Functions.identity();
        Supplier<Map<K, Collection<T>>> mapSupplier = new Supplier<Map<K, Collection<T>>>() {
            @Override
            public Map<K, Collection<T>> get() {
                return new HashMap<K, Collection<T>>();
            }
        };
        Function<K, Collection<T>> collectionFactory = new Function<K, Collection<T>>() {
            @Override
            public Collection<T> apply(K k) {
                return new ArrayList<T>();
            }
        };
        return toMultimap(keySelector, valueSelector, mapSupplier, collectionFactory);
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K, V> Flowable<Map<K, Collection<V>>> toMultimap(Function<? super T, ? extends K> keySelector, Function<? super T, ? extends V> valueSelector) {
        Supplier<Map<K, Collection<V>>> mapSupplier = new Supplier<Map<K, Collection<V>>>() {
            @Override
            public Map<K, Collection<V>> get() {
                return new HashMap<K, Collection<V>>();
            }
        };
        Function<K, Collection<V>> collectionFactory = new Function<K, Collection<V>>() {
            @Override
            public Collection<V> apply(K k) {
                return new ArrayList<V>();
            }
        };
        return toMultimap(keySelector, valueSelector, mapSupplier, collectionFactory);
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    @SuppressWarnings("unchecked")
    public final <K, V> Flowable<Map<K, Collection<V>>> toMultimap(
            final Function<? super T, ? extends K> keySelector, 
            final Function<? super T, ? extends V> valueSelector, 
            final Supplier<? extends Map<K, Collection<V>>> mapSupplier,
            final Function<? super K, ? extends Collection<? super V>> collectionFactory) {
        Objects.requireNonNull(keySelector, "keySelector is null");
        Objects.requireNonNull(valueSelector, "valueSelector is null");
        Objects.requireNonNull(mapSupplier, "mapSupplier is null");
        Objects.requireNonNull(collectionFactory, "collectionFactory is null");
        return collect(mapSupplier, new BiConsumer<Map<K, Collection<V>>, T>() {
            @Override
            public void accept(Map<K, Collection<V>> m, T t) {
                K key = keySelector.apply(t);

                Collection<V> coll = m.get(key);
                if (coll == null) {
                    coll = (Collection<V>)collectionFactory.apply(key);
                    m.put(key, coll);
                }

                V value = valueSelector.apply(t);

                coll.add(value);
            }
        });
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <K, V> Flowable<Map<K, Collection<V>>> toMultimap(
            Function<? super T, ? extends K> keySelector, 
            Function<? super T, ? extends V> valueSelector,
            Supplier<Map<K, Collection<V>>> mapSupplier
            ) {
        return toMultimap(keySelector, valueSelector, mapSupplier, new Function<K, Collection<V>>() {
            @Override
            public Collection<V> apply(K k) {
                return new ArrayList<V>();
            }
        });
    }
    
    @BackpressureSupport(BackpressureKind.NONE)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Observable<T> toNbpObservable() {
        return Observable.fromPublisher(this);
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Single<T> toSingle() {
        return Single.fromPublisher(this);
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes"})
    public final Flowable<List<T>> toSortedList() {
        return toSortedList(new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return ((Comparable)o1).compareTo(o2);
            }
        });
    }
    
    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<List<T>> toSortedList(final Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator, "comparator is null");
        return toList().map(new Function<List<T>, List<T>>() {
            @Override
            public List<T> apply(List<T> v) {
                Collections.sort(v, comparator);
                return v;
            }
        });
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<List<T>> toSortedList(final Comparator<? super T> comparator, int capacityHint) {
        Objects.requireNonNull(comparator, "comparator is null");
        return toList(capacityHint).map(new Function<List<T>, List<T>>() {
            @Override
            public List<T> apply(List<T> v) {
                Collections.sort(v, comparator);
                return v;
            }
        });
    }

    @BackpressureSupport(BackpressureKind.UNBOUNDED_IN)
    @SchedulerSupport(SchedulerKind.NONE)
    @SuppressWarnings({ "unchecked", "rawtypes"})
    public final Flowable<List<T>> toSortedList(int capacityHint) {
        return toSortedList(new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return ((Comparable)o1).compareTo(o2);
            }
        }, capacityHint);
    }

    @BackpressureSupport(BackpressureKind.SPECIAL)
    @SchedulerSupport(SchedulerKind.NONE)
    // TODO decide if safe subscription or unsafe should be the default
    public final void unsafeSubscribe(Subscriber<? super T> s) {
        Objects.requireNonNull(s, "s is null");
        subscribeActual(s);
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<T> unsubscribeOn(Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return lift(new OperatorUnsubscribeOn<T>(scheduler));
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<Flowable<T>> window(long count) {
        return window(count, count, bufferSize());
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<Flowable<T>> window(long count, long skip) {
        return window(count, skip, bufferSize());
    }
    
    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final Flowable<Flowable<T>> window(long count, long skip, int bufferSize) {
        if (skip <= 0) {
            throw new IllegalArgumentException("skip > 0 required but it was " + skip);
        }
        if (count <= 0) {
            throw new IllegalArgumentException("count > 0 required but it was " + count);
        }
        validateBufferSize(bufferSize);
        return lift(new OperatorWindow<T>(count, skip, bufferSize));
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<Flowable<T>> window(long timespan, long timeskip, TimeUnit unit) {
        return window(timespan, timeskip, unit, Schedulers.computation(), bufferSize());
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<Flowable<T>> window(long timespan, long timeskip, TimeUnit unit, Scheduler scheduler) {
        return window(timespan, timeskip, unit, scheduler, bufferSize());
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<Flowable<T>> window(long timespan, long timeskip, TimeUnit unit, Scheduler scheduler, int bufferSize) {
        validateBufferSize(bufferSize);
        Objects.requireNonNull(scheduler, "scheduler is null");
        Objects.requireNonNull(unit, "unit is null");
        return lift(new OperatorWindowTimed<T>(timespan, timeskip, unit, scheduler, Long.MAX_VALUE, bufferSize, false));
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<Flowable<T>> window(long timespan, TimeUnit unit) {
        return window(timespan, unit, Schedulers.computation(), Long.MAX_VALUE, false);
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<Flowable<T>> window(long timespan, TimeUnit unit, 
            long count) {
        return window(timespan, unit, Schedulers.computation(), count, false);
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.COMPUTATION)
    public final Flowable<Flowable<T>> window(long timespan, TimeUnit unit, 
            long count, boolean restart) {
        return window(timespan, unit, Schedulers.computation(), count, restart);
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<Flowable<T>> window(long timespan, TimeUnit unit, 
            Scheduler scheduler) {
        return window(timespan, unit, scheduler, Long.MAX_VALUE, false);
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<Flowable<T>> window(long timespan, TimeUnit unit, 
            Scheduler scheduler, long count) {
        return window(timespan, unit, scheduler, count, false);
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<Flowable<T>> window(long timespan, TimeUnit unit, 
            Scheduler scheduler, long count, boolean restart) {
        return window(timespan, unit, scheduler, count, restart, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.CUSTOM)
    public final Flowable<Flowable<T>> window(
            long timespan, TimeUnit unit, Scheduler scheduler, 
            long count, boolean restart, int bufferSize) {
        validateBufferSize(bufferSize);
        Objects.requireNonNull(scheduler, "scheduler is null");
        Objects.requireNonNull(unit, "unit is null");
        if (count <= 0) {
            throw new IllegalArgumentException("count > 0 required but it was " + count);
        }
        return lift(new OperatorWindowTimed<T>(timespan, timespan, unit, scheduler, count, bufferSize, restart));
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <B> Flowable<Flowable<T>> window(Publisher<B> boundary) {
        return window(boundary, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <B> Flowable<Flowable<T>> window(Publisher<B> boundary, int bufferSize) {
        Objects.requireNonNull(boundary, "boundary is null");
        return lift(new OperatorWindowBoundary<T, B>(boundary, bufferSize));
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, V> Flowable<Flowable<T>> window(
            Publisher<U> windowOpen, 
            Function<? super U, ? extends Publisher<V>> windowClose) {
        return window(windowOpen, windowClose, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, V> Flowable<Flowable<T>> window(
            Publisher<U> windowOpen, 
            Function<? super U, ? extends Publisher<V>> windowClose, int bufferSize) {
        Objects.requireNonNull(windowOpen, "windowOpen is null");
        Objects.requireNonNull(windowClose, "windowClose is null");
        return lift(new OperatorWindowBoundarySelector<T, U, V>(windowOpen, windowClose, bufferSize));
    }
    
    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <B> Flowable<Flowable<T>> window(Supplier<? extends Publisher<B>> boundary) {
        return window(boundary, bufferSize());
    }

    @BackpressureSupport(BackpressureKind.ERROR)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <B> Flowable<Flowable<T>> window(Supplier<? extends Publisher<B>> boundary, int bufferSize) {
        Objects.requireNonNull(boundary, "boundary is null");
        return lift(new OperatorWindowBoundarySupplier<T, B>(boundary, bufferSize));
    }

    @BackpressureSupport(BackpressureKind.PASS_THROUGH)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, R> Flowable<R> withLatestFrom(Publisher<? extends U> other, BiFunction<? super T, ? super U, ? extends R> combiner) {
        Objects.requireNonNull(other, "other is null");
        Objects.requireNonNull(combiner, "combiner is null");

        return lift(new OperatorWithLatestFrom<T, U, R>(combiner, other));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, R> Flowable<R> zipWith(Iterable<U> other,  BiFunction<? super T, ? super U, ? extends R> zipper) {
        Objects.requireNonNull(other, "other is null");
        Objects.requireNonNull(zipper, "zipper is null");
        return create(new PublisherZipIterable<T, U, R>(this, other, zipper));
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, R> Flowable<R> zipWith(Publisher<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        Objects.requireNonNull(other, "other is null");
        return zip(this, other, zipper);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, R> Flowable<R> zipWith(Publisher<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper, boolean delayError) {
        return zip(this, other, zipper, delayError);
    }

    @BackpressureSupport(BackpressureKind.FULL)
    @SchedulerSupport(SchedulerKind.NONE)
    public final <U, R> Flowable<R> zipWith(Publisher<? extends U> other, BiFunction<? super T, ? super U, ? extends R> zipper, boolean delayError, int bufferSize) {
        return zip(this, other, zipper, delayError, bufferSize);
    }

}