/**
 * Copyright 2015 Netflix, Inc.
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
import java.util.function.*;
import java.util.stream.Stream;

import org.reactivestreams.*;

import io.reactivex.internal.operators.*;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.SafeSubscriber;

public class Observable<T> implements Publisher<T> {
    final Publisher<T> onSubscribe;
    
    static final int BUFFER_SIZE;
    static {
        BUFFER_SIZE = Math.max(16, Integer.getInteger("rx2.buffer-size", 128));
    }
    
    protected Observable(Publisher<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }
    
    public static <T> Observable<T> create(Publisher<T> onSubscribe) {
        Objects.requireNonNull(onSubscribe);
        onSubscribe = RxJavaPlugins.onCreate(onSubscribe);
        return new Observable<>(onSubscribe);
    }
    
    private void subscribeActual(Subscriber<? super T> s) {
        try {
            s = RxJavaPlugins.onSubscribe(s);
            
            onSubscribe.subscribe(s);
        } catch (NullPointerException e) {
            throw e;
        } catch (Throwable e) {
            // TODO throw if fatal?
            // can't call onError because no way to know if a Subscription has been set or not
            // can't call onSubscribe because the call might have set a Subscription already
            RxJavaPlugins.onError(e);
        }
    }
    
    // TODO decide if safe subscription or unsafe should be the default
    @Override
    public final void subscribe(Subscriber<? super T> s) {
        Objects.requireNonNull(s);
        subscribeActual(s);
    }
    
    // TODO decide if safe subscription or unsafe should be the default
    public final void unsafeSubscribe(Subscriber<? super T> s) {
        Objects.requireNonNull(s);
        subscribeActual(s);
    }
    
    // TODO decide if safe subscription or unsafe should be the default
    public final void safeSubscribe(Subscriber<? super T> s) {
        Objects.requireNonNull(s);
        if (s instanceof SafeSubscriber) {
            subscribeActual(s);
        } else {
            subscribeActual(new SafeSubscriber<>(s));
        }
    }
    
    /**
     * Interface to map/wrap a downstream subscriber to an upstream subscriber.
     *
     * @param <Downstream> the value type of the downstream
     * @param <Upstream> the value type of the upstream
     */
    @FunctionalInterface
    public interface Operator<Downstream, Upstream> extends Function<Subscriber<? super Downstream>, Subscriber<? super Upstream>> {
        
    }
    
    public final <R> Observable<R> lift(Operator<? extends R, ? super T> lifter) {
        Objects.requireNonNull(lifter);
        return create(su -> {
            try {
                Subscriber<? super T> st = lifter.apply(su);
                
                st = RxJavaPlugins.onSubscribe(st);
                
                onSubscribe.subscribe(st);
            } catch (NullPointerException e) {
                throw e;
            } catch (Throwable e) {
                // TODO throw if fatal?
                // can't call onError because no way to know if a Subscription has been set or not
                // can't call onSubscribe because the call might have set a Subscription already
                RxJavaPlugins.onError(e);
            }
        });
    }
    
    // TODO generics
    @SuppressWarnings("unchecked")
    public final <R> Observable<R> compose(Function<? super Observable<T>, ? extends Observable<? extends R>> composer) {
        return (Observable<R>)to(composer);
    }
    
    // TODO generics
    public final <R> R to(Function<? super Observable<T>, R> converter) {
        return converter.apply(this);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Observable<T> fromPublisher(Publisher<? extends T> publisher) {
        if (publisher instanceof Observable) {
            return (Observable<T>)publisher;
        }
        Objects.requireNonNull(publisher);
        
        return create(s -> publisher.subscribe(s)); // javac fails to compile publisher::subscribe, Eclipse is just fine
    }
    
    public static int bufferSize() {
        return BUFFER_SIZE;
    }
    
    public static <T> Observable<T> just(T value) {
        Objects.requireNonNull(value);
        return create(new PublisherScalarSource<>(value));
    }
    
    static final Observable<Object> EMPTY = create(PublisherEmptySource.INSTANCE);
    
    @SuppressWarnings("unchecked")
    public static <T> Observable<T> empty() {
        return (Observable<T>)EMPTY;
    }
    
    public static <T> Observable<T> error(Throwable e) {
        Objects.requireNonNull(e);
        return error(() -> e);
    }
    
    public static <T> Observable<T> error(Supplier<? extends Throwable> errorSupplier) {
        Objects.requireNonNull(errorSupplier);
        return create(new PublisherErrorSource<>(errorSupplier));
    }
    
    static final Observable<Object> NEVER = create(s -> s.onSubscribe(EmptySubscription.INSTANCE));
    
    @SuppressWarnings("unchecked")
    public static <T> Observable<T> never() {
        return (Observable<T>)NEVER;
    }
    
    // TODO match naming with RxJava 1.x
    public static <T> Observable<T> fromCallable(Callable<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        return create(new PublisherScalarAsyncSource<>(supplier));
    }
    
    public final Observable<T> asObservable() {
        return create(s -> this.subscribe(s));
    }
    
    @SafeVarargs
    public static <T> Observable<T> fromArray(T... values) {
        Objects.requireNonNull(values);
        if (values.length == 0) {
            return empty();
        } else
        if (values.length == 1) {
            return just(values[0]);
        }
        return create(new PublisherArraySource<>(values));
    }
    
    public static <T> Observable<T> fromIterable(Iterable<? extends T> source) {
        Objects.requireNonNull(source);
        return create(new PublisherIterableSource<>(source));
    }
    
    public static <T> Observable<T> fromStream(Stream<? extends T> stream) {
        Objects.requireNonNull(stream);
        return create(new PublisherStreamSource<>(stream));
    }
    
    public static <T> Observable<T> fromFuture(CompletableFuture<? extends T> future) {
        Objects.requireNonNull(future);
        return create(new PublisherCompletableFutureSource<>(future));
    }
    
    public static Observable<Integer> range(int start, int count) {
        if (count == 0) {
            return empty();
        } else
        if (count == 1) {
            return just(start);
        }
        if (start + (long)count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Integer overflow");
        }
        return create(new PublisherRangeSource(start, count));
    }
    
    public static <T> Observable<T> defer(Supplier<? extends Publisher<? extends T>> supplier) {
        return create(new PublisherDefer<>(supplier));
    }
    
    public final <R> Observable<R> map(Function<? super T, ? extends R> mapper) {
        return lift(new OperatorMap<>(mapper));
    }
    
    public final <R> Observable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return flatMap(mapper, false, bufferSize(), bufferSize());
    }
    
    public final <R> Observable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper, int maxConcurrency) {
        return flatMap(mapper, false, maxConcurrency, bufferSize());
    }

    public final <R> Observable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper, boolean delayErrors) {
        return flatMap(mapper, delayErrors, bufferSize(), bufferSize());
    }
    
    public final <R> Observable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper, boolean delayErrors, int maxConcurrency) {
        return flatMap(mapper, delayErrors, maxConcurrency, bufferSize());
    }
    
    public final <R> Observable<R> flatMap(Function<? super T, ? extends Publisher<? extends R>> mapper, 
            boolean delayErrors, int maxConcurrency, int bufferSize) {
        Objects.requireNonNull(mapper);
        if (maxConcurrency <= 0) {
            throw new IllegalArgumentException("maxConcurrency > 0 required but it was " + maxConcurrency);
        }
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize > 0 required but it was " + bufferSize);
        }
        if (onSubscribe instanceof PublisherScalarSource) {
            PublisherScalarSource<T> scalar = (PublisherScalarSource<T>) onSubscribe;
            return create(scalar.flatMap(mapper));
        }
        return lift(new OperatorFlatMap<>(mapper, delayErrors, maxConcurrency, bufferSize));
    }
    
    @SafeVarargs
    public static <T> Observable<T> merge(Publisher<? extends T>... sources) {
        return fromArray(sources).flatMap(v -> v, sources.length);
    }
    
    public static <T> Observable<T> merge(Iterable<? extends Publisher<? extends T>> sources) {
        return fromIterable(sources).flatMap(v -> v);
    }
    
    @SafeVarargs
    public static <T> Observable<T> merge(int maxConcurrency, Publisher<? extends T>... sources) {
        return fromArray(sources).flatMap(v -> v, maxConcurrency);
    }
    
    public static <T> Observable<T> merge(int maxConcurrency, Iterable<? extends Publisher<? extends T>> sources) {
        return fromIterable(sources).flatMap(v -> v, maxConcurrency);
    }

    @SafeVarargs
    public static <T> Observable<T> merge(int maxConcurrency, int bufferSize, Publisher<? extends T>... sources) {
        return fromArray(sources).flatMap(v -> v, false, maxConcurrency, bufferSize);
    }
    
    public static <T> Observable<T> merge(int maxConcurrency, int bufferSize, Iterable<? extends Publisher<? extends T>> sources) {
        return fromIterable(sources).flatMap(v -> v, false, maxConcurrency, bufferSize);
    }

    @SafeVarargs
    public static <T> Observable<T> mergeDelayError(Publisher<? extends T>... sources) {
        return fromArray(sources).flatMap(v -> v, true, sources.length);
    }
    
    public static <T> Observable<T> mergeDelayError(boolean delayErrors, Iterable<? extends Publisher<? extends T>> sources) {
        return fromIterable(sources).flatMap(v -> v, true);
    }
    
    @SafeVarargs
    public static <T> Observable<T> mergeDelayError(int maxConcurrency, Publisher<? extends T>... sources) {
        return fromArray(sources).flatMap(v -> v, true, maxConcurrency);
    }
    
    public static <T> Observable<T> mergeDelayError(int maxConcurrency, Iterable<? extends Publisher<? extends T>> sources) {
        return fromIterable(sources).flatMap(v -> v, true, maxConcurrency);
    }

    @SafeVarargs
    public static <T> Observable<T> mergeDelayError(int maxConcurrency, int bufferSize, Publisher<? extends T>... sources) {
        return fromArray(sources).flatMap(v -> v, true, maxConcurrency, bufferSize);
    }
    
    public static <T> Observable<T> mergeDelayError(int maxConcurrency, int bufferSize, Iterable<? extends Publisher<? extends T>> sources) {
        return fromIterable(sources).flatMap(v -> v, true, maxConcurrency, bufferSize);
    }

    public final Observable<T> take(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("n >= required but it was " + n);
        } else
        if (n == 0) {
            return empty();
        }
        return lift(new OperatorTake<>(n));
    }
    
    public final <U> Observable<T> takeUntil(Publisher<U> other) {
        Objects.requireNonNull(other);
        return lift(new OperatorTakeUntil<>(other));
    }
    
    public final Observable<T> takeUntil(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return lift(new OperatorTakeUntilPredicate<>(predicate));
    }
    
    public final Observable<T> takeLast(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n >= required but it was " + n);
        } else
        if (n == 0) {
            return ignoreElements();
        } else
        if (n == 1) {
            return lift(OperatorTakeLastOne.instance());
        }
        return lift(new OperatorTakeLast<>(n));
    }
    
    public final Observable<T> ignoreElements() {
        return lift(OperatorIgnoreElements.instance());
    }
    
    public final Observable<T> skip(long n) {
        if (n < 0) {
            throw new IllegalArgumentException("n >= 0 required but it was " + n);
        } else
        if (n == 0) {
            return this;
        }
        return lift(new OperatorSkip<>(n));
    }
    
    public final Observable<T> skipLast(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n >= 0 required but it was " + n);
        } else
        if (n == 0) {
            return this;
        }
        return lift(new OperatorSkipLast<>(n));
    }
    
    public final Observable<T> skipWhile(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return lift(new OperatorSkipWhile<>(predicate));
    }
    
    public final Observable<T> skipUntil(Publisher<? extends T> other) {
        Objects.requireNonNull(other);
        return lift(new OperatorSkipUntil<>(other));
    }
    
    public final Observable<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return lift(new OperatorFilter<>(predicate));
    }
    
    public final Observable<List<T>> toList() {
        return lift(OperatorToList.defaultInstance());
    }
    
    public final Observable<List<T>> toList(int capacityHint) {
        if (capacityHint <= 0) {
            throw new IllegalArgumentException("capacityHint > 0 required but it was " + capacityHint);
        }
        return lift(new OperatorToList<>(() -> new ArrayList<>(capacityHint)));
    }
    
    public final <U extends Collection<? super T>> Observable<U> toList(Supplier<U> collectionSupplier) {
        Objects.requireNonNull(collectionSupplier);
        return lift(new OperatorToList<>(collectionSupplier));
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes"})
    public final Observable<List<T>> toSortedList() {
        return toSortedList((Comparator)Comparator.naturalOrder());
    }

    @SuppressWarnings({ "unchecked", "rawtypes"})
    public final Observable<List<T>> toSortedList(int capacityHint) {
        return toSortedList((Comparator)Comparator.naturalOrder(), capacityHint);
    }

    public final Observable<List<T>> toSortedList(Comparator<? super T> comparator) {
        Objects.requireNonNull(comparator);
        return toList().map(v -> {
            Collections.sort(v, comparator);
            return v;
        });
    }
    
    public final Observable<List<T>> toSortedList(Comparator<? super T> comparator, int capacityHint) {
        Objects.requireNonNull(comparator);
        return toList(capacityHint).map(v -> {
            Collections.sort(v, comparator);
            return v;
        });
    }
    
    public final Observable<T> observeOn(Scheduler scheduler) {
        return observeOn(scheduler, false, bufferSize());
    }
    
    public final Observable<T> observeOn(Scheduler scheduler, boolean delayError) {
        return observeOn(scheduler, delayError, bufferSize());
    }
    
    public final Observable<T> observeOn(Scheduler scheduler, boolean delayError, int bufferSize) {
        Objects.requireNonNull(scheduler);
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize > 0 required but it was " + bufferSize);
        }
        return lift(new OperatorObserveOn<>(scheduler, delayError, bufferSize));
    }
    
    public final Observable<T> subscribeOn(Scheduler scheduler) {
        return subscribeOn(scheduler, true);
    }
    
    public final Observable<T> subscribeOn(Scheduler scheduler, boolean requestOn) {
        Objects.requireNonNull(scheduler);
        return create(new PublisherSubscribeOn<>(this, scheduler, requestOn));
    }
    
    public final Observable<T> unsubscribeOn(Scheduler scheduler) {
        Objects.requireNonNull(scheduler);
        return lift(new OperatorUnsubscribeOn<>(scheduler));
    }
    
    public final Observable<T> cache() {
        return CachedObservable.from(this);
    }

    public final Observable<T> cache(int capacityHint) {
        return CachedObservable.from(this, capacityHint);
    }

    public final ConnectableObservable<T> publish() {
        return publish(bufferSize());
    }

    public final ConnectableObservable<T> publish(int bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize > 0 required but it was " + bufferSize);
        }
        return OperatorPublish.create(this, bufferSize);
    }

    public final <R> Observable<R> publish(Function<? super Observable<T>, ? extends Observable<R>> selector) {
        return publish(selector, bufferSize());
    }

    public final <R> Observable<R> publish(Function<? super Observable<T>, ? extends Observable<R>> selector, int bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException("bufferSize > 0 required but it was " + bufferSize);
        }
        Objects.requireNonNull(selector);
        return OperatorPublish.create(this, selector, bufferSize);
    }

    public final ConnectableObservable<T> replay() {
        return OperatorReplay.createFrom(this);
    }

    public final <R> Observable<R> replay(Function<? super Observable<T>, ? extends Observable<R>> selector) {
        Objects.requireNonNull(selector);
        return OperatorReplay.multicastSelector(this::replay, selector);
    }
    
    public final <R> Observable<R> replay(Function<? super Observable<T>, ? extends Observable<R>> selector, final int bufferSize) {
        Objects.requireNonNull(selector);
        return OperatorReplay.multicastSelector(() -> replay(bufferSize), selector);
    }
    
    public final <R> Observable<R> replay(Function<? super Observable<T>, ? extends Observable<R>> selector, int bufferSize, long time, TimeUnit unit) {
        return replay(selector, bufferSize, time, unit, Schedulers.computation());
    }

    public final <R> Observable<R> replay(Function<? super Observable<T>, ? extends Observable<R>> selector, final int bufferSize, final long time, final TimeUnit unit, final Scheduler scheduler) {
        if (bufferSize < 0) {
            throw new IllegalArgumentException("bufferSize < 0");
        }
        Objects.requireNonNull(selector);
        return OperatorReplay.multicastSelector(() -> replay(bufferSize, time, unit, scheduler), selector);
    }

    public final <R> Observable<R> replay(final Function<? super Observable<T>, ? extends Observable<R>> selector, final int bufferSize, final Scheduler scheduler) {
        return OperatorReplay.multicastSelector(() -> replay(bufferSize), 
                t -> selector.apply(t).observeOn(scheduler));
    }

    public final <R> Observable<R> replay(Function<? super Observable<T>, ? extends Observable<R>> selector, long time, TimeUnit unit) {
        return replay(selector, time, unit, Schedulers.computation());
    }

    public final <R> Observable<R> replay(Function<? super Observable<T>, ? extends Observable<R>> selector, final long time, final TimeUnit unit, final Scheduler scheduler) {
        return OperatorReplay.multicastSelector(() -> replay(time, unit, scheduler), selector);
    }

    public final <R> Observable<R> replay(final Function<? super Observable<T>, ? extends Observable<R>> selector, final Scheduler scheduler) {
        return OperatorReplay.multicastSelector(() -> replay(), 
                t -> selector.apply(t).observeOn(scheduler));
    }

    public final ConnectableObservable<T> replay(final int bufferSize) {
        return OperatorReplay.create(this, bufferSize);
    }

    public final ConnectableObservable<T> replay(int bufferSize, long time, TimeUnit unit) {
        return replay(bufferSize, time, unit, Schedulers.computation());
    }

    public final ConnectableObservable<T> replay(final int bufferSize, final long time, final TimeUnit unit, final Scheduler scheduler) {
        if (bufferSize < 0) {
            throw new IllegalArgumentException("bufferSize < 0");
        }
        return OperatorReplay.create(this, time, unit, scheduler, bufferSize);
    }

    public final ConnectableObservable<T> replay(final int bufferSize, final Scheduler scheduler) {
        return OperatorReplay.observeOn(replay(bufferSize), scheduler);
    }

    public final ConnectableObservable<T> replay(long time, TimeUnit unit) {
        return replay(time, unit, Schedulers.computation());
    }

    public final ConnectableObservable<T> replay(final long time, final TimeUnit unit, final Scheduler scheduler) {
        return OperatorReplay.create(this, time, unit, scheduler);
    }

    public final ConnectableObservable<T> replay(final Scheduler scheduler) {
        Objects.requireNonNull(scheduler);
        return OperatorReplay.observeOn(replay(), scheduler);
    }
    
    public final Observable<Boolean> any(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return lift(new OperatorAny<>(predicate));
    }
    
    public final Observable<Boolean> all(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        return lift(new OperatorAny<>(predicate));
    }
    
    public final Observable<Long> count() {
        return lift(OperatorCount.instance());
    }
    
    public final Observable<T> elementAt(long index) {
        if (index < 0) {
            throw new IllegalArgumentException("index >= 0 required but it was " + index);
        }
        return lift(new OperatorElementAt<>(index, null));
    }

    public final Observable<T> elementAt(long index, T defaultValue) {
        if (index < 0) {
            throw new IllegalArgumentException("index >= 0 required but it was " + index);
        }
        Objects.requireNonNull(defaultValue);
        return lift(new OperatorElementAt<>(index, defaultValue));
    }
    
    public final Observable<Boolean> isEmpty() {
        return any(v -> true);
    }
    
    public final Observable<T> single() {
        return lift(OperatorSingle.instanceNoDefault());
    }
    
    public final Observable<T> single(T defaultValue) {
        Objects.requireNonNull(defaultValue);
        return lift(new OperatorSingle<>(defaultValue));
    }
    
    public final Observable<T> mergeWith(Publisher<? extends T> other) {
        return merge(this, other);
    }
    
    public final <R> Observable<R> concatMap(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return concatMap(mapper, 2);
    }
    
    public final <R> Observable<R> concatMap(Function<? super T, ? extends Publisher<? extends R>> mapper, int prefetch) {
        Objects.requireNonNull(mapper);
        if (prefetch <= 0) {
            throw new IllegalArgumentException("prefetch > 0 required but it was " + prefetch);
        }
        return lift(new ConcatMap<>(mapper, prefetch));
    }
    
    public final Observable<T> concatWith(Publisher<? extends T> other) {
        Objects.requireNonNull(other);
        return concat(this, other);
    }
    
    @SafeVarargs
    public static <T> Observable<T> concat(Publisher<? extends T>... sources) {
        Objects.requireNonNull(sources);
        return fromArray(sources).concatMap(v -> v);
    }
    
    public static <T> Observable<T> concat(Iterable<? extends Publisher<? extends T>> sources) {
        Objects.requireNonNull(sources);
        return fromIterable(sources).concatMap(v -> v);
    }
    
    @SafeVarargs
    public static <T> Observable<T> concat(int prefetch, Publisher<? extends T>... sources) {
        Objects.requireNonNull(sources);
        return fromArray(sources).concatMap(v -> v, prefetch);
    }
    
    public static <T> Observable<T> concat(int prefetch, Iterable<? extends Publisher<? extends T>> sources) {
        Objects.requireNonNull(sources);
        return fromIterable(sources).concatMap(v -> v, prefetch);
    }
    
    @SafeVarargs
    public final Observable<T> startWith(T... values) {
        Observable<T> fromArray = fromArray(values);
        if (fromArray == empty()) {
            return this;
        }
        return concat(fromArray, this);
    }
    
    public final Observable<T> startWith(Iterable<? extends T> values) {
        return concat(fromIterable(values), this);
    }
    
    @SafeVarargs
    public final Observable<T> endWith(T... values) {
        Observable<T> fromArray = fromArray(values);
        if (fromArray == empty()) {
            return this;
        }
        return concat(this, fromArray);
    }
    
    public final Observable<T> endWith(Iterable<? extends T> values) {
        return concat(this, fromIterable(values));
    }
}
