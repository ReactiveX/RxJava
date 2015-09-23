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
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.reactivestreams.*;

import io.reactivex.disposables.*;
import io.reactivex.functions.*;
import io.reactivex.internal.disposables.EmptyDisposable;
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
    
    public static <T> Single<T> amb(Iterable<? extends Single<? extends T>> sources) {
        return create(s -> {
            AtomicBoolean once = new AtomicBoolean();
            CompositeDisposable set = new CompositeDisposable();
            s.onSubscribe(set);
            
            int c = 0;
            for (Single<? extends T> s1 : sources) {
                if (once.get()) {
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
        });
    }
    
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> Single<T> amb(Single<? extends T>... sources) {
        if (sources.length == 0) {
            return error(() -> new NoSuchElementException());
        }
        if (sources.length == 1) {
            return (Single<T>)sources[0];
        }
        return create(s -> {
            AtomicBoolean once = new AtomicBoolean();
            CompositeDisposable set = new CompositeDisposable();
            s.onSubscribe(set);
            
            for (Single<? extends T> s1 : sources) {
                if (once.get()) {
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
        });
    }

    public static <T> Observable<T> concat(Iterable<? extends Single<? extends T>> sources) {
        return concat(Observable.fromIterable(sources));
    }
    
    public static <T> Observable<T> concat(Observable<? extends Single<? extends T>> sources) {
        return sources.concatMap(Single::toFlowable);
    }
    
    public static <T> Observable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        return concat(Observable.fromArray(s1, s2));
    }
    
    public static <T> Observable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        return concat(Observable.fromArray(s1, s2, s3));
    }
    
    public static <T> Observable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        return concat(Observable.fromArray(s1, s2, s3, s4));
    }
    
    public static <T> Observable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        return concat(Observable.fromArray(s1, s2, s3, s4, s5));
    }
    
    public static <T> Observable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        Objects.requireNonNull(s6);
        return concat(Observable.fromArray(s1, s2, s3, s4, s5, s6));
    }
    
    public static <T> Observable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6,
            Single<? extends T> s7
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        Objects.requireNonNull(s6);
        Objects.requireNonNull(s7);
        return concat(Observable.fromArray(s1, s2, s3, s4, s5, s6, s7));
    }
    
    public static <T> Observable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6,
            Single<? extends T> s7, Single<? extends T> s8
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        Objects.requireNonNull(s6);
        Objects.requireNonNull(s7);
        Objects.requireNonNull(s8);
        return concat(Observable.fromArray(s1, s2, s3, s4, s5, s6, s7, s8));
    }
    
    public static <T> Observable<T> concat(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6,
            Single<? extends T> s7, Single<? extends T> s8,
            Single<? extends T> s9
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        Objects.requireNonNull(s6);
        Objects.requireNonNull(s7);
        Objects.requireNonNull(s8);
        Objects.requireNonNull(s9);
        return concat(Observable.fromArray(s1, s2, s3, s4, s5, s6, s7, s8, s9));
    }
    
    public static <T> Single<T> create(SingleOnSubscribe<T> onSubscribe) {
        Objects.requireNonNull(onSubscribe);
        // TODO plugin wrapper
        return new Single<>(onSubscribe);
    }
    
    public static <T> Single<T> defer(Supplier<? extends Single<? extends T>> singleSupplier) {
        return create(s -> {
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
        });
    }
    
    public static <T> Single<T> error(Supplier<? extends Throwable> errorSupplier) {
        Objects.requireNonNull(errorSupplier);
        return create(s -> {
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
        });
    }
    
    public static <T> Single<T> error(Throwable error) {
        Objects.requireNonNull(error);
        return error(() -> error);
    }
    
    public static <T> Single<T> fromCallable(Callable<? extends T> callable) {
        Objects.requireNonNull(callable);
        return create(s -> {
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
        });
    }
    
    public static <T> Single<T> fromFuture(CompletableFuture<? extends T> future) {
        Objects.requireNonNull(future);
        return create(s -> {
            BooleanDisposable bd = new BooleanDisposable(() -> future.cancel(true));
            s.onSubscribe(bd);
            future.whenComplete((v, e) -> {
                if (e != null) {
                    if (!bd.isDisposed()) {
                        s.onError(e);
                    }
                } else {
                    if (v != null) {
                        s.onSuccess(v);
                    } else {
                        s.onError(new NullPointerException());
                    }
                }
            });
        });
    }
    
    public static <T> Single<T> fromFuture(Future<? extends T> future) {
        return Observable.<T>fromFuture(future).toSingle();
    }

    public static <T> Single<T> fromFuture(Future<? extends T> future, long timeout, TimeUnit unit) {
        return Observable.<T>fromFuture(future, timeout, unit).toSingle();
    }

    public static <T> Single<T> fromFuture(Future<? extends T> future, long timeout, TimeUnit unit, Scheduler scheduler) {
        return Observable.<T>fromFuture(future, timeout, unit, scheduler).toSingle();
    }

    public static <T> Single<T> fromFuture(Future<? extends T> future, Scheduler scheduler) {
        return Observable.<T>fromFuture(future, scheduler).toSingle();
    }

    public static <T> Single<T> fromPublisher(Publisher<? extends T> publisher) {
        Objects.requireNonNull(publisher);
        return create(s -> {
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
                    s.onSubscribe(inner::cancel);
                    inner.request(Long.MAX_VALUE);
                }
                
            });
        });
    }

    public static <T> Single<T> just(T value) {
        Objects.requireNonNull(value);
        return create(s -> {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            s.onSuccess(value);
        });
    }

    public static <T> Observable<T> merge(Iterable<? extends Single<? extends T>> sources) {
        return merge(Observable.fromIterable(sources));
    }

    public static <T> Observable<T> merge(Observable<? extends Single<? extends T>> sources) {
        return sources.flatMap(Single::toFlowable);
    }
    
    public static <T> Single<T> merge(Single<? extends Single<? extends T>> source) {
        return source.flatMap(v -> v);
    }
    
    public static <T> Observable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        return merge(Observable.fromArray(s1, s2));
    }
    
    public static <T> Observable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        return merge(Observable.fromArray(s1, s2, s3));
    }
    
    public static <T> Observable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        return merge(Observable.fromArray(s1, s2, s3, s4));
    }
    
    public static <T> Observable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        return merge(Observable.fromArray(s1, s2, s3, s4, s5));
    }
    
    public static <T> Observable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        Objects.requireNonNull(s6);
        return merge(Observable.fromArray(s1, s2, s3, s4, s5, s6));
    }

    public static <T> Observable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6,
            Single<? extends T> s7
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        Objects.requireNonNull(s6);
        Objects.requireNonNull(s7);
        return merge(Observable.fromArray(s1, s2, s3, s4, s5, s6, s7));
    }

    public static <T> Observable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6,
            Single<? extends T> s7, Single<? extends T> s8
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        Objects.requireNonNull(s6);
        Objects.requireNonNull(s7);
        Objects.requireNonNull(s8);
        return merge(Observable.fromArray(s1, s2, s3, s4, s5, s6, s7, s8));
    }

    public static <T> Observable<T> merge(
            Single<? extends T> s1, Single<? extends T> s2,
            Single<? extends T> s3, Single<? extends T> s4,
            Single<? extends T> s5, Single<? extends T> s6,
            Single<? extends T> s7, Single<? extends T> s8,
            Single<? extends T> s9
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        Objects.requireNonNull(s6);
        Objects.requireNonNull(s7);
        Objects.requireNonNull(s8);
        Objects.requireNonNull(s9);
        return merge(Observable.fromArray(s1, s2, s3, s4, s5, s6, s7, s8, s9));
    }
    
    public static <T> Single<T> never() {
        return create(s -> {
            s.onSubscribe(EmptyDisposable.INSTANCE);
        });
    }
    
    public static Single<Long> timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }
    
    public static Single<Long> timer(long delay, TimeUnit unit, Scheduler scheduler) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(scheduler);
        return create(s -> {
            MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
            
            s.onSubscribe(mad);
            
            mad.set(scheduler.scheduleDirect(() -> s.onSuccess(0L), delay, unit));
        });
    }
    
    public static <T> Single<Boolean> equals(Single<? extends T> first, Single<? extends T> second) {
        Objects.requireNonNull(first);
        Objects.requireNonNull(second);
        return create(s -> {
            AtomicInteger count = new AtomicInteger();
            Object[] values = { null, null };
            
            CompositeDisposable set = new CompositeDisposable();
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
        });
    }

    @SuppressWarnings("unchecked")
    private static <T1, T2, R> Function<Object[], R> toFunction(BiFunction<? super T1, ? super T2, ? extends R> biFunction) {
        Objects.requireNonNull(biFunction);
        return a -> {
            if (a.length != 2) {
                throw new IllegalArgumentException("Array of size 2 expected but got " + a.length);
            }
            return ((BiFunction<Object, Object, R>)biFunction).apply(a[0], a[1]);
        };
    }

    public static <T, U> Single<T> using(Supplier<U> resourceSupplier, 
            Function<? super U, ? extends Single<? extends T>> singleFunction, Consumer<? super U> disposer) {
        return using(resourceSupplier, singleFunction, disposer);
    }
        
    public static <T, U> Single<T> using(Supplier<U> resourceSupplier, 
            Function<? super U, ? extends Single<? extends T>> singleFunction, Consumer<? super U> disposer, boolean eager) {
        Objects.requireNonNull(resourceSupplier);
        Objects.requireNonNull(singleFunction);
        Objects.requireNonNull(disposer);
        
        return create(s -> {
            U resource;
            
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
                        set.add(() -> {
                            try {
                                disposer.accept(resource);
                            } catch (Throwable e) {
                                RxJavaPlugins.onError(e);
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
                            e.addSuppressed(ex);
                        }
                    }
                    s.onError(e);
                    if (!eager) {
                        try {
                            disposer.accept(resource);
                        } catch (Throwable ex) {
                            e.addSuppressed(ex);
                            RxJavaPlugins.onError(e);
                        }
                    }
                }
                
            });
        });
    }

    public static <T, R> Observable<R> zip(Iterable<? extends Single<? extends T>> sources, Function<? super Object[], ? extends R> zipper) {
        Iterable<? extends Observable<T>> it = () -> {
            Iterator<? extends Single<? extends T>> sit = sources.iterator();
            return new Iterator<Observable<T>>() {

                @Override
                public boolean hasNext() {
                    return sit.hasNext();
                }

                @SuppressWarnings("unchecked")
                @Override
                public Observable<T> next() {
                    return ((Observable<T>)sit.next().toFlowable());
                }
                
            };
        };
        return Observable.zipIterable(zipper, false, 1, it);
    }

    public static <T1, T2, R> Observable<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            BiFunction<? super T1, ? super T2, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        return zipArray(toFunction(zipper), s1, s2);
    }

    public static <T1, T2, T3, R> Observable<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3,
            Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        return zipArray(zipper, s1, s2, s3);
    }

    public static <T1, T2, T3, T4, R> Observable<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3, Single<? extends T4> s4,
            Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        return zipArray(zipper, s1, s2, s3, s4);
    }

    public static <T1, T2, T3, T4, T5, R> Observable<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3, Single<? extends T4> s4,
            Single<? extends T5> s5,
            Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        return zipArray(zipper, s1, s2, s3, s4, s5);
    }

    public static <T1, T2, T3, T4, T5, T6, R> Observable<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3, Single<? extends T4> s4,
            Single<? extends T5> s5, Single<? extends T6> s6,
            Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        Objects.requireNonNull(s6);
        return zipArray(zipper, s1, s2, s3, s4, s5, s6);
    }

    public static <T1, T2, T3, T4, T5, T6, T7, R> Observable<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3, Single<? extends T4> s4,
            Single<? extends T5> s5, Single<? extends T6> s6,
            Single<? extends T7> s7, 
            Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        Objects.requireNonNull(s6);
        Objects.requireNonNull(s7);
        return zipArray(zipper, s1, s2, s3, s4, s5, s6, s7);
    }
    
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Observable<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3, Single<? extends T4> s4,
            Single<? extends T5> s5, Single<? extends T6> s6,
            Single<? extends T7> s7, Single<? extends T8> s8,
            Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        Objects.requireNonNull(s6);
        Objects.requireNonNull(s7);
        Objects.requireNonNull(s8);
        return zipArray(zipper, s1, s2, s3, s4, s5, s6, s7, s8);
    }
    
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Observable<R> zip(
            Single<? extends T1> s1, Single<? extends T2> s2,
            Single<? extends T3> s3, Single<? extends T4> s4,
            Single<? extends T5> s5, Single<? extends T6> s6,
            Single<? extends T7> s7, Single<? extends T8> s8,
            Single<? extends T9> s9,
            Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1);
        Objects.requireNonNull(s2);
        Objects.requireNonNull(s3);
        Objects.requireNonNull(s4);
        Objects.requireNonNull(s5);
        Objects.requireNonNull(s6);
        Objects.requireNonNull(s7);
        Objects.requireNonNull(s8);
        Objects.requireNonNull(s9);
        return zipArray(zipper, s1, s2, s3, s4, s5, s6, s7, s8, s9);
    }

    @SafeVarargs
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T, R> Observable<R> zipArray(Function<? super Object[], ? extends R> zipper, Single<? extends T>... sources) {
        Publisher[] sourcePublishers = new Publisher[sources.length];
        int i = 0;
        for (Single<? extends T> s : sources) {
            sourcePublishers[i] = s.toFlowable();
            i++;
        }
        return Observable.zipArray(zipper, false, 1, sourcePublishers);
    }

    protected final SingleOnSubscribe<T> onSubscribe;

    protected Single(SingleOnSubscribe<T> onSubscribe) {
        this.onSubscribe = onSubscribe;
    }

    public final Single<T> ambWith(Single<? extends T> other) {
        return amb(this, other);
    }
    
    public final Single<T> asSingle() {
        return create(s -> subscribe(s));
    }
    
    public final <R> Single<R> compose(Function<? super Single<T>, ? extends Single<R>> convert) {
        return to(convert);
    }

    public final Single<T> cache() {
        AtomicInteger wip = new AtomicInteger();
        AtomicReference<Object> notification = new AtomicReference<>();
        List<SingleSubscriber<? super T>> subscribers = new ArrayList<>();
        
        return create(s -> {
            Object o = notification.get();
            if (o != null) {
                s.onSubscribe(EmptyDisposable.INSTANCE);
                if (NotificationLite.isError(o)) {
                    s.onError(NotificationLite.getError(o));
                } else {
                    s.onSuccess(NotificationLite.getValue(o));
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
                    s.onSuccess(NotificationLite.getValue(o));
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
                        list = new ArrayList<>(subscribers);
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
                        list = new ArrayList<>(subscribers);
                        subscribers.clear();
                    }
                    for (SingleSubscriber<? super T> s1 : list) {
                        s1.onError(e);
                    }
                }
                
            });
        });
    }
    
    public final <U> Single<U> cast(Class<? extends U> clazz) {
        return create(s -> {
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
        });
    }
    
    public final Observable<T> concatWith(Single<? extends T> other) {
        return concat(this, other);
    }
    
    public final Single<T> delay(long time, TimeUnit unit) {
        return delay(time, unit, Schedulers.computation());
    }
    
    public final Single<T> delay(long time, TimeUnit unit, Scheduler scheduler) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(scheduler);
        return create(s -> {
            MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
            s.onSubscribe(mad);
            subscribe(new SingleSubscriber<T>() {
                @Override
                public void onSubscribe(Disposable d) {
                    mad.set(d);
                }

                @Override
                public void onSuccess(T value) {
                    mad.set(scheduler.scheduleDirect(() -> {
                        s.onSuccess(value);
                    }, time, unit));
                }

                @Override
                public void onError(Throwable e) {
                    s.onError(e);
                }
                
            });
        });
    }
    
    public final Single<T> doOnSubscribe(Consumer<? super Disposable> onSubscribe) {
        return create(s -> {
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
        });
    }
    
    public final Single<T> doOnSuccess(Consumer<? super T> onSuccess) {
        return create(s -> {
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
        });
    }
    
    public final Single<T> doOnError(Consumer<? super Throwable> onError) {
        return create(s -> {
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
                        e.addSuppressed(ex);
                    }
                    s.onError(e);
                }
                
            });
        });
    }
    
    public final Single<T> doOnCancel(Runnable onCancel) {
        return create(s -> {
            subscribe(new SingleSubscriber<T>() {
                @Override
                public void onSubscribe(Disposable d) {
                    CompositeDisposable set = new CompositeDisposable();
                    set.add(onCancel::run);
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
        });
    }

    public final <R> Single<R> flatMap(Function<? super T, ? extends Single<? extends R>> mapper) {
        return lift(new SingleOperatorFlatMap<>(mapper));   
    }

    public final <R> Observable<R> flatMapPublisher(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return toFlowable().flatMap(mapper);
    }
    
    public final T get() {
        AtomicReference<T> valueRef = new AtomicReference<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        CountDownLatch cdl = new CountDownLatch(1);
        
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
    
    public final <R> Single<R> lift(SingleOperator<? extends R, ? super T> onLift) {
        Objects.requireNonNull(onLift);
        return create(s -> {
            SingleSubscriber<? super T> sr = onLift.apply(s);
            // TODO plugin wrapper
            onSubscribe.accept(sr);
        });
    }
    
    public final <R> Single<R> map(Function<? super T, ? extends R> mapper) {
        return lift(new SingleOperatorMap<>(mapper));
    }

    public final Single<Boolean> contains(Object value) {
        return contains(value, Objects::equals);
    }

    public final Single<Boolean> contains(Object value, BiPredicate<Object, Object> comparer) {
        return create(s -> {
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
        });
    }
    
    public final Observable<T> mergeWith(Single<? extends T> other) {
        return merge(this, other);
    }
    
    public final Single<Single<T>> nest() {
        return just(this);
    }
    
    public final Single<T> observeOn(Scheduler scheduler) {
        return create(s -> {
            CompositeDisposable mad = new CompositeDisposable();
            s.onSubscribe(mad);
            
            subscribe(new SingleSubscriber<T>() {

                @Override
                public void onError(Throwable e) {
                    mad.add(scheduler.scheduleDirect(() -> s.onError(e)));
                }

                @Override
                public void onSubscribe(Disposable d) {
                    mad.add(d);
                }

                @Override
                public void onSuccess(T value) {
                    mad.add(scheduler.scheduleDirect(() -> s.onSuccess(value)));
                }
                
            });
        });
    }

    public final Single<T> onErrorReturn(Supplier<? extends T> valueSupplier) {
        return create(s -> {
            subscribe(new SingleSubscriber<T>() {

                @Override
                public void onError(Throwable e) {
                    T v;
                    
                    try {
                        v = valueSupplier.get();
                    } catch (Throwable ex) {
                        e.addSuppressed(ex);
                        s.onError(e);
                        return;
                    }
                    
                    if (v == null) {
                        e.addSuppressed(new NullPointerException("Value supplied was null"));
                        s.onError(e);
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
        });
    }
    
    public final Single<T> onErrorReturn(T value) {
        Objects.requireNonNull(value);
        return onErrorReturn(() -> value);
    }

    public final Single<T> onErrorResumeNext(Function<? super Throwable, ? extends Single<? extends T>> nextFunction) {
        return create(s -> {
            MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
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
                        e.addSuppressed(ex);
                        s.onError(e);
                        return;
                    }
                    
                    if (next == null) {
                        s.onError(new NullPointerException("The next Single supplied was null"));
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
        });
    }
    
    public final Observable<T> repeat() {
        return toFlowable().repeat();
    }
    
    public final Observable<T> repeat(long times) {
        return toFlowable().repeat(times);
    }
    
    public final Observable<T> repeatWhen(Function<? super Observable<Void>, ? extends Publisher<?>> handler) {
        return toFlowable().repeatWhen(handler);
    }
    
    public final Observable<T> repeatUntil(BooleanSupplier stop) {
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
    
    public final Single<T> retryWhen(Function<? super Observable<? extends Throwable>, ? extends Publisher<?>> handler) {
        return toFlowable().retryWhen(handler).toSingle();
    }
    
    public final void safeSubscribe(Subscriber<? super T> s) {
        toFlowable().safeSubscribe(s);
    }
    
    public final Disposable subscribe() {
        return subscribe(v -> { }, RxJavaPlugins::onError);
    }
    
    public final Disposable subscribe(BiConsumer<? super T, ? super Throwable> onCallback) {
        Objects.requireNonNull(onCallback);
        
        MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        
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
        return subscribe(onSuccess, RxJavaPlugins::onError);
    }
    
    public final Disposable subscribe(Consumer<? super T> onSuccess, Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);
        
        MultipleAssignmentDisposable mad = new MultipleAssignmentDisposable();
        
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
        Objects.requireNonNull(subscriber);
        onSubscribe.accept(subscriber);
    }
    
    public final void subscribe(Subscriber<? super T> s) {
        toFlowable().subscribe(s);
    }
    
    public final Single<T> subscribeOn(Scheduler scheduler) {
        Objects.requireNonNull(scheduler);
        return create(s -> {
            scheduler.scheduleDirect(() -> {
                subscribe(s);
            });
        });
    }
    
    public final Single<T> timeout(long timeout, TimeUnit unit) {
        Objects.requireNonNull(unit);
        return timeout0(timeout, unit, Schedulers.computation(), null);
    }
    
    public final Single<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(scheduler);
        return timeout0(timeout, unit, scheduler, null);
    }

    public final Single<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler, Single<? extends T> other) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(scheduler);
        Objects.requireNonNull(other);
        return timeout0(timeout, unit, scheduler, other);
    }

    public final Single<T> timeout(long timeout, TimeUnit unit, Single<? extends T> other) {
        Objects.requireNonNull(unit);
        Objects.requireNonNull(other);
        return timeout0(timeout, unit, Schedulers.computation(), other);
    }

    private Single<T> timeout0(long timeout, TimeUnit unit, Scheduler scheduler, Single<? extends T> other) {
        return create(s -> {
            CompositeDisposable set = new CompositeDisposable();
            s.onSubscribe(set);
            
            AtomicBoolean once = new AtomicBoolean();
            
            Disposable timer = scheduler.scheduleDirect(() -> {
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
        });
    }

    public final <R> R to(Function<? super Single<T>, R> convert) {
        return convert.apply(this);
    }
    
    public final Observable<T> toFlowable() {
        return Observable.create(s -> {
            ScalarAsyncSubscription<T> sas = new ScalarAsyncSubscription<>(s);
            AsyncSubscription as = new AsyncSubscription();
            as.setSubscription(sas);
            s.onSubscribe(as);
            
            subscribe(new SingleSubscriber<T>() {
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
        });
    }
    
    public final void unsafeSubscribe(Subscriber<? super T> s) {
        toFlowable().unsafeSubscribe(s);
    }
    
    public final <U, R> Observable<R> zipWith(Single<U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip(this, other, zipper);
    }
}
