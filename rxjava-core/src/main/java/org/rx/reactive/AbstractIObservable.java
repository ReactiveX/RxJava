package org.rx.reactive;

import static org.mockito.Mockito.*;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.runtime.InvokerHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rx.functions.Func1;
import org.rx.functions.Func2;
import org.rx.functions.Functions;
import org.rx.operations.OperationMaterialize;
import org.rx.operations.WatchableExtensions;


/**
 * Abstract parent class to provide common functionality of classes implementing the Watchable<T> interface.
 * <p>
 * It provides overloaded methods for subscribing as well as delegate methods to the WatchableExtensions.
 * 
 * @param <T>
 * 
 * @ExcludeFromSDKJavadoc
 */
public abstract class AbstractIObservable<T> implements IObservable<T> {
    private static volatile APIWatchableErrorHandler errorHandler;

    /**
     * An implementation of subscribe must be provided to comply with the IObservable<T> interface to allow subscribing for push notifications.
     * 
     * @see IObservable<T>
     */
    public abstract IDisposable subscribe(IObserver<T> observer);

    /**
     * Register a single global error handler which will be given all Exceptions that are passed through an onError method call.
     * 
     * @param handler
     */
    public static void registerErrorHandler(APIWatchableErrorHandler handler) {
        errorHandler = handler;
    }

    /**
     * When an error occurs in any Watcher we will invoke this to allow it to be handled by the global APIWatchableErrorHandler
     * 
     * @param e
     */
    private static void handleError(Exception e) {
        if (errorHandler != null) {
            try {
                errorHandler.handleError(e);
            } catch (Exception ex) {
                // TODO replace this with something other than logger.
                // logger.error("Failed to send exception to APIWatchableErrorHandler.", ex);
            }
        }
    }

    /**
     * @ExcludeFromSDKJavadoc
     */
    public static interface APIWatchableErrorHandler {
        public void handleError(Exception e);
    }

