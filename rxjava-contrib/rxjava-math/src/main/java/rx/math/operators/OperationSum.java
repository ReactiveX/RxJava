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

import rx.Observable;
import rx.functions.Func2;

/**
 * A few operators for implementing the sum operation.
 * 
 * @see <a
 *      href="http://msdn.microsoft.com/en-us/library/system.reactive.linq.observable.sum%28v=vs.103%29.aspx">MSDN:
 *      Observable.Sum</a>
 */
public final class OperationSum {

    public static Observable<Integer> sumIntegers(Observable<Integer> source) {
        return source.reduce(0, ACCUM_INT);
    }

    public static Observable<Long> sumLongs(Observable<Long> source) {
        return source.reduce(0l, ACCUM_LONG);
    }

    public static Observable<Float> sumFloats(Observable<Float> source) {
        return source.reduce(0.0f, ACCUM_FLOAT);
    }

    public static Observable<Double> sumDoubles(Observable<Double> source) {
        return source.reduce(0.0d, ACCUM_DOUBLE);
    }

    public static Observable<Integer> sumAtLeastOneIntegers(Observable<Integer> source) {
        return source.reduce(ACCUM_INT);
    }

    public static Observable<Long> sumAtLeastOneLongs(Observable<Long> source) {
        return source.reduce(ACCUM_LONG);
    }

    public static Observable<Float> sumAtLeastOneFloats(Observable<Float> source) {
        return source.reduce(ACCUM_FLOAT);
    }

    public static Observable<Double> sumAtLeastOneDoubles(Observable<Double> source) {
        return source.reduce(ACCUM_DOUBLE);
    }

    private static final Func2<Integer, Integer, Integer> ACCUM_INT = new Func2<Integer, Integer, Integer>() {
        @Override
        public Integer call(Integer accu, Integer next) {
            return accu + next;
        }
    };

    private static final Func2<Long, Long, Long> ACCUM_LONG = new Func2<Long, Long, Long>() {
        @Override
        public Long call(Long accu, Long next) {
            return accu + next;
        }
    };

    private static final Func2<Float, Float, Float> ACCUM_FLOAT = new Func2<Float, Float, Float>() {
        @Override
        public Float call(Float accu, Float next) {
            return accu + next;
        }
    };

    private static final Func2<Double, Double, Double> ACCUM_DOUBLE = new Func2<Double, Double, Double>() {
        @Override
        public Double call(Double accu, Double next) {
            return accu + next;
        }
    };
}
