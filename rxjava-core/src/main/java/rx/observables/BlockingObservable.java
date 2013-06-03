package rx.observables;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.operators.AtomicObservableSubscription;
import rx.operators.AtomicObserver;
import rx.operators.OperationMostRecent;
import rx.operators.OperationNext;
import rx.operators.OperationToFuture;
import rx.operators.OperationToIterator;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;
import rx.util.functions.Func1;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

/**
 * An extension of {@link Observable} that provides blocking operators.
 * <p>
 * You construct a BlockingObservable from an Observable with {@link #from(Observable)} or
 * {@link Observable#toBlockingObservable()}
 * <p>
 * The documentation for this interface makes use of a form of marble diagram that has been
 * modified to illustrate blocking operators. The following legend explains marble diagrams:
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/legend.png">
 * <p>
 * For more information see the <a href="https://github.com/Netflix/RxJava/wiki/Observable">RxJava
 * Wiki</a>
 * 
 * @param <T>
 */
public class BlockingObservable<T> extends Observable<T> {

    protected BlockingObservable(Func1<Observer<T>, Subscription> onSubscribe) {
        super(onSubscribe);
    }
    
    /**
     * Used to prevent public instantiation
     */
    @SuppressWarnings("unused")
    private BlockingObservable() {
        // prevent public instantiation
    }
    
    public static <T> BlockingObservable<T> from(final Observable<T> o) {
        return new BlockingObservable<T>(new Func1<Observer<T>, Subscription>() {

            @Override
            public Subscription call(Observer<T> observer) {
                return o.subscribe(observer);
            }
        });
    }

    /**
     * Returns an iterator that iterates all values of the observable.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.toIterator.png">
     * 
     * @param source
     *            an observable sequence to get an iterator for.
     * @param <T>
     *            the type of source.
     * @return the iterator that could be used to iterate over the elements of the observable.
     */
    public static <T> Iterator<T> toIterator(Observable<T> source) {
        return OperationToIterator.toIterator(source);
    }

