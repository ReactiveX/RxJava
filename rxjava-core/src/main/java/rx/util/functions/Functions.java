/**
 * Copyright 2013 Netflix, Inc.
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
package rx.util.functions;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class Functions {

    /**
     * Utility method for determining the type of closure/function and executing it.
     * 
     * @param function
     */
    @SuppressWarnings({ "rawtypes" })
    public static FuncN from(final Object function) {
        if (function == null) {
            throw new RuntimeException("function is null. Can't send arguments to null function.");
        }

        /* check for typed Rx Function implementation first */
        if (function instanceof Function) {
            return fromFunction((Function) function);
        }
        // no support found
        throw new RuntimeException("Unsupported closure type: " + function.getClass().getSimpleName());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static FuncN fromFunction(Function function) {
        // check Func* classes 
        if (function instanceof Func0) {
            return fromFunc((Func0) function);
        } else if (function instanceof Func1) {
            return fromFunc((Func1) function);
        } else if (function instanceof Func2) {
            return fromFunc((Func2) function);
        } else if (function instanceof Func3) {
            return fromFunc((Func3) function);
        } else if (function instanceof Func4) {
            return fromFunc((Func4) function);
        } else if (function instanceof Func5) {
            return fromFunc((Func5) function);
        } else if (function instanceof Func6) {
            return fromFunc((Func6) function);
        } else if (function instanceof Func7) {
            return fromFunc((Func7) function);
        } else if (function instanceof Func8) {
            return fromFunc((Func8) function);
        } else if (function instanceof Func9) {
            return fromFunc((Func9) function);
        } else if (function instanceof FuncN) {
            return (FuncN) function;
        } else if (function instanceof Action0) {
            return fromAction((Action0) function);
        } else if (function instanceof Action1) {
            return fromAction((Action1) function);
        } else if (function instanceof Action2) {
            return fromAction((Action2) function);
        } else if (function instanceof Action3) {
            return fromAction((Action3) function);
        }

        throw new RuntimeException("Unknown implementation of Function: " + function.getClass().getSimpleName());
    }

    /**
     * Convert a function to FuncN to allow heterogeneous handling of functions with different arities.
     * 
     * @param f
     * @return {@link FuncN}
     */
    public static <R> FuncN<R> fromFunc(final Func0<R> f) {
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
    public static <T0, R> FuncN<R> fromFunc(final Func1<T0, R> f) {
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
    public static <T0, T1, R> FuncN<R> fromFunc(final Func2<T0, T1, R> f) {
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
    public static <T0, T1, T2, R> FuncN<R> fromFunc(final Func3<T0, T1, T2, R> f) {
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
    public static <T0, T1, T2, T3, R> FuncN<R> fromFunc(final Func4<T0, T1, T2, T3, R> f) {
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
    public static <T0, T1, T2, T3, T4, R> FuncN<R> fromFunc(final Func5<T0, T1, T2, T3, T4, R> f) {
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
    public static <T0, T1, T2, T3, T4, T5, R> FuncN<R> fromFunc(final Func6<T0, T1, T2, T3, T4, T5, R> f) {
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
    public static <T0, T1, T2, T3, T4, T5, T6, R> FuncN<R> fromFunc(final Func7<T0, T1, T2, T3, T4, T5, T6, R> f) {
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
    public static <T0, T1, T2, T3, T4, T5, T6, T7, R> FuncN<R> fromFunc(final Func8<T0, T1, T2, T3, T4, T5, T6, T7, R> f) {
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
    public static <T0, T1, T2, T3, T4, T5, T6, T7, T8, R> FuncN<R> fromFunc(final Func9<T0, T1, T2, T3, T4, T5, T6, T7, T8, R> f) {
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
    public static <T0> FuncN<Void> fromAction(final Action1<T0> f) {
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
    public static <T0, T1> FuncN<Void> fromAction(final Action2<T0, T1> f) {
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
    public static <T0, T1, T2> FuncN<Void> fromAction(final Action3<T0, T1, T2> f) {
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

    @SuppressWarnings("unchecked")
    public static <T> Func1<T, Boolean> alwaysTrue() {
        return (Func1<T, Boolean>) AlwaysTrue.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Func1<T, T> identity() {
        return (Func1<T, T>) Identity.INSTANCE;
    }

    private enum AlwaysTrue implements Func1<Object, Boolean> {
        INSTANCE;

        @Override
        public Boolean call(Object o) {
            return true;
        }
    }

    private enum Identity implements Func1<Object, Object> {
        INSTANCE;

        @Override
        public Object call(Object o) {
            return o;
        }
    }

}
