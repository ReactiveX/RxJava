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
package io.reactivex.internal.functions;

import java.util.Comparator;

import io.reactivex.functions.*;

/**
 * Utility methods to convert the Function3..Function9 instances to Function of Object array.
 */
public enum Functions {
    ;
    
    @SuppressWarnings("unchecked")
    public static <T1, T2, R> Function<Object[], R> toFunction(final BiFunction<? super T1, ? super T2, ? extends R> biFunction) {
        Objects.requireNonNull(biFunction, "biFunction is null");
        return new Function<Object[], R>() {
            @Override
            public R apply(Object[] a) {
                if (a.length != 2) {
                    throw new IllegalArgumentException("Array of size 2 expected but got " + a.length);
                }
                return ((BiFunction<Object, Object, R>)biFunction).apply(a[0], a[1]);
            }
        };
    }
    
    public static <T1, T2, T3, R> Function<Object[], R> toFunction(final Function3<T1, T2, T3, R> f) {
        Objects.requireNonNull(f, "f is null");
        return new Function<Object[], R>() {
            @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) {
                if (a.length != 3) {
                    throw new IllegalArgumentException("Array of size 3 expected but got " + a.length);
                }
                return f.apply((T1)a[0], (T2)a[1], (T3)a[2]);
            }
        };
    }
    
    public static <T1, T2, T3, T4, R> Function<Object[], R> toFunction(final Function4<T1, T2, T3, T4, R> f) {
        Objects.requireNonNull(f, "f is null");
        return new Function<Object[], R>() {
            @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) {
                if (a.length != 4) {
                    throw new IllegalArgumentException("Array of size 4 expected but got " + a.length);
                }
                return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3]);
            }
        };
    }
    
    public static <T1, T2, T3, T4, T5, R> Function<Object[], R> toFunction(final Function5<T1, T2, T3, T4, T5, R> f) {
        Objects.requireNonNull(f, "f is null");
        return new Function<Object[], R>() {
            @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) {
                if (a.length != 5) {
                    throw new IllegalArgumentException("Array of size 5 expected but got " + a.length);
                }
                return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4]);
            }
        };
    }
    
    public static <T1, T2, T3, T4, T5, T6, R> Function<Object[], R> toFunction(
            final Function6<T1, T2, T3, T4, T5, T6, R> f) {
        Objects.requireNonNull(f, "f is null");
        return new Function<Object[], R>() {
            @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) {
                if (a.length != 6) {
                    throw new IllegalArgumentException("Array of size 6 expected but got " + a.length);
                }
                return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4], (T6)a[5]);
            }
        };
    }
    
    public static <T1, T2, T3, T4, T5, T6, T7, R> Function<Object[], R> toFunction(
            final Function7<T1, T2, T3, T4, T5, T6, T7, R> f) {
        Objects.requireNonNull(f, "f is null");
        return new Function<Object[], R>() {
            @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) {
                if (a.length != 7) {
                    throw new IllegalArgumentException("Array of size 7 expected but got " + a.length);
                }
                return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4], (T6)a[5], (T7)a[6]);
            }
        };
    }
    
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Function<Object[], R> toFunction(
            final Function8<T1, T2, T3, T4, T5, T6, T7, T8, R> f) {
        Objects.requireNonNull(f, "f is null");
        return new Function<Object[], R>() {
            @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) {
                if (a.length != 8) {
                    throw new IllegalArgumentException("Array of size 8 expected but got " + a.length);
                }
                return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4], (T6)a[5], (T7)a[6], (T8)a[7]);
            }
        };
    }
    
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Function<Object[], R> toFunction(
            final Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> f) {
        Objects.requireNonNull(f, "f is null");
        return new Function<Object[], R>() {
            @SuppressWarnings("unchecked")
            @Override
            public R apply(Object[] a) {
                if (a.length != 9) {
                    throw new IllegalArgumentException("Array of size 9 expected but got " + a.length);
                }
                return f.apply((T1)a[0], (T2)a[1], (T3)a[2], (T4)a[3], (T5)a[4], (T6)a[5], (T7)a[6], (T8)a[7], (T9)a[8]);
            }
        };
    }
    
    /** A singleton identity function. */
    static final Function<Object, Object> IDENTITY = new Function<Object, Object>() {
        @Override
        public Object apply(Object v) {
            return v;
        }
    };
    
    /**
     * Returns an identity function that simply returns its argument.
     * @param <T> the input and output value type
     * @return the identity function
     */
    @SuppressWarnings("unchecked")
    public static <T> Function<T, T> identity() {
        return (Function<T, T>)IDENTITY;
    }
    
    static final Runnable EMPTY = new Runnable() {
        @Override
        public void run() { }
    };
    
    /**
     * Returns an empty runnable that does nothing.
     * @return an empty runnable that does nothing
     */
    public static Runnable emptyRunnable() {
        return EMPTY;
    }
    
    static final Consumer<Object> EMPTY_CONSUMER = new Consumer<Object>() {
        @Override
        public void accept(Object v) { }
    };
    
    /**
     * Returns an empty consumer that does nothing.
     * @param <T> the consumed value type, the value is ignored
     * @return an empty consumer that does nothing.
     */
    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> emptyConsumer() {
        return (Consumer<T>)EMPTY_CONSUMER;
    }
    
    static final LongConsumer EMPTY_LONGCONSUMER = new LongConsumer() {
        @Override
        public void accept(long v) { }
    };
    
    /**
     * Returns an empty long consumer that does nothing.
     * @return an empty long consumer that does nothing.
     */
    public static LongConsumer emptyLongConsumer() {
        return EMPTY_LONGCONSUMER;
    }
    
    static final Predicate<Object> ALWAYS_TRUE = new Predicate<Object>() {
        @Override
        public boolean test(Object o) {
            return true;
        }
    };

    static final Predicate<Object> ALWAYS_FALSE = new Predicate<Object>() {
        @Override
        public boolean test(Object o) {
            return true;
        }
    };
    
    static final Supplier<Object> NULL_SUPPLIER = new Supplier<Object>() {
        @Override
        public Object get() {
            return null;
        }
    };
    
    static final Comparator<Object> NATURAL_COMPARATOR = new Comparator<Object>() {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public int compare(Object a, Object b) {
            return ((Comparable)a).compareTo(b);
        }
    };
    
    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysTrue() {
        return (Predicate<T>)ALWAYS_TRUE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> alwaysFalse() {
        return (Predicate<T>)ALWAYS_FALSE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> nullSupplier() {
        return (Supplier<T>)NULL_SUPPLIER;
    }
    
    /**
     * Returns a natural order comparator which casts the parameters to Comparable.
     * @param <T> the value type
     * @return a natural order comparator which casts the parameters to Comparable
     */
    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> naturalOrder() {
        return (Comparator<T>)NATURAL_COMPARATOR;
    }
}
