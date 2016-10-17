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
import java.util.concurrent.*;

import io.reactivex.*;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.observables.ConnectableObservable;

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
        public S apply(S t1, Emitter<T> t2) throws Exception {
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
        public S apply(S t1, Emitter<T> t2) throws Exception {
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
        public ObservableSource<T> apply(final T v) throws Exception {
            return new ObservableTake<U>(itemDelay.apply(v), 1).map(Functions.justFunction(v)).defaultIfEmpty(v);
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
        public R apply(U w) throws Exception {
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
        public ObservableSource<R> apply(final T t) throws Exception {
            @SuppressWarnings("unchecked")
            ObservableSource<U> u = (ObservableSource<U>)mapper.apply(t);
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
        public ObservableSource<U> apply(T t) throws Exception {
            return new ObservableFromIterable<U>(mapper.apply(t));
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

    static final class RepeatWhenOuterHandler
    implements Function<Observable<Notification<Object>>, ObservableSource<?>> {
        private final Function<? super Observable<Object>, ? extends ObservableSource<?>> handler;

        RepeatWhenOuterHandler(Function<? super Observable<Object>, ? extends ObservableSource<?>> handler) {
            this.handler = handler;
        }

        @Override
        public ObservableSource<?> apply(Observable<Notification<Object>> no) throws Exception {
            return handler.apply(no.map(MapToInt.INSTANCE));
        }
    }

    public static Function<Observable<Notification<Object>>, ObservableSource<?>> repeatWhenHandler(final Function<? super Observable<Object>, ? extends ObservableSource<?>> handler) {
        return new RepeatWhenOuterHandler(handler);
    }

    public static <T> Callable<ConnectableObservable<T>> replayCallable(final Observable<T> parent) {
        return new Callable<ConnectableObservable<T>>() {
            @Override
            public ConnectableObservable<T> call() {
                return parent.replay();
            }
        };
    }

    public static <T> Callable<ConnectableObservable<T>> replayCallable(final Observable<T> parent, final int bufferSize) {
        return new Callable<ConnectableObservable<T>>() {
            @Override
            public ConnectableObservable<T> call() {
                return parent.replay(bufferSize);
            }
        };
    }

    public static <T> Callable<ConnectableObservable<T>> replayCallable(final Observable<T> parent, final int bufferSize, final long time, final TimeUnit unit, final Scheduler scheduler) {
        return new Callable<ConnectableObservable<T>>() {
            @Override
            public ConnectableObservable<T> call() {
                return parent.replay(bufferSize, time, unit, scheduler);
            }
        };
    }

    public static <T> Callable<ConnectableObservable<T>> replayCallable(final Observable<T> parent, final long time, final TimeUnit unit, final Scheduler scheduler) {
        return new Callable<ConnectableObservable<T>>() {
            @Override
            public ConnectableObservable<T> call() {
                return parent.replay(time, unit, scheduler);
            }
        };
    }

    public static <T, R> Function<Observable<T>, ObservableSource<R>> replayFunction(final Function<? super Observable<T>, ? extends ObservableSource<R>> selector, final Scheduler scheduler) {
        return new Function<Observable<T>, ObservableSource<R>>() {
            @Override
            public ObservableSource<R> apply(Observable<T> t) throws Exception {
                return Observable.wrap(selector.apply(t)).observeOn(scheduler);
            }
        };
    }

    enum ErrorMapperFilter implements Function<Notification<Object>, Throwable>, Predicate<Notification<Object>> {
        INSTANCE;

        @Override
        public Throwable apply(Notification<Object> t) throws Exception {
            return t.getError();
        }

        @Override
        public boolean test(Notification<Object> t) throws Exception {
            return t.isOnError();
        }
    }

    static final class RetryWhenInner
    implements Function<Observable<Notification<Object>>, ObservableSource<?>> {
        private final Function<? super Observable<Throwable>, ? extends ObservableSource<?>> handler;

        RetryWhenInner(
                Function<? super Observable<Throwable>, ? extends ObservableSource<?>> handler) {
            this.handler = handler;
        }

        @Override
        public ObservableSource<?> apply(Observable<Notification<Object>> no) throws Exception {
            Observable<Throwable> map = no
                    .takeWhile(ErrorMapperFilter.INSTANCE)
                    .map(ErrorMapperFilter.INSTANCE);
            return handler.apply(map);
        }
    }

    public static <T> Function<Observable<Notification<Object>>, ObservableSource<?>> retryWhenHandler(final Function<? super Observable<Throwable>, ? extends ObservableSource<?>> handler) {
        return new RetryWhenInner(handler);
    }

    static final class ZipIterableFunction<T, R>
    implements Function<List<ObservableSource<? extends T>>, ObservableSource<? extends R>> {
        private final Function<? super Object[], ? extends R> zipper;

        ZipIterableFunction(Function<? super Object[], ? extends R> zipper) {
            this.zipper = zipper;
        }

        @Override
        public ObservableSource<? extends R> apply(List<ObservableSource<? extends T>> list) {
            return Observable.zipIterable(list, zipper, false, Observable.bufferSize());
        }
    }

    public static <T, R> Function<List<ObservableSource<? extends T>>, ObservableSource<? extends R>> zipIterable(final Function<? super Object[], ? extends R> zipper) {
        return new ZipIterableFunction<T, R>(zipper);
    }

}
