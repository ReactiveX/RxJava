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
package io.reactivex.internal.operators.flowable;

import java.util.List;
import java.util.concurrent.*;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;

/**
 * Helper utility class to support Flowable with inner classes.
 */
public enum FlowableInternalHelper {
    ;

    static final class SimpleGenerator<T, S> implements BiFunction<S, Emitter<T>, S> {
        final Consumer<Emitter<T>> consumer;
        
        public SimpleGenerator(Consumer<Emitter<T>> consumer) {
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
        
        public SimpleBiGenerator(BiConsumer<S, Emitter<T>> consumer) {
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
    
    static final class ItemDelayFunction<T, U> implements Function<T, Publisher<T>> {
        final Function<? super T, ? extends Publisher<U>> itemDelay;

        public ItemDelayFunction(Function<? super T, ? extends Publisher<U>> itemDelay) {
            this.itemDelay = itemDelay;
        }

        @Override
        public Publisher<T> apply(final T v) throws Exception {
            return new FlowableTake<U>(itemDelay.apply(v), 1).map(Functions.justFunction(v)).defaultIfEmpty(v);
        }
    }

    public static <T, U> Function<T, Publisher<T>> itemDelay(final Function<? super T, ? extends Publisher<U>> itemDelay) {
        return new ItemDelayFunction<T, U>(itemDelay);
    }
    
    static final class SubscriberOnNext<T> implements Consumer<T> {
        final Subscriber<T> subscriber;
        
        public SubscriberOnNext(Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }
        
        @Override
        public void accept(T v) throws Exception {
            subscriber.onNext(v);
        }
    }
    
    static final class SubscriberOnError<T> implements Consumer<Throwable> {
        final Subscriber<T> subscriber;
        
        public SubscriberOnError(Subscriber<T> subscriber) {
            this.subscriber = subscriber;
        }
        
        @Override
        public void accept(Throwable v) throws Exception {
            subscriber.onError(v);
        }
    }
    
    static final class SubscriberOnComplete<T> implements Action {
        final Subscriber<T> subscriber;
        
        public SubscriberOnComplete(Subscriber<T> subscriber) {
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
        public R apply(U w) throws Exception {
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
        public Publisher<R> apply(final T t) throws Exception {
            @SuppressWarnings("unchecked")
            Publisher<U> u = (Publisher<U>)mapper.apply(t);
            return new FlowableMap<U, R>(u, new FlatMapWithCombinerInner<U, R, T>(combiner, t));
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
        public Publisher<U> apply(T t) throws Exception {
            return new FlowableFromIterable<U>(mapper.apply(t));
        }
    }

    public static <T, U> Function<T, Publisher<U>> flatMapIntoIterable(final Function<? super T, ? extends Iterable<? extends U>> mapper) {
        return new FlatMapIntoIterable<T, U>(mapper);
    }
    
    enum MapToInt implements Function<Object, Object>{
        INSTANCE;
        @Override
        public Object apply(Object t) throws Exception {
            return 0;
        }
    }
    
    public static <T> Callable<ConnectableFlowable<T>> replayCallable(final Flowable<T> parent) {
        return new Callable<ConnectableFlowable<T>>() {
            @Override
            public ConnectableFlowable<T> call() {
                return parent.replay();
            }
        };
    }

    public static <T> Callable<ConnectableFlowable<T>> replayCallable(final Flowable<T> parent, final int bufferSize) {
        return new Callable<ConnectableFlowable<T>>() {
            @Override
            public ConnectableFlowable<T> call() {
                return parent.replay(bufferSize);
            }
        };
    }
    
    public static <T> Callable<ConnectableFlowable<T>> replayCallable(final Flowable<T> parent, final int bufferSize, final long time, final TimeUnit unit, final Scheduler scheduler) {
        return new Callable<ConnectableFlowable<T>>() {
            @Override
            public ConnectableFlowable<T> call() {
                return parent.replay(bufferSize, time, unit, scheduler);
            }
        };
    }

    public static <T> Callable<ConnectableFlowable<T>> replayCallable(final Flowable<T> parent, final long time, final TimeUnit unit, final Scheduler scheduler) {
        return new Callable<ConnectableFlowable<T>>() {
            @Override
            public ConnectableFlowable<T> call() {
                return parent.replay(time, unit, scheduler);
            }
        };
    }

    public static <T, R> Function<Flowable<T>, Publisher<R>> replayFunction(final Function<? super Flowable<T>, ? extends Publisher<R>> selector, final Scheduler scheduler) {
        return new Function<Flowable<T>, Publisher<R>>() {
            @Override
            public Publisher<R> apply(Flowable<T> t) throws Exception {
                return Flowable.fromPublisher(selector.apply(t)).observeOn(scheduler);
            }
        };
    }
    
    enum RequestMax implements Consumer<Subscription> {
        INSTANCE;
        @Override
        public void accept(Subscription t) throws Exception {
            t.request(Long.MAX_VALUE);
        }
    }
    
    public static Consumer<Subscription> requestMax() {
        return RequestMax.INSTANCE;
    }
    
    static final class ZipIterableFunction<T, R>
    implements Function<List<Publisher<? extends T>>, Publisher<? extends R>> {
        private final Function<? super Object[], ? extends R> zipper;

        ZipIterableFunction(Function<? super Object[], ? extends R> zipper) {
            this.zipper = zipper;
        }

        @Override
        public Publisher<? extends R> apply(List<Publisher<? extends T>> list) {
            return Flowable.zipIterable(list, zipper, false, Flowable.bufferSize());
        }
    }

    public static <T, R> Function<List<Publisher<? extends T>>, Publisher<? extends R>> zipIterable(final Function<? super Object[], ? extends R> zipper) {
        return new ZipIterableFunction<T, R>(zipper);
    }
    
}
