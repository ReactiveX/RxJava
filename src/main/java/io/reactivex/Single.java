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
import java.util.concurrent.atomic.*;

import org.reactivestreams.*;

import io.reactivex.disposables.*;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.functions.Objects;
import io.reactivex.internal.operators.single.*;
import io.reactivex.internal.subscriptions.*;
import io.reactivex.internal.util.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * Represents a deferred computation and emission of a single value or exception.
 * 
 * @param <T> the value type
 */
public class Single<T> {
    
    public interface SingleOnSubscribe<T> extends Consumer<SingleSubscriber<? super T>> {
        
    }
    
    public interface SingleOperator<Downstream, Upstream> extends Function<SingleSubscriber<? super Downstream>, SingleSubscriber<? super Upstream>> {
        
    }
    
    public interface SingleSubscriber<T> {
        
        
        void onSubscribe(Disposable d);
        
        void onSuccess(T value);

        void onError(Throwable e);
    }
    
    public interface SingleTransformer<Upstream, Downstream> extends Function<Single<Upstream>, Single<Downstream>> {
        
    }
    
    public static <T> Single<T> amb(final Iterable<? extends Single<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                final AtomicBoolean once = new AtomicBoolean();
                final CompositeDisposable set = new CompositeDisposable();
                s.onSubscribe(set);
                
                int c = 0;
                Iterator<? extends Single<? extends T>> iterator;
                
                try {
                    iterator = sources.iterator();
                } catch (Throwable e) {
                    s.onError(e);
                    return;
                }
                
                if (iterator == null) {
                    s.onError(new NullPointerException("The iterator returned is null"));
                    return;
                }
                for (;;) {
                    if (once.get()) {
                        return;
                    }
                    
                    boolean b;
                    
                    try {
                        b = iterator.hasNext();
                    } catch (Throwable e) {
                        s.onError(e);
                        return;
                    }
                    
                    if (once.get()) {
                        return;
                    }

                    if (!b) {
                        break;
                    }
                    
                    Single<? extends T> s1;

                    if (once.get()) {
                        return;
                    }

                    try {
                        s1 = iterator.next();
                    } catch (Throwable e) {
                        set.dispose();
                        s.onError(e);
                        return;
                    }
                    
                    if (s1 == null) {
                        set.dispose();
                        s.onError(new NullPointerException("The single source returned by the iterator is null"));
                        return;
                    }
                    
                    s1.subscribe(new SingleSubscriber<T>() {

                        @Override
                        public void onSubscribe(Disposable d) {
                            set.add(d);
                        }

                        @Override
                        public void onSuccess(T value) {
                            if (once.compareAndSet(false, true)) {
                                s.onSuccess(value);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (once.compareAndSet(false, true)) {
                                s.onError(e);
                            } else {
                                RxJavaPlugins.onError(e);
                            }
                        }
                        
                    });
                    c++;
                }
                
                if (c == 0 && !set.isDisposed()) {
                    s.onError(new NoSuchElementException());
                }
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Single<T> amb(final Single<? extends T>... sources) {
        if (sources.length == 0) {
            return error(new Supplier<Throwable>() {
                @Override
                public Throwable get() {
                    return new NoSuchElementException();
                }
            });
        }
        if (sources.length == 1) {
            return (Single<T>)sources[0];
        }
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                final AtomicBoolean once = new AtomicBoolean();
                final CompositeDisposable set = new CompositeDisposable();
                s.onSubscribe(set);
                
                for (Single<? extends T> s1 : sources) {
                    if (once.get()) {
                        return;
                    }
                    
                    if (s1 == null) {
                        set.dispose();
                        Throwable e = new NullPointerException("One of the sources is null");
                        if (once.compareAndSet(false, true)) {
                            s.onError(e);
                        } else {
                            RxJavaPlugins.onError(e);
                        }
                        return;
                    }
                    
                    s1.subscribe(new SingleSubscriber<T>() {

                        @Override
                        public void onSubscribe(Disposable d) {
                            set.add(d);
                        }

                        @Override
                        public void onSuccess(T value) {
                            if (once.compareAndSet(false, true)) {
                                s.onSuccess(value);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (once.compareAndSet(false, true)) {
                                s.onError(e);
                            } else {
                                RxJavaPlugins.onError(e);
                            }
                        }
                        
                    });
                }
            }
        });
    }

    public static <T> Flowable<T> concat(Iterable<? extends Single<? extends T>> sources) {
        return concat(Flowable.fromIterable(sources));
    }
    
