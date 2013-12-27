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

import rx.IObservable;
import rx.Observable;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

/**
 * A few operators for implementing the averaging operation.
 * 
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

    public static Observable<Integer> average(IObservable<Integer> isource) {
        final Observable<Integer> source = Observable.from(isource);
        return source.reduce(new Tuple2<Integer>(0, 0), new Func2<Tuple2<Integer>, Integer, Tuple2<Integer>>() {
            @Override
            public Tuple2<Integer> call(Tuple2<Integer> accu, Integer next) {
                return new Tuple2<Integer>(accu.current + next, accu.count + 1);
            }
        }).map(new Func1<Tuple2<Integer>, Integer>() {
            @Override
            public Integer call(Tuple2<Integer> result) {
                if (result.count == 0) {
                    throw new IllegalArgumentException("Sequence contains no elements");
                }
                return result.current / result.count;
            }
        });
    }

    public static Observable<Long> averageLongs(IObservable<Long> isource) {
        final Observable<Long> source = Observable.from(isource);
        return source.reduce(new Tuple2<Long>(0L, 0), new Func2<Tuple2<Long>, Long, Tuple2<Long>>() {
            @Override
            public Tuple2<Long> call(Tuple2<Long> accu, Long next) {
                return new Tuple2<Long>(accu.current + next, accu.count + 1);
            }
        }).map(new Func1<Tuple2<Long>, Long>() {
            @Override
            public Long call(Tuple2<Long> result) {
                if (result.count == 0) {
                    throw new IllegalArgumentException("Sequence contains no elements");
                }
                return result.current / result.count;
            }
        });
    }

    public static Observable<Float> averageFloats(IObservable<Float> isource) {
        final Observable<Float> source = Observable.from(isource);
        return source.reduce(new Tuple2<Float>(0.0f, 0), new Func2<Tuple2<Float>, Float, Tuple2<Float>>() {
            @Override
            public Tuple2<Float> call(Tuple2<Float> accu, Float next) {
                return new Tuple2<Float>(accu.current + next, accu.count + 1);
            }
        }).map(new Func1<Tuple2<Float>, Float>() {
            @Override
            public Float call(Tuple2<Float> result) {
                if (result.count == 0) {
                    throw new IllegalArgumentException("Sequence contains no elements");
                }
                return result.current / result.count;
            }
        });
    }

    public static Observable<Double> averageDoubles(IObservable<Double> isource) {
        final Observable<Double> source = Observable.from(isource);
        return source.reduce(new Tuple2<Double>(0.0d, 0), new Func2<Tuple2<Double>, Double, Tuple2<Double>>() {
            @Override
            public Tuple2<Double> call(Tuple2<Double> accu, Double next) {
                return new Tuple2<Double>(accu.current + next, accu.count + 1);
            }
        }).map(new Func1<Tuple2<Double>, Double>() {
            @Override
            public Double call(Tuple2<Double> result) {
                if (result.count == 0) {
                    throw new IllegalArgumentException("Sequence contains no elements");
                }
                return result.current / result.count;
            }
        });
    }
}
