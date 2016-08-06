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

import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.Functions;
import io.reactivex.internal.functions.Objects;
import io.reactivex.internal.operators.single.*;
import io.reactivex.internal.subscribers.single.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * Represents a deferred computation and emission of a single value or exception.
 * 
 * @param <T> the value type
 */
public abstract class Single<T> implements SingleConsumable<T> {

    static final Single<Object> NEVER = new SingleNever<Object>();
    
    public interface SingleOperator<Downstream, Upstream> extends Function<SingleSubscriber<? super Downstream>, SingleSubscriber<? super Upstream>> {
        
    }
    
    public interface SingleTransformer<Upstream, Downstream> extends Function<Single<Upstream>, SingleConsumable<Downstream>> {
        
    }
    
    static <T> Single<T> wrap(SingleConsumable<T> source) {
        Objects.requireNonNull(source, "source is null");
        if (source instanceof Single) {
            return (Single<T>)source;
        }
        return new SingleWrapper<T>(source);
    }
    
    public static <T> Single<T> amb(final Iterable<? extends SingleConsumable<? extends T>> sources) {
        Objects.requireNonNull(sources, "sources is null");
        return new SingleAmbIterable<T>(sources);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Single<T> amb(final SingleConsumable<? extends T>... sources) {
        if (sources.length == 0) {
            return error(new Callable<Throwable>() {
                @Override
                public Throwable call() {
                    return new NoSuchElementException();
                }
            });
        }
        if (sources.length == 1) {
            return wrap((SingleConsumable<T>)sources[0]);
        }
        return new SingleAmbArray<T>(sources);
    }

    public static <T> Flowable<T> concat(Iterable<? extends SingleConsumable<? extends T>> sources) {
        return concat(Flowable.fromIterable(sources));
    }
    
    public static <T> Flowable<T> concat(Flowable<? extends SingleConsumable<? extends T>> sources) { // FIXME Publisher
        return sources.concatMap(new Function<SingleConsumable<? extends T>, Publisher<? extends T>>() {
            @Override 
            public Publisher<? extends T> apply(SingleConsumable<? extends T> v){
                return new SingleToFlowable<T>(v);
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        return concat(Flowable.fromArray(s1, s2));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        return concat(Flowable.fromArray(s1, s2, s3));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3, SingleConsumable<? extends T> s4
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3, SingleConsumable<? extends T> s4,
            SingleConsumable<? extends T> s5
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
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3, SingleConsumable<? extends T> s4,
            SingleConsumable<? extends T> s5, SingleConsumable<? extends T> s6
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
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3, SingleConsumable<? extends T> s4,
            SingleConsumable<? extends T> s5, SingleConsumable<? extends T> s6,
            SingleConsumable<? extends T> s7
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
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3, SingleConsumable<? extends T> s4,
            SingleConsumable<? extends T> s5, SingleConsumable<? extends T> s6,
            SingleConsumable<? extends T> s7, SingleConsumable<? extends T> s8
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
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3, SingleConsumable<? extends T> s4,
            SingleConsumable<? extends T> s5, SingleConsumable<? extends T> s6,
            SingleConsumable<? extends T> s7, SingleConsumable<? extends T> s8,
            SingleConsumable<? extends T> s9
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
    
    public static <T> Single<T> create(SingleConsumable<T> onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        // TODO plugin wrapper
        return new SingleWrapper<T>(onSubscribe);
    }
    
    public static <T> Single<T> defer(final Callable<? extends SingleConsumable<? extends T>> singleSupplier) {
        Objects.requireNonNull(singleSupplier, "singleSupplier is null");
        return new SingleDefer<T>(singleSupplier);
    }
    
    public static <T> Single<T> error(final Callable<? extends Throwable> errorSupplier) {
        Objects.requireNonNull(errorSupplier, "errorSupplier is null");
        return new SingleError<T>(errorSupplier);
    }
    
    public static <T> Single<T> error(final Throwable error) {
        Objects.requireNonNull(error, "error is null");
        return error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return error;
            }
        });
    }
    
    public static <T> Single<T> fromCallable(final Callable<? extends T> callable) {
        Objects.requireNonNull(callable, "callable is null");
        return new SingleFromCallable<T>(callable);
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
        return new SingleFromPublisher<T>(publisher);
    }

    public static <T> Single<T> just(final T value) {
        Objects.requireNonNull(value, "value is null");
        return new SingleJust<T>(value);
    }

    public static <T> Flowable<T> merge(Iterable<? extends SingleConsumable<? extends T>> sources) {
        return merge(Flowable.fromIterable(sources));
    }

    public static <T> Flowable<T> merge(Flowable<? extends SingleConsumable<? extends T>> sources) { // FIXME Publisher
        return sources.flatMap(new Function<SingleConsumable<? extends T>, Publisher<? extends T>>() {
            @Override 
            public Publisher<? extends T> apply(SingleConsumable<? extends T> v){
                return new SingleToFlowable<T>(v);
            }
        });
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Single<T> merge(SingleConsumable<? extends SingleConsumable<? extends T>> source) {
        Objects.requireNonNull(source, "source is null");
        return new SingleFlatMap<SingleConsumable<? extends T>, T>(source, (Function)Functions.identity());
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        return merge(Flowable.fromArray(s1, s2));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        return merge(Flowable.fromArray(s1, s2, s3));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3, SingleConsumable<? extends T> s4
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        Objects.requireNonNull(s4, "s4 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3, SingleConsumable<? extends T> s4,
            SingleConsumable<? extends T> s5
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
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3, SingleConsumable<? extends T> s4,
            SingleConsumable<? extends T> s5, SingleConsumable<? extends T> s6
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
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3, SingleConsumable<? extends T> s4,
            SingleConsumable<? extends T> s5, SingleConsumable<? extends T> s6,
            SingleConsumable<? extends T> s7
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
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3, SingleConsumable<? extends T> s4,
            SingleConsumable<? extends T> s5, SingleConsumable<? extends T> s6,
            SingleConsumable<? extends T> s7, SingleConsumable<? extends T> s8
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
            SingleConsumable<? extends T> s1, SingleConsumable<? extends T> s2,
            SingleConsumable<? extends T> s3, SingleConsumable<? extends T> s4,
            SingleConsumable<? extends T> s5, SingleConsumable<? extends T> s6,
            SingleConsumable<? extends T> s7, SingleConsumable<? extends T> s8,
            SingleConsumable<? extends T> s9
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
        return new SingleTimer(delay, unit, scheduler);
    }
    
    public static <T> Single<Boolean> equals(final SingleConsumable<? extends T> first, final SingleConsumable<? extends T> second) { // NOPMD
        Objects.requireNonNull(first, "first is null");
        Objects.requireNonNull(second, "second is null");
        return new SingleEquals<T>(first, second);
    }

    public static <T, U> Single<T> using(Callable<U> resourceSupplier, 
            Function<? super U, ? extends SingleConsumable<? extends T>> singleFunction, Consumer<? super U> disposer) {
        return using(resourceSupplier, singleFunction, disposer, true);
    }
        
    public static <T, U> Single<T> using(
            final Callable<U> resourceSupplier, 
            final Function<? super U, ? extends SingleConsumable<? extends T>> singleFunction, 
            final Consumer<? super U> disposer, 
            final boolean eager) {
        Objects.requireNonNull(resourceSupplier, "resourceSupplier is null");
        Objects.requireNonNull(singleFunction, "singleFunction is null");
        Objects.requireNonNull(disposer, "disposer is null");
        
        return new SingleUsing<T, U>(resourceSupplier, singleFunction, disposer, eager);
    }

    public static <T, R> Single<R> zip(final Iterable<? extends SingleConsumable<? extends T>> sources, Function<? super Object[], ? extends R> zipper) {
        Objects.requireNonNull(sources, "sources is null");
        
        Iterable<? extends Flowable<T>> it = new Iterable<Flowable<T>>() {
            @Override
            public Iterator<Flowable<T>> iterator() {
                final Iterator<? extends SingleConsumable<? extends T>> sit = sources.iterator();
                return new Iterator<Flowable<T>>() {

                    @Override
                    public boolean hasNext() {
                        return sit.hasNext();
                    }

                    @Override
                    public Flowable<T> next() {
                        return new SingleToFlowable<T>(sit.next());
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
            SingleConsumable<? extends T1> s1, SingleConsumable<? extends T2> s2,
            BiFunction<? super T1, ? super T2, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, R> Single<R> zip(
            SingleConsumable<? extends T1> s1, SingleConsumable<? extends T2> s2,
            SingleConsumable<? extends T3> s3,
            Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper
     ) {
        Objects.requireNonNull(s1, "s1 is null");
        Objects.requireNonNull(s2, "s2 is null");
        Objects.requireNonNull(s3, "s3 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, R> Single<R> zip(
            SingleConsumable<? extends T1> s1, SingleConsumable<? extends T2> s2,
            SingleConsumable<? extends T3> s3, SingleConsumable<? extends T4> s4,
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
            SingleConsumable<? extends T1> s1, SingleConsumable<? extends T2> s2,
            SingleConsumable<? extends T3> s3, SingleConsumable<? extends T4> s4,
            SingleConsumable<? extends T5> s5,
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
            SingleConsumable<? extends T1> s1, SingleConsumable<? extends T2> s2,
            SingleConsumable<? extends T3> s3, SingleConsumable<? extends T4> s4,
            SingleConsumable<? extends T5> s5, SingleConsumable<? extends T6> s6,
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
            SingleConsumable<? extends T1> s1, SingleConsumable<? extends T2> s2,
            SingleConsumable<? extends T3> s3, SingleConsumable<? extends T4> s4,
            SingleConsumable<? extends T5> s5, SingleConsumable<? extends T6> s6,
            SingleConsumable<? extends T7> s7, 
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
            SingleConsumable<? extends T1> s1, SingleConsumable<? extends T2> s2,
            SingleConsumable<? extends T3> s3, SingleConsumable<? extends T4> s4,
            SingleConsumable<? extends T5> s5, SingleConsumable<? extends T6> s6,
            SingleConsumable<? extends T7> s7, SingleConsumable<? extends T8> s8,
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
            SingleConsumable<? extends T1> s1, SingleConsumable<? extends T2> s2,
            SingleConsumable<? extends T3> s3, SingleConsumable<? extends T4> s4,
            SingleConsumable<? extends T5> s5, SingleConsumable<? extends T6> s6,
            SingleConsumable<? extends T7> s7, SingleConsumable<? extends T8> s8,
            SingleConsumable<? extends T9> s9,
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
    public static <T, R> Single<R> zipArray(Function<? super Object[], ? extends R> zipper, SingleConsumable<? extends T>... sources) {
        Objects.requireNonNull(sources, "sources is null");
        Publisher[] sourcePublishers = new Publisher[sources.length];
        int i = 0;
        for (SingleConsumable<? extends T> s : sources) {
            Objects.requireNonNull(s, "The " + i + "th source is null");
            sourcePublishers[i] = new SingleToFlowable<T>(s);
            i++;
        }
        return Flowable.zipArray(zipper, false, 1, sourcePublishers).toSingle();
    }

    @SuppressWarnings("unchecked")
    public final Single<T> ambWith(SingleConsumable<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return amb(this, other);
    }
    
    public final Single<T> asSingle() {
        return new SingleHide<T>(this);
    }
    
    public final <R> Single<R> compose(Function<? super Single<T>, ? extends SingleConsumable<R>> convert) {
        return wrap(to(convert));
    }

    public final Single<T> cache() {
        return new SingleCache<T>(this);
    }
    
    public final <U> Single<U> cast(final Class<? extends U> clazz) {
        Objects.requireNonNull(clazz, "clazz is null");
        return map(new Function<T, U>() {
            @Override
            public U apply(T v) {
                return clazz.cast(v);
            }
        });
    }
    
    public final Flowable<T> concatWith(SingleConsumable<? extends T> other) {
        return concat(this, other);
    }
    
    public final Single<T> delay(long time, TimeUnit unit) {
        return delay(time, unit, Schedulers.computation());
    }
    
    public final Single<T> delay(final long time, final TimeUnit unit, final Scheduler scheduler) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return new SingleDelay<T>(this, time, unit, scheduler);
    }

    public final Single<T> delaySubscription(CompletableConsumable other) {
        return new SingleDelayWithCompletable<T>(this, other);
    }

    public final <U> Single<T> delaySubscription(SingleConsumable<U> other) {
        return new SingleDelayWithSingle<T, U>(this, other);
    }

    public final <U> Single<T> delaySubscription(ObservableConsumable<U> other) {
        return new SingleDelayWithObservable<T, U>(this, other);
    }

    public final <U> Single<T> delaySubscription(Publisher<U> other) {
        return new SingleDelayWithPublisher<T, U>(this, other);
    }
    
    public final <U> Single<T> delaySubscription(long time, TimeUnit unit) {
        return delaySubscription(time, unit, Schedulers.computation());
    }

    public final <U> Single<T> delaySubscription(long time, TimeUnit unit, Scheduler scheduler) {
        return delaySubscription(Observable.timer(time, unit, scheduler));
    }


    public final Single<T> doOnSubscribe(final Consumer<? super Disposable> onSubscribe) {
        Objects.requireNonNull(onSubscribe, "onSubscribe is null");
        return new SingleDoOnSubscribe<T>(this, onSubscribe);
    }
    
    public final Single<T> doOnSuccess(final Consumer<? super T> onSuccess) {
        Objects.requireNonNull(onSuccess, "onSuccess is null");
        return new SingleDoOnSuccess<T>(this, onSuccess);
    }
    
    public final Single<T> doOnError(final Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onError, "onError is null");
        return new SingleDoOnError<T>(this, onError);
    }
    
    public final Single<T> doOnCancel(final Runnable onCancel) {
        Objects.requireNonNull(onCancel, "onCancel is null");
        return new SingleDoOnCancel<T>(this, onCancel);
    }

    public final <R> Single<R> flatMap(Function<? super T, ? extends SingleConsumable<? extends R>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return new SingleFlatMap<T, R>(this, mapper);
    }

    public final <R> Flowable<R> flatMapPublisher(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return toFlowable().flatMap(mapper);
    }
    
    public final T get() {
        return SingleAwait.get(this);
    }
    
    public final <R> Single<R> lift(final SingleOperator<? extends R, ? super T> onLift) {
        Objects.requireNonNull(onLift, "onLift is null");
        return new SingleLift<T, R>(this, onLift);
    }
    
    public final <R> Single<R> map(Function<? super T, ? extends R> mapper) {
        return new SingleMap<T, R>(this, mapper);
    }

    public final Single<Boolean> contains(Object value) {
        return contains(value, Objects.equalsPredicate());
    }

    public final Single<Boolean> contains(final Object value, final BiPredicate<Object, Object> comparer) {
        Objects.requireNonNull(value, "value is null");
        Objects.requireNonNull(comparer, "comparer is null");
        return new SingleContains<T>(this, value, comparer);
    }
    
    public final Flowable<T> mergeWith(SingleConsumable<? extends T> other) {
        return merge(this, other);
    }
    
    public final Single<Single<T>> nest() {
        return just(this);
    }
    
    public final Single<T> observeOn(final Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return new SingleObserveOn<T>(this, scheduler);
    }

    public final Single<T> onErrorReturn(final Callable<? extends T> valueSupplier) {
        Objects.requireNonNull(valueSupplier, "valueSupplier is null");
        return new SingleOnErrorReturn<T>(this, valueSupplier, null);
    }
    
    public final Single<T> onErrorReturn(final T value) {
        Objects.requireNonNull(value, "value is null");
        return new SingleOnErrorReturn<T>(this, null, value);
    }

    public final Single<T> onErrorResumeNext(
            final Function<? super Throwable, ? extends SingleConsumable<? extends T>> nextFunction) {
        Objects.requireNonNull(nextFunction, "nextFunction is null");
        return new SingleResumeNext<T>(this, nextFunction);
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
        
        BiConsumerSingleSubscriber<T> s = new BiConsumerSingleSubscriber<T>(onCallback);
        subscribe(s);
        return s;
    }
    
    public final Disposable subscribe(Consumer<? super T> onSuccess) {
        return subscribe(onSuccess, RxJavaPlugins.errorConsumer());
    }
    
    public final Disposable subscribe(final Consumer<? super T> onSuccess, final Consumer<? super Throwable> onError) {
        Objects.requireNonNull(onSuccess, "onSuccess is null");
        Objects.requireNonNull(onError, "onError is null");
        
        ConsumerSingleSubscriber<T> s = new ConsumerSingleSubscriber<T>(onSuccess, onError);
        subscribe(s);
        return s;
    }
    
    @Override
    public final void subscribe(SingleSubscriber<? super T> subscriber) {
        Objects.requireNonNull(subscriber, "subscriber is null");
        // TODO plugin wrapper
        subscribeActual(subscriber);
    }
    
    protected abstract void subscribeActual(SingleSubscriber<? super T> subscriber);
    
    public final void subscribe(Subscriber<? super T> s) {
        toFlowable().subscribe(s);
    }
    
    public final void subscribe(Observer<? super T> s) {
        toObservable().subscribe(s);
    }
    
    public final Single<T> subscribeOn(final Scheduler scheduler) {
        Objects.requireNonNull(scheduler, "scheduler is null");
        return new SingleSubscribeOn<T>(this, scheduler);
    }
    
    public final Single<T> timeout(long timeout, TimeUnit unit) {
        return timeout0(timeout, unit, Schedulers.computation(), null);
    }
    
    public final Single<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        return timeout0(timeout, unit, scheduler, null);
    }

    public final Single<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler, SingleConsumable<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(timeout, unit, scheduler, other);
    }

    public final Single<T> timeout(long timeout, TimeUnit unit, SingleConsumable<? extends T> other) {
        Objects.requireNonNull(other, "other is null");
        return timeout0(timeout, unit, Schedulers.computation(), other);
    }

    private Single<T> timeout0(final long timeout, final TimeUnit unit, final Scheduler scheduler, final SingleConsumable<? extends T> other) {
        Objects.requireNonNull(unit, "unit is null");
        Objects.requireNonNull(scheduler, "scheduler is null");
        return new SingleTimeout<T>(this, timeout, unit, scheduler, other);
    }

    public final <R> R to(Function<? super Single<T>, R> convert) {
        try {
            return convert.apply(this);
        } catch (Throwable ex) {
            throw Exceptions.propagate(ex);
        }
    }
    
    public final Flowable<T> toFlowable() {
        return new SingleToFlowable<T>(this);
    }
    
    public final Observable<T> toObservable() {
        return new SingleToObservable<T>(this);
    }
    
    public final void unsafeSubscribe(Subscriber<? super T> s) {
        toFlowable().unsafeSubscribe(s);
    }
    
    public final <U, R> Single<R> zipWith(SingleConsumable<U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip(this, other, zipper);
    }
}