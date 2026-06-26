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

package io.reactivex.rxjava4.internal.functions;

import java.util.*;
import java.util.concurrent.*;

import static java.util.concurrent.Flow.*;

import io.reactivex.rxjava4.annotations.NonNull;
import io.reactivex.rxjava4.core.*;
import io.reactivex.rxjava4.exceptions.OnErrorNotImplementedException;
import io.reactivex.rxjava4.functions.*;
import io.reactivex.rxjava4.plugins.RxJavaPlugins;
import io.reactivex.rxjava4.schedulers.Timed;

/**
 * Utility methods to convert the BiFunction, Function3 .. Function9 instances to Function of Object array.
 */
public final class Functions {

    /** Utility class. */
    private Functions() {
        throw new IllegalStateException("No instances!");
    }

    @NonNull
    public static <T1, T2, R> Function<Object[], R> toFunction(@NonNull BiFunction<? super T1, ? super T2, ? extends R> f) {
        return new Array2Func<>(f);
    }

    @NonNull
    public static <T1, T2, T3, R> Function<Object[], R> toFunction(@NonNull Function3<T1, T2, T3, R> f) {
        return new Array3Func<>(f);
    }

    @NonNull
    public static <T1, T2, T3, T4, R> Function<Object[], R> toFunction(@NonNull Function4<T1, T2, T3, T4, R> f) {
        return new Array4Func<>(f);
    }

    @NonNull
    public static <T1, T2, T3, T4, T5, R> Function<Object[], R> toFunction(@NonNull Function5<T1, T2, T3, T4, T5, R> f) {
        return new Array5Func<>(f);
    }

    @NonNull
    public static <T1, T2, T3, T4, T5, T6, R> Function<Object[], R> toFunction(
            @NonNull Function6<T1, T2, T3, T4, T5, T6, R> f) {
        return new Array6Func<>(f);
    }

    @NonNull
    public static <T1, T2, T3, T4, T5, T6, T7, R> Function<Object[], R> toFunction(
            @NonNull Function7<T1, T2, T3, T4, T5, T6, T7, R> f) {
        return new Array7Func<>(f);
    }

    @NonNull
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function<Object[], R> toFunction(
            @NonNull Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> f) {
        return new Array8Func<>(f);
    }

    @NonNull
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Function<Object[], R> toFunction(
            @NonNull Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> f) {
        return new Array9Func<>(f);
    }

    /** A singleton identity function. */
    static final Function<Object, Object> IDENTITY = new Identity();

    /**
     * Returns an identity function that simply returns its argument.
     * @param <T> the input and output value type
     * @return the identity function
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> Function<T, T> identity() {
        return (Function<T, T>)IDENTITY;
    }

    public static final Runnable EMPTY_RUNNABLE = new EmptyRunnable();

    public static final Action EMPTY_ACTION = new EmptyAction();

    static final Consumer<Object> EMPTY_CONSUMER = new EmptyConsumer();

    /**
     * Returns an empty consumer that does nothing.
     * @param <T> the consumed value type, the value is ignored
     * @return an empty consumer that does nothing.
     */
    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> emptyConsumer() {
        return (Consumer<T>)EMPTY_CONSUMER;
    }

    public static final Consumer<Throwable> ERROR_CONSUMER = new ErrorConsumer();

    /**
     * Wraps the consumed Throwable into an OnErrorNotImplementedException and
     * signals it to the plugin error handler.
     */
    public static final Consumer<Throwable> ON_ERROR_MISSING = new OnErrorMissingConsumer();

    public static final LongConsumer EMPTY_LONG_CONSUMER = new EmptyLongConsumer();

    static final Predicate<Object> ALWAYS_TRUE = new TruePredicate();

    static final Predicate<Object> ALWAYS_FALSE = new FalsePredicate();

