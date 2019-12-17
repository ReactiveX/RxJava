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
package io.reactivex.rxjava3.internal.operators.observable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.*;
import io.reactivex.rxjava3.observables.ConnectableObservable;

/**
 * Helper utility class to support Observable with inner classes.
 */
public final class ObservableInternalHelper {

    private ObservableInternalHelper() {
        throw new IllegalStateException("No instances!");
    }

    static final class SimpleGenerator<T, S> implements BiFunction<S, Emitter<T>, S> {
        final Consumer<Emitter<T>> consumer;

        SimpleGenerator(Consumer<Emitter<T>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public S apply(S t1, Emitter<T> t2) throws Throwable {
            consumer.accept(t2);
            return t1;
        }
    }

    public static <T, S> BiFunction<S, Emitter<T>, S> simpleGenerator(Consumer<Emitter<T>> consumer) {
        return new SimpleGenerator<T, S>(consumer);
    }

    static final class SimpleBiGenerator<T, S> implements BiFunction<S, Emitter<T>, S> {
        final BiConsumer<S, Emitter<T>> consumer;

        SimpleBiGenerator(BiConsumer<S, Emitter<T>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public S apply(S t1, Emitter<T> t2) throws Throwable {
            consumer.accept(t1, t2);
            return t1;
        }
    }

    public static <T, S> BiFunction<S, Emitter<T>, S> simpleBiGenerator(BiConsumer<S, Emitter<T>> consumer) {
        return new SimpleBiGenerator<T, S>(consumer);
    }

    static final class ItemDelayFunction<T, U> implements Function<T, ObservableSource<T>> {
        final Function<? super T, ? extends ObservableSource<U>> itemDelay;

        ItemDelayFunction(Function<? super T, ? extends ObservableSource<U>> itemDelay) {
            this.itemDelay = itemDelay;
        }

        @Override
        public ObservableSource<T> apply(final T v) throws Throwable {
            ObservableSource<U> o = Objects.requireNonNull(itemDelay.apply(v), "The itemDelay returned a null ObservableSource");
            return new ObservableTake<U>(o, 1).map(Functions.justFunction(v)).defaultIfEmpty(v);
        }
    }

    public static <T, U> Function<T, ObservableSource<T>> itemDelay(final Function<? super T, ? extends ObservableSource<U>> itemDelay) {
        return new ItemDelayFunction<T, U>(itemDelay);
    }

    static final class ObserverOnNext<T> implements Consumer<T> {
        final Observer<T> observer;

        ObserverOnNext(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public void accept(T v) throws Exception {
            observer.onNext(v);
        }
    }

    static final class ObserverOnError<T> implements Consumer<Throwable> {
        final Observer<T> observer;

        ObserverOnError(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public void accept(Throwable v) throws Exception {
            observer.onError(v);
        }
    }

    static final class ObserverOnComplete<T> implements Action {
        final Observer<T> observer;

        ObserverOnComplete(Observer<T> observer) {
            this.observer = observer;
        }

        @Override
        public void run() throws Exception {
            observer.onComplete();
        }
    }

    public static <T> Consumer<T> observerOnNext(Observer<T> observer) {
        return new ObserverOnNext<T>(observer);
    }

    public static <T> Consumer<Throwable> observerOnError(Observer<T> observer) {
        return new ObserverOnError<T>(observer);
    }

    public static <T> Action observerOnComplete(Observer<T> observer) {
        return new ObserverOnComplete<T>(observer);
    }

    static final class FlatMapWithCombinerInner<U, R, T> implements Function<U, R> {
        private final BiFunction<? super T, ? super U, ? extends R> combiner;
        private final T t;

        FlatMapWithCombinerInner(BiFunction<? super T, ? super U, ? extends R> combiner, T t) {
            this.combiner = combiner;
            this.t = t;
        }

        @Override
        public R apply(U w) throws Throwable {
            return combiner.apply(t, w);
        }
    }

    static final class FlatMapWithCombinerOuter<T, R, U> implements Function<T, ObservableSource<R>> {
        private final BiFunction<? super T, ? super U, ? extends R> combiner;
        private final Function<? super T, ? extends ObservableSource<? extends U>> mapper;

        FlatMapWithCombinerOuter(BiFunction<? super T, ? super U, ? extends R> combiner,
                Function<? super T, ? extends ObservableSource<? extends U>> mapper) {
            this.combiner = combiner;
            this.mapper = mapper;
        }

