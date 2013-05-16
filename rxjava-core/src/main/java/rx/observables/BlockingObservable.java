package rx.observables;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.operators.OperationMostRecent;
import rx.operators.OperationNext;
import rx.operators.OperationToFuture;
import rx.operators.OperationToIterator;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;
import rx.util.functions.FuncN;
import rx.util.functions.Functions;

/**
 * Extension of {@link Observable} that provides blocking operators.
 * <p>
 * Constructud via {@link #from(Observable)} or {@link Observable#toBlockingObservable()}
 * 
 * @param <T>
 */
public class BlockingObservable<T> extends Observable<T> {

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
     * 
     * @param that
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
     * 
     * @param that
     *            the source Observable
     * @return the last element in the observable sequence.
     */
    public static <T> T last(final Observable<T> source) {
        return from(source).last();
    }

    /**
     * Returns the last element of an observable sequence that matches the predicate.
     * 
     * @param that
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
     * 
     * @param that
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
     * 
     * @param that
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
     * 
     * @param that
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
     * 
     * @param that
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
     * 
     * @param that
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
     * 
     * @param that
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
     * 
     * @param that
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
     * 
     * @param that
     *            the source Observable
     * @return a Future that expects a single item emitted by the source Observable
     */
    public static <T> Future<T> toFuture(final Observable<T> source) {
        return OperationToFuture.toFuture(source);
    }

    /**
     * Converts an observable sequence to an Iterable.
     * 
     * @param that
     *            the source Observable
     * @return Observable converted to Iterable.
     */
    public static <T> Iterable<T> toIterable(final Observable<T> source) {
        return from(source).toIterable();
    }

    protected BlockingObservable(Func1<Observer<T>, Subscription> onSubscribe) {
        super(onSubscribe);
    }

    /**
     * Returns an iterator that iterates all values of the observable.
     * 
     * @return the iterator that could be used to iterate over the elements of the observable.
     */
    public Iterator<T> getIterator() {
        return OperationToIterator.toIterator(this);
    }

    /**
     * Returns the last element of an observable sequence with a specified source.
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
     * 
     * @return iterable that blocks upon each iteration until the next element in the observable source sequence becomes available.
     */
    public Iterable<T> next() {
        return next(this);
    }

    /**
     * Returns the only element of an observable sequence and throws an exception if there is not exactly one element in the observable sequence.
     * 
     * @return The single element in the observable sequence.
     */
    public T single() {
        return _singleOrDefault(this, false, null);
    }

    /**
     * Returns the only element of an observable sequence that matches the predicate and throws an exception if there is not exactly one element in the observable sequence.
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
     * 
     * @return a Future that expects a single item emitted by the source Observable
     */
    public Future<T> toFuture() {
        return toFuture(this);
    }

    /**
     * Converts an observable sequence to an Iterable.
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

        private static class TestException extends RuntimeException {
            private static final long serialVersionUID = 1L;
        }
    }
}
