package org.rx.functions;

import groovy.lang.Closure;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Functions {

    private static final Logger logger = LoggerFactory.getLogger(Functions.class);

    /**
     * Utility method for determining the type of closure/function and executing it.
     * 
     * @param closure
     * @param args
     */
    @SuppressWarnings("unchecked")
    public static <R> R execute(Object closure, Object... args) {
        // if we have a tracer then log the start
        long startTime = -1;
        if (tracer != null && tracer.isTraceEnabled()) {
            try {
                startTime = System.nanoTime();
                tracer.traceStart(closure, args);
            } catch (Exception e) {
                logger.warn("Failed to trace log.", e);
            }
        }
        // perform controller logic to determine what type of function we received and execute it
        try {
            if (closure == null) {
                throw new RuntimeException("closure is null. Can't send arguments to null closure.");
            }
            if (closure instanceof Closure) {
                /* handle Groovy */
                return (R) ((Closure<?>) closure).call(args);
            } else if (closure instanceof RubyProc) {
                // handle JRuby
                RubyProc rubyProc = ((RubyProc) closure);
                Ruby ruby = rubyProc.getRuntime();
                IRubyObject rubyArgs[] = new IRubyObject[args.length];
                for (int i = 0; i < args.length; i++) {
                    rubyArgs[i] = JavaEmbedUtils.javaToRuby(ruby, args[i]);
                }
                return (R) rubyProc.getBlock().call(ruby.getCurrentContext(), rubyArgs);
            } else if (closure instanceof Func0) {
                Func0<R> f = (Func0<R>) closure;
                if (args.length != 0) {
                    throw new RuntimeException("The closure was Func0 and expected no arguments, but we received: " + args.length);
                }
                return (R) f.call();
            } else if (closure instanceof Func1) {
                Func1<R, Object> f = (Func1<R, Object>) closure;
                if (args.length != 1) {
                    throw new RuntimeException("The closure was Func1 and expected 1 argument, but we received: " + args.length);
                }
                return f.call(args[0]);
            } else if (closure instanceof Func2) {
                Func2<R, Object, Object> f = (Func2<R, Object, Object>) closure;
                if (args.length != 2) {
                    throw new RuntimeException("The closure was Func2 and expected 2 arguments, but we received: " + args.length);
                }
                return f.call(args[0], args[1]);
            } else if (closure instanceof Func3) {
                Func3<R, Object, Object, Object> f = (Func3<R, Object, Object, Object>) closure;
                if (args.length != 3) {
                    throw new RuntimeException("The closure was Func3 and expected 3 arguments, but we received: " + args.length);
                }
                return (R) f.call(args[0], args[1], args[2]);
            } else if (closure instanceof Func4) {
                Func4<R, Object, Object, Object, Object> f = (Func4<R, Object, Object, Object, Object>) closure;
                if (args.length != 1) {
                    throw new RuntimeException("The closure was Func4 and expected 4 arguments, but we received: " + args.length);
                }
                return f.call(args[0], args[1], args[2], args[3]);
            } else if (closure instanceof FuncN) {
                FuncN<R> f = (FuncN<R>) closure;
                return f.call(args);
            } else {
                throw new RuntimeException("Unsupported closure type: " + closure.getClass().getSimpleName());
            }
        } finally {
            // if we have a tracer then log the end
            if (tracer != null && tracer.isTraceEnabled()) {
                try {
                    tracer.traceEnd(startTime, System.nanoTime(), closure, args);
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
