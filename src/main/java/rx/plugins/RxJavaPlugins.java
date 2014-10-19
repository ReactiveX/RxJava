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
package rx.plugins;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registry for plugin implementations that allows global override and handles the retrieval of correct
 * implementation based on order of precedence:
 * <ol>
 * <li>plugin registered globally via {@code register} methods in this class</li>
 * <li>plugin registered and retrieved using {@link java.lang.System#getProperty(String)} (see get methods for
 * property names)</li>
 * <li>default implementation</li>
 * </ol>
 *
 * @see <a href="https://github.com/ReactiveX/RxJava/wiki/Plugins">RxJava Wiki: Plugins</a>
 */
public class RxJavaPlugins {
    private final static RxJavaPlugins INSTANCE = new RxJavaPlugins();

    private final AtomicReference<RxJavaErrorHandler> errorHandler = new AtomicReference<RxJavaErrorHandler>();
    private final AtomicReference<RxJavaObservableExecutionHook> observableExecutionHook = new AtomicReference<RxJavaObservableExecutionHook>();
    private final AtomicReference<RxJavaSchedulersHook> schedulersHook = new AtomicReference<RxJavaSchedulersHook>();

    /**
     * Retrieves the single {@code RxJavaPlugins} instance.
     *
     * @return the single {@code RxJavaPlugins} instance
     */
    public static RxJavaPlugins getInstance() {
        return INSTANCE;
    }

    /* package accessible for unit tests */RxJavaPlugins() {

    }

    /* package accessible for unit tests */void reset() {
        INSTANCE.errorHandler.set(null);
        INSTANCE.observableExecutionHook.set(null);
        INSTANCE.schedulersHook.set(null);
    }

    static final RxJavaErrorHandler DEFAULT_ERROR_HANDLER = new RxJavaErrorHandler() {
    };

    /**
     * Retrieves an instance of {@link RxJavaErrorHandler} to use based on order of precedence as defined in
     * {@link RxJavaPlugins} class header.
     * <p>
     * Override the default by calling {@link #registerErrorHandler(RxJavaErrorHandler)} or by setting the
     * property {@code rxjava.plugin.RxJavaErrorHandler.implementation} with the full classname to load.
     * 
     * @return {@link RxJavaErrorHandler} implementation to use
     */
    public RxJavaErrorHandler getErrorHandler() {
        if (errorHandler.get() == null) {
            // check for an implementation from System.getProperty first
            Object impl = getPluginImplementationViaProperty(RxJavaErrorHandler.class);
            if (impl == null) {
                // nothing set via properties so initialize with default 
                errorHandler.compareAndSet(null, DEFAULT_ERROR_HANDLER);
                // we don't return from here but call get() again in case of thread-race so the winner will always get returned
            } else {
                // we received an implementation from the system property so use it
                errorHandler.compareAndSet(null, (RxJavaErrorHandler) impl);
            }
        }
        return errorHandler.get();
    }

    /**
     * Registers an {@link RxJavaErrorHandler} implementation as a global override of any injected or default
     * implementations.
     * 
     * @param impl
     *            {@link RxJavaErrorHandler} implementation
     * @throws IllegalStateException
     *             if called more than once or after the default was initialized (if usage occurs before trying
     *             to register)
     */
    public void registerErrorHandler(RxJavaErrorHandler impl) {
        if (!errorHandler.compareAndSet(null, impl)) {
            throw new IllegalStateException("Another strategy was already registered: " + errorHandler.get());
        }
    }

