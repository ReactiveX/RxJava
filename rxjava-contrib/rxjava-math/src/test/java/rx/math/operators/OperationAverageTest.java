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
package rx.math.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.math.operators.OperationAverage.*;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.observables.MathObservable;

public class OperationAverageTest {

    @SuppressWarnings("unchecked")
    Observer<Integer> w = mock(Observer.class);
    @SuppressWarnings("unchecked")
    Observer<Long> wl = mock(Observer.class);
    @SuppressWarnings("unchecked")
    Observer<Float> wf = mock(Observer.class);
    @SuppressWarnings("unchecked")
    Observer<Double> wd = mock(Observer.class);

    @Test
    public void testAverageOfAFewInts() throws Throwable {
        Observable<Integer> src = Observable.from(1, 2, 3, 4, 6);
        average(src).subscribe(w);

        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(3);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testEmptyAverage() throws Throwable {
        Observable<Integer> src = Observable.empty();
        average(src).subscribe(w);

        verify(w, never()).onNext(anyInt());
        verify(w, times(1)).onError(isA(IllegalArgumentException.class));
        verify(w, never()).onCompleted();
    }

    @Test
    public void testAverageOfAFewLongs() throws Throwable {
        Observable<Long> src = Observable.from(1L, 2L, 3L, 4L, 6L);
        averageLongs(src).subscribe(wl);

        verify(wl, times(1)).onNext(anyLong());
        verify(wl).onNext(3L);
        verify(wl, never()).onError(any(Throwable.class));
        verify(wl, times(1)).onCompleted();
    }

    @Test
    public void testEmptyAverageLongs() throws Throwable {
        Observable<Long> src = Observable.empty();
        averageLongs(src).subscribe(wl);

        verify(wl, never()).onNext(anyLong());
        verify(wl, times(1)).onError(isA(IllegalArgumentException.class));
        verify(wl, never()).onCompleted();
    }

    @Test
    public void testAverageOfAFewFloats() throws Throwable {
        Observable<Float> src = Observable.from(1.0f, 2.0f);
        averageFloats(src).subscribe(wf);

        verify(wf, times(1)).onNext(anyFloat());
        verify(wf).onNext(1.5f);
        verify(wf, never()).onError(any(Throwable.class));
        verify(wf, times(1)).onCompleted();
    }

    @Test
    public void testEmptyAverageFloats() throws Throwable {
        Observable<Float> src = Observable.empty();
        averageFloats(src).subscribe(wf);

        verify(wf, never()).onNext(anyFloat());
        verify(wf, times(1)).onError(isA(IllegalArgumentException.class));
        verify(wf, never()).onCompleted();
    }

    @Test
    public void testAverageOfAFewDoubles() throws Throwable {
        Observable<Double> src = Observable.from(1.0d, 2.0d);
        averageDoubles(src).subscribe(wd);

        verify(wd, times(1)).onNext(anyDouble());
        verify(wd).onNext(1.5d);
        verify(wd, never()).onError(any(Throwable.class));
        verify(wd, times(1)).onCompleted();
    }

    @Test
    public void testEmptyAverageDoubles() throws Throwable {
        Observable<Double> src = Observable.empty();
        averageDoubles(src).subscribe(wd);

        verify(wd, never()).onNext(anyDouble());
        verify(wd, times(1)).onError(isA(IllegalArgumentException.class));
        verify(wd, never()).onCompleted();
    }

    void testThrows(Observer<Object> o, Class<? extends Throwable> errorClass) {
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();
        verify(o, times(1)).onError(any(errorClass));
    }

    <N extends Number> void testValue(Observer<Object> o, N value) {
        verify(o, times(1)).onNext(value);
        verify(o, times(1)).onCompleted();
        verify(o, never()).onError(any(Throwable.class));
    }

    @Test
    public void testIntegerAverageSelector() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");
        Func1<String, Integer> length = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                return t1.length();
            }
        };

        Observable<Integer> result = MathObservable.from(source).averageInteger(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testValue(o, 2);
    }

    @Test
    public void testLongAverageSelector() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");
        Func1<String, Long> length = new Func1<String, Long>() {
            @Override
            public Long call(String t1) {
                return (long) t1.length();
            }
        };

        Observable<Long> result = MathObservable.from(source).averageLong(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testValue(o, 2L);
    }

    @Test
    public void testFloatAverageSelector() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");
        Func1<String, Float> length = new Func1<String, Float>() {
            @Override
            public Float call(String t1) {
                return (float) t1.length();
            }
        };

        Observable<Float> result = MathObservable.from(source).averageFloat(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testValue(o, 2.5f);
    }

    @Test
    public void testDoubleAverageSelector() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");
        Func1<String, Double> length = new Func1<String, Double>() {
            @Override
            public Double call(String t1) {
                return (double) t1.length();
            }
        };

        Observable<Double> result = MathObservable.from(source).averageDouble(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testValue(o, 2.5d);
    }

    @Test
    public void testIntegerAverageSelectorEmpty() {
        Observable<String> source = Observable.empty();
        Func1<String, Integer> length = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                return t1.length();
            }
        };

        Observable<Integer> result = MathObservable.from(source).averageInteger(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, IllegalArgumentException.class);
    }

    @Test
    public void testLongAverageSelectorEmpty() {
        Observable<String> source = Observable.empty();
        Func1<String, Long> length = new Func1<String, Long>() {
            @Override
            public Long call(String t1) {
                return (long) t1.length();
            }
        };

        Observable<Long> result = MathObservable.from(source).averageLong(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, IllegalArgumentException.class);
    }

    @Test
    public void testFloatAverageSelectorEmpty() {
        Observable<String> source = Observable.empty();
        Func1<String, Float> length = new Func1<String, Float>() {
            @Override
            public Float call(String t1) {
                return (float) t1.length();
            }
        };

        Observable<Float> result = MathObservable.from(source).averageFloat(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, IllegalArgumentException.class);
    }

    @Test
    public void testDoubleAverageSelectorEmpty() {
        Observable<String> source = Observable.empty();
        Func1<String, Double> length = new Func1<String, Double>() {
            @Override
            public Double call(String t1) {
                return (double) t1.length();
            }
        };

        Observable<Double> result = MathObservable.from(source).averageDouble(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, IllegalArgumentException.class);
    }

    @Test
    public void testIntegerAverageSelectorThrows() {
        Observable<String> source = Observable.from("a");
        Func1<String, Integer> length = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                throw new CustomException();
            }
        };

        Observable<Integer> result = MathObservable.from(source).averageInteger(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, CustomException.class);
    }

    @Test
    public void testLongAverageSelectorThrows() {
        Observable<String> source = Observable.from("a");
        Func1<String, Long> length = new Func1<String, Long>() {
            @Override
            public Long call(String t1) {
                throw new CustomException();
            }
        };

        Observable<Long> result = MathObservable.from(source).averageLong(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, CustomException.class);
    }

    @Test
    public void testFloatAverageSelectorThrows() {
        Observable<String> source = Observable.from("a");
        Func1<String, Float> length = new Func1<String, Float>() {
            @Override
            public Float call(String t1) {
                throw new CustomException();
            }
        };

        Observable<Float> result = MathObservable.from(source).averageFloat(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, CustomException.class);
    }

    @Test
    public void testDoubleAverageSelectorThrows() {
        Observable<String> source = Observable.from("a");
        Func1<String, Double> length = new Func1<String, Double>() {
            @Override
            public Double call(String t1) {
                throw new CustomException();
            }
        };

        Observable<Double> result = MathObservable.from(source).averageDouble(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, CustomException.class);
    }
    
    static class CustomException extends RuntimeException {
    }
}
