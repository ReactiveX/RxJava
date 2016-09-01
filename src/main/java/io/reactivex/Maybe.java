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

import java.util.concurrent.*;

import org.reactivestreams.*;

import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.*;
import io.reactivex.internal.functions.*;
import io.reactivex.internal.operators.maybe.*;
import io.reactivex.internal.subscribers.maybe.*;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;

/**
 * Represents a deferred computation and emission of a maybe value or exception.
 * 
 * @param <T> the value type
 */
public abstract class Maybe<T> implements MaybeSource<T> {
    static <T> Maybe<T> wrap(MaybeSource<T> source) {
        ObjectHelper.requireNonNull(source, "source is null");
        if (source instanceof Maybe) {
            return (Maybe<T>)source;
        }
        return new MaybeFromUnsafeSource<T>(source);
    }
    
    public static <T> Maybe<T> amb(final Iterable<? extends MaybeSource<? extends T>> sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return new MaybeAmbIterable<T>(sources);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> amb(final MaybeSource<? extends T>... sources) {
        if (sources.length == 0) {
            return Maybe.complete();
        }
        if (sources.length == 1) {
            return wrap((MaybeSource<T>)sources[0]);
        }
        return new MaybeAmbArray<T>(sources);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> complete() {
        return (Maybe<T>) MaybeComplete.INSTANCE;
    }

    public static <T> Flowable<T> concat(Iterable<? extends MaybeSource<? extends T>> sources) {
        return concat(Flowable.fromIterable(sources));
    }
    
    public static <T> Flowable<T> concat(Flowable<? extends MaybeSource<? extends T>> sources) { // FIXME Publisher
        return sources.concatMap(new Function<MaybeSource<? extends T>, Publisher<? extends T>>() {
            @Override 
            public Publisher<? extends T> apply(MaybeSource<? extends T> v){
                return new MaybeToFlowable<T>(v);
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        return concat(Flowable.fromArray(s1, s2));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        return concat(Flowable.fromArray(s1, s2, s3));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3, MaybeSource<? extends T> s4
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3, MaybeSource<? extends T> s4,
            MaybeSource<? extends T> s5
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4, s5));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3, MaybeSource<? extends T> s4,
            MaybeSource<? extends T> s5, MaybeSource<? extends T> s6
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        ObjectHelper.requireNonNull(s6, "s6 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4, s5, s6));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3, MaybeSource<? extends T> s4,
            MaybeSource<? extends T> s5, MaybeSource<? extends T> s6,
            MaybeSource<? extends T> s7
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        ObjectHelper.requireNonNull(s6, "s6 is null");
        ObjectHelper.requireNonNull(s7, "s7 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4, s5, s6, s7));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3, MaybeSource<? extends T> s4,
            MaybeSource<? extends T> s5, MaybeSource<? extends T> s6,
            MaybeSource<? extends T> s7, MaybeSource<? extends T> s8
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        ObjectHelper.requireNonNull(s6, "s6 is null");
        ObjectHelper.requireNonNull(s7, "s7 is null");
        ObjectHelper.requireNonNull(s8, "s8 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4, s5, s6, s7, s8));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> concat(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3, MaybeSource<? extends T> s4,
            MaybeSource<? extends T> s5, MaybeSource<? extends T> s6,
            MaybeSource<? extends T> s7, MaybeSource<? extends T> s8,
            MaybeSource<? extends T> s9
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        ObjectHelper.requireNonNull(s6, "s6 is null");
        ObjectHelper.requireNonNull(s7, "s7 is null");
        ObjectHelper.requireNonNull(s8, "s8 is null");
        ObjectHelper.requireNonNull(s9, "s9 is null");
        return concat(Flowable.fromArray(s1, s2, s3, s4, s5, s6, s7, s8, s9));
    }

    public static <T> Maybe<T> create(MaybeSource<T> source) {
        ObjectHelper.requireNonNull(source, "source is null");
        // TODO plugin wrapper
        return new MaybeFromSource<T>(source);
    }

    public static <T> Maybe<T> unsafeCreate(MaybeSource<T> source) {
        ObjectHelper.requireNonNull(source, "source is null");
        // TODO plugin wrapper
        return new MaybeFromUnsafeSource<T>(source);
    }
    
    public static <T> Maybe<T> defer(final Callable<? extends MaybeSource<? extends T>> maybeSupplier) {
        ObjectHelper.requireNonNull(maybeSupplier, "maybeSupplier is null");
        return new MaybeDefer<T>(maybeSupplier);
    }
    
    public static <T> Maybe<T> error(final Callable<? extends Throwable> errorSupplier) {
        ObjectHelper.requireNonNull(errorSupplier, "errorSupplier is null");
        return new MaybeError<T>(errorSupplier);
    }
    
    public static <T> Maybe<T> error(final Throwable error) {
        ObjectHelper.requireNonNull(error, "error is null");
        return error(new Callable<Throwable>() {
            @Override
            public Throwable call() {
                return error;
            }
        });
    }
    
    public static <T> Maybe<T> fromCallable(final Callable<? extends T> callable) {
        ObjectHelper.requireNonNull(callable, "callable is null");
        return new MaybeFromCallable<T>(callable);
    }
    
    public static <T> Maybe<T> fromFuture(Future<? extends T> future) {
        return Flowable.<T>fromFuture(future).toMaybe();
    }

    public static <T> Maybe<T> fromFuture(Future<? extends T> future, long timeout, TimeUnit unit) {
        return Flowable.<T>fromFuture(future, timeout, unit).toMaybe();
    }

    public static <T> Maybe<T> fromFuture(Future<? extends T> future, long timeout, TimeUnit unit, Scheduler scheduler) {
        return Flowable.<T>fromFuture(future, timeout, unit, scheduler).toMaybe();
    }

    public static <T> Maybe<T> fromFuture(Future<? extends T> future, Scheduler scheduler) {
        return Flowable.<T>fromFuture(future, scheduler).toMaybe();
    }

    public static <T> Maybe<T> fromPublisher(final Publisher<? extends T> publisher) {
        ObjectHelper.requireNonNull(publisher, "publisher is null");
        return new MaybeFromPublisher<T>(publisher);
    }

    public static <T> Maybe<T> just(final T value) {
        ObjectHelper.requireNonNull(value, "value is null");
        return new MaybeJust<T>(value);
    }

    public static <T> Flowable<T> merge(Iterable<? extends MaybeSource<? extends T>> sources) {
        return merge(Flowable.fromIterable(sources));
    }

    public static <T> Flowable<T> merge(Flowable<? extends MaybeSource<? extends T>> sources) { // FIXME Publisher
        return sources.flatMap(new Function<MaybeSource<? extends T>, Publisher<? extends T>>() {
            @Override 
            public Publisher<? extends T> apply(MaybeSource<? extends T> v){
                return new MaybeToFlowable<T>(v);
            }
        });
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Maybe<T> merge(MaybeSource<? extends MaybeSource<? extends T>> source) {
        ObjectHelper.requireNonNull(source, "source is null");
        return new MaybeFlatMap<MaybeSource<? extends T>, T>(source, (Function)Functions.identity());
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        return merge(Flowable.fromArray(s1, s2));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        return merge(Flowable.fromArray(s1, s2, s3));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3, MaybeSource<? extends T> s4
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3, MaybeSource<? extends T> s4,
            MaybeSource<? extends T> s5
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4, s5));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3, MaybeSource<? extends T> s4,
            MaybeSource<? extends T> s5, MaybeSource<? extends T> s6
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        ObjectHelper.requireNonNull(s6, "s6 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4, s5, s6));
    }

    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3, MaybeSource<? extends T> s4,
            MaybeSource<? extends T> s5, MaybeSource<? extends T> s6,
            MaybeSource<? extends T> s7
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        ObjectHelper.requireNonNull(s6, "s6 is null");
        ObjectHelper.requireNonNull(s7, "s7 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4, s5, s6, s7));
    }

    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3, MaybeSource<? extends T> s4,
            MaybeSource<? extends T> s5, MaybeSource<? extends T> s6,
            MaybeSource<? extends T> s7, MaybeSource<? extends T> s8
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        ObjectHelper.requireNonNull(s6, "s6 is null");
        ObjectHelper.requireNonNull(s7, "s7 is null");
        ObjectHelper.requireNonNull(s8, "s8 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4, s5, s6, s7, s8));
    }

    @SuppressWarnings("unchecked")
    public static <T> Flowable<T> merge(
            MaybeSource<? extends T> s1, MaybeSource<? extends T> s2,
            MaybeSource<? extends T> s3, MaybeSource<? extends T> s4,
            MaybeSource<? extends T> s5, MaybeSource<? extends T> s6,
            MaybeSource<? extends T> s7, MaybeSource<? extends T> s8,
            MaybeSource<? extends T> s9
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        ObjectHelper.requireNonNull(s6, "s6 is null");
        ObjectHelper.requireNonNull(s7, "s7 is null");
        ObjectHelper.requireNonNull(s8, "s8 is null");
        ObjectHelper.requireNonNull(s9, "s9 is null");
        return merge(Flowable.fromArray(s1, s2, s3, s4, s5, s6, s7, s8, s9));
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Maybe<T> never() {
        return (Maybe<T>) MaybeNever.INSTANCE;
    }
    
    public static Maybe<Long> timer(long delay, TimeUnit unit) {
        return timer(delay, unit, Schedulers.computation());
    }
    
    public static Maybe<Long> timer(final long delay, final TimeUnit unit, final Scheduler scheduler) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return new MaybeTimer(delay, unit, scheduler);
    }
    
    public static <T> Maybe<Boolean> equals(final MaybeSource<? extends T> first, final MaybeSource<? extends T> second) { // NOPMD
        ObjectHelper.requireNonNull(first, "first is null");
        ObjectHelper.requireNonNull(second, "second is null");
        return new MaybeEquals<T>(first, second);
    }

    public static <T, U> Maybe<T> using(Callable<U> resourceSupplier,
                                         Function<? super U, ? extends MaybeSource<? extends T>> maybeFunction, Consumer<? super U> disposer) {
        return using(resourceSupplier, maybeFunction, disposer, true);
    }
        
    public static <T, U> Maybe<T> using(
            final Callable<U> resourceSupplier, 
            final Function<? super U, ? extends MaybeSource<? extends T>> maybeFunction,
            final Consumer<? super U> disposer, 
            final boolean eager) {
        ObjectHelper.requireNonNull(resourceSupplier, "resourceSupplier is null");
        ObjectHelper.requireNonNull(maybeFunction, "maybeFunction is null");
        ObjectHelper.requireNonNull(disposer, "disposer is null");
        
        return new MaybeUsing<T, U>(resourceSupplier, maybeFunction, disposer, eager);
    }

    public static <T, R> Maybe<R> zip(final Iterable<? extends MaybeSource<? extends T>> sources, Function<? super Object[], ? extends R> zipper) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        return Flowable.zipIterable(MaybeInternalHelper.iterableToFlowable(sources), zipper, false, 1).toMaybe();
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, R> Maybe<R> zip(
            MaybeSource<? extends T1> s1, MaybeSource<? extends T2> s2,
            BiFunction<? super T1, ? super T2, ? extends R> zipper
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, R> Maybe<R> zip(
            MaybeSource<? extends T1> s1, MaybeSource<? extends T2> s2,
            MaybeSource<? extends T3> s3,
            Function3<? super T1, ? super T2, ? super T3, ? extends R> zipper
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, R> Maybe<R> zip(
            MaybeSource<? extends T1> s1, MaybeSource<? extends T2> s2,
            MaybeSource<? extends T3> s3, MaybeSource<? extends T4> s4,
            Function4<? super T1, ? super T2, ? super T3, ? super T4, ? extends R> zipper
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3, s4);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, R> Maybe<R> zip(
            MaybeSource<? extends T1> s1, MaybeSource<? extends T2> s2,
            MaybeSource<? extends T3> s3, MaybeSource<? extends T4> s4,
            MaybeSource<? extends T5> s5,
            Function5<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> zipper
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3, s4, s5);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, R> Maybe<R> zip(
            MaybeSource<? extends T1> s1, MaybeSource<? extends T2> s2,
            MaybeSource<? extends T3> s3, MaybeSource<? extends T4> s4,
            MaybeSource<? extends T5> s5, MaybeSource<? extends T6> s6,
            Function6<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> zipper
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        ObjectHelper.requireNonNull(s6, "s6 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3, s4, s5, s6);
    }

    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, R> Maybe<R> zip(
            MaybeSource<? extends T1> s1, MaybeSource<? extends T2> s2,
            MaybeSource<? extends T3> s3, MaybeSource<? extends T4> s4,
            MaybeSource<? extends T5> s5, MaybeSource<? extends T6> s6,
            MaybeSource<? extends T7> s7,
            Function7<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> zipper
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        ObjectHelper.requireNonNull(s6, "s6 is null");
        ObjectHelper.requireNonNull(s7, "s7 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3, s4, s5, s6, s7);
    }
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Maybe<R> zip(
            MaybeSource<? extends T1> s1, MaybeSource<? extends T2> s2,
            MaybeSource<? extends T3> s3, MaybeSource<? extends T4> s4,
            MaybeSource<? extends T5> s5, MaybeSource<? extends T6> s6,
            MaybeSource<? extends T7> s7, MaybeSource<? extends T8> s8,
            Function8<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> zipper
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        ObjectHelper.requireNonNull(s6, "s6 is null");
        ObjectHelper.requireNonNull(s7, "s7 is null");
        ObjectHelper.requireNonNull(s8, "s8 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3, s4, s5, s6, s7, s8);
    }
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Maybe<R> zip(
            MaybeSource<? extends T1> s1, MaybeSource<? extends T2> s2,
            MaybeSource<? extends T3> s3, MaybeSource<? extends T4> s4,
            MaybeSource<? extends T5> s5, MaybeSource<? extends T6> s6,
            MaybeSource<? extends T7> s7, MaybeSource<? extends T8> s8,
            MaybeSource<? extends T9> s9,
            Function9<? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? super T9, ? extends R> zipper
     ) {
        ObjectHelper.requireNonNull(s1, "s1 is null");
        ObjectHelper.requireNonNull(s2, "s2 is null");
        ObjectHelper.requireNonNull(s3, "s3 is null");
        ObjectHelper.requireNonNull(s4, "s4 is null");
        ObjectHelper.requireNonNull(s5, "s5 is null");
        ObjectHelper.requireNonNull(s6, "s6 is null");
        ObjectHelper.requireNonNull(s7, "s7 is null");
        ObjectHelper.requireNonNull(s8, "s8 is null");
        ObjectHelper.requireNonNull(s9, "s9 is null");
        return zipArray(Functions.toFunction(zipper), s1, s2, s3, s4, s5, s6, s7, s8, s9);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T, R> Maybe<R> zipArray(Function<? super Object[], ? extends R> zipper, MaybeSource<? extends T>... sources) {
        ObjectHelper.requireNonNull(sources, "sources is null");
        Publisher[] sourcePublishers = new Publisher[sources.length];
        int i = 0;
        for (MaybeSource<? extends T> s : sources) {
            ObjectHelper.requireNonNull(s, "The " + i + "th source is null");
            sourcePublishers[i] = new MaybeToFlowable<T>(s);
            i++;
        }
        return Flowable.zipArray(zipper, false, 1, sourcePublishers).toMaybe();
    }

    @SuppressWarnings("unchecked")
    public final Maybe<T> ambWith(MaybeSource<? extends T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return amb(this, other);
    }
    
    public final Maybe<T> asMaybe() {
        return new MaybeHide<T>(this);
    }
    
    public final <R> Maybe<R> compose(Function<? super Maybe<T>, ? extends MaybeSource<R>> convert) {
        return wrap(to(convert));
    }

    public final Maybe<T> cache() {
        return new MaybeCache<T>(this);
    }
    
    public final <U> Maybe<U> cast(final Class<? extends U> clazz) {
        ObjectHelper.requireNonNull(clazz, "clazz is null");
        return map(new Function<T, U>() {
            @Override
            public U apply(T v) {
                return clazz.cast(v);
            }
        });
    }
    
    public final Flowable<T> concatWith(MaybeSource<? extends T> other) {
        return concat(this, other);
    }
    
    public final Maybe<T> delay(long time, TimeUnit unit) {
        return delay(time, unit, Schedulers.computation());
    }
    
    public final Maybe<T> delay(final long time, final TimeUnit unit, final Scheduler scheduler) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return new MaybeDelay<T>(this, time, unit, scheduler);
    }

    public final Maybe<T> delaySubscription(CompletableSource other) {
        return new MaybeDelayWithCompletable<T>(this, other);
    }

    public final <U> Maybe<T> delaySubscription(MaybeSource<U> other) {
        return new MaybeDelayWithMaybe<T, U>(this, other);
    }

    public final <U> Maybe<T> delaySubscription(ObservableSource<U> other) {
        return new MaybeDelayWithObservable<T, U>(this, other);
    }

    public final <U> Maybe<T> delaySubscription(Publisher<U> other) {
        return new MaybeDelayWithPublisher<T, U>(this, other);
    }
    
    public final <U> Maybe<T> delaySubscription(long time, TimeUnit unit) {
        return delaySubscription(time, unit, Schedulers.computation());
    }

    public final <U> Maybe<T> delaySubscription(long time, TimeUnit unit, Scheduler scheduler) {
        return delaySubscription(Observable.timer(time, unit, scheduler));
    }

    public final Maybe<T> doOnSubscribe(final Consumer<? super Disposable> onSubscribe) {
        ObjectHelper.requireNonNull(onSubscribe, "onSubscribe is null");
        return new MaybeDoOnSubscribe<T>(this, onSubscribe);
    }
    
    public final Maybe<T> doOnSuccess(final Consumer<? super T> onSuccess) {
        ObjectHelper.requireNonNull(onSuccess, "onSuccess is null");
        return new MaybeDoOnSuccess<T>(this, onSuccess);
    }
    
    public final Maybe<T> doOnComplete(final Action onComplete) {
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");
        return new MaybeDoOnComplete<T>(this, onComplete);
    }

    public final Maybe<T> doOnError(final Consumer<? super Throwable> onError) {
        ObjectHelper.requireNonNull(onError, "onError is null");
        return new MaybeDoOnError<T>(this, onError);
    }
    
    public final Maybe<T> doOnCancel(final Action onCancel) {
        ObjectHelper.requireNonNull(onCancel, "onCancel is null");
        return new MaybeDoOnCancel<T>(this, onCancel);
    }

    public final <R> Maybe<R> flatMap(Function<? super T, ? extends MaybeSource<? extends R>> mapper) {
        ObjectHelper.requireNonNull(mapper, "mapper is null");
        return new MaybeFlatMap<T, R>(this, mapper);
    }

    public final <R> Flowable<R> flatMapPublisher(Function<? super T, ? extends Publisher<? extends R>> mapper) {
        return toFlowable().flatMap(mapper);
    }
    
    public final T get(T valueIfComplete) {
        return MaybeAwait.get(this, valueIfComplete);
    }
    
    public final <R> Maybe<R> lift(final MaybeOperator<? extends R, ? super T> onLift) {
        ObjectHelper.requireNonNull(onLift, "onLift is null");
        return new MaybeLift<T, R>(this, onLift);
    }
    
    public final <R> Maybe<R> map(Function<? super T, ? extends R> mapper) {
        return new MaybeMap<T, R>(this, mapper);
    }

    public final Maybe<Boolean> contains(Object value) {
        return contains(value, ObjectHelper.equalsPredicate());
    }

    public final Maybe<Boolean> contains(final Object value, final BiPredicate<Object, Object> comparer) {
        ObjectHelper.requireNonNull(value, "value is null");
        ObjectHelper.requireNonNull(comparer, "comparer is null");
        return new MaybeContains<T>(this, value, comparer);
    }
    
    public final Flowable<T> mergeWith(MaybeSource<? extends T> other) {
        return merge(this, other);
    }
    
    public final Maybe<Maybe<T>> nest() {
        return just(this);
    }
    
    public final Maybe<T> observeOn(final Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return new MaybeObserveOn<T>(this, scheduler);
    }

    public final Maybe<T> onErrorReturn(final Function<? super Throwable, ? extends T> valueFunction) {
        ObjectHelper.requireNonNull(valueFunction, "valueFunction is null");
        return new MaybeOnErrorReturn<T>(this, valueFunction, null);
    }
    
    public final Maybe<T> onErrorReturn(final T value) {
        ObjectHelper.requireNonNull(value, "value is null");
        return new MaybeOnErrorReturn<T>(this, null, value);
    }

    public final Maybe<T> onErrorResumeNext(
            final Function<? super Throwable, ? extends MaybeSource<? extends T>> nextFunction) {
        ObjectHelper.requireNonNull(nextFunction, "nextFunction is null");
        return new MaybeOnErrorResumeNext<T>(this, nextFunction);
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
    
    public final Maybe<T> retry() {
        return toFlowable().retry().toMaybe();
    }
    
    public final Maybe<T> retry(long times) {
        return toFlowable().retry(times).toMaybe();
    }
    
    public final Maybe<T> retry(BiPredicate<? super Integer, ? super Throwable> predicate) {
        return toFlowable().retry(predicate).toMaybe();
    }
    
    public final Maybe<T> retry(Predicate<? super Throwable> predicate) {
        return toFlowable().retry(predicate).toMaybe();
    }
    
    public final Maybe<T> retryWhen(Function<? super Flowable<? extends Throwable>, ? extends Publisher<Object>> handler) {
        return toFlowable().retryWhen(handler).toMaybe();
    }
    
    public final void safeSubscribe(Subscriber<? super T> s) {
        toFlowable().safeSubscribe(s);
    }
    
    public final Disposable subscribe() {
        return subscribe(Functions.emptyConsumer(), Functions.EMPTY_ACTION, RxJavaPlugins.getErrorHandler());
    }
    
    public final Disposable subscribe(Consumer<? super T> onSuccess) {
        return subscribe(onSuccess, Functions.EMPTY_ACTION, RxJavaPlugins.getErrorHandler());
    }
    
    public final Disposable subscribe(Consumer<? super T> onSuccess, final Action onComplete) {
        return subscribe(onSuccess, onComplete, RxJavaPlugins.getErrorHandler());
    }
    
    public final Disposable subscribe(final Consumer<? super T> onSuccess, final Action onComplete, final Consumer<? super Throwable> onError) {
        ObjectHelper.requireNonNull(onSuccess, "onSuccess is null");
        ObjectHelper.requireNonNull(onComplete, "onComplete is null");
        ObjectHelper.requireNonNull(onError, "onError is null");
        
        ConsumerMaybeObserver<T> s = new ConsumerMaybeObserver<T>(onSuccess, onComplete, onError);
        subscribe(s);
        return s;
    }
    
    @Override
    public final void subscribe(MaybeObserver<? super T> subscriber) {
        ObjectHelper.requireNonNull(subscriber, "subscriber is null");
        // TODO plugin wrapper
        subscribeActual(subscriber);
    }
    
    protected abstract void subscribeActual(MaybeObserver<? super T> subscriber);
    
    public final void subscribe(Subscriber<? super T> s) {
        toFlowable().subscribe(s);
    }
    
    public final void subscribe(Observer<? super T> s) {
        toObservable().subscribe(s);
    }
    
    public final Maybe<T> subscribeOn(final Scheduler scheduler) {
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return new MaybeSubscribeOn<T>(this, scheduler);
    }
    
    public final Maybe<T> timeout(long timeout, TimeUnit unit) {
        return timeout0(timeout, unit, Schedulers.computation(), null);
    }
    
    public final Maybe<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler) {
        return timeout0(timeout, unit, scheduler, null);
    }

    public final Maybe<T> timeout(long timeout, TimeUnit unit, Scheduler scheduler, MaybeSource<? extends T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return timeout0(timeout, unit, scheduler, other);
    }

    public final Maybe<T> timeout(long timeout, TimeUnit unit, MaybeSource<? extends T> other) {
        ObjectHelper.requireNonNull(other, "other is null");
        return timeout0(timeout, unit, Schedulers.computation(), other);
    }

    private Maybe<T> timeout0(final long timeout, final TimeUnit unit, final Scheduler scheduler, final MaybeSource<? extends T> other) {
        ObjectHelper.requireNonNull(unit, "unit is null");
        ObjectHelper.requireNonNull(scheduler, "scheduler is null");
        return new MaybeTimeout<T>(this, timeout, unit, scheduler, other);
    }

    public final <R> R to(Function<? super Maybe<T>, R> convert) {
        try {
            return convert.apply(this);
        } catch (Throwable ex) {
            throw Exceptions.propagate(ex);
        }
    }
    
    public final Flowable<T> toFlowable() {
        return new MaybeToFlowable<T>(this);
    }
    
    public final Observable<T> toObservable() {
        return new MaybeToObservable<T>(this);
    }
    
    public final <U, R> Maybe<R> zipWith(MaybeSource<U> other, BiFunction<? super T, ? super U, ? extends R> zipper) {
        return zip(this, other, zipper);
    }
}
