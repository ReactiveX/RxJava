package org.rx.functions;

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
        String className = "org.rx.lang." + name.toLowerCase() + "." + name + "Adaptor";
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

    public static void registerLanguageAdaptor(Class<?> functionClass, FunctionLanguageAdaptor adaptor) {
        languageAdaptors.put(functionClass, adaptor);
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
    @SuppressWarnings("unchecked")
    public static <R> R execute(Object function, Object... args) {
        // if we have a tracer then log the start
        long startTime = -1;
        if (tracer != null && tracer.isTraceEnabled()) {
            try {
                startTime = System.nanoTime();
                tracer.traceStart(function, args);
            } catch (Exception e) {
                logger.warn("Failed to trace log.", e);
            }
        }
        // perform controller logic to determine what type of function we received and execute it
        try {
            if (function == null) {
                throw new RuntimeException("function is null. Can't send arguments to null function.");
            }

            /*
             * TODO the following code needs to be evaluated for performance
             * 
             * The c.isInstance and keySet() functions may be areas of concern with as often as this will be executed
             */

            // check for language adaptor
            for (@SuppressWarnings("rawtypes")
            Class c : languageAdaptors.keySet()) {
                if (c.isInstance(function)) {
                    // found the language adaptor so execute
                    return (R) languageAdaptors.get(c).call(function, args);
                }
            }
            // no language adaptor found

            // check Func* classes 
            if (function instanceof Func0) {
                Func0<R> f = (Func0<R>) function;
                if (args.length != 0) {
                    throw new RuntimeException("The closure was Func0 and expected no arguments, but we received: " + args.length);
                }
                return (R) f.call();
            } else if (function instanceof Func1) {
                Func1<R, Object> f = (Func1<R, Object>) function;
                if (args.length != 1) {
                    throw new RuntimeException("The closure was Func1 and expected 1 argument, but we received: " + args.length);
                }
                return f.call(args[0]);
            } else if (function instanceof Func2) {
                Func2<R, Object, Object> f = (Func2<R, Object, Object>) function;
                if (args.length != 2) {
                    throw new RuntimeException("The closure was Func2 and expected 2 arguments, but we received: " + args.length);
                }
                return f.call(args[0], args[1]);
            } else if (function instanceof Func3) {
                Func3<R, Object, Object, Object> f = (Func3<R, Object, Object, Object>) function;
                if (args.length != 3) {
                    throw new RuntimeException("The closure was Func3 and expected 3 arguments, but we received: " + args.length);
                }
                return (R) f.call(args[0], args[1], args[2]);
            } else if (function instanceof Func4) {
                Func4<R, Object, Object, Object, Object> f = (Func4<R, Object, Object, Object, Object>) function;
                if (args.length != 1) {
                    throw new RuntimeException("The closure was Func4 and expected 4 arguments, but we received: " + args.length);
                }
                return f.call(args[0], args[1], args[2], args[3]);
            } else if (function instanceof FuncN) {
                FuncN<R> f = (FuncN<R>) function;
                return f.call(args);
            }

            // no support found
            throw new RuntimeException("Unsupported closure type: " + function.getClass().getSimpleName());
        } finally {
            // if we have a tracer then log the end
            if (tracer != null && tracer.isTraceEnabled()) {
                try {
                    tracer.traceEnd(startTime, System.nanoTime(), function, args);
                } catch (Exception e) {
                    logger.warn("Failed to trace log.", e);
                }
            }
        }
    }

    public static <R, T0> FuncN<R> fromFunc(final Func1<R, T0> f) {
        return new FuncN<R>() {

            /**
             * If it can't cast to this it should throw an exception as that means code is using this wrong.
             * <p>
             * We unfortunately need FuncN to be Object and this is a bridge between typed and non-typed hence this being unchecked
             */
            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length == 0) {
                    return f.call(null);
                } else {
                    return f.call((T0) args[0]);
                }
            }

        };
    }

    public static <R, T0, T1> FuncN<R> fromFunc(final Func2<R, T0, T1> f) {
        return new FuncN<R>() {

            /**
             * If it can't cast to this it should throw an exception as that means code is using this wrong.
             * <p>
             * We unfortunately need FuncN to be Object and this is a bridge between typed and non-typed hence this being unchecked
             */
            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length < 2) {
                    throw new RuntimeException("Func2 expecting 2 arguments.");
                }
                return f.call((T0) args[0], (T1) args[1]);
            }

        };
    }

    public static <R, T0, T1, T2> FuncN<R> fromFunc(final Func3<R, T0, T1, T2> f) {
        return new FuncN<R>() {

            /**
             * If it can't cast to this it should throw an exception as that means code is using this wrong.
             * <p>
             * We unfortunately need FuncN to be Object and this is a bridge between typed and non-typed hence this being unchecked
             */
            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length < 3) {
                    throw new RuntimeException("Func3 expecting 3 arguments.");
                }
                return f.call((T0) args[0], (T1) args[1], (T2) args[2]);
            }

        };
    }

    public static <R, T0, T1, T2, T3> FuncN<R> fromFunc(final Func4<R, T0, T1, T2, T3> f) {
        return new FuncN<R>() {

            /**
             * If it can't cast to this it should throw an exception as that means code is using this wrong.
             * <p>
             * We unfortunately need FuncN to be Object and this is a bridge between typed and non-typed hence this being unchecked
             */
            @SuppressWarnings("unchecked")
            @Override
            public R call(Object... args) {
                if (args.length < 4) {
                    throw new RuntimeException("Func4 expecting 4 arguments.");
                }
                return f.call((T0) args[0], (T1) args[1], (T2) args[2], (T3) args[3]);
            }

        };
    }

    private static volatile FunctionTraceLogger tracer = null;

    public static interface FunctionTraceLogger {
        public boolean isTraceEnabled();

        public void traceStart(Object closure, Object... args);

        /**
         * 
         * @param start
         *            nanoTime
         * @param end
         *            nanoTime
         * @param closure
         * @param args
         */
        public void traceEnd(long start, long end, Object closure, Object... args);
    }

    public static void registerTraceLogger(FunctionTraceLogger tracer) {
        Functions.tracer = tracer;
    }

}
