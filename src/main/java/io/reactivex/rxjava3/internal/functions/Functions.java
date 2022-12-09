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

package io.reactivex.rxjava3.internal.functions;

import java.util.*;
import java.util.concurrent.*;

import org.reactivestreams.Subscription;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Timed;

/**
 * Utility methods to convert the BiFunction, Function3..Function9 instances to Function of Object array.
 */
public final class Functions {

    /** Utility class. */
    private Functions() {
        throw new IllegalStateException("No instances!");
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T1, T2, R> Function<Object[], R> toFunction(@NonNull BiFunction<? super T1, ? super T2, ? extends R> f) {
        return a -> {
            if (a.length != 2) {
                throw new IllegalArgumentException("Array of size 2 expected but got " + a.length);
            }
            return f.apply((T1)a[0], (T2)a[1]);
        };
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T1, T2, T3, R> Function<Object[], R> toFunction(@NonNull Function3<T1, T2, T3, R> f) {
        return a -> {
            if (a.length != 3) {
                throw new IllegalArgumentException("Array of size 3 expected but got " + a.length);
            }
            return f.apply((T1)a[0], (T2)a[1], (T3)a[2]);
        };
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T1, T2, T3, T4, R> Function<Object[], R> toFunction(@NonNull Function4<T1, T2, T3, T4, R> f) {
        return a -> {
            if (a.length != 4) {
                throw new IllegalArgumentException("Array of size 4 expected but got " + a.length);
            }
            return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3]);
        };
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T1, T2, T3, T4, T5, R> Function<Object[], R> toFunction(@NonNull Function5<T1, T2, T3, T4, T5, R> f) {
        return a -> {
            if (a.length != 5) {
                throw new IllegalArgumentException("Array of size 5 expected but got " + a.length);
            }
            return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4]);
        };
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T1, T2, T3, T4, T5, T6, R> Function<Object[], R> toFunction(
            @NonNull Function6<T1, T2, T3, T4, T5, T6, R> f) {
        return a -> {
            if (a.length != 6) {
                throw new IllegalArgumentException("Array of size 6 expected but got " + a.length);
            }
            return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4], (T6)a[5]);
        };
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T1, T2, T3, T4, T5, T6, T7, R> Function<Object[], R> toFunction(
            @NonNull Function7<T1, T2, T3, T4, T5, T6, T7, R> f) {
        return a -> {
            if (a.length != 7) {
                throw new IllegalArgumentException("Array of size 7 expected but got " + a.length);
            }
            return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4], (T6)a[5], (T7)a[6]);
        };
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function<Object[], R> toFunction(
            @NonNull Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> f) {
        return a -> {
            if (a.length != 8) {
                throw new IllegalArgumentException("Array of size 8 expected but got " + a.length);
            }
            return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4], (T6)a[5], (T7)a[6], (T8)a[7]);
        };
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Function<Object[], R> toFunction(
            @NonNull Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> f) {
        return a -> {
            if (a.length != 9) {
                throw new IllegalArgumentException("Array of size 9 expected but got " + a.length);
            }
            return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4], (T6)a[5], (T7)a[6], (T8)a[7], (T9)a[8]);
        };
    }

    /**
     * Returns an identity function that simply returns its argument.
     * @param <T> the input and output value type
     * @return the identity function
     */
    @NonNull
    public static <T> Function<T, T> identity() {
        return v -> v;
    }

    public static final Runnable EMPTY_RUNNABLE = () -> {};

    public static final Action EMPTY_ACTION = () -> {};

    /**
     * Returns an empty consumer that does nothing.
     * @param <T> the consumed value type, the value is ignored
     * @return an empty consumer that does nothing.
     */
    public static <T> Consumer<T> emptyConsumer() {
        return o -> {};
    }

    public static final Consumer<Throwable> ERROR_CONSUMER = RxJavaPlugins::onError;

    /**
     * Wraps the consumed Throwable into an OnErrorNotImplementedException and
     * signals it to the plugin error handler.
     */
    public static final Consumer<Throwable> ON_ERROR_MISSING = error ->
            RxJavaPlugins.onError(new OnErrorNotImplementedException(error));

    public static final LongConsumer EMPTY_LONG_CONSUMER = v -> {};

    @NonNull
    public static <T> Predicate<T> alwaysTrue() {
        return o -> true;
    }

    @NonNull
    public static <T> Predicate<T> alwaysFalse() {
        return o -> false;
    }

    @NonNull
    public static <T> Supplier<T> nullSupplier() {
        return () -> null;
    }

    /**
     * Wraps the blocking get call of the Future into an Action.
     * @param future the future to call get() on, not null
     * @return the new Action instance
     */
    @NonNull
    public static Action futureAction(@NonNull Future<?> future) {
        return future::get;
    }

    /**
     * Returns a Callable that returns the given value.
     * @param <T> the value type
     * @param value the value to return
     * @return the new Callable instance
     */
    @NonNull
    public static <T> Callable<T> justCallable(@NonNull T value) {
        return () -> value;
    }

    /**
     * Returns a Supplier that returns the given value.
     * @param <T> the value type
     * @param value the value to return
     * @return the new Callable instance
     */
    @NonNull
    public static <T> Supplier<T> justSupplier(@NonNull T value) {
        return () -> value;
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
        return v -> value;
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
        return target::cast;
    }

    public static <T> Supplier<List<T>> createArrayList(int capacity) {
        return () -> new ArrayList<>(capacity);
    }

    public static <T> Predicate<T> equalsWith(T value) {
        return t -> Objects.equals(t, value);
    }

    public static <T> Supplier<Set<T>> createHashSet() {
        return HashSet::new;
    }

    public static <T> Consumer<T> notificationOnNext(Consumer<? super Notification<T>> onNotification) {
        return v -> onNotification.accept(Notification.createOnNext(v));
    }

    public static <T> Consumer<Throwable> notificationOnError(Consumer<? super Notification<T>> onNotification) {
        return error -> onNotification.accept(Notification.createOnError(error));
    }

    public static <T> Action notificationOnComplete(Consumer<? super Notification<T>> onNotification) {
        return () -> onNotification.accept(Notification.createOnComplete());
    }

    public static <T> Consumer<T> actionConsumer(Action action) {
        return t -> action.run();
    }

    public static <T, U> Predicate<T> isInstanceOf(Class<U> clazz) {
        return clazz::isInstance;
    }

    public static <T> Predicate<T> predicateReverseFor(BooleanSupplier supplier) {
        return t -> !supplier.getAsBoolean();
    }

    public static <T> Function<T, Timed<T>> timestampWith(TimeUnit unit, Scheduler scheduler) {
        return t -> new Timed<>(t, scheduler.now(unit), unit);
    }

    public static <T, K> BiConsumer<Map<K, T>, T> toMapKeySelector(final Function<? super T, ? extends K> keySelector) {
        return (m, t) -> m.put(keySelector.apply(t), t);
    }

    public static <T, K, V> BiConsumer<Map<K, V>, T> toMapKeyValueSelector(final Function<? super T, ? extends K> keySelector, final Function<? super T, ? extends V> valueSelector) {
        return (m, t) -> m.put(keySelector.apply(t), valueSelector.apply(t));
    }

    // A version of Map.computeIfAbsent() which accepts a RxJava Function.
    @SuppressWarnings("unchecked")
    private static <K, V> Collection<V> computeIfAbsent(
            final Map<K, Collection<V>> map, final K key,
            final Function<? super K, ? extends Collection<? super V>> mappingFunction) throws Throwable {
        Collection<V> v = map.get(key);
        if (v == null) {
            Collection<V> newValue = (Collection<V>) mappingFunction.apply(key);
            map.put(key, newValue);
            return newValue;
        }

        return v;
    }

    public static <T, K, V> BiConsumer<Map<K, Collection<V>>, T> toMultimapKeyValueSelector(
            final Function<? super T, ? extends K> keySelector, final Function<? super T, ? extends V> valueSelector,
            final Function<? super K, ? extends Collection<? super V>> collectionFactory) {
        return (m, t) -> {
            final K key = keySelector.apply(t);
            final Collection<V> coll = computeIfAbsent(m, key, collectionFactory);
            coll.add(valueSelector.apply(t));
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> naturalComparator() {
        return (o1, o2) -> ((Comparable<Object>)o1).compareTo(o2);
    }

    public static <T> Function<List<T>, List<T>> listSorter(final Comparator<? super T> comparator) {
        return list -> {
            Collections.sort(list, comparator);
            return list;
        };
    }

    public static final Consumer<Subscription> REQUEST_MAX = s -> s.request(Long.MAX_VALUE);

    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> boundedConsumer(int bufferSize) {
        final Consumer<Subscription> consumer = s -> s.request(bufferSize);
        return (Consumer<T>) consumer;
    }
}
