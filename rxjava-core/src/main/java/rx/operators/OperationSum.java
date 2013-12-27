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
import rx.util.functions.Func2;

/**
 * A few operators for implementing the sum operation.
 * 
 * @see <a href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.sum%28v=vs.103%29.aspx">MSDN: Observable.Sum</a>
 */
public final class OperationSum {
    public static Observable<Integer> sum(IObservable<Integer> isource) {
        final Observable<Integer> source = Observable.from(isource);
        return source.reduce(0, new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer accu, Integer next) {
                return accu + next;
            }
        });
    }

    public static Observable<Long> sumLongs(IObservable<Long> isource) {
        final Observable<Long> source = Observable.from(isource);
        return source.reduce(0L, new Func2<Long, Long, Long>() {
            @Override
            public Long call(Long accu, Long next) {
                return accu + next;
            }
        });
    }

    public static Observable<Float> sumFloats(IObservable<Float> isource) {
        final Observable<Float> source = Observable.from(isource);
        return source.reduce(0.0f, new Func2<Float, Float, Float>() {
            @Override
            public Float call(Float accu, Float next) {
                return accu + next;
            }
        });
    }

    public static Observable<Double> sumDoubles(IObservable<Double> isource) {
        final Observable<Double> source = Observable.from(isource);
        return source.reduce(0.0d, new Func2<Double, Double, Double>() {
            @Override
            public Double call(Double accu, Double next) {
                return accu + next;
            }
        });
    }
}