    /**
     * Execute the callback with the given arguments.
     * <p>
     * The callbacks align with the onCompleted, onError and onNext methods an an IObserver.
     * 
     * @param callback
     *            Object to be invoked. It is left to the implementing class to determine the type, such as a Groovy Closure or JRuby closure conversion.
     * @param args
     */
    protected void executeCallback(final Object callback, Object... args) {
        Functions.execute(callback, args);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public IDisposable subscribe(final Object onNext, final Object onError, final Object onComplete) {
        return subscribe(new IObserver() {

            @Override
            public void onCompleted() {
                if (onComplete != null) {
                    executeCallback(onComplete);
                }
            }

            @Override
            public void onError(Exception e) {
                handleError(e);
                if (onError != null) {
                    executeCallback(onError, e);
                }
            }

            @Override
            public void onNext(Object args) {
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                executeCallback(onNext, args);
            }

        });
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public IDisposable subscribe(final Object onNext, final Object onError) {
        return subscribe(new IObserver() {

            @Override
            public void onCompleted() {
                // do nothing
            }

            @Override
            public void onError(Exception e) {
                handleError(e);
                if (onError != null) {
                    executeCallback(onError, e);
                }
            }

            @Override
            public void onNext(Object args) {
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                executeCallback(onNext, args);
            }

        });
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public IDisposable subscribe(final Object onNext) {
        if (onNext instanceof IObserver) {
            throw new RuntimeException("Watchers are not intended to be passed to this generic method. Your generic type is most likely wrong. This method is for dynamic code to send in closures.");
        }
        return subscribe(new IObserver() {

            @Override
            public void onCompleted() {
                // do nothing
            }

            @Override
            public void onError(Exception e) {
                handleError(e);
                // no callback defined
            }

            @Override
            public void onNext(Object args) {
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                executeCallback(onNext, args);
            }

        });
    }

    /**
     * Helper method which acts as a synonym to <code>execute(APIServiceCallback<T> callback)</code> so that groovy can pass in a closure without the <code>as com.netflix.api.service.APIServiceCallback</code> at the end of it.
     * 
     * @param callbacks
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public IDisposable subscribe(final Map<String, Object> callbacks) {
        return subscribe(new IObserver() {

            @Override
            public void onCompleted() {
                Object completed = callbacks.get("onCompleted");
                if (completed != null) {
                    executeCallback(completed);
                }
            }

            @Override
            public void onError(Exception e) {
                handleError(e);
                Object onError = callbacks.get("onError");
                if (onError != null) {
                    executeCallback(onError, e);
                }
            }

            @Override
            public void onNext(Object args) {
                Object onNext = callbacks.get("onNext");
                if (onNext == null) {
                    throw new RuntimeException("onNext must be implemented");
                }
                executeCallback(onNext, args);
            }

        });
    }

    @Override
    public <R> IObservable<R> mapMany(final Object callback) {
        return WatchableExtensions.mapMany(this, new Func1<IObservable<R>, T>() {

            @Override
            public IObservable<R> call(T t1) {
                return Functions.execute(callback, t1);
            }
        });
    }

    @Override
    public <R> IObservable<R> mapMany(Func1<IObservable<R>, T> func) {
        return WatchableExtensions.mapMany(this, func);
    }

    @Override
    public <R> IObservable<R> map(final Object callback) {
        return WatchableExtensions.map(this, new Func1<R, T>() {

            @Override
            public R call(T t1) {
                return Functions.execute(callback, t1);
            }
        });
    }

    @Override
    public <R> IObservable<R> map(Func1<R, T> func) {
        return WatchableExtensions.map(this, func);
    }

    @Override
    public IObservable<List<T>> toList() {
        return WatchableExtensions.toList(this);
    }

    @Override
    public IObservable<List<T>> toSortedList() {
        return WatchableExtensions.toSortedList(this);
    }

    @Override
    public IObservable<List<T>> toSortedList(Func2<Integer, T, T> sortFunction) {
        return WatchableExtensions.toSortedList(this, sortFunction);
    }

    @Override
    public IObservable<List<T>> toSortedList(final Object sortFunction) {
        return WatchableExtensions.toSortedList(this, sortFunction);
    }

    @Override
    public IObservable<T> take(final int num) {
        return WatchableExtensions.take(this, num);
    }

    @Override
    public IObservable<T> filter(final Object callback) {
        return WatchableExtensions.filter(this, new Func1<Boolean, T>() {

            @Override
            public Boolean call(T t1) {
                return Functions.execute(callback, t1);
            }
        });
    }

    @Override
    public IObservable<T> filter(Func1<Boolean, T> predicate) {
        return WatchableExtensions.filter(this, predicate);
    }

    @Override
    public IObservable<T> last() {
        return WatchableExtensions.last(this);
    }

    @Override
    public IObservable<T> onErrorResumeNext(final IObservable<T> resumeSequence) {
        return WatchableExtensions.onErrorResumeNext(this, resumeSequence);
    }

    @Override
    public IObservable<T> onErrorResumeNext(final Func1<IObservable<T>, Exception> resumeFunction) {
        return WatchableExtensions.onErrorResumeNext(this, resumeFunction);
    }

    @Override
    public IObservable<T> onErrorResumeNext(final Object resumeFunction) {
        return WatchableExtensions.onErrorResumeNext(this, new Func1<IObservable<T>, Exception>() {

            @Override
            public IObservable<T> call(Exception e) {
                return Functions.execute(resumeFunction, e);
            }
        });
    }

    @Override
    public IObservable<T> onErrorReturn(final Object resumeFunction) {
        return WatchableExtensions.onErrorReturn(this, new Func1<T, Exception>() {

            @Override
            public T call(Exception e) {
                return Functions.execute(resumeFunction, e);
            }
        });
    }

    @Override
    public IObservable<T> onErrorReturn(Func1<T, Exception> resumeFunction) {
        return WatchableExtensions.onErrorReturn(this, resumeFunction);
    }

    @Override
    public IObservable<T> reduce(Func2<T, T, T> accumulator) {
        return WatchableExtensions.reduce(this, accumulator);
    }

    @Override
    public IObservable<T> reduce(Object accumulator) {
        return WatchableExtensions.reduce(this, accumulator);
    }

    @Override
    public IObservable<T> reduce(T initialValue, Func2<T, T, T> accumulator) {
        return WatchableExtensions.reduce(this, initialValue, accumulator);
    }

    @Override
    public IObservable<T> reduce(T initialValue, Object accumulator) {
        return WatchableExtensions.reduce(this, initialValue, accumulator);
    }

    @Override
    public IObservable<T> skip(int num) {
        return WatchableExtensions.skip(this, num);
    }

    @Override
    public IObservable<T> scan(Func2<T, T, T> accumulator) {
        return WatchableExtensions.scan(this, accumulator);
    }

    @Override
    public IObservable<T> scan(final Object accumulator) {
        return WatchableExtensions.scan(this, accumulator);
    }

    @Override
    public IObservable<T> scan(T initialValue, Func2<T, T, T> accumulator) {
        return WatchableExtensions.scan(this, initialValue, accumulator);
    }

    @Override
    public IObservable<T> scan(final T initialValue, final Object accumulator) {
        return WatchableExtensions.scan(this, initialValue, accumulator);
    }

    @Override
    public IObservable<Notification<T>> materialize() {
        return OperationMaterialize.materialize(this);
    }

    public static class UnitTest {
        @Mock
        ScriptAssertion assertion;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testLast() {
            String script = "mockApiCall.getObservable().last().subscribe({ result -> a.received(result)});";
            runGroovyScript(script);
            verify(assertion, times(1)).received("hello_1");
        }

        @Test
        public void testMap() {
            String script = "mockApiCall.getObservable().map({v -> 'say' + v}).subscribe({ result -> a.received(result)});";
            runGroovyScript(script);
            verify(assertion, times(1)).received("sayhello_1");
        }

        @Test
        public void testScriptWithOnNext() {
            String script = "mockApiCall.getObservable().subscribe({ result -> a.received(result)})";
            runGroovyScript(script);
            verify(assertion).received("hello_1");
        }

        @Test
        public void testScriptWithMerge() {
            String script = "o.merge(mockApiCall.getObservable(), mockApiCall.getObservable()).subscribe({ result -> a.received(result)});";
            runGroovyScript(script);
            verify(assertion, times(1)).received("hello_1");
            verify(assertion, times(1)).received("hello_2");
        }

        @Test
        public void testScriptWithMaterialize() {
            String script = "mockApiCall.getObservable().materialize().subscribe({ result -> a.received(result)});";
            runGroovyScript(script);
            // 2 times: once for hello_1 and once for onCompleted
            verify(assertion, times(2)).received(any(Notification.class));
        }

        @Test
        public void testToSortedList() {
            runGroovyScript("mockApiCall.getNumbers().toSortedList().subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
        }

        @Test
        public void testToSortedListWithFunction() {
            runGroovyScript("mockApiCall.getNumbers().toSortedList({a, b -> a - b}).subscribe({ result -> a.received(result)});");
            verify(assertion, times(1)).received(Arrays.asList(1, 2, 3, 4, 5));
        }

        private void runGroovyScript(String script) {
            ClassLoader parent = getClass().getClassLoader();
            GroovyClassLoader loader = new GroovyClassLoader(parent);

            Binding binding = new Binding();
            binding.setVariable("mockApiCall", new TestFactory());
            binding.setVariable("a", assertion);
            binding.setVariable("o", org.rx.operations.WatchableExtensions.class);

            /* parse the script and execute it */
            InvokerHelper.createScript(loader.parseClass(script), binding).run();
        }

        private static class TestFactory {
            int counter = 1;

            @SuppressWarnings("unused")
            public TestObservable getObservable() {
                return new TestObservable(counter++);
            }

            @SuppressWarnings("unused")
            public IObservable<Integer> getNumbers() {
                return WatchableExtensions.toWatchable(1, 3, 2, 5, 4);
            }
        }

        private static class TestObservable extends AbstractIObservable<String> {
            private final int count;

            public TestObservable(int count) {
                this.count = count;
            }

            @Override
            public IDisposable subscribe(IObserver<String> observer) {

                observer.onNext("hello_" + count);
                observer.onCompleted();

                return new IDisposable() {

                    @Override
                    public void unsubscribe() {
                        // unregister ... will never be called here since we are executing synchronously
                    }

                };
            }
        }

        private static interface ScriptAssertion {
            public void received(Object o);
        }
    }
}
