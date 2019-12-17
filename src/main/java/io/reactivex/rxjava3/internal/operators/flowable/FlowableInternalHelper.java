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
package io.reactivex.rxjava3.internal.operators.flowable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.flowables.ConnectableFlowable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.internal.functions.*;

/**
 * Helper utility class to support Flowable with inner classes.
 */
public final class FlowableInternalHelper {

    /** Utility class. */
    private FlowableInternalHelper() {
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

    static final class ItemDelayFunction<T, U> implements Function<T, Publisher<T>> {
        final Function<? super T, ? extends Publisher<U>> itemDelay;

        ItemDelayFunction(Function<? super T, ? extends Publisher<U>> itemDelay) {
            this.itemDelay = itemDelay;
        }

        @Override
        public Publisher<T> apply(final T v) throws Throwable {
            Publisher<U> p = Objects.requireNonNull(itemDelay.apply(v), "The itemDelay returned a null Publisher");
            return new FlowableTakePublisher<U>(p, 1).map(Functions.justFunction(v)).defaultIfEmpty(v);
        }
    }

    public static <T, U> Function<T, Publisher<T>> itemDelay(final Function<? super T, ? extends Publisher<U>> itemDelay) {
        return new ItemDelayFunction<T, U>(itemDelay);
    }

    static final class SubscriberOnNext<T> implements Consumer<T> {
        final Subscriber<T> subscriber;

        SubscriberOnNext(Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void accept(T v) throws Exception {
            subscriber.onNext(v);
        }
    }

    static final class SubscriberOnError<T> implements Consumer<Throwable> {
        final Subscriber<T> subscriber;

        SubscriberOnError(Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void accept(Throwable v) throws Exception {
            subscriber.onError(v);
        }
    }

    static final class SubscriberOnComplete<T> implements Action {
        final Subscriber<T> subscriber;

        SubscriberOnComplete(Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void run() throws Exception {
            subscriber.onComplete();
        }
    }

    public static <T> Consumer<T> subscriberOnNext(Subscriber<T> subscriber) {
        return new SubscriberOnNext<T>(subscriber);
    }

    public static <T> Consumer<Throwable> subscriberOnError(Subscriber<T> subscriber) {
        return new SubscriberOnError<T>(subscriber);
    }

    public static <T> Action subscriberOnComplete(Subscriber<T> subscriber) {
        return new SubscriberOnComplete<T>(subscriber);
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
            return new FlowableMapPublisher<U, R>(u, new FlatMapWithCombinerInner<U, R, T>(combiner, t));
        }
    }

    public static <T, U, R> Function<T, Publisher<R>> flatMapWithCombiner(
            final Function<? super T, ? extends Publisher<? extends U>> mapper,
                    final BiFunction<? super T, ? super U, ? extends R> combiner) {
        return new FlatMapWithCombinerOuter<T, R, U>(combiner, mapper);
    }

    static final class FlatMapIntoIterable<T, U> implements Function<T, Publisher<U>> {
        private final Function<? super T, ? extends Iterable<? extends U>> mapper;

        FlatMapIntoIterable(Function<? super T, ? extends Iterable<? extends U>> mapper) {
            this.mapper = mapper;
        }

        @Override
        public Publisher<U> apply(T t) throws Throwable {
            return new FlowableFromIterable<U>(Objects.requireNonNull(mapper.apply(t), "The mapper returned a null Iterable"));
        }
    }

    public static <T, U> Function<T, Publisher<U>> flatMapIntoIterable(final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        return new FlatMapIntoIterable<T, U>(mapper);
    }

    public static <T> Supplier<ConnectableFlowable<T>> replaySupplier(final Flowable<T> parent) {
        return new ReplaySupplier<T>(parent);
    }

    public static <T> Supplier<ConnectableFlowable<T>> replaySupplier(final Flowable<T> parent, final int bufferSize, boolean eagerTruncate) {
        return new BufferedReplaySupplier<T>(parent, bufferSize, eagerTruncate);
    }

    public static <T> Supplier<ConnectableFlowable<T>> replaySupplier(final Flowable<T> parent, final int bufferSize, final long time, final TimeUnit unit, final Scheduler scheduler, boolean eagerTruncate) {
        return new BufferedTimedReplay<T>(parent, bufferSize, time, unit, scheduler, eagerTruncate);
    }

    public static <T> Supplier<ConnectableFlowable<T>> replaySupplier(final Flowable<T> parent, final long time, final TimeUnit unit, final Scheduler scheduler, boolean eagerTruncate) {
        return new TimedReplay<T>(parent, time, unit, scheduler, eagerTruncate);
    }

    public enum RequestMax implements Consumer<Subscription> {
        INSTANCE;
        @Override
        public void accept(Subscription t) throws Exception {
            t.request(Long.MAX_VALUE);
        }
    }

    static final class ReplaySupplier<T> implements Supplier<ConnectableFlowable<T>> {

        final Flowable<T> parent;

        ReplaySupplier(Flowable<T> parent) {
            this.parent = parent;
        }

        @Override
        public ConnectableFlowable<T> get() {
            return parent.replay();
        }
    }

    static final class BufferedReplaySupplier<T> implements Supplier<ConnectableFlowable<T>> {

        final Flowable<T> parent;

        final int bufferSize;

        final boolean eagerTruncate;

        BufferedReplaySupplier(Flowable<T> parent, int bufferSize, boolean eagerTruncate) {
            this.parent = parent;
            this.bufferSize = bufferSize;
            this.eagerTruncate = eagerTruncate;
        }

        @Override
        public ConnectableFlowable<T> get() {
            return parent.replay(bufferSize, eagerTruncate);
        }
    }

    static final class BufferedTimedReplay<T> implements Supplier<ConnectableFlowable<T>> {
        final Flowable<T> parent;
        final int bufferSize;
        final long time;
        final TimeUnit unit;
        final Scheduler scheduler;

        final boolean eagerTruncate;

        BufferedTimedReplay(Flowable<T> parent, int bufferSize, long time, TimeUnit unit, Scheduler scheduler, boolean eagerTruncate) {
            this.parent = parent;
            this.bufferSize = bufferSize;
            this.time = time;
            this.unit = unit;
            this.scheduler = scheduler;
            this.eagerTruncate = eagerTruncate;
        }

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
