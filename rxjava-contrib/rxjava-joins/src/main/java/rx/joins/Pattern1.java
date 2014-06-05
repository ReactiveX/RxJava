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
import rx.functions.Func1;

/**
 * Represents a join pattern over one observable sequence.
 */
public final class Pattern1<T1> implements Pattern {
    private final Observable<T1> o1;

    public Pattern1(Observable<T1> o1) {
        this.o1 = o1;
    }

    Observable<T1> o1() {
        return o1;
    }

    /**
     * Matches when all observable sequences have an available
     * element and projects the elements by invoking the selector function.
     * 
     * @param selector
     *            the function that will be invoked for elements in the source sequences.
     * @return the plan for the matching
     * @throws NullPointerException
     *             if selector is null
     */
    public <R> Plan0<R> then(Func1<T1, R> selector) {
        if (selector == null) {
            throw new NullPointerException();
        }
        return new Plan1<T1, R>(this, selector);
    }
}
