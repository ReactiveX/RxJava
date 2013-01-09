package org.rx.operations;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rx.functions.Func1;
import org.rx.reactive.AbstractIObservable;
import org.rx.reactive.IObservable;
import org.rx.reactive.IDisposable;
import org.rx.reactive.IObserver;


/* package */class OperationMap {

    /**
     * Accepts a sequence and a transformation function. Returns a sequence that is the result of
     * applying the transformation function to each item in the sequence.
     * 
     * @param sequence
     *            the input sequence.
     * @param func
     *            a function to apply to each item in the sequence.
     * @param <T>
     *            the type of the input sequence.
     * @param <R>
     *            the type of the output sequence.
     * @return a sequence that is the result of applying the transformation function to each item in the input sequence.
     */
    public static <T, R> IObservable<R> map(IObservable<T> sequence, Func1<R, T> func) {
        return new MapObservable<T, R>(sequence, func);
    }

    /**
     * Accepts a sequence of observable sequences and a transformation function. Returns a flattened sequence that is the result of
     * applying the transformation function to each item in the sequence of each observable sequence.
     * <p>
     * The closure should return an IObservable which will then be merged.
     * 
     * @param sequence
     *            the input sequence.
     * @param func
     *            a function to apply to each item in the sequence.
     * @param <T>
     *            the type of the input sequence.
     * @param <R>
     *            the type of the output sequence.
     * @return a sequence that is the result of applying the transformation function to each item in the input sequence.
     */
    public static <T, R> IObservable<R> mapMany(IObservable<T> sequence, Func1<IObservable<R>, T> func) {
        return OperationMerge.merge(map(sequence, func));
    }

    /**
     * An observable sequence that is the result of applying a transformation to each item in an input sequence.
     * 
     * @param <T>
     *            the type of the input sequence.
     * @param <R>
     *            the type of the output sequence.
     */
    private static class MapObservable<T, R> extends AbstractIObservable<R> {
        public MapObservable(IObservable<T> sequence, Func1<R, T> func) {
            this.sequence = sequence;
            this.func = func;
        }

        private IObservable<T> sequence;

        private Func1<R, T> func;

        public IDisposable subscribe(IObserver<R> watcher) {
            final AtomicWatchableSubscription subscription = new AtomicWatchableSubscription();
            final IObserver<R> observer = new AtomicWatcher<R>(watcher, subscription);
            subscription.setActual(sequence.subscribe(new MapObserver<T, R>(observer, func)));
            return subscription;
        }
    }

    /**
     * An observer that applies a transformation function to each item and forwards the result to an inner observer.
     * 
     * @param <T>
     *            the type of the observer items.
     * @param <R>
     *            the type of the inner observer items.
     */
    private static class MapObserver<T, R> implements IObserver<T> {
        public MapObserver(IObserver<R> observer, Func1<R, T> func) {
            this.observer = observer;
            this.func = func;
        }

        IObserver<R> observer;

        Func1<R, T> func;

        public void onNext(T value) {
            try {
                observer.onNext(func.call(value));
            } catch (Exception ex) {
                observer.onError(ex);
            }
        }

        public void onError(Exception ex) {
            observer.onError(ex);
        }

        public void onCompleted() {
            observer.onCompleted();
        }
    }

    public static class UnitTest {
        @Mock
        IObserver<String> stringObserver;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testMap() {
            Map<String, String> m1 = getMap("One");
            Map<String, String> m2 = getMap("Two");
            @SuppressWarnings("unchecked")
            IObservable<Map<String, String>> observable = WatchableExtensions.toWatchable(m1, m2);

            IObservable<String> m = map(observable, new Func1<String, Map<String, String>>() {

                @Override
                public String call(Map<String, String> map) {
                    return map.get("firstName");
                }

            });
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(1)).onNext("OneFirst");
            verify(stringObserver, times(1)).onNext("TwoFirst");
            verify(stringObserver, times(1)).onCompleted();

        }

        @Test
        public void testMapMany() {
            /* simulate a top-level async call which returns IDs */
            IObservable<Integer> ids = WatchableExtensions.toWatchable(1, 2);

            /* now simulate the behavior to take those IDs and perform nested async calls based on them */
            IObservable<String> m = mapMany(ids, new Func1<IObservable<String>, Integer>() {

                @SuppressWarnings("unchecked")
                @Override
                public IObservable<String> call(Integer id) {
                    /* simulate making a nested async call which creates another IObservable */
                    IObservable<Map<String, String>> subObservable = null;
                    if (id == 1) {
                        Map<String, String> m1 = getMap("One");
                        Map<String, String> m2 = getMap("Two");
                        subObservable = WatchableExtensions.toWatchable(m1, m2);
                    } else {
                        Map<String, String> m3 = getMap("Three");
                        Map<String, String> m4 = getMap("Four");
                        subObservable = WatchableExtensions.toWatchable(m3, m4);
                    }

                    /* simulate kicking off the async call and performing a select on it to transform the data */
                    return map(subObservable, new Func1<String, Map<String, String>>() {
                        @Override
                        public String call(Map<String, String> map) {
                            return map.get("firstName");
                        }
                    });
                }

            });
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(1)).onNext("OneFirst");
            verify(stringObserver, times(1)).onNext("TwoFirst");
            verify(stringObserver, times(1)).onNext("ThreeFirst");
            verify(stringObserver, times(1)).onNext("FourFirst");
            verify(stringObserver, times(1)).onCompleted();
        }

        @Test
        public void testMapMany2() {
            Map<String, String> m1 = getMap("One");
            Map<String, String> m2 = getMap("Two");
            @SuppressWarnings("unchecked")
            IObservable<Map<String, String>> observable1 = WatchableExtensions.toWatchable(m1, m2);

            Map<String, String> m3 = getMap("Three");
            Map<String, String> m4 = getMap("Four");
            @SuppressWarnings("unchecked")
            IObservable<Map<String, String>> observable2 = WatchableExtensions.toWatchable(m3, m4);

            @SuppressWarnings("unchecked")
            IObservable<IObservable<Map<String, String>>> observable = WatchableExtensions.toWatchable(observable1, observable2);

            IObservable<String> m = mapMany(observable, new Func1<IObservable<String>, IObservable<Map<String, String>>>() {

                @Override
                public IObservable<String> call(IObservable<Map<String, String>> o) {
                    return map(o, new Func1<String, Map<String, String>>() {

                        @Override
                        public String call(Map<String, String> map) {
                            return map.get("firstName");
                        }
                    });
                }

            });
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(1)).onNext("OneFirst");
            verify(stringObserver, times(1)).onNext("TwoFirst");
            verify(stringObserver, times(1)).onNext("ThreeFirst");
            verify(stringObserver, times(1)).onNext("FourFirst");
            verify(stringObserver, times(1)).onCompleted();

        }

        private Map<String, String> getMap(String prefix) {
            Map<String, String> m = new HashMap<String, String>();
            m.put("firstName", prefix + "First");
            m.put("lastName", prefix + "Last");
            return m;
        }

    }
}
