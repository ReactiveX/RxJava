/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.joins;

import rx.Observable;
import rx.functions.Func3;

/**
 * Represents a join pattern over observable sequences.
 */
public class Pattern3<T1, T2, T3> implements Pattern {
    private final Observable<T1> first;
    private final Observable<T2> second;
    private final Observable<T3> third;

    public Pattern3(Observable<T1> first, Observable<T2> second,
            Observable<T3> third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public Observable<T1> first() {
        return first;
    }

    public Observable<T2> second() {
        return second;
    }

    public Observable<T3> third() {
        return third;
    }

    //    public <T4> Pattern4<T1, T2, T3, T4> and(Observable<T4> other) {
    //        if (other == null) {
    //            throw new NullPointerException();
    //        }
    //        return new Pattern4<T1, T2, T3, T4>(first, second, third, other);
    //    }
    public <R> Plan0<R> then(Func3<T1, T2, T3, R> selector) {
        if (selector == null) {
            throw new NullPointerException();
        }
        return new Plan3<T1, T2, T3, R>(this, selector);
    }
}
