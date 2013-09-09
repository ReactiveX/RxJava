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

import static org.mockito.Mockito.*;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * A few operators for implementing the averaging operation.
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.average%28v=vs.103%29.aspx">MSDN: Observable.Average</a>
 */
public final class OperationAverage {
    private static final class Tuple2<T> {
        private final T current;
        private final Integer count;
        
        private Tuple2(T v1, Integer v2) {
            current = v1;
            count = v2;
        }
    }
    
    public static Observable<Integer> average(Observable<Integer> source) {
        return source.reduce(new Tuple2<Integer>(0, 0), new Func2<Tuple2<Integer>, Integer, Tuple2<Integer>>() {
            @Override
            public Tuple2<Integer> call(Tuple2<Integer> accu, Integer next) {
                return new Tuple2<Integer>(accu.current + next, accu.count + 1);
            }
        }).map(new Func1<Tuple2<Integer>, Integer>() {
            @Override
            public Integer call(Tuple2<Integer> result) {
                return result.current / result.count; // may throw DivisionByZero, this should be correct...
            }
        });
    }

    public static Observable<Long> averageLongs(Observable<Long> source) {
        return source.reduce(new Tuple2<Long>(0L, 0), new Func2<Tuple2<Long>, Long, Tuple2<Long>>() {
            @Override
            public Tuple2<Long> call(Tuple2<Long> accu, Long next) {
                return new Tuple2<Long>(accu.current + next, accu.count + 1);
            }
        }).map(new Func1<Tuple2<Long>, Long>() {
            @Override
            public Long call(Tuple2<Long> result) {
                return result.current / result.count; // may throw DivisionByZero, this should be correct...
            }
        });
    }

    public static Observable<Float> averageFloats(Observable<Float> source) {
        return source.reduce(new Tuple2<Float>(0.0f, 0), new Func2<Tuple2<Float>, Float, Tuple2<Float>>() {
            @Override
            public Tuple2<Float> call(Tuple2<Float> accu, Float next) {
                return new Tuple2<Float>(accu.current + next, accu.count + 1);
            }
        }).map(new Func1<Tuple2<Float>, Float>() {
            @Override
            public Float call(Tuple2<Float> result) {
                if (result.count == 0) {
                    throw new ArithmeticException("divide by zero");
                }
                return result.current / result.count;
            }
        });
    }

    public static Observable<Double> averageDoubles(Observable<Double> source) {
        return source.reduce(new Tuple2<Double>(0.0d, 0), new Func2<Tuple2<Double>, Double, Tuple2<Double>>() {
            @Override
            public Tuple2<Double> call(Tuple2<Double> accu, Double next) {
                return new Tuple2<Double>(accu.current + next, accu.count + 1);
            }
        }).map(new Func1<Tuple2<Double>, Double>() {
            @Override
            public Double call(Tuple2<Double> result) {
                if (result.count == 0) {
                    throw new ArithmeticException("divide by zero");
                }
                return result.current / result.count;
            }
        });
    }

    public static class UnitTest {
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
            Observable<Integer> src = Observable.from();
            average(src).subscribe(w);

            verify(w, never()).onNext(anyInt());
            verify(w, times(1)).onError(any(ArithmeticException.class));
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
            Observable<Long> src = Observable.from();
            averageLongs(src).subscribe(wl);

            verify(wl, never()).onNext(anyLong());
            verify(wl, times(1)).onError(any(ArithmeticException.class));
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
            Observable<Float> src = Observable.from();
            averageFloats(src).subscribe(wf);

            verify(wf, never()).onNext(anyFloat());
            verify(wf, times(1)).onError(any(ArithmeticException.class));
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
            Observable<Double> src = Observable.from();
            averageDoubles(src).subscribe(wd);

            verify(wd, never()).onNext(anyDouble());
            verify(wd, times(1)).onError(any(ArithmeticException.class));
            verify(wd, never()).onCompleted();
        }
    }
}
