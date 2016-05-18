/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.functions;

/**
 * Utility class for the Action interfaces.
 */
public final class Actions {
    private Actions() {
        throw new IllegalStateException("No instances!");
    }

    @SuppressWarnings("unchecked")
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8> EmptyAction<T0, T1, T2, T3, T4, T5, T6, T7, T8> empty() {
        return EMPTY_ACTION;
    }

    @SuppressWarnings("rawtypes")
    private static final EmptyAction EMPTY_ACTION = new EmptyAction();

    private static final class EmptyAction<T0, T1, T2, T3, T4, T5, T6, T7, T8> implements
            Action0,
            Action1<T0>,
            Action2<T0, T1>,
            Action3<T0, T1, T2>,
            Action4<T0, T1, T2, T3>,
            Action5<T0, T1, T2, T3, T4>,
            Action6<T0, T1, T2, T3, T4, T5>,
            Action7<T0, T1, T2, T3, T4, T5, T6>,
            Action8<T0, T1, T2, T3, T4, T5, T6, T7>,
            Action9<T0, T1, T2, T3, T4, T5, T6, T7, T8>,
            ActionN {
        EmptyAction() {
        }

        @Override
        public void call() {
        }

        @Override
        public void call(T0 t1) {
        }

        @Override
        public void call(T0 t1, T1 t2) {
        }

        @Override
        public void call(T0 t1, T1 t2, T2 t3) {
        }

        @Override
        public void call(T0 t1, T1 t2, T2 t3, T3 t4) {
        }

        @Override
        public void call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5) {
        }