        @Override
        public ObservableSource<R> apply(final T t) throws Throwable {
            @SuppressWarnings("unchecked")
            ObservableSource<U> u = (ObservableSource<U>)Objects.requireNonNull(mapper.apply(t), "The mapper returned a null ObservableSource");
            return new ObservableMap<U, R>(u, new FlatMapWithCombinerInner<U, R, T>(combiner, t));
        }
    }

    public static <T, U, R> Function<T, ObservableSource<R>> flatMapWithCombiner(
            final Function<? super T, ? extends ObservableSource<? extends U>> mapper,
                    final BiFunction<? super T, ? super U, ? extends R> combiner) {
        return new FlatMapWithCombinerOuter<T, R, U>(combiner, mapper);
    }

    static final class FlatMapIntoIterable<T, U> implements Function<T, ObservableSource<U>> {
        private final Function<? super T, ? extends Iterable<? extends U>> mapper;

        FlatMapIntoIterable(Function<? super T, ? extends Iterable<? extends U>> mapper) {
            this.mapper = mapper;
        }

        @Override
        public ObservableSource<U> apply(T t) throws Throwable {
            return new ObservableFromIterable<U>(Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Iterable"));
        }
    }

    public static <T, U> Function<T, ObservableSource<U>> flatMapIntoIterable(final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        return new FlatMapIntoIterable<T, U>(mapper);
    }

    enum MapToInt implements Function<Object, Object> {
        INSTANCE;
        @Override
        public Object apply(Object t) throws Exception {
            return 0;
        }
    }

    public static <T> Supplier<ConnectableObservable<T>> replaySupplier(final Observable<T> parent) {
        return new ReplaySupplier<T>(parent);
    }

    public static <T> Supplier<ConnectableObservable<T>> replaySupplier(final Observable<T> parent, final int bufferSize, boolean eagerTruncate) {
        return new BufferedReplaySupplier<T>(parent, bufferSize, eagerTruncate);
    }

    public static <T> Supplier<ConnectableObservable<T>> replaySupplier(final Observable<T> parent, final int bufferSize, final long time, final TimeUnit unit, final Scheduler scheduler, boolean eagerTruncate) {
        return new BufferedTimedReplaySupplier<T>(parent, bufferSize, time, unit, scheduler, eagerTruncate);
    }

    public static <T> Supplier<ConnectableObservable<T>> replaySupplier(final Observable<T> parent, final long time, final TimeUnit unit, final Scheduler scheduler, boolean eagerTruncate) {
        return new TimedReplayCallable<T>(parent, time, unit, scheduler, eagerTruncate);
    }

    static final class ReplaySupplier<T> implements Supplier<ConnectableObservable<T>> {
        private final Observable<T> parent;

        ReplaySupplier(Observable<T> parent) {
            this.parent = parent;
        }

        @Override
        public ConnectableObservable<T> get() {
            return parent.replay();
        }
    }

    static final class BufferedReplaySupplier<T> implements Supplier<ConnectableObservable<T>> {
        final Observable<T> parent;
        final int bufferSize;

        final boolean eagerTruncate;

        BufferedReplaySupplier(Observable<T> parent, int bufferSize, boolean eagerTruncate) {
            this.parent = parent;
            this.bufferSize = bufferSize;
            this.eagerTruncate = eagerTruncate;
        }

        @Override
        public ConnectableObservable<T> get() {
            return parent.replay(bufferSize, eagerTruncate);
        }
    }

    static final class BufferedTimedReplaySupplier<T> implements Supplier<ConnectableObservable<T>> {
        final Observable<T> parent;
        final int bufferSize;
        final long time;
        final TimeUnit unit;
        final Scheduler scheduler;

        final boolean eagerTruncate;

        BufferedTimedReplaySupplier(Observable<T> parent, int bufferSize, long time, TimeUnit unit, Scheduler scheduler, boolean eagerTruncate) {
            this.parent = parent;
            this.bufferSize = bufferSize;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
            this.eagerTruncate = eagerTruncate;
        }

        @Override
        public ConnectableObservable<T> get() {
            return parent.replay(bufferSize, time, unit, scheduler, eagerTruncate);
        }
    }

    static final class TimedReplayCallable<T> implements Supplier<ConnectableObservable<T>> {
        final Observable<T> parent;
        final long time;
        final TimeUnit unit;
        final Scheduler scheduler;

        final boolean eagerTruncate;

        TimedReplayCallable(Observable<T> parent, long time, TimeUnit unit, Scheduler scheduler, boolean eagerTruncate) {
            this.parent = parent;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
            this.eagerTruncate = eagerTruncate;
        }

        @Override
        public ConnectableObservable<T> get() {
            return parent.replay(time, unit, scheduler, eagerTruncate);
        }
    }
}
