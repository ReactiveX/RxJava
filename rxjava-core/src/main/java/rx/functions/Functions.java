/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.functions;

public class Functions {

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <R> FuncN<R> fromFunc(final Func0<? extends R> f) {
        return new FuncN<R>() {

            @Override
            public R call(Object... args) {
                if (args.length != 0) {
                    throw new RuntimeException("Func0 expecting 0 arguments.");
                }
                return f.call();
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <T0, R> FuncN<R> fromFunc(final Func1<? super T0, ? extends R> f) {
        return new FuncN<R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length != 1) {
                    throw new RuntimeException("Func1 expecting 1 argument.");
                }
                return f.call((T0) args[0]);
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <T0, T1, R> FuncN<R> fromFunc(final Func2<? super T0, ? super T1, ? extends R> f) {
        return new FuncN<R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length != 2) {
                    throw new RuntimeException("Func2 expecting 2 arguments.");
                }
                return f.call((T0) args[0], (T1) args[1]);
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <T0, T1, T2, R> FuncN<R> fromFunc(final Func3<? super T0, ? super T1, ? super T2, ? extends R> f) {
        return new FuncN<R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length != 3) {
                    throw new RuntimeException("Func3 expecting 3 arguments.");
                }
                return f.call((T0) args[0], (T1) args[1], (T2) args[2]);
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <T0, T1, T2, T3, R> FuncN<R> fromFunc(final Func4<? super T0, ? super T1, ? super T2, ? super T3, ? extends R> f) {
        return new FuncN<R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length != 4) {
                    throw new RuntimeException("Func4 expecting 4 arguments.");
                }
                return f.call((T0) args[0], (T1) args[1], (T2) args[2], (T3) args[3]);
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <T0, T1, T2, T3, T4, R> FuncN<R> fromFunc(final Func5<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? extends R> f) {
        return new FuncN<R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length != 5) {
                    throw new RuntimeException("Func5 expecting 5 arguments.");
                }
                return f.call((T0) args[0], (T1) args[1], (T2) args[2], (T3) args[3], (T4) args[4]);
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <T0, T1, T2, T3, T4, T5, R> FuncN<R> fromFunc(final Func6<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? extends R> f) {
        return new FuncN<R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length != 6) {
                    throw new RuntimeException("Func6 expecting 6 arguments.");
                }
                return f.call((T0) args[0], (T1) args[1], (T2) args[2], (T3) args[3], (T4) args[4], (T5) args[5]);
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <T0, T1, T2, T3, T4, T5, T6, R> FuncN<R> fromFunc(final Func7<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? extends R> f) {
        return new FuncN<R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length != 7) {
                    throw new RuntimeException("Func7 expecting 7 arguments.");
                }
                return f.call((T0) args[0], (T1) args[1], (T2) args[2], (T3) args[3], (T4) args[4], (T5) args[5], (T6) args[6]);
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <T0, T1, T2, T3, T4, T5, T6, T7, R> FuncN<R> fromFunc(final Func8<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? extends R> f) {
        return new FuncN<R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length != 8) {
                    throw new RuntimeException("Func8 expecting 8 arguments.");
                }
                return f.call((T0) args[0], (T1) args[1], (T2) args[2], (T3) args[3], (T4) args[4], (T5) args[5], (T6) args[6], (T7) args[7]);
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, R> FuncN<R> fromFunc(final Func9<? super T0, ? super T1, ? super T2, ? super T3, ? super T4, ? super T5, ? super T6, ? super T7, ? super T8, ? extends R> f) {
        return new FuncN<R>() {

            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length != 9) {
                    throw new RuntimeException("Func9 expecting 9 arguments.");
                }
                return f.call((T0) args[0], (T1) args[1], (T2) args[2], (T3) args[3], (T4) args[4], (T5) args[5], (T6) args[6], (T7) args[7], (T8) args[8]);
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static FuncN<Void> fromAction(final Action0 f) {
        return new FuncN<Void>() {

            @Override
            public Void call(Object... args) {
                if (args.length != 0) {
                    throw new RuntimeException("Action0 expecting 0 arguments.");
                }
                f.call();
                return null;
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <T0> FuncN<Void> fromAction(final Action1<? super T0> f) {
        return new FuncN<Void>() {

            @SuppressWarnings("unchecked")
            @Override
            public Void call(Object... args) {
                if (args.length != 1) {
                    throw new RuntimeException("Action1 expecting 1 argument.");
                }
                f.call((T0) args[0]);
                return null;
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <T0, T1> FuncN<Void> fromAction(final Action2<? super T0, ? super T1> f) {
        return new FuncN<Void>() {

            @SuppressWarnings("unchecked")
            @Override
            public Void call(Object... args) {
                if (args.length != 2) {
                    throw new RuntimeException("Action3 expecting 2 arguments.");
                }
                f.call((T0) args[0], (T1) args[1]);
                return null;
            }

        };
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <T0, T1, T2> FuncN<Void> fromAction(final Action3<? super T0, ? super T1, ? super T2> f) {
        return new FuncN<Void>() {

            @SuppressWarnings("unchecked")
            @Override
            public Void call(Object... args) {
                if (args.length != 3) {
                    throw new RuntimeException("Action3 expecting 3 arguments.");
                }
                f.call((T0) args[0], (T1) args[1], (T2) args[2]);
                return null;
            }

        };
    }

    /**
     * Constructs a predicate that returns true for each input that the source
     * predicate returns false for and vice versa.
     * 
     * @param predicate
     *            The source predicate to negate.
     */
    public static <T> Func1<T, Boolean> not(Func1<? super T, Boolean> predicate) {
        return new Not<T>(predicate);
    }

    public static <T> Func1<? super T, Boolean> alwaysTrue() {
        return AlwaysTrue.INSTANCE;
    }

    public static <T> Func1<? super T, Boolean> alwaysFalse() {
        return AlwaysFalse.INSTANCE;
    }

    public static <T> Func1<T, T> identity() {
        return new Func1<T, T>() {
            @Override
            public T call(T o) {
                return o;
            }
        };
    }

    private enum AlwaysTrue implements Func1<Object, Boolean> {
        INSTANCE;

        @Override
        public Boolean call(Object o) {
            return true;
        }
    }

    private enum AlwaysFalse implements Func1<Object, Boolean> {
        INSTANCE;

        @Override
        public Boolean call(Object o) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, R> NullFunction<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, R> returnNull() {
        return (NullFunction<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, R>) NULL_FUNCTION;
    }

    @SuppressWarnings("rawtypes")
    private static final NullFunction NULL_FUNCTION = new NullFunction();

    private static final class NullFunction<T0, T1, T2, T3, T4, T5, T6, T7, T8, T9, R> implements
            Func0<R>,
            Func1<T0, R>,
            Func2<T0, T1, R>,
            Func3<T0, T1, T2, R>,
            Func4<T0, T1, T2, T3, R>,
            Func5<T0, T1, T2, T3, T4, R>,
            Func6<T0, T1, T2, T3, T4, T5, R>,
            Func7<T0, T1, T2, T3, T4, T5, T6, R>,
            Func8<T0, T1, T2, T3, T4, T5, T6, T7, R>,
            Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, R>,
            FuncN<R> {
        @Override
        public R call() {
            return null;
        }

        @Override
        public R call(T0 t1) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3, T3 t4) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5, T5 t6) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5, T5 t6, T6 t7) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5, T5 t6, T6 t7, T7 t8) {
            return null;
        }

        @Override
        public R call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5, T5 t6, T6 t7, T7 t8, T8 t9) {
            return null;
        }

        @Override
        public R call(Object... args) {
            return null;
        }
    }
}
