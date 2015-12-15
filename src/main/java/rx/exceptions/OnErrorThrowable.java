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
package rx.exceptions;

import java.util.HashSet;
import java.util.Set;

import rx.plugins.RxJavaErrorHandler;
import rx.plugins.RxJavaPlugins;

/**
 * Represents a {@code Throwable} that an {@code Observable} might notify its subscribers of, but that then can
 * be handled by an operator that is designed to recover from or react appropriately to such an error. You can
 * recover more information from an {@code OnErrorThrowable} than is found in a typical {@code Throwable}, such
 * as the item the {@code Observable} was trying to emit at the time the error was encountered.
 */
public final class OnErrorThrowable extends RuntimeException {

    private static final long serialVersionUID = -569558213262703934L;

    private final boolean hasValue;
    private final Object value;

    private OnErrorThrowable(Throwable exception) {
        super(exception);
        hasValue = false;
        this.value = null;
    }

    private OnErrorThrowable(Throwable exception, Object value) {
        super(exception);
        hasValue = true;
        this.value = value;
    }

    /**
     * Get the value associated with this {@code OnErrorThrowable}
     *
     * @return the value associated with this {@code OnErrorThrowable} (or {@code null} if there is none)
     */
    public Object getValue() {
        return value;
    }

    /**
     * Indicates whether or not there is a value associated with this {@code OnErrorThrowable}
     *
     * @return {@code true} if there is a value associated with this {@code OnErrorThrowable}, otherwise
     *         {@code false}
     */
    public boolean isValueNull() {
        return hasValue;
    }

    /**
     * Converts a {@link Throwable} into an {@link OnErrorThrowable}.
     *
     * @param t
     *          the {@code Throwable} to convert; if null, a NullPointerException is constructed
     * @return an {@code OnErrorThrowable} representation of {@code t}
     */
    public static OnErrorThrowable from(Throwable t) {
        if (t == null) {
            t = new NullPointerException();
        }
        Throwable cause = Exceptions.getFinalCause(t);
        if (cause instanceof OnErrorThrowable.OnNextValue) {
            return new OnErrorThrowable(t, ((OnNextValue) cause).getValue());
        }
        return new OnErrorThrowable(t);
    }

    /**
     * Adds the given item as the final cause of the given {@code Throwable}, wrapped in {@code OnNextValue}
     * (which extends {@code RuntimeException}).
     * 
     * @param e
     *          the {@link Throwable} to which you want to add a cause
     * @param value
     *          the item you want to add to {@code e} as the cause of the {@code Throwable}
     * @return the same {@code Throwable} ({@code e}) that was passed in, with {@code value} added to it as a
     *         cause
     */
    public static Throwable addValueAsLastCause(Throwable e, Object value) {
        if (e == null) {
            e = new NullPointerException();
        }
        Throwable lastCause = Exceptions.getFinalCause(e);
        if (lastCause != null && lastCause instanceof OnNextValue) {
            // purposefully using == for object reference check
            if (((OnNextValue) lastCause).getValue() == value) {
                // don't add another
                return e;
            }
        }
        Exceptions.addCause(e, new OnNextValue(value));
        return e;
    }

    /**
     * Represents an exception that was encountered while trying to emit an item from an Observable, and
     * tries to preserve that item for future use and/or reporting.
     */
    public static class OnNextValue extends RuntimeException {

        private static final long serialVersionUID = -3454462756050397899L;
        
        // Lazy loaded singleton 
        private static final class Primitives {
            
            static final Set<Class<?>> INSTANCE = create();

            private static Set<Class<?>> create() {
                Set<Class<?>> set = new HashSet<Class<?>>();
                set.add(Boolean.class);
                set.add(Character.class);
                set.add(Byte.class);
                set.add(Short.class);
                set.add(Integer.class);
                set.add(Long.class);
                set.add(Float.class);
                set.add(Double.class);
                // Void is another primitive but cannot be instantiated 
                // and is caught by the null check in renderValue
                return set;
            }
        }

        private final Object value;

        /**
         * Create an {@code OnNextValue} exception and include in its error message a string representation of
         * the item that was intended to be emitted at the time the exception was handled.
         *
         * @param value
         *         the item that the Observable was trying to emit at the time of the exception
         */
        public OnNextValue(Object value) {
            super("OnError while emitting onNext value: " + renderValue(value));
            this.value = value;
        }

        /**
         * Retrieve the item that the Observable was trying to emit at the time this exception occurred.
         *
         * @return the item that the Observable was trying to emit at the time of the exception
         */
        public Object getValue() {
            return value;
        }

        /**
         * Render the object if it is a basic type. This avoids the library making potentially expensive
         * or calls to toString() which may throw exceptions.
         *
         * If a specific behavior has been defined in the {@link RxJavaErrorHandler} plugin, some types
         * may also have a specific rendering. Non-primitive types not managed by the plugin are rendered
         * as the classname of the object.
         * <p>
         * See PR #1401 and Issue #2468 for details.
         *
         * @param value
         *        the item that the Observable was trying to emit at the time of the exception
         * @return a string version of the object if primitive or managed through error plugin,
         *        otherwise the classname of the object
         */
        static String renderValue(Object value){
            if (value == null) {
                return "null";
            }
            if (Primitives.INSTANCE.contains(value.getClass())) {
                return value.toString();
            }
            if (value instanceof String) {
                return (String) value;
            }
            if (value instanceof Enum) {
                return ((Enum<?>) value).name();
            }

            String pluggedRendering = RxJavaPlugins.getInstance().getErrorHandler().handleOnNextValueRendering(value);
            if (pluggedRendering != null) {
                return pluggedRendering;
            }

            return value.getClass().getName() + ".class";
        }
    }
}