    /**
     * Returns the last element of an observable sequence with a specified source.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.last.png">
     * 
     * @param source
     *            the source Observable
     * @return the last element in the observable sequence.
     */
    public static <T> T last(final Observable<T> source) {
        return from(source).last();
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.last.p.png">
     * 
     * @param source
     *            the source Observable
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @return the last element in the observable sequence.
     */
    public static <T> T last(final Observable<T> source, final Func1<T, Boolean> predicate) {
        return last(source.filter(predicate));
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.last.p.png">
     * 
     * @param source
     *            the source Observable
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @return the last element in the observable sequence.
     */
    public static <T> T last(final Observable<T> source, final Object predicate) {
        return last(source.filter(predicate));
    }

    /**
     * Returns the last element of an observable sequence, or a default value if no value is found.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.lastOrDefault.png">
     * 
     * @param source
     *            the source observable.
     * @param defaultValue
     *            a default value that would be returned if observable is empty.
     * @param <T>
     *            the type of source.
     * @return the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     */
    public static <T> T lastOrDefault(Observable<T> source, T defaultValue) {
        return from(source).lastOrDefault(defaultValue);
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.lastOrDefault.p.png">
     * 
     * @param source
     *            the source observable.
     * @param defaultValue
     *            a default value that would be returned if observable is empty.
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @param <T>
     *            the type of source.
     * @return the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     */
    public static <T> T lastOrDefault(Observable<T> source, T defaultValue, Func1<T, Boolean> predicate) {
        return lastOrDefault(source.filter(predicate), defaultValue);
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.lastOrDefault.p.png">
     * 
     * @param source
     *            the source observable.
     * @param defaultValue
     *            a default value that would be returned if observable is empty.
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @param <T>
     *            the type of source.
     * @return the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     */
    public static <T> T lastOrDefault(Observable<T> source, T defaultValue, Object predicate) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(predicate);

        return lastOrDefault(source, defaultValue, new Func1<T, Boolean>() {
            @Override
            public Boolean call(T args) {
                return (Boolean) _f.call(args);
            }
        });
    }

    /**
     * Samples the most recent value in an observable sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.mostRecent.png">
     * 
     * @param source
     *            the source observable sequence.
     * @param <T>
     *            the type of observable.
     * @param initialValue
     *            the initial value that will be yielded by the enumerable sequence if no element has been sampled yet.
     * @return the iterable that returns the last sampled element upon each iteration.
     */
    public static <T> Iterable<T> mostRecent(Observable<T> source, T initialValue) {
        return OperationMostRecent.mostRecent(source, initialValue);
    }

    /**
     * Samples the next value (blocking without buffering) from in an observable sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.next.png">
     * 
     * @param items
     *            the source observable sequence.
     * @param <T>
     *            the type of observable.
     * @return iterable that blocks upon each iteration until the next element in the observable source sequence becomes available.
     */
    public static <T> Iterable<T> next(Observable<T> items) {
        return OperationNext.next(items);
    }

    private static <T> T _singleOrDefault(BlockingObservable<T> source, boolean hasDefault, T defaultValue) {
        Iterator<T> it = source.toIterable().iterator();

        if (!it.hasNext()) {
            if (hasDefault) {
                return defaultValue;
            }
            throw new IllegalStateException("Expected single entry. Actually empty stream.");
        }

        T result = it.next();

        if (it.hasNext()) {
            throw new IllegalStateException("Expected single entry. Actually more than one entry.");
        }

        return result;
    }

    /**
     * Returns the only element of an observable sequence and throws an exception if there is not exactly one element in the observable sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.single.png">
     * 
     * @param source
     *            the source Observable
     * @return The single element in the observable sequence.
     * @throws IllegalStateException
     *             if there is not exactly one element in the observable sequence
     */
    public static <T> T single(Observable<T> source) {
        return from(source).single();
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate and throws an exception if there is not exactly one element in the observable sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.single.p.png">
     * 
     * @param source
     *            the source Observable
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence.
     * @throws IllegalStateException
     *             if there is not exactly one element in the observable sequence that matches the predicate
     */
    public static <T> T single(Observable<T> source, Func1<T, Boolean> predicate) {
        return from(source).single(predicate);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate and throws an exception if there is not exactly one element in the observable sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.single.p.png">
     * 
     * @param source
     *            the source Observable
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence.
     * @throws IllegalStateException
     *             if there is not exactly one element in the observable sequence that matches the predicate
     */
    public static <T> T single(Observable<T> source, Object predicate) {
        return from(source).single(predicate);
    }

    /**
     * Returns the only element of an observable sequence, or a default value if the observable sequence is empty.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.singleOrDefault.png">
     * 
     * @param source
     *            the source Observable
     * @param defaultValue
     *            default value for a sequence.
     * @return The single element in the observable sequence, or a default value if no value is found.
     */
    public static <T> T singleOrDefault(Observable<T> source, T defaultValue) {
        return from(source).singleOrDefault(defaultValue);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate, or a default value if no value is found.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.singleOrDefault.p.png">
     * 
     * @param source
     *            the source Observable
     * @param defaultValue
     *            default value for a sequence.
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence, or a default value if no value is found.
     */
    public static <T> T singleOrDefault(Observable<T> source, T defaultValue, Func1<T, Boolean> predicate) {
        return from(source).singleOrDefault(defaultValue, predicate);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate, or a default value if no value is found.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.singleOrDefault.p.png">
     * 
     * @param source
     *            the source Observable
     * @param defaultValue
     *            default value for a sequence.
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence, or a default value if no value is found.
     */
    public static <T> T singleOrDefault(Observable<T> source, T defaultValue, Object predicate) {
        return from(source).singleOrDefault(defaultValue, predicate);
    }

    /**
     * Return a Future representing a single value of the Observable.
     * <p>
     * This will throw an exception if the Observable emits more than 1 value. If more than 1 are expected then use <code>toList().toFuture()</code>.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.toFuture.png">
     * 
     * @param source
     *            the source Observable
     * @return a Future that expects a single item emitted by the source Observable
     */
    public static <T> Future<T> toFuture(final Observable<T> source) {
        return OperationToFuture.toFuture(source);
    }

    /**
     * Converts an observable sequence to an Iterable.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.toIterable.png">
     * 
     * @param source
     *            the source Observable
     * @return Observable converted to Iterable.
     */
    public static <T> Iterable<T> toIterable(final Observable<T> source) {
        return from(source).toIterable();
    }

    /**
     * Used for protecting against errors being thrown from Observer implementations and ensuring onNext/onError/onCompleted contract compliance.
     * <p>
     * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
     */
    private Subscription protectivelyWrapAndSubscribe(Observer<T> o) {
        AtomicObservableSubscription subscription = new AtomicObservableSubscription();
        return subscription.wrap(subscribe(new AtomicObserver<T>(subscription, o)));
    }
    
    /**
     * Invokes an action for each element in the observable sequence, and blocks until the sequence is terminated.
     * <p>
     * NOTE: This will block even if the Observable is asynchronous.
     * <p>
     * This is similar to {@link #subscribe(Observer)} but blocks. Because it blocks it does not need the {@link Observer#onCompleted()} or {@link Observer#onError(Exception)} methods.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.forEach.png">
     * 
     * @param onNext
     *            {@link Action1}
     * @throws RuntimeException
     *             if error occurs
     */
    public void forEach(final Action1<T> onNext) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Exception> exceptionFromOnError = new AtomicReference<Exception>();

        /**
         * Wrapping since raw functions provided by the user are being invoked.
         * 
         * See https://github.com/Netflix/RxJava/issues/216 for discussion on "Guideline 6.4: Protect calls to user code from within an operator"
         */
        protectivelyWrapAndSubscribe(new Observer<T>() {
            @Override
            public void onCompleted() {
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                /*
                 * If we receive an onError event we set the reference on the outer thread
                 * so we can git it and throw after the latch.await().
                 * 
                 * We do this instead of throwing directly since this may be on a different thread and the latch is still waiting.
                 */
                exceptionFromOnError.set(e);
                latch.countDown();
            }

            @Override
            public void onNext(T args) {
                onNext.call(args);
            }
        });
        // block until the subscription completes and then return
        try {
            latch.await();
        } catch (InterruptedException e) {
            // set the interrupted flag again so callers can still get it
            // for more information see https://github.com/Netflix/RxJava/pull/147#issuecomment-13624780
            Thread.currentThread().interrupt();
            // using Runtime so it is not checked
            throw new RuntimeException("Interrupted while waiting for subscription to complete.", e);
        }

        if (exceptionFromOnError.get() != null) {
            if (exceptionFromOnError.get() instanceof RuntimeException) {
                throw (RuntimeException) exceptionFromOnError.get();
            } else {
                throw new RuntimeException(exceptionFromOnError.get());
            }
        }
    }

    /**
     * Invokes an action for each element in the observable sequence, and blocks until the sequence is terminated.
     * <p>
     * NOTE: This will block even if the Observable is asynchronous.
     * <p>
     * This is similar to {@link #subscribe(Observer)} but blocks. Because it blocks it does not need the {@link Observer#onCompleted()} or {@link Observer#onError(Exception)} methods.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.forEach.png">
     * 
     * @param o
     *            onNext {@link Action1 action}
     * @throws RuntimeException
     *             if error occurs
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void forEach(final Object o) {
        if (o instanceof Action1) {
            // in case a dynamic language is not correctly handling the overloaded methods and we receive an Action1 just forward to the correct method.
            forEach((Action1) o);
        }

        // lookup and memoize onNext
        if (o == null) {
            throw new RuntimeException("onNext must be implemented");
        }
        final FuncN onNext = Functions.from(o);

        forEach(new Action1() {

            @Override
            public void call(Object args) {
                onNext.call(args);
            }

        });
    }
    
    /**
     * Returns an iterator that iterates all values of the observable.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.getIterator.png">
     * 
     * @return the iterator that could be used to iterate over the elements of the observable.
     */
    public Iterator<T> getIterator() {
        return OperationToIterator.toIterator(this);
    }

    /**
     * Returns the last element of an observable sequence with a specified source.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.last.png">
     * 
     * @return the last element in the observable sequence.
     */
    public T last() {
        T result = null;
        for (T value : toIterable()) {
            result = value;
        }
        return result;
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.last.p.png">
     * 
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @return the last element in the observable sequence.
     */
    public T last(final Func1<T, Boolean> predicate) {
        return last(this, predicate);
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.last.p.png">
     * 
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @return the last element in the observable sequence.
     */
    public T last(final Object predicate) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(predicate);

        return last(this, new Func1<T, Boolean>() {
            @Override
            public Boolean call(T args) {
                return (Boolean) _f.call(args);
            }
        });
    }

    /**
     * Returns the last element, or a default value if no value is found.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.lastOrDefault.png">
     * 
     * @param defaultValue
     *            a default value that would be returned if observable is empty.
     * @return the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     */
    public T lastOrDefault(T defaultValue) {
        boolean found = false;
        T result = null;

        for (T value : toIterable()) {
            found = true;
            result = value;
        }

        if (!found) {
            return defaultValue;
        }

        return result;
    }

    /**
     * Returns the last element that matches the predicate, or a default value if no value is found.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.lastOrDefault.p.png">
     * 
     * @param defaultValue
     *            a default value that would be returned if observable is empty.
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @return the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     */
    public T lastOrDefault(T defaultValue, Func1<T, Boolean> predicate) {
        return lastOrDefault(this, defaultValue, predicate);
    }

    /**
     * Returns the last element that matches the predicate, or a default value if no value is found.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.lastOrDefault.p.png">
     * 
     * @param defaultValue
     *            a default value that would be returned if observable is empty.
     * @param predicate
     *            a predicate function to evaluate for elements in the sequence.
     * @return the last element of an observable sequence that matches the predicate, or a default value if no value is found.
     */
    public T lastOrDefault(T defaultValue, Object predicate) {
        return lastOrDefault(this, defaultValue, predicate);
    }

    /**
     * Samples the most recent value in an observable sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.mostRecent.png">
     * 
     * @param initialValue
     *            the initial value that will be yielded by the enumerable sequence if no element has been sampled yet.
     * @return the iterable that returns the last sampled element upon each iteration.
     */
    public Iterable<T> mostRecent(T initialValue) {
        return mostRecent(this, initialValue);
    }

    /**
     * Samples the next value (blocking without buffering) from in an observable sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.next.png">
     * 
     * @return iterable that blocks upon each iteration until the next element in the observable source sequence becomes available.
     */
    public Iterable<T> next() {
        return next(this);
    }

    /**
     * Returns the only element of an observable sequence and throws an exception if there is not exactly one element in the observable sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.single.png">
     * 
     * @return The single element in the observable sequence.
     */
    public T single() {
        return _singleOrDefault(this, false, null);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate and throws an exception if there is not exactly one element in the observable sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.single.p.png">
     * 
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence.
     */
    public T single(Func1<T, Boolean> predicate) {
        return _singleOrDefault(from(this.filter(predicate)), false, null);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate and throws an exception if there is not exactly one element in the observable sequence.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.single.p.png">
     * 
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence.
     */
    public T single(Object predicate) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(predicate);

        return single(new Func1<T, Boolean>() {
            @Override
            public Boolean call(T t) {
                return (Boolean) _f.call(t);
            }
        });
    }

    /**
     * Returns the only element of an observable sequence, or a default value if the observable sequence is empty.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.singleOrDefault.png">
     * 
     * @param defaultValue
     *            default value for a sequence.
     * @return The single element in the observable sequence, or a default value if no value is found.
     */
    public T singleOrDefault(T defaultValue) {
        return _singleOrDefault(this, true, defaultValue);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate, or a default value if no value is found.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.singleOrDefault.p.png">
     * 
     * @param defaultValue
     *            default value for a sequence.
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence, or a default value if no value is found.
     */
    public T singleOrDefault(T defaultValue, Func1<T, Boolean> predicate) {
        return _singleOrDefault(from(this.filter(predicate)), true, defaultValue);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate, or a default value if no value is found.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.singleOrDefault.p.png">
     * 
     * @param defaultValue
     *            default value for a sequence.
     * @param predicate
     *            A predicate function to evaluate for elements in the sequence.
     * @return The single element in the observable sequence, or a default value if no value is found.
     */
    public T singleOrDefault(T defaultValue, final Object predicate) {
        @SuppressWarnings("rawtypes")
        final FuncN _f = Functions.from(predicate);

        return singleOrDefault(defaultValue, new Func1<T, Boolean>() {
            @Override
            public Boolean call(T t) {
                return (Boolean) _f.call(t);
            }
        });
    }

    /**
     * Return a Future representing a single value of the Observable.
     * <p>
     * This will throw an exception if the Observable emits more than 1 value. If more than 1 are expected then use <code>toList().toFuture()</code>.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.toFuture.png">
     * 
     * @return a Future that expects a single item emitted by the source Observable
     */
    public Future<T> toFuture() {
        return toFuture(this);
    }

    /**
     * Converts an observable sequence to an Iterable.
     * <p>
     * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/B.toIterable.png">
     * 
     * @return Observable converted to Iterable.
     */
    public Iterable<T> toIterable() {
        return new Iterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return getIterator();
            }
        };
    }

    public static class UnitTest {

        @Mock
        Observer<Integer> w;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testLast() {
            BlockingObservable<String> obs = BlockingObservable.from(Observable.from("one", "two", "three"));

            assertEquals("three", obs.last());
        }

        @Test
        public void testLastEmptyObservable() {
            BlockingObservable<Object> obs = BlockingObservable.from(Observable.from());

            assertNull(obs.last());
        }

        @Test
        public void testLastOrDefault() {
            BlockingObservable<Integer> observable = BlockingObservable.from(from(1, 0, -1));
            int last = observable.lastOrDefault(-100, new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer args) {
                    return args >= 0;
                }
            });
            assertEquals(0, last);
        }

        @Test
        public void testLastOrDefault1() {
            BlockingObservable<String> observable = BlockingObservable.from(from("one", "two", "three"));
            assertEquals("three", observable.lastOrDefault("default"));
        }

        @Test
        public void testLastOrDefault2() {
            BlockingObservable<Object> observable = BlockingObservable.from(from());
            assertEquals("default", observable.lastOrDefault("default"));
        }

        @Test
        public void testLastOrDefaultWithPredicate() {
            BlockingObservable<Integer> observable = BlockingObservable.from(from(1, 0, -1));
            int last = observable.lastOrDefault(0, new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer args) {
                    return args < 0;
                }
            });

            assertEquals(-1, last);
        }

        @Test
        public void testLastOrDefaultWrongPredicate() {
            BlockingObservable<Integer> observable = BlockingObservable.from(from(-1, -2, -3));
            int last = observable.lastOrDefault(0, new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer args) {
                    return args >= 0;
                }
            });
            assertEquals(0, last);
        }

        @Test
        public void testLastWithPredicate() {
            BlockingObservable<String> obs = BlockingObservable.from(Observable.from("one", "two", "three"));

            assertEquals("two", obs.last(new Func1<String, Boolean>() {
                @Override
                public Boolean call(String s) {
                    return s.length() == 3;
                }
            }));
        }

        public void testSingle() {
            BlockingObservable<String> observable = BlockingObservable.from(from("one"));
            assertEquals("one", observable.single());
        }

        @Test
        public void testSingleDefault() {
            BlockingObservable<Object> observable = BlockingObservable.from(from());
            assertEquals("default", observable.singleOrDefault("default"));
        }

        @Test(expected = IllegalStateException.class)
        public void testSingleDefaultPredicateMatchesMoreThanOne() {
            BlockingObservable.from(from("one", "two")).singleOrDefault("default", new Func1<String, Boolean>() {
                @Override
                public Boolean call(String args) {
                    return args.length() == 3;
                }
            });
        }

        @Test
        public void testSingleDefaultPredicateMatchesNothing() {
            BlockingObservable<String> observable = BlockingObservable.from(from("one", "two"));
            String result = observable.singleOrDefault("default", new Func1<String, Boolean>() {
                @Override
                public Boolean call(String args) {
                    return args.length() == 4;
                }
            });
            assertEquals("default", result);
        }

        @Test(expected = IllegalStateException.class)
        public void testSingleDefaultWithMoreThanOne() {
            BlockingObservable<String> observable = BlockingObservable.from(from("one", "two", "three"));
            observable.singleOrDefault("default");
        }

        @Test
        public void testSingleWithPredicateDefault() {
            BlockingObservable<String> observable = BlockingObservable.from(from("one", "two", "four"));
            assertEquals("four", observable.single(new Func1<String, Boolean>() {
                @Override
                public Boolean call(String s) {
                    return s.length() == 4;
                }
            }));
        }

        @Test(expected = IllegalStateException.class)
        public void testSingleWrong() {
            BlockingObservable<Integer> observable = BlockingObservable.from(from(1, 2));
            observable.single();
        }

        @Test(expected = IllegalStateException.class)
        public void testSingleWrongPredicate() {
            BlockingObservable<Integer> observable = BlockingObservable.from(from(-1));
            observable.single(new Func1<Integer, Boolean>() {
                @Override
                public Boolean call(Integer args) {
                    return args > 0;
                }
            });
        }

        @Test
        public void testToIterable() {
            BlockingObservable<String> obs = BlockingObservable.from(from("one", "two", "three"));

            Iterator<String> it = obs.toIterable().iterator();

            assertEquals(true, it.hasNext());
            assertEquals("one", it.next());

            assertEquals(true, it.hasNext());
            assertEquals("two", it.next());

            assertEquals(true, it.hasNext());
            assertEquals("three", it.next());

            assertEquals(false, it.hasNext());

        }

        @Test(expected = TestException.class)
        public void testToIterableWithException() {
            BlockingObservable<String> obs = BlockingObservable.from(create(new Func1<Observer<String>, Subscription>() {

                @Override
                public Subscription call(Observer<String> observer) {
                    observer.onNext("one");
                    observer.onError(new TestException());
                    return Subscriptions.empty();
                }
            }));

            Iterator<String> it = obs.toIterable().iterator();

            assertEquals(true, it.hasNext());
            assertEquals("one", it.next());

            assertEquals(true, it.hasNext());
            it.next();

        }
        
        @Test
        public void testForEachWithError() {
            try {
                BlockingObservable.from(Observable.create(new Func1<Observer<String>, Subscription>() {

                    @Override
                    public Subscription call(final Observer<String> observer) {
                        final BooleanSubscription subscription = new BooleanSubscription();
                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                observer.onNext("one");
                                observer.onNext("two");
                                observer.onNext("three");
                                observer.onCompleted();
                            }
                        }).start();
                        return subscription;
                    }
                })).forEach(new Action1<String>() {

                    @Override
                    public void call(String t1) {
                        throw new RuntimeException("fail");
                    }
                });
                fail("we expect an exception to be thrown");
            } catch (Exception e) {
                // do nothing as we expect this
            }
        }

        private static class TestException extends RuntimeException {
            private static final long serialVersionUID = 1L;
        }
    }
}
