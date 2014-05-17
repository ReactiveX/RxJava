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
package rx.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observer;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.operators.OperatorToMultimap.DefaultMultimapCollectionFactory;
import rx.operators.OperatorToMultimap.DefaultToMultimapFactory;

public class OperatorToMultimapTest {
    @Mock
    Observer<Object> objectObserver;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    Func1<String, Integer> lengthFunc = new Func1<String, Integer>() {
        @Override
        public Integer call(String t1) {
            return t1.length();
        }
    };
    Func1<String, String> duplicate = new Func1<String, String>() {
        @Override
        public String call(String t1) {
            return t1 + t1;
        }
    };

    @Test
    public void testToMultimap() {
        Observable<String> source = Observable.from("a", "b", "cc", "dd");

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onCompleted();
    }

    @Test
    public void testToMultimapWithValueSelector() {
        Observable<String> source = Observable.from("a", "b", "cc", "dd");

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, duplicate);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onCompleted();
    }

    @Test
    public void testToMultimapWithMapFactory() {
        Observable<String> source = Observable.from("a", "b", "cc", "dd", "eee", "fff");

        Func0<Map<Integer, Collection<String>>> mapFactory = new Func0<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> call() {
                return new LinkedHashMap<Integer, Collection<String>>() {
                    /** */
                    private static final long serialVersionUID = -2084477070717362859L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Integer, Collection<String>> eldest) {
                        return size() > 2;
                    }
                };
            }
        };

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(
                lengthFunc, Functions.<String>identity(),
                mapFactory, new DefaultMultimapCollectionFactory<Integer, String>());

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onCompleted();
    }

    @Test
    public void testToMultimapWithCollectionFactory() {
        Observable<String> source = Observable.from("cc", "dd", "eee", "eee");

        Func1<Integer, Collection<String>> collectionFactory = new Func1<Integer, Collection<String>>() {

            @Override
            public Collection<String> call(Integer t1) {
                if (t1 == 2) {
                    return new ArrayList<String>();
                } else {
                    return new HashSet<String>();
                }
            }
        };

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(
                lengthFunc, Functions.<String>identity(),
                new DefaultToMultimapFactory<Integer, String>(), collectionFactory);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, new HashSet<String>(Arrays.asList("eee")));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onCompleted();
    }

    @Test
    public void testToMultimapWithError() {
        Observable<String> source = Observable.from("a", "b", "cc", "dd");

        Func1<String, Integer> lengthFuncErr = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                if ("b".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFuncErr);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onCompleted();
    }

    @Test
    public void testToMultimapWithErrorInValueSelector() {
        Observable<String> source = Observable.from("a", "b", "cc", "dd");

        Func1<String, String> duplicateErr = new Func1<String, String>() {
            @Override
            public String call(String t1) {
                if ("b".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, duplicateErr);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onCompleted();
    }

    @Test
    public void testToMultimapWithMapThrowingFactory() {
        Observable<String> source = Observable.from("a", "b", "cc", "dd", "eee", "fff");

        Func0<Map<Integer, Collection<String>>> mapFactory = new Func0<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> call() {
                throw new RuntimeException("Forced failure");
            }
        };

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, Functions.<String>identity(), mapFactory);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onCompleted();
    }

    @Test
    public void testToMultimapWithThrowingCollectionFactory() {
        Observable<String> source = Observable.from("cc", "cc", "eee", "eee");

        Func1<Integer, Collection<String>> collectionFactory = new Func1<Integer, Collection<String>>() {

            @Override
            public Collection<String> call(Integer t1) {
                if (t1 == 2) {
                    throw new RuntimeException("Forced failure");
                } else {
                    return new HashSet<String>();
                }
            }
        };

        Observable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, Functions.<String>identity(), new DefaultToMultimapFactory<Integer, String>(), collectionFactory);

        Map<Integer, Collection<String>> expected = new HashMap<Integer, Collection<String>>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Collections.singleton("eee"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onCompleted();
    }
}