        @Override
        public void call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5, T5 t6) {
        }

        @Override
        public void call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5, T5 t6, T6 t7) {
        }

        @Override
        public void call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5, T5 t6, T6 t7, T7 t8) {
        }

        @Override
        public void call(T0 t1, T1 t2, T2 t3, T3 t4, T4 t5, T5 t6, T6 t7, T7 t8, T8 t9) {
        }

        @Override
        public void call(Object... args) {
        }
    }
    
    /**
     * Converts an {@link Action0} to a function that calls the action and returns {@code null}.
     * 
     * @param action
     *            the {@link Action0} to convert
     * @return a {@link Func0} that calls {@code action} and returns {@code null}
     */
    public static Func0<Void> toFunc(final Action0 action) {
        return toFunc(action, (Void) null);
    }

    /**
     * Converts an {@link Action1} to a function that calls the action and returns {@code null}.
     * 
     * @param <T1> the first argument type
     * @param action
     *            the {@link Action1} to convert
     * @return a {@link Func1} that calls {@code action} and returns {@code null}
     */
    public static <T1> Func1<T1, Void> toFunc(final Action1<T1> action) {
        return toFunc(action, (Void) null);
    }

    /**
     * Converts an {@link Action2} to a function that calls the action and returns {@code null}.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param action
     *            the {@link Action2} to convert
     * @return a {@link Func2} that calls {@code action} and returns {@code null}
     */
    public static <T1, T2> Func2<T1, T2, Void> toFunc(final Action2<T1, T2> action) {
        return toFunc(action, (Void) null);
    }

    /**
     * Converts an {@link Action3} to a function that calls the action and returns {@code null}.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param action
     *            the {@link Action3} to convert
     * @return a {@link Func3} that calls {@code action} and returns {@code null}
     */
    public static <T1, T2, T3> Func3<T1, T2, T3, Void> toFunc(final Action3<T1, T2, T3> action) {
        return toFunc(action, (Void) null);
    }

    /**
     * Converts an {@link Action4} to a function that calls the action and returns {@code null}.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <T4> the fourth argument type
     * @param action
     *            the {@link Action4} to convert
     * @return a {@link Func4} that calls {@code action} and returns {@code null}
     */
    public static <T1, T2, T3, T4> Func4<T1, T2, T3, T4, Void> toFunc(final Action4<T1, T2, T3, T4> action) {
        return toFunc(action, (Void) null);
    }

    /**
     * Converts an {@link Action5} to a function that calls the action and returns {@code null}.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <T4> the fourth argument type
     * @param <T5> the fifth argument type
     * @param action
     *            the {@link Action5} to convert
     * @return a {@link Func5} that calls {@code action} and returns {@code null}
     */
    public static <T1, T2, T3, T4, T5> Func5<T1, T2, T3, T4, T5, Void> toFunc(
            final Action5<T1, T2, T3, T4, T5> action) {
        return toFunc(action, (Void) null);
    }

    /**
     * Converts an {@link Action6} to a function that calls the action and returns {@code null}.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <T4> the fourth argument type
     * @param <T5> the fifth argument type
     * @param <T6> the sixth argument type
     * @param action
     *            the {@link Action6} to convert
     * @return a {@link Func6} that calls {@code action} and returns {@code null}
     */
    public static <T1, T2, T3, T4, T5, T6> Func6<T1, T2, T3, T4, T5, T6, Void> toFunc(
            final Action6<T1, T2, T3, T4, T5, T6> action) {
        return toFunc(action, (Void) null);
    }

    /**
     * Converts an {@link Action7} to a function that calls the action and returns {@code null}.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <T4> the fourth argument type
     * @param <T5> the fifth argument type
     * @param <T6> the sixth argument type
     * @param <T7> the seventh argument type
     * @param action
     *            the {@link Action7} to convert
     * @return a {@link Func7} that calls {@code action} and returns {@code null}
     */
    public static <T1, T2, T3, T4, T5, T6, T7> Func7<T1, T2, T3, T4, T5, T6, T7, Void> toFunc(
            final Action7<T1, T2, T3, T4, T5, T6, T7> action) {
        return toFunc(action, (Void) null);
    }

    /**
     * Converts an {@link Action8} to a function that calls the action and returns {@code null}.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <T4> the fourth argument type
     * @param <T5> the fifth argument type
     * @param <T6> the sixth argument type
     * @param <T7> the seventh argument type
     * @param <T8> the eigth argument type
     * @param action
     *            the {@link Action8} to convert
     * @return a {@link Func8} that calls {@code action} and returns {@code null}
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Func8<T1, T2, T3, T4, T5, T6, T7, T8, Void> toFunc(
            final Action8<T1, T2, T3, T4, T5, T6, T7, T8> action) {
        return toFunc(action, (Void) null);
    }

    /**
     * Converts an {@link Action9} to a function that calls the action and returns {@code null}.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <T4> the fourth argument type
     * @param <T5> the fifth argument type
     * @param <T6> the sixth argument type
     * @param <T7> the seventh argument type
     * @param <T8> the eigth argument type
     * @param <T9> the ninth argument type
     * @param action
     *            the {@link Action9} to convert
     * @return a {@link Func9} that calls {@code action} and returns {@code null}
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Void> toFunc(
            final Action9<T1, T2, T3, T4, T5, T6, T7, T8, T9> action) {
        return toFunc(action, (Void) null);
    }

    /**
     * Converts an {@link ActionN} to a function that calls the action and returns {@code null}.
     * 
     * @param action
     *            the {@link ActionN} to convert
     * @return a {@link FuncN} that calls {@code action} and returns {@code null}
     */
    public static FuncN<Void> toFunc(
            final ActionN action) {
        return toFunc(action, (Void) null);
    }

    /**
     * Converts an {@link Action0} to a function that calls the action and returns a specified value.
     * 
     * @param <R> the result type
     * @param action
     *            the {@link Action0} to convert
     * @param result
     *            the value to return from the function call
     * @return a {@link Func0} that calls {@code action} and returns {@code result}
     */
    public static <R> Func0<R> toFunc(final Action0 action, final R result) {
        return new Func0<R>() {
            @Override
            public R call() {
                action.call();
                return result;
            }
        };
    }

    /**
     * Converts an {@link Action1} to a function that calls the action and returns a specified value.
     * 
     * @param <T1> the first argument type
     * @param <R> the result type
     * @param action
     *            the {@link Action1} to convert
     * @param result
     *            the value to return from the function call
     * @return a {@link Func1} that calls {@code action} and returns {@code result}
     */
    public static <T1, R> Func1<T1, R> toFunc(final Action1<T1> action, final R result) {
        return new Func1<T1, R>() {
            @Override
            public R call(T1 t1) {
                action.call(t1);
                return result;
            }
        };
    }

    /**
     * Converts an {@link Action2} to a function that calls the action and returns a specified value.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <R> the result type
     * @param action
     *            the {@link Action2} to convert
     * @param result
     *            the value to return from the function call
     * @return a {@link Func2} that calls {@code action} and returns {@code result}
     */
    public static <T1, T2, R> Func2<T1, T2, R> toFunc(final Action2<T1, T2> action, final R result) {
        return new Func2<T1, T2, R>() {
            @Override
            public R call(T1 t1, T2 t2) {
                action.call(t1, t2);
                return result;
            }
        };
    }

    /**
     * Converts an {@link Action3} to a function that calls the action and returns a specified value.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <R> the result type
     * @param action
     *            the {@link Action3} to convert
     * @param result
     *            the value to return from the function call
     * @return a {@link Func3} that calls {@code action} and returns {@code result}
     */
    public static <T1, T2, T3, R> Func3<T1, T2, T3, R> toFunc(final Action3<T1, T2, T3> action, final R result) {
        return new Func3<T1, T2, T3, R>() {
            @Override
            public R call(T1 t1, T2 t2, T3 t3) {
                action.call(t1, t2, t3);
                return result;
            }
        };
    }

    /**
     * Converts an {@link Action4} to a function that calls the action and returns a specified value.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <T4> the fourth argument type
     * @param <R> the result type
     * @param action
     *            the {@link Action4} to convert
     * @param result
     *            the value to return from the function call
     * @return a {@link Func4} that calls {@code action} and returns {@code result}
     */
    public static <T1, T2, T3, T4, R> Func4<T1, T2, T3, T4, R> toFunc(final Action4<T1, T2, T3, T4> action, final R result) {
        return new Func4<T1, T2, T3, T4, R>() {
            @Override
            public R call(T1 t1, T2 t2, T3 t3, T4 t4) {
                action.call(t1, t2, t3, t4);
                return result;
            }
        };
    }

    /**
     * Converts an {@link Action5} to a function that calls the action and returns a specified value.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <T4> the fourth argument type
     * @param <T5> the fifth argument type
     * @param <R> the result type
     * @param action
     *            the {@link Action5} to convert
     * @param result
     *            the value to return from the function call
     * @return a {@link Func5} that calls {@code action} and returns {@code result}
     */
    public static <T1, T2, T3, T4, T5, R> Func5<T1, T2, T3, T4, T5, R> toFunc(
            final Action5<T1, T2, T3, T4, T5> action, final R result) {
        return new Func5<T1, T2, T3, T4, T5, R>() {
            @Override
            public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
                action.call(t1, t2, t3, t4, t5);
                return result;
            }
        };
    }

    /**
     * Converts an {@link Action6} to a function that calls the action and returns a specified value.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <T4> the fourth argument type
     * @param <T5> the fifth argument type
     * @param <T6> the sixth argument type
     * @param <R> the result type
     * @param action
     *            the {@link Action6} to convert
     * @param result
     *            the value to return from the function call
     * @return a {@link Func6} that calls {@code action} and returns {@code result}
     */
    public static <T1, T2, T3, T4, T5, T6, R> Func6<T1, T2, T3, T4, T5, T6, R> toFunc(
            final Action6<T1, T2, T3, T4, T5, T6> action, final R result) {
        return new Func6<T1, T2, T3, T4, T5, T6, R>() {
            @Override
            public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
                action.call(t1, t2, t3, t4, t5, t6);
                return result;
            }
        };
    }

    /**
     * Converts an {@link Action7} to a function that calls the action and returns a specified value.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <T4> the fourth argument type
     * @param <T5> the fifth argument type
     * @param <T6> the sixth argument type
     * @param <T7> the seventh argument type
     * @param <R> the result type
     * @param action
     *            the {@link Action7} to convert
     * @param result
     *            the value to return from the function call
     * @return a {@link Func7} that calls {@code action} and returns {@code result}
     */
    public static <T1, T2, T3, T4, T5, T6, T7, R> Func7<T1, T2, T3, T4, T5, T6, T7, R> toFunc(
            final Action7<T1, T2, T3, T4, T5, T6, T7> action, final R result) {
        return new Func7<T1, T2, T3, T4, T5, T6, T7, R>() {
            @Override
            public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
                action.call(t1, t2, t3, t4, t5, t6, t7);
                return result;
            }
        };
    }

    /**
     * Converts an {@link Action8} to a function that calls the action and returns a specified value.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <T4> the fourth argument type
     * @param <T5> the fifth argument type
     * @param <T6> the sixth argument type
     * @param <T7> the seventh argument type
     * @param <T8> the eigth argument type
     * @param <R> the result type
     * @param action
     *            the {@link Action8} to convert
     * @param result
     *            the value to return from the function call
     * @return a {@link Func8} that calls {@code action} and returns {@code result}
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, R> Func8<T1, T2, T3, T4, T5, T6, T7, T8, R> toFunc(
            final Action8<T1, T2, T3, T4, T5, T6, T7, T8> action, final R result) {
        return new Func8<T1, T2, T3, T4, T5, T6, T7, T8, R>() {
            @Override
            public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
                action.call(t1, t2, t3, t4, t5, t6, t7, t8);
                return result;
            }
        };
    }

    /**
     * Converts an {@link Action9} to a function that calls the action and returns a specified value.
     * 
     * @param <T1> the first argument type
     * @param <T2> the second argument type
     * @param <T3> the third argument type
     * @param <T4> the fourth argument type
     * @param <T5> the fifth argument type
     * @param <T6> the sixth argument type
     * @param <T7> the seventh argument type
     * @param <T8> the eigth argument type
     * @param <T9> the ninth argument type
     * @param <R> the result type
     * @param action
     *            the {@link Action9} to convert
     * @param result
     *            the value to return from the function call
     * @return a {@link Func9} that calls {@code action} and returns {@code result}
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> toFunc(
            final Action9<T1, T2, T3, T4, T5, T6, T7, T8, T9> action, final R result) {
        return new Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R>() {
            @Override
            public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
                action.call(t1, t2, t3, t4, t5, t6, t7, t8, t9);
                return result;
            }
        };
    }

    /**
     * Converts an {@link ActionN} to a function that calls the action and returns a specified value.
     * 
     * @param <R> the result type
     * @param action
     *            the {@link ActionN} to convert
     * @param result
     *            the value to return from the function call
     * @return a {@link FuncN} that calls {@code action} and returns {@code result}
     */
    public static <R> FuncN<R> toFunc(
            final ActionN action, final R result) {
        return new FuncN<R>() {
            @Override
            public R call(Object... args) {
                action.call(args);
                return result;
            }
        };
    }
    
    /**
     * Wraps an Action0 instance into an Action1 instance where the latter calls
     * the former.
     * @param <T> the first argument type
     * @param action the action to call
     * @return the new Action1 instance
     */
    public static <T> Action1<T> toAction1(Action0 action) {
        return new Action1CallsAction0<T>(action);
    }
    
    static final class Action1CallsAction0<T> implements Action1<T> {
        final Action0 action;
        
        public Action1CallsAction0(Action0 action) {
            this.action = action;
        }
        
        @Override
        public void call(T t) {
            action.call();
        }
    }
}
