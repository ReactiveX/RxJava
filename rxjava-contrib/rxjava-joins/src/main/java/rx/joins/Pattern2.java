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
import rx.functions.Func2;

/**
 * Represents a join pattern over observable sequences.
 */
public class Pattern2<T1, T2> implements Pattern {
    private final Observable<T1> first;
    private final Observable<T2> second;

    public Pattern2(Observable<T1> first, Observable<T2> second) {
        this.first = first;
        this.second = second;
    }

    public Observable<T1> first() {
        return first;
    }

    public Observable<T2> second() {
        return second;
    }

    /**
     * Creates a pattern that matches when all three observable sequences have an available element.
     * 
     * @param other
     *            Observable sequence to match with the two previous sequences.
     * @return Pattern object that matches when all observable sequences have an available element.
     */
    public <T3> Pattern3<T1, T2, T3> and(Observable<T3> other) {
        if (other == null) {
            throw new NullPointerException();
        }
        return new Pattern3<T1, T2, T3>(first, second, other);
    }

    public <R> Plan0<R> then(Func2<T1, T2, R> selector) {
        if (selector == null) {
            throw new NullPointerException();
        }
        return new Plan2<T1, T2, R>(this, selector);
    }
}
