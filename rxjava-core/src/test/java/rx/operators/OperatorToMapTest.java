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

import java.util.HashMap;
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

public class OperatorToMapTest {
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
    public void testToMap() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");

        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc);

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onCompleted();
    }

    @Test
    public void testToMapWithValueSelector() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");

        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc, duplicate);

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onCompleted();
    }

    @Test
    public void testToMapWithError() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");

        Func1<String, Integer> lengthFuncErr = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced Failure");
                }
                return t1.length();
            }
        };
        Observable<Map<Integer, String>> mapped = source.toMap(lengthFuncErr);

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "a");
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onCompleted();
        verify(objectObserver, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void testToMapWithErrorInValueSelector() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");

        Func1<String, String> duplicateErr = new Func1<String, String>() {
            @Override
            public String call(String t1) {
                if ("bb".equals(t1)) {
                    throw new RuntimeException("Forced failure");
                }
                return t1 + t1;
            }
        };

        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc, duplicateErr);

        Map<Integer, String> expected = new HashMap<Integer, String>();
        expected.put(1, "aa");
        expected.put(2, "bbbb");
        expected.put(3, "cccccc");
        expected.put(4, "dddddddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onCompleted();
        verify(objectObserver, times(1)).onError(any(Throwable.class));

    }

    @Test
    public void testToMapWithFactory() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");

        Func0<Map<Integer, String>> mapFactory = new Func0<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> call() {
                return new LinkedHashMap<Integer, String>() {
                    /** */
                    private static final long serialVersionUID = -3296811238780863394L;

                    @Override
                    protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                        return size() > 3;
                    }
                };
            }
        };

        Func1<String, Integer> lengthFunc = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                return t1.length();
            }
        };
        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc, Functions.<String>identity(), mapFactory);

        Map<Integer, String> expected = new LinkedHashMap<Integer, String>();
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onError(any(Throwable.class));
        verify(objectObserver, times(1)).onNext(expected);
        verify(objectObserver, times(1)).onCompleted();
    }

    @Test
    public void testToMapWithErrorThrowingFactory() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");

        Func0<Map<Integer, String>> mapFactory = new Func0<Map<Integer, String>>() {
            @Override
            public Map<Integer, String> call() {
                throw new RuntimeException("Forced failure");
            }
        };

        Func1<String, Integer> lengthFunc = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                return t1.length();
            }
        };
        Observable<Map<Integer, String>> mapped = source.toMap(lengthFunc, Functions.<String>identity(), mapFactory);

        Map<Integer, String> expected = new LinkedHashMap<Integer, String>();
        expected.put(2, "bb");
        expected.put(3, "ccc");
        expected.put(4, "dddd");

        mapped.subscribe(objectObserver);

        verify(objectObserver, never()).onNext(expected);
        verify(objectObserver, never()).onCompleted();
        verify(objectObserver, times(1)).onError(any(Throwable.class));
    }

}
