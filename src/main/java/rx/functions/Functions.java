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

public final class Functions {
    private Functions() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Converts a {@link Func0} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <R> the result type
     * @param f
     *          the {@code Func0} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts a {@link Func1} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <T0> the first argument type
     * @param <R> the result type
     * @param f
     *          the {@code Func1} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts a {@link Func2} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <T0> the first argument type
     * @param <T1> the second argument type
     * @param <R> the result type
     * @param f
     *          the {@code Func2} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts a {@link Func3} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <T0> the first argument type
     * @param <T1> the second argument type
     * @param <T2> the third argument type
     * @param <R> the result type
     * @param f
     *          the {@code Func3} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts a {@link Func4} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <T0> the first argument type
     * @param <T1> the second argument type
     * @param <T2> the third argument type
     * @param <T3> the fourth argument type
     * @param <R> the result type
     * @param f
     *          the {@code Func4} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts a {@link Func5} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <T0> the first argument type
     * @param <T1> the second argument type
     * @param <T2> the third argument type
     * @param <T3> the fourth argument type
     * @param <T4> the fifth argument type
     * @param <R> the result type
     * @param f
     *          the {@code Func5} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts a {@link Func6} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <T0> the first argument type
     * @param <T1> the second argument type
     * @param <T2> the third argument type
     * @param <T3> the fourth argument type
     * @param <T4> the fifth argument type
     * @param <T5> the sixth argument type
     * @param <R> the result type
     * @param f
     *          the {@code Func6} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts a {@link Func7} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <T0> the first argument type
     * @param <T1> the second argument type
     * @param <T2> the third argument type
     * @param <T3> the fourth argument type
     * @param <T4> the fifth argument type
     * @param <T5> the sixth argument type
     * @param <T6> the seventh argument type
     * @param <R> the result type
     * @param f
     *          the {@code Func7} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts a {@link Func8} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <T0> the first argument type
     * @param <T1> the second argument type
     * @param <T2> the third argument type
     * @param <T3> the fourth argument type
     * @param <T4> the fifth argument type
     * @param <T5> the sixth argument type
     * @param <T6> the seventh argument type
     * @param <T7> the eigth argument type
     * @param <R> the result type
     * @param f
     *          the {@code Func8} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts a {@link Func9} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <T0> the first argument type
     * @param <T1> the second argument type
     * @param <T2> the third argument type
     * @param <T3> the fourth argument type
     * @param <T4> the fifth argument type
     * @param <T5> the sixth argument type
     * @param <T6> the seventh argument type
     * @param <T7> the eigth argument type
     * @param <T8> the ninth argument type
     * @param <R> the result type
     * @param f
     *          the {@code Func9} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts an {@link Action0} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     *          the {@code Action0} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts an {@link Action1} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <T0> the first argument type
     * @param f
     *          the {@code Action1} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts an {@link Action2} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <T0> the first argument type
     * @param <T1> the second argument type
     * @param f
     *          the {@code Action2} to convert
     * @return a {@link FuncN} representation of {@code f}
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
     * Converts an {@link Action3} to a {@link FuncN} to allow heterogeneous handling of functions with different arities.
     * 
     * @param <T0> the first argument type
     * @param <T1> the second argument type
     * @param <T2> the third argument type
     * @param f
     *          the {@code Action3} to convert
     * @return a {@link FuncN} representation of {@code f}
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

}