    static final Supplier<Object> NULL_SUPPLIER = new NullProvider();

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> Predicate<T> alwaysTrue() {
        return (Predicate<T>)ALWAYS_TRUE;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> Predicate<T> alwaysFalse() {
        return (Predicate<T>)ALWAYS_FALSE;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> Supplier<T> nullSupplier() {
        return (Supplier<T>)NULL_SUPPLIER;
    }

    record FutureAction(Future<?> future) implements Action {

        @Override
            public void run() throws Exception {
                future.get();
            }
        }

    /**
     * Wraps the blocking get call of the Future into an Action.
     * @param future the future to call get() on, not null
     * @return the new Action instance
     */
    @NonNull
    public static Action futureAction(@NonNull Future<?> future) {
        return new FutureAction(future);
    }

    record JustValue<T, U>(U value) implements Callable<U>, Supplier<U>, Function<T, U> {

        @Override
            public U call() {
                return value;
            }

            @Override
            public U apply(T t) {
                return value;
            }

            @Override
            public U get() {
                return value;
            }
        }

    /**
     * Returns a Callable that returns the given value.
     * @param <T> the value type
     * @param value the value to return
     * @return the new Callable instance
     */
    @NonNull
    public static <T> Callable<T> justCallable(@NonNull T value) {
        return new JustValue<>(value);
    }

    /**
     * Returns a Supplier that returns the given value.
     * @param <T> the value type
     * @param value the value to return
     * @return the new Callable instance
     */
    @NonNull
    public static <T> Supplier<T> justSupplier(@NonNull T value) {
        return new JustValue<>(value);
    }

    /**
     * Returns a Function that ignores its parameter and returns the given value.
     * @param <T> the function's input type
     * @param <U> the value and return type of the function
     * @param value the value to return
     * @return the new Function instance
     */
    @NonNull
    public static <T, U> Function<T, U> justFunction(@NonNull U value) {
        return new JustValue<>(value);
    }

    record CastToClass<T, U>(Class<U> clazz) implements Function<T, U> {

        @Override
            public U apply(T t) {
                return clazz.cast(t);
            }
        }

    /**
     * Returns a function that cast the incoming values via a Class object.
     * @param <T> the input value type
     * @param <U> the output and target type
     * @param target the target class
     * @return the new Function instance
     */
    @NonNull
    public static <T, U> Function<T, U> castFunction(@NonNull Class<U> target) {
        return new CastToClass<>(target);
    }

    record ArrayListCapacityCallable<T>(int capacity) implements Supplier<List<T>> {

        @Override
            public List<T> get() {
                return new ArrayList<>(capacity);
            }
        }

    public static <T> Supplier<List<T>> createArrayList(int capacity) {
        return new ArrayListCapacityCallable<>(capacity);
    }

    record EqualsPredicate<T>(T value) implements Predicate<T> {

        @Override
            public boolean test(T t) {
                return Objects.equals(t, value);
            }
        }

    public static <T> Predicate<T> equalsWith(T value) {
        return new EqualsPredicate<>(value);
    }

    enum HashSetSupplier implements Supplier<Set<Object>> {
        INSTANCE;
        @Override
        public Set<Object> get() {
            return new HashSet<>();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <T> Supplier<Set<T>> createHashSet() {
        return (Supplier)HashSetSupplier.INSTANCE;
    }

    record NotificationOnNext<T>(Consumer<? super Notification<T>> onNotification) implements Consumer<T> {

        @Override
            public void accept(T v) throws Throwable {
                onNotification.accept(Notification.createOnNext(v));
            }
        }

    record NotificationOnError<T>(Consumer<? super Notification<T>> onNotification) implements Consumer<Throwable> {

        @Override
            public void accept(Throwable v) throws Throwable {
                onNotification.accept(Notification.createOnError(v));
            }
        }

    record NotificationOnComplete<T>(Consumer<? super Notification<T>> onNotification) implements Action {

        @Override
            public void run() throws Throwable {
                onNotification.accept(Notification.createOnComplete());
            }
        }

    public static <T> Consumer<T> notificationOnNext(Consumer<? super Notification<T>> onNotification) {
        return new NotificationOnNext<>(onNotification);
    }

    public static <T> Consumer<Throwable> notificationOnError(Consumer<? super Notification<T>> onNotification) {
        return new NotificationOnError<>(onNotification);
    }

    public static <T> Action notificationOnComplete(Consumer<? super Notification<T>> onNotification) {
        return new NotificationOnComplete<>(onNotification);
    }

    record ActionConsumer<T>(Action action) implements Consumer<T> {

        @Override
            public void accept(T t) throws Throwable {
                action.run();
            }
        }

    public static <T> Consumer<T> actionConsumer(Action action) {
        return new ActionConsumer<>(action);
    }

    record ClassFilter<T, U>(Class<U> clazz) implements Predicate<T> {

        @Override
            public boolean test(T t) {
                return clazz.isInstance(t);
            }
        }

    public static <T, U> Predicate<T> isInstanceOf(Class<U> clazz) {
        return new ClassFilter<>(clazz);
    }

    record BooleanSupplierPredicateReverse<T>(BooleanSupplier supplier) implements Predicate<T> {

        @Override
            public boolean test(T t) throws Throwable {
                return !supplier.getAsBoolean();
            }
        }

    public static <T> Predicate<T> predicateReverseFor(BooleanSupplier supplier) {
        return new BooleanSupplierPredicateReverse<>(supplier);
    }

    record TimestampFunction<T>(TimeUnit unit, Scheduler scheduler) implements Function<T, Timed<T>> {

        @Override
            public Timed<T> apply(T t) {
                return new Timed<>(t, scheduler.now(unit), unit);
            }
        }

    public static <T> Function<T, Timed<T>> timestampWith(TimeUnit unit, Scheduler scheduler) {
        return new TimestampFunction<>(unit, scheduler);
    }

    static final class ToMapKeySelector<K, T> implements BiConsumer<Map<K, T>, T> {
        private final Function<? super T, ? extends K> keySelector;

        ToMapKeySelector(Function<? super T, ? extends K> keySelector) {
            this.keySelector = keySelector;
        }

        @Override
        public void accept(Map<K, T> m, T t) throws Throwable {
            K key = keySelector.apply(t);
            m.put(key, t);
        }
    }

    public static <T, K> BiConsumer<Map<K, T>, T> toMapKeySelector(final Function<? super T, ? extends K> keySelector) {
        return new ToMapKeySelector<>(keySelector);
    }

    static final class ToMapKeyValueSelector<K, V, T> implements BiConsumer<Map<K, V>, T> {
        private final Function<? super T, ? extends V> valueSelector;
        private final Function<? super T, ? extends K> keySelector;

        ToMapKeyValueSelector(Function<? super T, ? extends V> valueSelector,
                Function<? super T, ? extends K> keySelector) {
            this.valueSelector = valueSelector;
            this.keySelector = keySelector;
        }

        @Override
        public void accept(Map<K, V> m, T t) throws Throwable {
            K key = keySelector.apply(t);
            V value = valueSelector.apply(t);
            m.put(key, value);
        }
    }

    public static <T, K, V> BiConsumer<Map<K, V>, T> toMapKeyValueSelector(final Function<? super T, ? extends K> keySelector,
            final Function<? super T, ? extends V> valueSelector) {
        return new ToMapKeyValueSelector<>(valueSelector, keySelector);
    }

    static final class ToMultimapKeyValueSelector<K, V, T> implements BiConsumer<Map<K, Collection<V>>, T> {
        private final Function<? super K, ? extends Collection<? super V>> collectionFactory;
        private final Function<? super T, ? extends V> valueSelector;
        private final Function<? super T, ? extends K> keySelector;

        ToMultimapKeyValueSelector(Function<? super K, ? extends Collection<? super V>> collectionFactory,
                Function<? super T, ? extends V> valueSelector, Function<? super T, ? extends K> keySelector) {
            this.collectionFactory = collectionFactory;
            this.valueSelector = valueSelector;
            this.keySelector = keySelector;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void accept(Map<K, Collection<V>> m, T t) throws Throwable {
            K key = keySelector.apply(t);

            Collection<V> coll = m.get(key);
            if (coll == null) {
                coll = (Collection<V>)collectionFactory.apply(key);
                m.put(key, coll);
            }

            V value = valueSelector.apply(t);

            coll.add(value);
        }
    }

    public static <T, K, V> BiConsumer<Map<K, Collection<V>>, T> toMultimapKeyValueSelector(
            final Function<? super T, ? extends K> keySelector, final Function<? super T, ? extends V> valueSelector,
            final Function<? super K, ? extends Collection<? super V>> collectionFactory) {
        return new ToMultimapKeyValueSelector<>(collectionFactory, valueSelector, keySelector);
    }

    enum NaturalComparator implements Comparator<Object> {
        INSTANCE;

        @SuppressWarnings("unchecked")
        @Override
        public int compare(Object o1, Object o2) {
            return ((Comparable<Object>)o1).compareTo(o2);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> naturalComparator() {
        return (Comparator<T>)NaturalComparator.INSTANCE;
    }

    record ListSorter<T>(Comparator<? super T> comparator) implements Function<List<T>, List<T>> {

        @Override
            public List<T> apply(List<T> v) {
                v.sort(comparator);
                return v;
            }
        }

    public static <T> Function<List<T>, List<T>> listSorter(final Comparator<? super T> comparator) {
        return new ListSorter<>(comparator);
    }

    public static final Consumer<Subscription> REQUEST_MAX = new MaxRequestSubscription();

    record Array2Func<T1, T2, R>(BiFunction<? super T1, ? super T2, ? extends R> f) implements Function<Object[], R> {

        @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) throws Throwable {
                if (a.length != 2) {
                    throw new IllegalArgumentException("Array of size 2 expected but got " + a.length);
                }
                return f.apply((T1) a[0], (T2) a[1]);
            }
        }

    record Array3Func<T1, T2, T3, R>(Function3<T1, T2, T3, R> f) implements Function<Object[], R> {

        @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) throws Throwable {
                if (a.length != 3) {
                    throw new IllegalArgumentException("Array of size 3 expected but got " + a.length);
                }
                return f.apply((T1) a[0], (T2) a[1], (T3) a[2]);
            }
        }

    record Array4Func<T1, T2, T3, T4, R>(Function4<T1, T2, T3, T4, R> f) implements Function<Object[], R> {

        @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) throws Throwable {
                if (a.length != 4) {
                    throw new IllegalArgumentException("Array of size 4 expected but got " + a.length);
                }
                return f.apply((T1) a[0], (T2) a[1], (T3) a[2], (T4) a[3]);
            }
        }

    static final class Array5Func<T1, T2, T3, T4, T5, R> implements Function<Object[], R> {
        private final Function5<T1, T2, T3, T4, T5, R> f;

        Array5Func(Function5<T1, T2, T3, T4, T5, R> f) {
            this.f = f;
        }

        @SuppressWarnings("unchecked")
        @Override
        public R apply(Object[] a) throws Throwable {
            if (a.length != 5) {
                throw new IllegalArgumentException("Array of size 5 expected but got " + a.length);
            }
            return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4]);
        }
    }

    record Array6Func<T1, T2, T3, T4, T5, T6, R>(
            Function6<T1, T2, T3, T4, T5, T6, R> f) implements Function<Object[], R> {

        @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) throws Throwable {
                if (a.length != 6) {
                    throw new IllegalArgumentException("Array of size 6 expected but got " + a.length);
                }
                return f.apply((T1) a[0], (T2) a[1], (T3) a[2], (T4) a[3], (T5) a[4], (T6) a[5]);
            }
        }

    record Array7Func<T1, T2, T3, T4, T5, T6, T7, R>(
            Function7<T1, T2, T3, T4, T5, T6, T7, R> f) implements Function<Object[], R> {

        @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) throws Throwable {
                if (a.length != 7) {
                    throw new IllegalArgumentException("Array of size 7 expected but got " + a.length);
                }
                return f.apply((T1) a[0], (T2) a[1], (T3) a[2], (T4) a[3], (T5) a[4], (T6) a[5], (T7) a[6]);
            }
        }

