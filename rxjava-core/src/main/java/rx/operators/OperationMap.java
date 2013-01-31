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
package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

public final class OperationMap {

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
    public static <T, R> Func1<Observer<R>, Subscription> map(Observable<T> sequence, Func1<T, R> func) {
        return new MapObservable<T, R>(sequence, func);
    }

    /**
     * Accepts a sequence of observable sequences and a transformation function. Returns a flattened sequence that is the result of
     * applying the transformation function to each item in the sequence of each observable sequence.
     * <p>
     * The closure should return an Observable which will then be merged.
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
    public static <T, R> Func1<Observer<R>, Subscription> mapMany(Observable<T> sequence, Func1<T, Observable<R>> func) {
        return OperationMerge.merge(Observable.create(map(sequence, func)));
    }

    /**
     * An observable sequence that is the result of applying a transformation to each item in an input sequence.
     * 
     * @param <T>
     *            the type of the input sequence.
     * @param <R>
     *            the type of the output sequence.
     */
    private static class MapObservable<T, R> implements Func1<Observer<R>, Subscription> {
        public MapObservable(Observable<T> sequence, Func1<T, R> func) {
            this.sequence = sequence;
            this.func = func;
        }

        private Observable<T> sequence;

        private Func1<T, R> func;

        public Subscription call(Observer<R> observer) {
            return sequence.subscribe(new MapObserver<T, R>(observer, func));
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
    private static class MapObserver<T, R> implements Observer<T> {
        public MapObserver(Observer<R> observer, Func1<T, R> func) {
            this.observer = observer;
            this.func = func;
        }

        Observer<R> observer;

        Func1<T, R> func;

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
        Observer<String> stringObserver;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testMap() {
            Map<String, String> m1 = getMap("One");
            Map<String, String> m2 = getMap("Two");
            @SuppressWarnings("unchecked")
            Observable<Map<String, String>> observable = Observable.toObservable(m1, m2);

            Observable<String> m = Observable.create(map(observable, new Func1<Map<String, String>, String>() {

                @Override
                public String call(Map<String, String> map) {
                    return map.get("firstName");
                }

            }));
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Exception.class));
            verify(stringObserver, times(1)).onNext("OneFirst");
            verify(stringObserver, times(1)).onNext("TwoFirst");
            verify(stringObserver, times(1)).onCompleted();

        }

        @Test
        public void testMapMany() {
            /* simulate a top-level async call which returns IDs */
            Observable<Integer> ids = Observable.toObservable(1, 2);

            /* now simulate the behavior to take those IDs and perform nested async calls based on them */
            Observable<String> m = Observable.create(mapMany(ids, new Func1<Integer, Observable<String>>() {

                @SuppressWarnings("unchecked")
                @Override
                public Observable<String> call(Integer id) {
                    /* simulate making a nested async call which creates another Observable */
                    Observable<Map<String, String>> subObservable = null;
                    if (id == 1) {
                        Map<String, String> m1 = getMap("One");
                        Map<String, String> m2 = getMap("Two");
                        subObservable = Observable.toObservable(m1, m2);
                    } else {
                        Map<String, String> m3 = getMap("Three");
                        Map<String, String> m4 = getMap("Four");
                        subObservable = Observable.toObservable(m3, m4);
                    }

                    /* simulate kicking off the async call and performing a select on it to transform the data */
                    return Observable.create(map(subObservable, new Func1<Map<String, String>, String>() {
                        @Override
                        public String call(Map<String, String> map) {
                            return map.get("firstName");
                        }
                    }));
                }

            }));
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
            Observable<Map<String, String>> observable1 = Observable.toObservable(m1, m2);

            Map<String, String> m3 = getMap("Three");
            Map<String, String> m4 = getMap("Four");
            @SuppressWarnings("unchecked")
            Observable<Map<String, String>> observable2 = Observable.toObservable(m3, m4);

            @SuppressWarnings("unchecked")
            Observable<Observable<Map<String, String>>> observable = Observable.toObservable(observable1, observable2);

            Observable<String> m = Observable.create(mapMany(observable, new Func1<Observable<Map<String, String>>, Observable<String>>() {

                @Override
                public Observable<String> call(Observable<Map<String, String>> o) {
                    return Observable.create(map(o, new Func1<Map<String, String>, String>() {

                        @Override
                        public String call(Map<String, String> map) {
                            return map.get("firstName");
                        }
                    }));
                }

            }));
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
