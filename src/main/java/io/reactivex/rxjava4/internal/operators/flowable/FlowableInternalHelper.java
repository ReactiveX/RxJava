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

package io.reactivex.rxjava4.internal.operators.flowable;

import java.util.Objects;
import java.util.concurrent.Flow.*;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.internal.functions.Functions;

/**
 * Helper utility class to support Flowable with inner classes.
 */
public final class FlowableInternalHelper {

    /** Utility class. */
    private FlowableInternalHelper() {
        throw new IllegalStateException("No instances!");
    }

    record SimpleGenerator<T, S>(Consumer<Emitter<T>> consumer) implements BiFunction<S, Emitter<T>, S> {

        @Override
            public S apply(S t1, Emitter<T> t2) throws Throwable {
                consumer.accept(t2);
                return t1;
            }
        }

    public static <T, S> BiFunction<S, Emitter<T>, S> simpleGenerator(Consumer<Emitter<T>> consumer) {
        return new SimpleGenerator<>(consumer);
    }

    record SimpleBiGenerator<T, S>(BiConsumer<S, Emitter<T>> consumer) implements BiFunction<S, Emitter<T>, S> {

        @Override
            public S apply(S t1, Emitter<T> t2) throws Throwable {
                consumer.accept(t1, t2);
                return t1;
            }
        }

    public static <T, S> BiFunction<S, Emitter<T>, S> simpleBiGenerator(BiConsumer<S, Emitter<T>> consumer) {
        return new SimpleBiGenerator<>(consumer);
    }

    record ItemDelayFunction<T, U>(
            Function<? super T, ? extends Publisher<U>> itemDelay) implements Function<T, Publisher<T>> {

        @Override
            public Publisher<T> apply(final T v) throws Throwable {
                Publisher<U> p = Objects.requireNonNull(itemDelay.apply(v), "The itemDelay returned a null Publisher");
                return new FlowableTakePublisher<>(p, 1).map(Functions.justFunction(v)).defaultIfEmpty(v);
            }
        }

    public static <T, U> Function<T, Publisher<T>> itemDelay(final Function<? super T, ? extends Publisher<U>> itemDelay) {
        return new ItemDelayFunction<>(itemDelay);
    }

    record SubscriberOnNext<T>(Subscriber<T> subscriber) implements Consumer<T> {

        @Override
            public void accept(T v) {
                subscriber.onNext(v);
            }
        }

    record SubscriberOnError<T>(Subscriber<T> subscriber) implements Consumer<Throwable> {

        @Override
            public void accept(Throwable v) {
                subscriber.onError(v);
            }
        }

    record SubscriberOnComplete<T>(Subscriber<T> subscriber) implements Action {

        @Override
            public void run() {
                subscriber.onComplete();
            }
        }

    public static <T> Consumer<T> subscriberOnNext(Subscriber<T> subscriber) {
        return new SubscriberOnNext<>(subscriber);
    }

    public static <T> Consumer<Throwable> subscriberOnError(Subscriber<T> subscriber) {
        return new SubscriberOnError<>(subscriber);
    }

    public static <T> Action subscriberOnComplete(Subscriber<T> subscriber) {
        return new SubscriberOnComplete<>(subscriber);
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

    static final class FlatMapWithCombinerOuter<T, R, U> implements Function<T, Publisher<R>> {
        private final BiFunction<? super T, ? super U, ? extends R> combiner;
        private final Function<? super T, ? extends Publisher<? extends U>> mapper;

        FlatMapWithCombinerOuter(BiFunction<? super T, ? super U, ? extends R> combiner,
                Function<? super T, ? extends Publisher<? extends U>> mapper) {
            this.combiner = combiner;
            this.mapper = mapper;
        }

        @Override
        public Publisher<R> apply(final T t) throws Throwable {
            @SuppressWarnings("unchecked")
            Publisher<U> u = (Publisher<U>)Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Publisher");
            return new FlowableMapPublisher<>(u, new FlatMapWithCombinerInner<U, R, T>(combiner, t));
        }
    }

    public static <T, U, R> Function<T, Publisher<R>> flatMapWithCombiner(
            final Function<? super T, ? extends Publisher<? extends U>> mapper,
                    final BiFunction<? super T, ? super U, ? extends R> combiner) {
        return new FlatMapWithCombinerOuter<>(combiner, mapper);
    }

    static final class FlatMapIntoIterable<T, U> implements Function<T, Publisher<U>> {
        private final Function<? super T, ? extends Iterable<? extends U>> mapper;

        FlatMapIntoIterable(Function<? super T, ? extends Iterable<? extends U>> mapper) {
            this.mapper = mapper;
        }

        @Override
        public Publisher<U> apply(T t) throws Throwable {
            return new FlowableFromIterable<>(Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Iterable"));
        }
    }

    public static <T, U> Function<T, Publisher<U>> flatMapIntoIterable(final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        return new FlatMapIntoIterable<>(mapper);
    }

    public static <T> Supplier<ConnectableFlowable<T>> replaySupplier(final Flowable<T> parent) {
        return new ReplaySupplier<>(parent);
    }

    public static <T> Supplier<ConnectableFlowable<T>> replaySupplier(final Flowable<T> parent, final int bufferSize, boolean eagerTruncate) {
        return new BufferedReplaySupplier<>(parent, bufferSize, eagerTruncate);
    }

    public static <T> Supplier<ConnectableFlowable<T>> replaySupplier(final Flowable<T> parent,
            final int bufferSize, final long time, final TimeUnit unit, final Scheduler scheduler, boolean eagerTruncate) {
        return new BufferedTimedReplay<>(parent, bufferSize, time, unit, scheduler, eagerTruncate);
    }

    public static <T> Supplier<ConnectableFlowable<T>> replaySupplier(final Flowable<T> parent,
            final long time, final TimeUnit unit, final Scheduler scheduler, boolean eagerTruncate) {
        return new TimedReplay<>(parent, time, unit, scheduler, eagerTruncate);
    }

    public enum RequestMax implements Consumer<Subscription> {
        INSTANCE;
        @Override
        public void accept(Subscription t) {
            t.request(Long.MAX_VALUE);
        }
    }

    record ReplaySupplier<T>(Flowable<T> parent) implements Supplier<ConnectableFlowable<T>> {

        @Override
            public ConnectableFlowable<T> get() {
                return parent.replay();
            }
        }

    record BufferedReplaySupplier<T>(Flowable<T> parent, int bufferSize,
                                     boolean eagerTruncate) implements Supplier<ConnectableFlowable<T>> {

        @Override
            public ConnectableFlowable<T> get() {
                return parent.replay(bufferSize, eagerTruncate);
            }
        }

    record BufferedTimedReplay<T>(Flowable<T> parent, int bufferSize, long time, TimeUnit unit, Scheduler scheduler,
                                  boolean eagerTruncate) implements Supplier<ConnectableFlowable<T>> {

        @Override
            public ConnectableFlowable<T> get() {
                return parent.replay(bufferSize, time, unit, scheduler, eagerTruncate);
            }
        }

    static final class TimedReplay<T> implements Supplier<ConnectableFlowable<T>> {
        private final Flowable<T> parent;
        private final long time;
        private final TimeUnit unit;
        private final Scheduler scheduler;

        final boolean eagerTruncate;

        TimedReplay(Flowable<T> parent, long time, TimeUnit unit, Scheduler scheduler, boolean eagerTruncate) {
            this.parent = parent;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
            this.eagerTruncate = eagerTruncate;
        }

        @Override
        public ConnectableFlowable<T> get() {
            return parent.replay(time, unit, scheduler, eagerTruncate);
        }
    }
}
