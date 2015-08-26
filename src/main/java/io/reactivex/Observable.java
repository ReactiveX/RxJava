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


import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.Stream;

import org.reactivestreams.*;

import io.reactivex.internal.operators.*;
import io.reactivex.internal.subscriptions.EmptySubscription;

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
        // TODO plugin wrapping
        return new Observable<>(onSubscribe);
    }
    
    @Override
    public final void subscribe(Subscriber<? super T> s) {
        Objects.requireNonNull(s);
        try {
            onSubscribe.subscribe(s);
        } catch (NullPointerException e) {
            throw e;
        } catch (Throwable e) {
            // TODO throw if fatal
            // TODO plugin error handler
            // can't call onError because no way to know if a Subscription has been set or not
            // can't call onSubscribe because the call might have set a Subscription already
            e.printStackTrace();
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
        return create(su -> {
            try {
                Subscriber<? super T> st = lifter.apply(su);
                // TODO plugin wrapping
                onSubscribe.subscribe(st);
            } catch (NullPointerException e) {
                throw e;
            } catch (Throwable e) {
                // TODO throw if fatal
                // TODO plugin error handler
                // can't call onError because no way to know if a Subscription has been set or not
                // can't call onSubscribe because the call might have set a Subscription already
                e.printStackTrace();
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
        return error(() -> e);
    }
    
    public static <T> Observable<T> error(Supplier<? extends Throwable> errorSupplier) {
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
    
    public Observable<T> asObservable() {
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
        return create(new PublisherIterableSource<>(source));
    }
    
    public static <T> Observable<T> fromStream(Stream<? extends T> stream) {
        return create(new PublisherStreamSource<>(stream));
    }
    
    public static <T> Observable<T> fromFuture(CompletableFuture<? extends T> future) {
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
}
