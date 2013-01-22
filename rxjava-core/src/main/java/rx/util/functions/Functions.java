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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows execution of functions from multiple different languages.
 * <p>
 * Language support is provided via implementations of {@link FunctionLanguageAdaptor}.
 * <p>
 * This class will dynamically look for known language adaptors on the classpath at startup or new ones can be registered using {@link #registerLanguageAdaptor(Class, FunctionLanguageAdaptor)}.
 */
public class Functions {

    private static final Logger logger = LoggerFactory.getLogger(Functions.class);

    private final static ConcurrentHashMap<Class<?>, FunctionLanguageAdaptor> languageAdaptors = new ConcurrentHashMap<Class<?>, FunctionLanguageAdaptor>();

    static {
        /* optimistically look for supported languages if they are in the classpath */
        loadLanguageAdaptor("Groovy");
        loadLanguageAdaptor("JRuby");
        loadLanguageAdaptor("Clojure");
        loadLanguageAdaptor("Scala");
        // as new languages arise we can add them here but this does not prevent someone from using 'registerLanguageAdaptor' directly
    }

    private static void loadLanguageAdaptor(String name) {
        String className = "rx.lang." + name.toLowerCase() + "." + name + "Adaptor";
        try {
            Class<?> c = Class.forName(className);
            FunctionLanguageAdaptor a = (FunctionLanguageAdaptor) c.newInstance();
            registerLanguageAdaptor(a.getFunctionClass(), a);
        } catch (ClassNotFoundException e) {
            logger.info("Could not found function language adaptor: " + name + " with path: " + className);
        } catch (Exception e) {
            logger.error("Failed trying to initialize function language adaptor: " + className, e);
        }
    }

    public static void registerLanguageAdaptor(Class<?>[] functionClasses, FunctionLanguageAdaptor adaptor) {
        for (Class<?> functionClass : functionClasses) {
            if (functionClass.getPackage().getName().startsWith("java.")) {
                throw new IllegalArgumentException("FunctionLanguageAdaptor implementations can not specify java.lang.* classes.");
            }
            languageAdaptors.put(functionClass, adaptor);
        }
    }

    public static void removeLanguageAdaptor(Class<?> functionClass) {
        languageAdaptors.remove(functionClass);
    }

    public static Collection<FunctionLanguageAdaptor> getRegisteredLanguageAdaptors() {
        return languageAdaptors.values();
    }

    /**
     * Utility method for determining the type of closure/function and executing it.
     * 
     * @param function
     * @param args
     */
    @SuppressWarnings({ "rawtypes" })
    public static FuncN from(final Object function) {
        if (function == null) {
            throw new RuntimeException("function is null. Can't send arguments to null function.");
        }

        /* check for typed Rx Function implementation first */
        if (function instanceof Function) {
            return fromFunction((Function) function);
        } else {
            /* not an Rx Function so try language adaptors */

            // check for language adaptor
            for (final Class c : languageAdaptors.keySet()) {
                if (c.isInstance(function)) {
                    final FunctionLanguageAdaptor la = languageAdaptors.get(c);
                    // found the language adaptor so wrap in FuncN and return
                    return new FuncN() {

                        @Override
                        public Object call(Object... args) {
                            return la.call(function, args);
                        }

                    };
                }
            }
            // no language adaptor found
        }

        // no support found
        throw new RuntimeException("Unsupported closure type: " + function.getClass().getSimpleName());
    }

    //
    //    @SuppressWarnings("unchecked")
    //    private static <R> R executionRxFunction(Function function, Object... args) {
    //        // check Func* classes 
    //        if (function instanceof Func0) {
    //            Func0<R> f = (Func0<R>) function;
    //            if (args.length != 0) {
    //                throw new RuntimeException("The closure was Func0 and expected no arguments, but we received: " + args.length);
    //            }
    //            return (R) f.call();
    //        } else if (function instanceof Func1) {
    //            Func1<Object, R> f = (Func1<Object, R>) function;
    //            if (args.length != 1) {
    //                throw new RuntimeException("The closure was Func1 and expected 1 argument, but we received: " + args.length);
    //            }
    //            return f.call(args[0]);
    //        } else if (function instanceof Func2) {
    //            Func2<Object, Object, R> f = (Func2<Object, Object, R>) function;
    //            if (args.length != 2) {
    //                throw new RuntimeException("The closure was Func2 and expected 2 arguments, but we received: " + args.length);
    //            }
    //            return f.call(args[0], args[1]);
    //        } else if (function instanceof Func3) {
    //            Func3<Object, Object, Object, R> f = (Func3<Object, Object, Object, R>) function;
    //            if (args.length != 3) {
    //                throw new RuntimeException("The closure was Func3 and expected 3 arguments, but we received: " + args.length);
    //            }
    //            return (R) f.call(args[0], args[1], args[2]);
    //        } else if (function instanceof Func4) {
    //            Func4<Object, Object, Object, Object, R> f = (Func4<Object, Object, Object, Object, R>) function;
    //            if (args.length != 1) {
    //                throw new RuntimeException("The closure was Func4 and expected 4 arguments, but we received: " + args.length);
    //            }
    //            return f.call(args[0], args[1], args[2], args[3]);
    //        } else if (function instanceof Func5) {
    //            Func5<Object, Object, Object, Object, Object, R> f = (Func5<Object, Object, Object, Object, Object, R>) function;
    //            if (args.length != 1) {
    //                throw new RuntimeException("The closure was Func5 and expected 5 arguments, but we received: " + args.length);
    //            }
    //            return f.call(args[0], args[1], args[2], args[3], args[4]);
    //        } else if (function instanceof Func6) {
    //            Func6<Object, Object, Object, Object, Object, Object, R> f = (Func6<Object, Object, Object, Object, Object, Object, R>) function;
    //            if (args.length != 1) {
    //                throw new RuntimeException("The closure was Func6 and expected 6 arguments, but we received: " + args.length);
    //            }
    //            return f.call(args[0], args[1], args[2], args[3], args[4], args[5]);
    //        } else if (function instanceof Func7) {
    //            Func7<Object, Object, Object, Object, Object, Object, Object, R> f = (Func7<Object, Object, Object, Object, Object, Object, Object, R>) function;
    //            if (args.length != 1) {
    //                throw new RuntimeException("The closure was Func7 and expected 7 arguments, but we received: " + args.length);
    //            }
    //            return f.call(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);
    //        } else if (function instanceof Func8) {
    //            Func8<Object, Object, Object, Object, Object, Object, Object, Object, R> f = (Func8<Object, Object, Object, Object, Object, Object, Object, Object, R>) function;
    //            if (args.length != 1) {
    //                throw new RuntimeException("The closure was Func8 and expected 8 arguments, but we received: " + args.length);
    //            }
    //            return f.call(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
    //        } else if (function instanceof Func9) {
    //            Func9<Object, Object, Object, Object, Object, Object, Object, Object, Object, R> f = (Func9<Object, Object, Object, Object, Object, Object, Object, Object, Object, R>) function;
    //            if (args.length != 1) {
    //                throw new RuntimeException("The closure was Func9 and expected 9 arguments, but we received: " + args.length);
    //            }
    //            return f.call(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
    //        } else if (function instanceof FuncN) {
    //            FuncN<R> f = (FuncN<R>) function;
    //            return f.call(args);
    //        } else if (function instanceof Action0) {
    //            Action0 f = (Action0) function;
    //            if (args.length != 1) {
    //                throw new RuntimeException("The closure was Action0 and expected 0 arguments, but we received: " + args.length);
    //            }
    //            f.call();
    //            return null;
    //        } else if (function instanceof Action1) {
    //            Action1<Object> f = (Action1<Object>) function;
    //            if (args.length != 1) {
    //                throw new RuntimeException("The closure was Action1 and expected 1 argument, but we received: " + args.length);
    //            }
    //            f.call(args[0]);
    //            return null;
    //        } else if (function instanceof Action2) {
    //            Action2<Object, Object> f = (Action2<Object, Object>) function;
    //            if (args.length != 1) {
    //                throw new RuntimeException("The closure was Action2 and expected 2 argument, but we received: " + args.length);
    //            }
    //            f.call(args[0], args[1]);
    //            return null;
    //        } else if (function instanceof Action3) {
    //            Action3<Object, Object, Object> f = (Action3<Object, Object, Object>) function;
    //            if (args.length != 1) {
    //                throw new RuntimeException("The closure was Action1 and expected 1 argument, but we received: " + args.length);
    //            }
    //            f.call(args[0], args[1], args[2]);
    //            return null;
    //        }
    //
    //        throw new RuntimeException("Unknown implementation of Function: " + function.getClass().getSimpleName());
    //    }

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

}
