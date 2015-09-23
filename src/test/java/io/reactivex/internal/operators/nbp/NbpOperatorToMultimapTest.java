/**
 * Copyright 2015 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.nbp;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import java.util.function.*;

import org.junit.*;

import io.reactivex.*;
import io.reactivex.NbpObservable.NbpSubscriber;

public class NbpOperatorToMultimapTest {
    NbpSubscriber<Object> objectObserver;

    @Before
    public void before() {
        objectObserver = TestHelper.mockNbpSubscriber();
    }

    Function<String, Integer> lengthFunc = t1 -> t1.length();
    Function<String, String> duplicate = t1 -> t1 + t1;

    @Test
    public void testToMultimap() {
        NbpObservable<String> source = NbpObservable.just("a", "b", "cc", "dd");

        NbpObservable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void testToMultimapWithValueSelector() {
        NbpObservable<String> source = NbpObservable.just("a", "b", "cc", "dd");

        NbpObservable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, duplicate);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void testToMultimapWithMapFactory() {
        NbpObservable<String> source = NbpObservable.just("a", "b", "cc", "dd", "eee", "fff");

        Supplier<Map<Integer, Collection<String>>> mapFactory = new Supplier<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> get() {
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

        Function<String, String> identity = v -> v;
        
        NbpObservable<Map<Integer, Collection<String>>> mapped = source.toMultimap(
                lengthFunc, identity,
                mapFactory, ArrayList::new);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void testToMultimapWithCollectionFactory() {
        NbpObservable<String> source = NbpObservable.just("cc", "dd", "eee", "eee");

        Function<Integer, Collection<String>> collectionFactory = t1 -> {
            if (t1 == 2) {
                return new ArrayList<>();
            } else {
                return new HashSet<>();
            }
        };

        Function<String, String> identity = v -> v;
        Supplier<Map<Integer, Collection<String>>> mapSupplier = HashMap::new;
        
        NbpObservable<Map<Integer, Collection<String>>> mapped = source
                .toMultimap(lengthFunc, identity, mapSupplier, collectionFactory);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, new HashSet<>(Arrays.asList("eee")));

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onComplete();
    }

    @Test
    public void testToMultimapWithError() {
        NbpObservable<String> source = NbpObservable.just("a", "b", "cc", "dd");

        Function<String, Integer> lengthFuncErr = new Function<String, Integer>() {
            @Override
            public Integer apply(String t1) {
                if ("b".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };

        NbpObservable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFuncErr);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("a", "b"));
        expected.put(2, Arrays.asList("cc", "dd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
    }

    @Test
    public void testToMultimapWithErrorInValueSelector() {
        NbpObservable<String> source = NbpObservable.just("a", "b", "cc", "dd");

        Function<String, String> duplicateErr = new Function<String, String>() {
            @Override
            public String apply(String t1) {
                if ("b".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        NbpObservable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, duplicateErr);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(1, Arrays.asList("aa", "bb"));
        expected.put(2, Arrays.asList("cccc", "dddd"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
    }

    @Test
    public void testToMultimapWithMapThrowingFactory() {
        NbpObservable<String> source = NbpObservable.just("a", "b", "cc", "dd", "eee", "fff");

        Supplier<Map<Integer, Collection<String>>> mapFactory = new Supplier<Map<Integer, Collection<String>>>() {
            @Override
            public Map<Integer, Collection<String>> get() {
                throw new RuntimeException("Forced failure");
            }
        };

        NbpObservable<Map<Integer, Collection<String>>> mapped = source
                .toMultimap(lengthFunc, v -> v, mapFactory);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Arrays.asList("eee", "fff"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
    }

    @Test
    public void testToMultimapWithThrowingCollectionFactory() {
        NbpObservable<String> source = NbpObservable.just("cc", "cc", "eee", "eee");

        Function<Integer, Collection<String>> collectionFactory = t1 -> {
            if (t1 == 2) {
                throw new RuntimeException("Forced failure");
            } else {
                return new HashSet<>();
            }
        };

        Function<String, String> identity = v -> v;
        Supplier<Map<Integer, Collection<String>>> mapSupplier = HashMap::new;
        
        NbpObservable<Map<Integer, Collection<String>>> mapped = source.toMultimap(lengthFunc, 
                identity, mapSupplier, collectionFactory);

        Map<Integer, Collection<String>> expected = new HashMap<>();
        expected.put(2, Arrays.asList("cc", "dd"));
        expected.put(3, Collections.singleton("eee"));

        mapped.subscribe(objectObserver);

        verify(objectObserver, times(1)).onError(any(Throwable.class));
        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onComplete();
    }
}