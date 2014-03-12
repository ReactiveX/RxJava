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
import static rx.math.operators.OperationSum.*;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.observables.MathObservable;

public class OperationSumTest {

    @SuppressWarnings("unchecked")
    Observer<Integer> w = mock(Observer.class);
    @SuppressWarnings("unchecked")
    Observer<Long> wl = mock(Observer.class);
    @SuppressWarnings("unchecked")
    Observer<Float> wf = mock(Observer.class);
    @SuppressWarnings("unchecked")
    Observer<Double> wd = mock(Observer.class);

    @Test
    public void testSumOfAFewInts() throws Throwable {
        Observable<Integer> src = Observable.from(1, 2, 3, 4, 5);
        sumIntegers(src).subscribe(w);

        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(15);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testEmptySum() throws Throwable {
        Observable<Integer> src = Observable.empty();
        sumIntegers(src).subscribe(w);

        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(0);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testSumOfAFewLongs() throws Throwable {
        Observable<Long> src = Observable.from(1L, 2L, 3L, 4L, 5L);
        sumLongs(src).subscribe(wl);

        verify(wl, times(1)).onNext(anyLong());
        verify(wl).onNext(15L);
        verify(wl, never()).onError(any(Throwable.class));
        verify(wl, times(1)).onCompleted();
    }

    @Test
    public void testEmptySumLongs() throws Throwable {
        Observable<Long> src = Observable.empty();
        sumLongs(src).subscribe(wl);

        verify(wl, times(1)).onNext(anyLong());
        verify(wl).onNext(0L);
        verify(wl, never()).onError(any(Throwable.class));
        verify(wl, times(1)).onCompleted();
    }

    @Test
    public void testSumOfAFewFloats() throws Throwable {
        Observable<Float> src = Observable.from(1.0f);
        sumFloats(src).subscribe(wf);

        verify(wf, times(1)).onNext(anyFloat());
        verify(wf).onNext(1.0f);
        verify(wf, never()).onError(any(Throwable.class));
        verify(wf, times(1)).onCompleted();
    }

    @Test
    public void testEmptySumFloats() throws Throwable {
        Observable<Float> src = Observable.empty();
        sumFloats(src).subscribe(wf);

        verify(wf, times(1)).onNext(anyFloat());
        verify(wf).onNext(0.0f);
        verify(wf, never()).onError(any(Throwable.class));
        verify(wf, times(1)).onCompleted();
    }

    @Test
    public void testSumOfAFewDoubles() throws Throwable {
        Observable<Double> src = Observable.from(0.0d, 1.0d, 0.5d);
        sumDoubles(src).subscribe(wd);

        verify(wd, times(1)).onNext(anyDouble());
        verify(wd).onNext(1.5d);
        verify(wd, never()).onError(any(Throwable.class));
        verify(wd, times(1)).onCompleted();
    }

    @Test
    public void testEmptySumDoubles() throws Throwable {
        Observable<Double> src = Observable.empty();
        sumDoubles(src).subscribe(wd);

        verify(wd, times(1)).onNext(anyDouble());
        verify(wd).onNext(0.0d);
        verify(wd, never()).onError(any(Throwable.class));
        verify(wd, times(1)).onCompleted();
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
    public void testIntegerSumSelector() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");
        Func1<String, Integer> length = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                return t1.length();
            }
        };

        Observable<Integer> result = MathObservable.from(source).sumInteger(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testValue(o, 10);
    }

    @Test
    public void testLongSumSelector() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");
        Func1<String, Long> length = new Func1<String, Long>() {
            @Override
            public Long call(String t1) {
                return (long) t1.length();
            }
        };

        Observable<Long> result = MathObservable.from(source).sumLong(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testValue(o, 10L);
    }

    @Test
    public void testFloatSumSelector() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");
        Func1<String, Float> length = new Func1<String, Float>() {
            @Override
            public Float call(String t1) {
                return (float) t1.length();
            }
        };

        Observable<Float> result = MathObservable.from(source).sumFloat(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testValue(o, 10f);
    }

    @Test
    public void testDoubleSumSelector() {
        Observable<String> source = Observable.from("a", "bb", "ccc", "dddd");
        Func1<String, Double> length = new Func1<String, Double>() {
            @Override
            public Double call(String t1) {
                return (double) t1.length();
            }
        };

        Observable<Double> result = MathObservable.from(source).sumDouble(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testValue(o, 10d);
    }

    @Test
    public void testIntegerSumSelectorEmpty() {
        Observable<String> source = Observable.empty();
        Func1<String, Integer> length = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                return t1.length();
            }
        };

        Observable<Integer> result = MathObservable.from(source).sumInteger(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, IllegalArgumentException.class);
    }

    @Test
    public void testLongSumSelectorEmpty() {
        Observable<String> source = Observable.empty();
        Func1<String, Long> length = new Func1<String, Long>() {
            @Override
            public Long call(String t1) {
                return (long) t1.length();
            }
        };

        Observable<Long> result = MathObservable.from(source).sumLong(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, IllegalArgumentException.class);
    }

    @Test
    public void testFloatSumSelectorEmpty() {
        Observable<String> source = Observable.empty();
        Func1<String, Float> length = new Func1<String, Float>() {
            @Override
            public Float call(String t1) {
                return (float) t1.length();
            }
        };

        Observable<Float> result = MathObservable.from(source).sumFloat(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, IllegalArgumentException.class);
    }

    @Test
    public void testDoubleSumSelectorEmpty() {
        Observable<String> source = Observable.empty();
        Func1<String, Double> length = new Func1<String, Double>() {
            @Override
            public Double call(String t1) {
                return (double) t1.length();
            }
        };

        Observable<Double> result = MathObservable.from(source).sumDouble(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, IllegalArgumentException.class);
    }

    @Test
    public void testIntegerSumSelectorThrows() {
        Observable<String> source = Observable.from("a");
        Func1<String, Integer> length = new Func1<String, Integer>() {
            @Override
            public Integer call(String t1) {
                throw new CustomException();
            }
        };

        Observable<Integer> result = MathObservable.from(source).sumInteger(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, CustomException.class);
    }

    @Test
    public void testLongSumSelectorThrows() {
        Observable<String> source = Observable.from("a");
        Func1<String, Long> length = new Func1<String, Long>() {
            @Override
            public Long call(String t1) {
                throw new CustomException();
            }
        };

        Observable<Long> result = MathObservable.from(source).sumLong(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, CustomException.class);
    }

    @Test
    public void testFloatSumSelectorThrows() {
        Observable<String> source = Observable.from("a");
        Func1<String, Float> length = new Func1<String, Float>() {
            @Override
            public Float call(String t1) {
                throw new CustomException();
            }
        };

        Observable<Float> result = MathObservable.from(source).sumFloat(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, CustomException.class);
    }

    @Test
    public void testDoubleSumSelectorThrows() {
        Observable<String> source = Observable.from("a");
        Func1<String, Double> length = new Func1<String, Double>() {
            @Override
            public Double call(String t1) {
                throw new CustomException();
            }
        };

        Observable<Double> result = MathObservable.from(source).sumDouble(length);
        Observer<Object> o = mock(Observer.class);
        result.subscribe(o);

        testThrows(o, CustomException.class);
    }

    static class CustomException extends RuntimeException {
    }
}