    record Array8Func<T1, T2, T3, T4, T5, T6, T7, T8, R>(
            Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> f) implements Function<Object[], R> {

        @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) throws Throwable {
                if (a.length != 8) {
                    throw new IllegalArgumentException("Array of size 8 expected but got " + a.length);
                }
                return f.apply((T1) a[0], (T2) a[1], (T3) a[2], (T4) a[3], (T5) a[4], (T6) a[5], (T7) a[6], (T8) a[7]);
            }
        }

    record Array9Func<T1, T2, T3, T4, T5, T6, T7, T8, T9, R>(
            Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> f) implements Function<Object[], R> {

        @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) throws Throwable {
                if (a.length != 9) {
                    throw new IllegalArgumentException("Array of size 9 expected but got " + a.length);
                }
                return f.apply((T1) a[0], (T2) a[1], (T3) a[2], (T4) a[3], (T5) a[4], (T6) a[5], (T7) a[6], (T8) a[7], (T9) a[8]);
            }
        }

    static final class Identity implements Function<Object, Object> {
        @Override
        public Object apply(Object v) {
            return v;
        }

        @Override
        public String toString() {
            return "IdentityFunction";
        }
    }

    static final class EmptyRunnable implements Runnable {
        @Override
        public void run() { }

        @Override
        public String toString() {
            return "EmptyRunnable";
        }
    }

    static final class EmptyAction implements Action {
        @Override
        public void run() { }

        @Override
        public String toString() {
            return "EmptyAction";
        }
    }

    static final class EmptyConsumer implements Consumer<Object> {
        @Override
        public void accept(Object v) { }

        @Override
        public String toString() {
            return "EmptyConsumer";
        }
    }

    static final class ErrorConsumer implements Consumer<Throwable> {
        @Override
        public void accept(Throwable error) {
            RxJavaPlugins.onError(error);
        }
    }

    static final class OnErrorMissingConsumer implements Consumer<Throwable> {
        @Override
        public void accept(Throwable error) {
            RxJavaPlugins.onError(new OnErrorNotImplementedException(error));
        }
    }

    static final class EmptyLongConsumer implements LongConsumer {
        @Override
        public void accept(long v) { }
    }

    static final class TruePredicate implements Predicate<Object> {
        @Override
        public boolean test(Object o) {
            return true;
        }
    }

    static final class FalsePredicate implements Predicate<Object> {
        @Override
        public boolean test(Object o) {
            return false;
        }
    }

    static final class NullProvider implements Supplier<Object> {
        @Override
        public Object get() {
            return null;
        }
    }

    static final class MaxRequestSubscription implements Consumer<Subscription> {
        @Override
        public void accept(Subscription t) {
            t.request(Long.MAX_VALUE);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> boundedConsumer(int bufferSize) {
        return (Consumer<T>) new BoundedConsumer(bufferSize);
    }

    public static class BoundedConsumer implements Consumer<Subscription> {

        final int bufferSize;

        BoundedConsumer(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        @Override
        public void accept(Subscription s) {
            s.request(bufferSize);
        }
    }
}