    /**
     * Retrieves the instance of {@link RxJavaObservableExecutionHook} to use based on order of precedence as
     * defined in {@link RxJavaPlugins} class header.
     * <p>
     * Override the default by calling {@link #registerObservableExecutionHook(RxJavaObservableExecutionHook)}
     * or by setting the property {@code rxjava.plugin.RxJavaObservableExecutionHook.implementation} with the
     * full classname to load.
     * 
     * @return {@link RxJavaObservableExecutionHook} implementation to use
     */
    public RxJavaObservableExecutionHook getObservableExecutionHook() {
        if (observableExecutionHook.get() == null) {
            // check for an implementation from System.getProperty first
            Object impl = getPluginImplementationViaProperty(RxJavaObservableExecutionHook.class);
            if (impl == null) {
                // nothing set via properties so initialize with default 
                observableExecutionHook.compareAndSet(null, RxJavaObservableExecutionHookDefault.getInstance());
                // we don't return from here but call get() again in case of thread-race so the winner will always get returned
            } else {
                // we received an implementation from the system property so use it
                observableExecutionHook.compareAndSet(null, (RxJavaObservableExecutionHook) impl);
            }
        }
        return observableExecutionHook.get();
    }

    /**
     * Register an {@link RxJavaObservableExecutionHook} implementation as a global override of any injected or
     * default implementations.
     * 
     * @param impl
     *            {@link RxJavaObservableExecutionHook} implementation
     * @throws IllegalStateException
     *             if called more than once or after the default was initialized (if usage occurs before trying
     *             to register)
     */
    public void registerObservableExecutionHook(RxJavaObservableExecutionHook impl) {
        if (!observableExecutionHook.compareAndSet(null, impl)) {
            throw new IllegalStateException("Another strategy was already registered: " + observableExecutionHook.get());
        }
    }

    private static Object getPluginImplementationViaProperty(Class<?> pluginClass) {
        String classSimpleName = pluginClass.getSimpleName();
        /*
         * Check system properties for plugin class.
         * <p>
         * This will only happen during system startup thus it's okay to use the synchronized
         * System.getProperties as it will never get called in normal operations.
         */
        String implementingClass = System.getProperty("rxjava.plugin." + classSimpleName + ".implementation");
        if (implementingClass != null) {
            try {
                Class<?> cls = Class.forName(implementingClass);
                // narrow the scope (cast) to the type we're expecting
                cls = cls.asSubclass(pluginClass);
                return cls.newInstance();
            } catch (ClassCastException e) {
                throw new RuntimeException(classSimpleName + " implementation is not an instance of " + classSimpleName + ": " + implementingClass);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(classSimpleName + " implementation class not found: " + implementingClass, e);
            } catch (InstantiationException e) {
                throw new RuntimeException(classSimpleName + " implementation not able to be instantiated: " + implementingClass, e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(classSimpleName + " implementation not able to be accessed: " + implementingClass, e);
            }
        } else {
            return null;
        }
    }

    /**
     * Retrieves the instance of {@link RxJavaSchedulersHook} to use based on order of precedence as defined
     * in the {@link RxJavaPlugins} class header.
     * <p>
     * Override the default by calling {@link #registerSchedulersHook(RxJavaSchedulersHook)} or by setting
     * the property {@code rxjava.plugin.RxJavaSchedulersHook.implementation} with the full classname to
     * load.
     *
     * @return the {@link RxJavaSchedulersHook} implementation in use
     */
    public RxJavaSchedulersHook getSchedulersHook() {
        if (schedulersHook.get() == null) {
            // check for an implementation from System.getProperty first
            Object impl = getPluginImplementationViaProperty(RxJavaSchedulersHook.class);
            if (impl == null) {
                // nothing set via properties so initialize with default
                schedulersHook.compareAndSet(null, RxJavaSchedulersHook.getDefaultInstance());
                // we don't return from here but call get() again in case of thread-race so the winner will always get returned
            } else {
                // we received an implementation from the system property so use it
                schedulersHook.compareAndSet(null, (RxJavaSchedulersHook) impl);
            }
        }
        return schedulersHook.get();
    }

    /**
     * Registers an {@link RxJavaSchedulersHook} implementation as a global override of any injected or
     * default implementations.
     *
     * @param impl
     *            {@link RxJavaSchedulersHook} implementation
     * @throws IllegalStateException
     *             if called more than once or after the default was initialized (if usage occurs before trying
     *             to register)
     */
    public void registerSchedulersHook(RxJavaSchedulersHook impl) {
        if (!schedulersHook.compareAndSet(null, impl)) {
            throw new IllegalStateException("Another strategy was already registered: " + schedulersHook.get());
        }
    }
}
