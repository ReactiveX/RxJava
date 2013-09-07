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
import rx.util.functions.Func2;

/**
 * A few operators for implementing the sum operation.
 */
public final class OperationSum {
    public static Observable<Integer> sum(Observable<Integer> source) {
        return source.reduce(0, new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer accu, Integer next) {
                return accu + next;
            }
        });
    }

    public static Observable<Long> sumLongs(Observable<Long> source) {
        return source.reduce(0L, new Func2<Long, Long, Long>() {
            @Override
            public Long call(Long accu, Long next) {
                return accu + next;
            }
        });
    }

    public static Observable<Float> sumFloats(Observable<Float> source) {
        return source.reduce(0.0f, new Func2<Float, Float, Float>() {
            @Override
            public Float call(Float accu, Float next) {
                return accu + next;
            }
        });
    }

    public static Observable<Double> sumDoubles(Observable<Double> source) {
        return source.reduce(0.0d, new Func2<Double, Double, Double>() {
            @Override
            public Double call(Double accu, Double next) {
                return accu + next;
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
        public void testSumOfAFewInts() throws Throwable {
            Observable<Integer> src = Observable.from(1, 2, 3, 4, 5);
            sum(src).subscribe(w);

            verify(w, times(1)).onNext(anyInt());
            verify(w).onNext(15);
            verify(w, never()).onError(any(Throwable.class));
            verify(w, times(1)).onCompleted();
        }

        @Test
        public void testEmptySum() throws Throwable {
            Observable<Integer> src = Observable.from();
            sum(src).subscribe(w);

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
            Observable<Long> src = Observable.from();
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
            Observable<Float> src = Observable.from();
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
            Observable<Double> src = Observable.from();
            sumDoubles(src).subscribe(wd);

            verify(wd, times(1)).onNext(anyDouble());
            verify(wd).onNext(0.0d);
            verify(wd, never()).onError(any(Throwable.class));
            verify(wd, times(1)).onCompleted();
        }
    }
}