    public static <T> Flowable<T> concat(Flowable<? extends Single<? extends T>> sources) {
        return sources.concatMap(new Function<Single<? extends T>, Publisher<? extends T>>() {
            @Override 
            public Publisher<? extends T> apply(Single<? extends T> v){
                return v.toFlowable();
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        return concat(Flowable.fromArray(s1, s2));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        return concat(Flowable.fromArray(s1, s2, s3));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4, s5));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        Objects.requireNonNull(s6, "s6 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4, s5, s6));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6,
            Single<? extends T> s7
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        Objects.requireNonNull(s6, "s6 is null");
        Objects.requireNonNull(s7, "s7 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4, s5, s6, s7));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6,
            Single<? extends T> s7, Single<? extends T> s8
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        Objects.requireNonNull(s6, "s6 is null");
        Objects.requireNonNull(s7, "s7 is null");
        Objects.requireNonNull(s8, "s8 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4, s5, s6, s7, s8));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6,
            Single<? extends T> s7, Single<? extends T> s8,
            Single<? extends T> s9
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        Objects.requireNonNull(s6, "s6 is null");
        Objects.requireNonNull(s7, "s7 is null");
        Objects.requireNonNull(s8, "s8 is null");
        Objects.requireNonNull(s9, "s9 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4, s5, s6, s7, s8, s9));
    }
    
    public static <T> Single<T> create(SingleOnSubscribe<T> onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        // TODO plugin wrapper
        return new Single<T>(onSubscribe);
    }
    
    public static <T> Single<T> defer(final Supplier<? extends Single<? extends T>> singleSupplier) {
        Objects.requireNonNull(singleSupplier, "singleSupplier is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(SingleSubscriber<? super T> s) {
                Single<? extends T> next;
                
                try {
                    next = singleSupplier.get();
                } catch (Throwable e) {
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onError(e);
                    return;
                }
                
                if (next == null) {
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onError(new NullPointerException("The Single supplied was null"));
                    return;
                }
                
                next.subscribe(s);
            }
        });
    }
    
    public static <T> Single<T> error(final Supplier<? extends Throwable> errorSupplier) {
        Objects.requireNonNull(errorSupplier, "errorSupplier is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(SingleSubscriber<? super T> s) {
                Throwable error;
                
                try {
                    error = errorSupplier.get();
                } catch (Throwable e) {
                    error = e;
                }
                
                if (error == null) {
                    error = new NullPointerException();
                }
                
                s.onSubscribe(EmptyDisposable.INSTANCE);
                s.onError(error);
            }
        });
    }
    
    public static <T> Single<T> error(final Throwable error) {
        Objects.requireNonNull(error, "error is null");
        return error(new Supplier<Throwable>() {
            @Override
            public Throwable get() {
                return error;
            }
        });
    }
    
    public static <T> Single<T> fromCallable(final Callable<? extends T> callable) {
        Objects.requireNonNull(callable, "callable is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(SingleSubscriber<? super T> s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                try {
                    T v = callable.call();
                    if (v != null) {
                        s.onSuccess(v);
                    } else {
                        s.onError(new NullPointerException());
                    }
                } catch (Throwable e) {
                    s.onError(e);
                }
            }
        });
    }
    
    public static <T> Single<T> fromFuture(Future<? extends T> future) {
        return Flowable.<T>fromFuture(future).toSingle();
    }

    public static <T> Single<T> fromFuture(Future<? extends T> future, long timeout, TimeUnit unit) {
        return Flowable.<T>fromFuture(future, timeout, unit).toSingle();
    }

    public static <T> Single<T> fromFuture(Future<? extends T> future, long timeout, TimeUnit unit, Scheduler scheduler) {
        return Flowable.<T>fromFuture(future, timeout, unit, scheduler).toSingle();
    }

    public static <T> Single<T> fromFuture(Future<? extends T> future, Scheduler scheduler) {
        return Flowable.<T>fromFuture(future, scheduler).toSingle();
    }

    public static <T> Single<T> fromPublisher(final Publisher<? extends T> publisher) {
        Objects.requireNonNull(publisher, "publisher is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                publisher.subscribe(new Subscriber<T>() {
                    T value;
                    @Override
                    public void onComplete() {
                        T v = value;
                        value = null;
                        if (v != null) {
                            s.onSuccess(v);
                        } else {
                            s.onError(new NoSuchElementException());
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        value = null;
                        s.onError(t);
                    }

                    @Override
                    public void onNext(T t) {
                        value = t;
                    }

                    @Override
                    public void onSubscribe(Subscription inner) {
                        s.onSubscribe(Disposables.from(inner));
                        inner.request(Long.MAX_VALUE);
                    }
                    
                });
            }
        });
    }

    public static <T> Single<T> just(final T value) {
        Objects.requireNonNull(value, "value is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(SingleSubscriber<? super T> s) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                s.onSuccess(value);
            }
        });
    }

    public static <T> Flowable<T> merge(Iterable<? extends Single<? extends T>> sources) {
        return merge(Flowable.fromIterable(sources));
    }

    public static <T> Flowable<T> merge(Flowable<? extends Single<? extends T>> sources) {
        return sources.flatMap(new Function<Single<? extends T>, Publisher<? extends T>>() {
            @Override 
            public Publisher<? extends T> apply(Single<? extends T> v){
                return v.toFlowable();
            }
        });
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Single<T> merge(Single<? extends Single<? extends T>> source) {
        return source.flatMap((Function)Functions.identity());
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        return merge(Flowable.fromArray(s1, s2));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        return merge(Flowable.fromArray(s1, s2, s3));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4, s5));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        Objects.requireNonNull(s6, "s6 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4, s5, s6));
    }

    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6,
            Single<? extends T> s7
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        Objects.requireNonNull(s6, "s6 is null");
        Objects.requireNonNull(s7, "s7 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4, s5, s6, s7));
    }

    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6,
            Single<? extends T> s7, Single<? extends T> s8
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        Objects.requireNonNull(s6, "s6 is null");
        Objects.requireNonNull(s7, "s7 is null");
        Objects.requireNonNull(s8, "s8 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4, s5, s6, s7, s8));
    }

    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6,
            Single<? extends T> s7, Single<? extends T> s8,
            Single<? extends T> s9
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        Objects.requireNonNull(s6, "s6 is null");
        Objects.requireNonNull(s7, "s7 is null");
        Objects.requireNonNull(s8, "s8 is null");
        Objects.requireNonNull(s9, "s9 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4, s5, s6, s7, s8, s9));
    }
    
    static final Single<Object> NEVER = create(new SingleOnSubscribe<Object>() {
        @Override
        public void accept(SingleSubscriber<? super Object> s) {
            s.onSubscribe(EmptyDisposable.INSTANCE);
        }
    });
    
    @SuppressWarnings("unchecked")
    public static <T> Single<T> never() {
        return (Single<T>)NEVER; 
    }
    
    public static Single<Long> timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }
    
    public static Single<Long> timer(final long delay, final TimeUnit unit, final Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return create(new SingleOnSubscribe<Long>() {
            @Override
            public void accept(final SingleSubscriber<? super Long> s) {
                MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
                
                s.onSubscribe(mad);
                
                mad.set(scheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        s.onSuccess(0L);
                    }
                }, delay, unit));
            }
        });
    }
    
    public static <T> Single<Boolean> equals(final Single<? extends T> first, final Single<? extends T> second) {
        Objects.requireNonNull(first, "first is null");
        Objects.requireNonNull(second, "second is null");
        return create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void accept(final SingleSubscriber<? super Boolean> s) {
                final AtomicInteger count = new AtomicInteger();
                final Object[] values = { null, null };
                
                final CompositeDisposable set = new CompositeDisposable();
                s.onSubscribe(set);
                
                class InnerSubscriber implements SingleSubscriber<T> {
                    final int index;
                    public InnerSubscriber(int index) {
                        this.index = index;
                    }
                    @Override
                    public void onSubscribe(Disposable d) {
                        set.add(d);
                    }

                    @Override
                    public void onSuccess(T value) {
                        values[index] = value;
                        
                        if (count.incrementAndGet() == 2) {
                            s.onSuccess(Objects.equals(values[0], values[1]));
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        for (;;) {
                            int state = count.get();
                            if (state >= 2) {
                                RxJavaPlugins.onError(e);
                                return;
                            }
                            if (count.compareAndSet(state, 2)) {
                                s.onError(e);
                                return;
                            }
                        }
                    }
                    
                }
                
                first.subscribe(new InnerSubscriber(0));
                second.subscribe(new InnerSubscriber(1));
            }
        });
    }

    public static <T, U> Single<T> using(Supplier<U> resourceSupplier, 
            Function<? super U, ? extends Single<? extends T>> singleFunction, Consumer<? super U> disposer) {
        return using(resourceSupplier, singleFunction, disposer, true);
    }
        
    public static <T, U> Single<T> using(
            final Supplier<U> resourceSupplier, 
            final Function<? super U, ? extends Single<? extends T>> singleFunction, 
            final Consumer<? super U> disposer, 
            final boolean eager) {
        Objects.requireNonNull(resourceSupplier, "resourceSupplier is null");
        Objects.requireNonNull(singleFunction, "singleFunction is null");
        Objects.requireNonNull(disposer, "disposer is null");
        
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                final U resource;
                
                try {
                    resource = resourceSupplier.get();
                } catch (Throwable ex) {
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onError(ex);
                    return;
                }
                
                Single<? extends T> s1;
                
                try {
                    s1 = singleFunction.apply(resource);
                } catch (Throwable ex) {
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onError(ex);
                    return;
                }
                
                if (s1 == null) {
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    s.onError(new NullPointerException("The Single supplied by the function was null"));
                    return;
                }
                
                s1.subscribe(new SingleSubscriber<T>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        if (eager) {
                            CompositeDisposable set = new CompositeDisposable();
                            set.add(d);
                            set.add(new Disposable() {
                                @Override
                                public void dispose() {
                                    try {
                                        disposer.accept(resource);
                                    } catch (Throwable e) {
                                        RxJavaPlugins.onError(e);
                                    }
                                }
                            });
                        } else {
                            s.onSubscribe(d);
                        }
                    }

                    @Override
                    public void onSuccess(T value) {
                        if (eager) {
                            try {
                                disposer.accept(resource);
                            } catch (Throwable e) {
                                s.onError(e);
                                return;
                            }
                        }
                        s.onSuccess(value);
                        if (!eager) {
                            try {
                                disposer.accept(resource);
                            } catch (Throwable e) {
                                RxJavaPlugins.onError(e);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (eager) {
                            try {
                                disposer.accept(resource);
                            } catch (Throwable ex) {
                                e = new CompositeException(ex, e);
                            }
                        }
                        s.onError(e);
                        if (!eager) {
                            try {
                                disposer.accept(resource);
                            } catch (Throwable ex) {
                                RxJavaPlugins.onError(ex);
                            }
                        }
                    }
                    
                });
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static <T, R> Single<R> zip(final Iterable<? extends Single<? extends T>> sources, Function<? super Object[], ? extends R> zipper) {
        Objects.requireNonNull(sources, "sources is null");
        
        Iterable<? extends Flowable<T>> it = new Iterable<Flowable<T>>() {
            @Override
            public Iterator<Flowable<T>> iterator() {
                final Iterator<? extends Single<? extends T>> sit = sources.iterator();
                return new Iterator<Flowable<T>>() {

                    @Override
                    public boolean hasNext() {
                        return sit.hasNext();
                    }

                    @Override
                    public Flowable<T> next() {
                        return ((Flowable<T>)sit.next().toFlowable());
                    }
                    
                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
        return Flowable.zipIterable(zipper, false, 1, it).toSingle();
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, R> Single<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            BiFunction<? super T1, ? super T2, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, R> Single<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3,
            Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, R> Single<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3, Single<? extends T4> s4,
            Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3, s4);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, R> Single<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3, Single<? extends T4> s4,
            Single<? extends T5> s5,
            Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3, s4, s5);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, R> Single<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3, Single<? extends T4> s4,
            Single<? extends T5> s5, Single<? extends T6> s6,
            Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        Objects.requireNonNull(s6, "s6 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3, s4, s5, s6);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, R> Single<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3, Single<? extends T4> s4,
            Single<? extends T5> s5, Single<? extends T6> s6,
            Single<? extends T7> s7, 
            Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        Objects.requireNonNull(s6, "s6 is null");
        Objects.requireNonNull(s7, "s7 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3, s4, s5, s6, s7);
    }
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Single<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3, Single<? extends T4> s4,
            Single<? extends T5> s5, Single<? extends T6> s6,
            Single<? extends T7> s7, Single<? extends T8> s8,
            Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        Objects.requireNonNull(s6, "s6 is null");
        Objects.requireNonNull(s7, "s7 is null");
        Objects.requireNonNull(s8, "s8 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3, s4, s5, s6, s7, s8);
    }
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Single<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3, Single<? extends T4> s4,
            Single<? extends T5> s5, Single<? extends T6> s6,
            Single<? extends T7> s7, Single<? extends T8> s8,
            Single<? extends T9> s9,
            Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        Objects.requireNonNull(s5, "s5 is null");
        Objects.requireNonNull(s6, "s6 is null");
        Objects.requireNonNull(s7, "s7 is null");
        Objects.requireNonNull(s8, "s8 is null");
        Objects.requireNonNull(s9, "s9 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3, s4, s5, s6, s7, s8, s9);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T, R> Single<R> zipArray(Function<? super Object[], ? extends R> zipper, Single<? extends T>... sources) {
        Publisher[] sourcePublishers = new Publisher[sources.length];
        int i = 0;
        for (Single<? extends T> s : sources) {
            sourcePublishers[i] = s.toFlowable();
            i++;
        }
        return Flowable.zipArray(zipper, false, 1, sourcePublishers).toSingle();
    }

    protected final SingleOnSubscribe<T> onSubscribe;

    protected Single(SingleOnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    @SuppressWarnings("unchecked")
    public final Single<T> ambWith(Single<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return amb(this, other);
    }
    
    public final Single<T> asSingle() {
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(SingleSubscriber<? super T> s) {
                subscribe(s);
            }
        });
    }
    
    public final <R> Single<R> compose(Function<? super Single<T>, ? extends Single<R>> convert) {
        return to(convert);
    }

    public final Single<T> cache() {
        final AtomicInteger wip = new AtomicInteger();
        final AtomicReference<Object> notification = new AtomicReference<Object>();
        final List<SingleSubscriber<? super T>> subscribers = new ArrayList<SingleSubscriber<? super T>>();
        
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(SingleSubscriber<? super T> s) {
                Object o = notification.get();
                if (o != null) {
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    if (NotificationLite.isError(o)) {
                        s.onError(NotificationLite.getError(o));
                    } else {
                        s.onSuccess(NotificationLite.<T>getValue(o));
                    }
                    return;
                }
                
                synchronized (subscribers) {
                    o = notification.get();
                    if (o == null) {
                        subscribers.add(s);
                    }
                }
                if (o != null) {
                    s.onSubscribe(EmptyDisposable.INSTANCE);
                    if (NotificationLite.isError(o)) {
                        s.onError(NotificationLite.getError(o));
                    } else {
                        s.onSuccess(NotificationLite.<T>getValue(o));
                    }
                    return;
                }
                
                if (wip.getAndIncrement() != 0) {
                    return;
                }
                
                subscribe(new SingleSubscriber<T>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        
                    }

                    @Override
                    public void onSuccess(T value) {
                        notification.set(NotificationLite.next(value));
                        List<SingleSubscriber<? super T>> list;
                        synchronized (subscribers) {
                            list = new ArrayList<SingleSubscriber<? super T>>(subscribers);
                            subscribers.clear();
                        }
                        for (SingleSubscriber<? super T> s1 : list) {
                            s1.onSuccess(value);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        notification.set(NotificationLite.error(e));
                        List<SingleSubscriber<? super T>> list;
                        synchronized (subscribers) {
                            list = new ArrayList<SingleSubscriber<? super T>>(subscribers);
                            subscribers.clear();
                        }
                        for (SingleSubscriber<? super T> s1 : list) {
                            s1.onError(e);
                        }
                    }
                    
                });
            }
        });
    }
    
    public final <U> Single<U> cast(final Class<? extends U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return create(new SingleOnSubscribe<U>() {
            @Override
            public void accept(final SingleSubscriber<? super U> s) {
                subscribe(new SingleSubscriber<T>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        s.onSubscribe(d);
                    }

                    @Override
                    public void onSuccess(T value) {
                        U v;
                        try {
                            v = clazz.cast(value);
                        } catch (ClassCastException ex) {
                            s.onError(ex);
                            return;
                        }
                        s.onSuccess(v);
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }
                    
                });
            }
        });
    }
    
    public final Flowable<T> concatWith(Single<? extends T> other) {
        return concat(this, other);
    }
    
    public final Single<T> delay(long time, TimeUnit unit) {
        return delay(time, unit, Schedulers.computation());
    }
    
    public final Single<T> delay(final long time, final TimeUnit unit, final Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                final MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
                s.onSubscribe(mad);
                subscribe(new SingleSubscriber<T>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        mad.set(d);
                    }

                    @Override
                    public void onSuccess(final T value) {
                        mad.set(scheduler.scheduleDirect(new Runnable() {
                            @Override
                            public void run() {
                                s.onSuccess(value);
                            }
                        }, time, unit));
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }
                    
                });
            }
        });
    }
    
    public final Single<T> doOnSubscribe(final Consumer<? super Disposable> onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                subscribe(new SingleSubscriber<T>() {
                    boolean done;
                    @Override
                    public void onSubscribe(Disposable d) {
                        try {
                            onSubscribe.accept(d);
                        } catch (Throwable ex) {
                            done = true;
                            d.dispose();
                            s.onSubscribe(EmptyDisposable.INSTANCE);
                            s.onError(ex);
                            return;
                        }
                        
                        s.onSubscribe(d);
                    }

                    @Override
                    public void onSuccess(T value) {
                        if (done) {
                            return;
                        }
                        s.onSuccess(value);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (done) {
                            RxJavaPlugins.onError(e);
                            return;
                        }
                        s.onError(e);
                    }
                    
                });
            }
        });
    }
    
    public final Single<T> doOnSuccess(final Consumer<? super T> onSuccess) {
        Objects.requireNonNull(onSuccess, "onSuccess is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                subscribe(new SingleSubscriber<T>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        s.onSubscribe(d);
                    }

                    @Override
                    public void onSuccess(T value) {
                        try {
                            onSuccess.accept(value);
                        } catch (Throwable ex) {
                            s.onError(ex);
                            return;
                        }
                        s.onSuccess(value);
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }
                    
                });
            }
        });
    }
    
    public final Single<T> doOnError(final Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onError, "onError is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                subscribe(new SingleSubscriber<T>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        s.onSubscribe(d);
                    }

                    @Override
                    public void onSuccess(T value) {
                        s.onSuccess(value);
                    }

                    @Override
                    public void onError(Throwable e) {
                        try {
                            onError.accept(e);
                        } catch (Throwable ex) {
                            e = new CompositeException(ex, e);
                        }
                        s.onError(e);
                    }
                    
                });
            }
        });
    }
    
    public final Single<T> doOnCancel(final Runnable onCancel) {
        Objects.requireNonNull(onCancel, "onCancel is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                subscribe(new SingleSubscriber<T>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        CompositeDisposable set = new CompositeDisposable();
                        set.add(Disposables.from(onCancel));
                        set.add(d);
                        s.onSubscribe(set);
                    }

                    @Override
                    public void onSuccess(T value) {
                        s.onSuccess(value);
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }
                    
                });
            }
        });
    }

    public final <R> Single<R> flatMap(Function<? super T, ? extends Single<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return lift(new SingleOperatorFlatMap<T, R>(mapper));   
    }

    public final <R> Flowable<R> flatMapPublisher(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return toFlowable().flatMap(mapper);
    }
    
    public final T get() {
        final AtomicReference<T> valueRef = new AtomicReference<T>();
        final AtomicReference<Throwable> errorRef = new AtomicReference<Throwable>();
        final CountDownLatch cdl = new CountDownLatch(1);
        
        subscribe(new SingleSubscriber<T>() {
            @Override
            public void onError(Throwable e) {
                errorRef.lazySet(e);
                cdl.countDown();
            }

            @Override
            public void onSubscribe(Disposable d) {
            }
            @Override
            public void onSuccess(T value) {
                valueRef.lazySet(value);
                cdl.countDown();
            }
        });
        
        if (cdl.getCount() != 0L) {
            try {
                cdl.await();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
        Throwable e = errorRef.get();
        if (e != null) {
            throw Exceptions.propagate(e);
        }
        return valueRef.get();
    }
    
    public final <R> Single<R> lift(final SingleOperator<? extends R, ? super T> onLift) {
        Objects.requireNonNull(onLift, "onLift is null");
        return create(new SingleOnSubscribe<R>() {
            @Override
            public void accept(SingleSubscriber<? super R> s) {
                try {
                    SingleSubscriber<? super T> sr = onLift.apply(s);
                    
                    if (sr == null) {
                        throw new NullPointerException("The onLift returned a null subscriber");
                    }
                    // TODO plugin wrapper
                    onSubscribe.accept(sr);
                } catch (NullPointerException ex) {
                    throw ex;
                } catch (Throwable ex) {
                    RxJavaPlugins.onError(ex);
                    NullPointerException npe = new NullPointerException("Not really but can't throw other than NPE");
                    npe.initCause(ex);
                    throw npe;
                }
            }
        });
    }
    
    public final <R> Single<R> map(Function<? super T, ? extends R> mapper) {
        return lift(new SingleOperatorMap<T, R>(mapper));
    }

    public final Single<Boolean> contains(Object value) {
        return contains(value, Objects.equalsPredicate());
    }

    public final Single<Boolean> contains(final Object value, final BiPredicate<Object, Object> comparer) {
        Objects.requireNonNull(value, "value is null");
        Objects.requireNonNull(comparer, "comparer is null");
        return create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void accept(final SingleSubscriber<? super Boolean> s) {
                subscribe(new SingleSubscriber<T>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        s.onSubscribe(d);
                    }

                    @Override
                    public void onSuccess(T v) {
                        s.onSuccess(comparer.test(v, value));
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }
                    
                });
            }
        });
    }
    
    public final Flowable<T> mergeWith(Single<? extends T> other) {
        return merge(this, other);
    }
    
    public final Single<Single<T>> nest() {
        return just(this);
    }
    
    public final Single<T> observeOn(final Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                final CompositeDisposable mad = new CompositeDisposable();
                s.onSubscribe(mad);
                
                subscribe(new SingleSubscriber<T>() {

                    @Override
                    public void onError(final Throwable e) {
                        mad.add(scheduler.scheduleDirect(new Runnable() {
                            @Override
                            public void run() {
                                s.onError(e);
                            }
                        }));
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
                        mad.add(d);
                    }

                    @Override
                    public void onSuccess(final T value) {
                        mad.add(scheduler.scheduleDirect(new Runnable() {
                            @Override
                            public void run() {
                                s.onSuccess(value);
                            }
                        }));
                    }
                    
                });
            }
        });
    }

    public final Single<T> onErrorReturn(final Supplier<? extends T> valueSupplier) {
        Objects.requireNonNull(valueSupplier, "valueSupplier is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                subscribe(new SingleSubscriber<T>() {

                    @Override
                    public void onError(Throwable e) {
                        T v;
                        
                        try {
                            v = valueSupplier.get();
                        } catch (Throwable ex) {
                            s.onError(new CompositeException(ex, e));
                            return;
                        }
                        
                        if (v == null) {
                            NullPointerException npe = new NullPointerException("Value supplied was null");
                            npe.initCause(e);
                            s.onError(npe);
                            return;
                        }
                        
                        s.onSuccess(v);
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
                        s.onSubscribe(d);
                    }

                    @Override
                    public void onSuccess(T value) {
                        s.onSuccess(value);
                    }
                    
                });
            }
        });
    }
    
    public final Single<T> onErrorReturn(final T value) {
        Objects.requireNonNull(value, "value is null");
        return onErrorReturn(new Supplier<T>() {
            @Override
            public T get() {
                return value;
            }
        });
    }

    public final Single<T> onErrorResumeNext(
            final Function<? super Throwable, ? extends Single<? extends T>> nextFunction) {
        Objects.requireNonNull(nextFunction, "nextFunction is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                final MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
                s.onSubscribe(mad);
                
                subscribe(new SingleSubscriber<T>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        mad.set(d);
                    }

                    @Override
                    public void onSuccess(T value) {
                        s.onSuccess(value);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Single<? extends T> next;
                        
                        try {
                            next = nextFunction.apply(e);
                        } catch (Throwable ex) {
                            s.onError(new CompositeException(ex, e));
                            return;
                        }
                        
                        if (next == null) {
                            NullPointerException npe = new NullPointerException("The next Single supplied was null");
                            npe.initCause(e);
                            s.onError(npe);
                            return;
                        }
                        
                        next.subscribe(new SingleSubscriber<T>() {

                            @Override
                            public void onSubscribe(Disposable d) {
                                mad.set(d);
                            }

                            @Override
                            public void onSuccess(T value) {
                                s.onSuccess(value);
                            }

                            @Override
                            public void onError(Throwable e) {
                                s.onError(e);
                            }
                            
                        });
                    }
                    
                });
            }
        });
    }
    
    public final Flowable<T> repeat() {
        return toFlowable().repeat();
    }
    
    public final Flowable<T> repeat(long times) {
        return toFlowable().repeat(times);
    }
    
    public final Flowable<T> repeatWhen(Function<? super Flowable<Object>, ? extends Publisher<Object>> handler) {
        return toFlowable().repeatWhen(handler);
    }
    
    public final Flowable<T> repeatUntil(BooleanSupplier stop) {
        return toFlowable().repeatUntil(stop);
    }
    
    public final Single<T> retry() {
        return toFlowable().retry().toSingle();
    }
    
    public final Single<T> retry(long times) {
        return toFlowable().retry(times).toSingle();
    }
    
    public final Single<T> retry(BiPredicate<? super Integer, ? super Throwable> predicate) {
        return toFlowable().retry(predicate).toSingle();
    }
    
    public final Single<T> retry(Predicate<? super Throwable> predicate) {
        return toFlowable().retry(predicate).toSingle();
    }
    
    public final Single<T> retryWhen(Function<? super Flowable<? extends Throwable>, ? extends Publisher<Object>> handler) {
        return toFlowable().retryWhen(handler).toSingle();
    }
    
    public final void safeSubscribe(Subscriber<? super T> s) {
        toFlowable().safeSubscribe(s);
    }
    
    public final Disposable subscribe() {
        return subscribe(Functions.emptyConsumer(), RxJavaPlugins.errorConsumer());
    }
    
    public final Disposable subscribe(final BiConsumer<? super T, ? super Throwable> onCallback) {
        Objects.requireNonNull(onCallback, "onCallback is null");
        
        final MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        
        subscribe(new SingleSubscriber<T>() {
            @Override
            public void onError(Throwable e) {
                onCallback.accept(null, e);
            }
            
            @Override
            public void onSubscribe(Disposable d) {
                mad.set(d);
            }
            
            @Override
            public void onSuccess(T value) {
                onCallback.accept(value, null);
            }
        });
        
        return mad;
    }
    
    public final Disposable subscribe(Consumer<? super T> onSuccess) {
        return subscribe(onSuccess, RxJavaPlugins.errorConsumer());
    }
    
    public final Disposable subscribe(final Consumer<? super T> onSuccess, final Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onSuccess, "onSuccess is null");
        Objects.requireNonNull(onError, "onError is null");
        
        final MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        
        subscribe(new SingleSubscriber<T>() {
            @Override
            public void onError(Throwable e) {
                onError.accept(e);
            }
            
            @Override
            public void onSubscribe(Disposable d) {
                mad.set(d);
            }
            
            @Override
            public void onSuccess(T value) {
                onSuccess.accept(value);
            }
        });
        
        return mad;
    }
    
    public final void subscribe(SingleSubscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber is null");
        // TODO plugin wrapper
        onSubscribe.accept(subscriber);
    }
    
    public final void subscribe(Subscriber<? super T> s) {
        toFlowable().subscribe(s);
    }
    
    public final Single<T> subscribeOn(final Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                scheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        subscribe(s);
                    }
                });
            }
        });
    }
    
    public final Single<T> timeout(long timeout, TimeUnit unit) {
        return timeout0(timeout, unit, Schedulers.computation(), null);
    }
    
    public final Single<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        return timeout0(timeout, unit, scheduler, null);
    }

    public final Single<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler, Single<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(timeout, unit, scheduler, other);
    }

    public final Single<T> timeout(long timeout, TimeUnit unit, Single<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(timeout, unit, Schedulers.computation(), other);
    }

    private Single<T> timeout0(final long timeout, final TimeUnit unit, final Scheduler scheduler, final Single<? extends T> other) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return create(new SingleOnSubscribe<T>() {
            @Override
            public void accept(final SingleSubscriber<? super T> s) {
                final CompositeDisposable set = new CompositeDisposable();
                s.onSubscribe(set);
                
                final AtomicBoolean once = new AtomicBoolean();
                
                Disposable timer = scheduler.scheduleDirect(new Runnable() {
                    @Override
                    public void run() {
                        if (once.compareAndSet(false, true)) {
                            if (other != null) {
                                set.clear();
                                other.subscribe(new SingleSubscriber<T>() {

                                    @Override
                                    public void onError(Throwable e) {
                                        set.dispose();
                                        s.onError(e);
                                    }

                                    @Override
                                    public void onSubscribe(Disposable d) {
                                        set.add(d);
                                    }

                                    @Override
                                    public void onSuccess(T value) {
                                        set.dispose();
                                        s.onSuccess(value);
                                    }
                                    
                                });
                            } else {
                                set.dispose();
                                s.onError(new TimeoutException());
                            }
                        }
                    }
                }, timeout, unit);
                
                set.add(timer);
                
                subscribe(new SingleSubscriber<T>() {

                    @Override
                    public void onError(Throwable e) {
                        if (once.compareAndSet(false, true)) {
                            set.dispose();
                            s.onError(e);
                        }
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
                        set.add(d);
                    }

                    @Override
                    public void onSuccess(T value) {
                        if (once.compareAndSet(false, true)) {
                            set.dispose();
                            s.onSuccess(value);
                        }
                    }
                    
                });
            }
        });
    }

    public final <R> R to(Function<? super Single<T>, R> convert) {
        return convert.apply(this);
    }
    
    public final Flowable<T> toFlowable() {
        return Flowable.create(new Publisher<T>() {
            @Override
            public void subscribe(final Subscriber<? super T> s) {
                final ScalarAsyncSubscription<T> sas = new ScalarAsyncSubscription<T>(s);
                final AsyncSubscription as = new AsyncSubscription();
                as.setSubscription(sas);
                s.onSubscribe(as);
                
                Single.this.subscribe(new SingleSubscriber<T>() {
                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                    }

                    @Override
                    public void onSubscribe(Disposable d) {
                        as.setResource(d);
                    }

                    @Override
                    public void onSuccess(T value) {
                        sas.setValue(value);
                    }
                    
                });
            }
        });
    }
    
    public final void unsafeSubscribe(Subscriber<? super T> s) {
        toFlowable().unsafeSubscribe(s);
    }
    
    public final <U, R> Single<R> zipWith(Single<U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip(this, other, zipper);
    }
}