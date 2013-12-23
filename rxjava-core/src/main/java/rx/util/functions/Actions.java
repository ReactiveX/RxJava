/**
 * Copyright 2013 Netflix, Inc.
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
package rx.util.functions;

import rx.Observer;

/**
 * Utility class for the Action interfaces.
 */
public final class Actions {
    private Actions() { throw new IllegalStateException("No instances!"); }
    /**
     * Extracts a method reference to the observer's onNext method
     * in the form of an Action1.
     * <p>Java 8: observer::onNext</p>
     * @param observer the observer to use
     * @return an action which calls the observer's onNext method.
     */
    public static <T> Action1<T> onNextFrom(final Observer<T> observer) {
        return new Action1<T>() {
            @Override
            public void call(T t1) {
                observer.onNext(t1);
            }
        };
    }
    /**
     * Extracts a method reference to the observer's onError method
     * in the form of an Action1.
     * <p>Java 8: observer::onError</p>
     * @param observer the observer to use
     * @return an action which calls the observer's onError method.
     */
    public static <T> Action1<Throwable> onErrorFrom(final Observer<T> observer) {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable t1) {
                observer.onError(t1);
            }
        };
    }
    /**
     * Extracts a method reference to the observer's onCompleted method
     * in the form of an Action0.
     * <p>Java 8: observer::onCompleted</p>
     * @param observer the observer to use
     * @return an action which calls the observer's onCompleted method.
     */
    public static <T> Action0 onCompletedFrom(final Observer<T> observer) {
        return new Action0() {
            @Override
            public void call() {
                observer.onCompleted();
            }
        };
    }
    /**
     * Return an action which takes a Throwable and does nothing.
     * <p>(To avoid casting from the generic empty1().)
     * @return the action
     */
    public static Action1<Throwable> emptyThrowable() {
        return EMPTY_THROWABLE;
    }
    /**
     * An action that takes a Throwable and does nothing.
     */
    private static final Action1<Throwable> EMPTY_THROWABLE = new EmptyThrowable();
    /** An empty throwable class. */
    private static final class EmptyThrowable implements Action1<Throwable> {
        public void call(Throwable t1) {
        }
    }
    /**
     * Return an Action0 instance which does nothing.
     * @return an Action0 instance which does nothing
     */
    public static Action0 empty() {
        return EMPTY;
    }
    /** A single empty instance. */
    private static final Action0 EMPTY = new EmptyAction();
    /** An empty action class. */
    private static final class EmptyAction implements Action0 {
        @Override
        public void call() {
        }
    }
    
    /**
     * Converts a runnable instance into an Action0 instance.
     * @param run the Runnable to run when the Action0 is called
     * @return the Action0 wrapping the Runnable
     */
    public static Action0 fromRunnable(Runnable run) {
        if (run == null) {
            throw new NullPointerException("run");
        }
        return new ActionWrappingRunnable(run);
    }
    /** An Action0 which wraps and calls a Runnable. */
    private static final class ActionWrappingRunnable implements Action0 {
        final Runnable run;

        public ActionWrappingRunnable(Runnable run) {
            this.run = run;
        }

        @Override
        public void call() {
            run.run();
        }
        
    }
    /**
     * Converts an Action0 instance into a Runnable instance.
     * @param action the Action0 to call when the Runnable is run
     * @return the Runnable wrapping the Action0
     */
    public static Runnable toRunnable(Action0 action) {
        if (action == null) {
            throw new NullPointerException("action");
        }
        return new RunnableAction(action);
    }
    /** An Action0 which wraps and calls a Runnable. */
    private static final class RunnableAction implements Runnable {
        final Action0 action;

        public RunnableAction(Action0 action) {
            this.action = action;
        }

        @Override
        public void run() {
            action.call();
        }
        
    }
    /**
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @return {@link Func0}
     */
    public static Func0<Void> toFunc(final Action0 action) {
        return toFunc(action, (Void)null);
    }
    /**
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @return {@link Func0}
     */
    public static <T1> Func1<T1, Void> toFunc(final Action1<T1> action) {
        return toFunc(action, (Void)null);
    }
    /**
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @return {@link Func0}
     */
    public static <T1, T2> Func2<T1, T2, Void> toFunc(final Action2<T1, T2> action) {
        return toFunc(action, (Void)null);
    }
    /**
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @return {@link Func0}
     */
    public static <T1, T2, T3> Func3<T1, T2, T3, Void> toFunc(final Action3<T1, T2, T3> action) {
        return toFunc(action, (Void)null);
    }
    /**
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @return {@link Func0}
     */
    public static <T1, T2, T3, T4> Func4<T1, T2, T3, T4, Void> toFunc(final Action4<T1, T2, T3, T4> action) {
        return toFunc(action, (Void)null);
    }
    /**
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @return {@link Func0}
     */
    public static <T1, T2, T3, T4, T5> Func5<T1, T2, T3, T4, T5, Void> toFunc(
            final Action5<T1, T2, T3, T4, T5> action) {
        return toFunc(action, (Void)null);
    }
    /**
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @return {@link Func0}
     */
    public static <T1, T2, T3, T4, T5, T6> Func6<T1, T2, T3, T4, T5, T6, Void> toFunc(
            final Action6<T1, T2, T3, T4, T5, T6> action) {
        return toFunc(action, (Void)null);
    }
    /**
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @return {@link Func0}
     */
    public static <T1, T2, T3, T4, T5, T6, T7> Func7<T1, T2, T3, T4, T5, T6, T7, Void> toFunc(
            final Action7<T1, T2, T3, T4, T5, T6, T7> action) {
        return toFunc(action, (Void)null);
    }
    /**
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @return {@link Func0}
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8> Func8<T1, T2, T3, T4, T5, T6, T7, T8, Void> toFunc(
            final Action8<T1, T2, T3, T4, T5, T6, T7, T8> action) {
        return toFunc(action, (Void)null);
    }
    /**
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @return {@link Func0}
     */
    public static <T1, T2, T3, T4, T5, T6, T7, T8, T9> Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, Void> toFunc(
            final Action9<T1, T2, T3, T4, T5, T6, T7, T8, T9> action) {
        return toFunc(action, (Void)null);
    }
    /**
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @return {@link Func0}
     */
    public static FuncN<Void> toFunc(
            final ActionN action) {
        return toFunc(action, (Void)null);
    }
    /**
     * Convert an action to a function which calls
     * the action returns the given result.
     * @param action
     * @param result
     * @return {@link Func0}
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
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @param result
     * @return {@link Func0}
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
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @param result
     * @return {@link Func0}
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
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @param result
     * @return {@link Func0}
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
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @param result
     * @return {@link Func0}
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
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @param result
     * @return {@link Func0}
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
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @param result
     * @return {@link Func0}
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
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @param result
     * @return {@link Func0}
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
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @param result
     * @return {@link Func0}
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
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @param result
     * @return {@link Func0}
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
     * Convert an action to a function which calls
     * the action returns Void (null).
     * @param action
     * @param result
     * @return {@link Func0}
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
}
