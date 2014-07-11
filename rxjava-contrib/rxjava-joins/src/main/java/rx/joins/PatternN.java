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

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.FuncN;

/**
 * Represents a join pattern over observable sequences.
 */
public final class PatternN implements Pattern {
    private final List<Observable<? extends Object>> observables;

    public PatternN(List<Observable<? extends Object>> observables) {
    	this.observables = observables;
    }

    public PatternN(List<Observable<? extends Object>> observables, Observable<? extends Object> other) {
    	this.observables = new ArrayList<Observable<? extends Object>>(observables);
    	this.observables.add(other);
    }

    /**
     * @return the number of observables in this pattern.
     */
    int size() {
    	return observables.size();
    }
    /**
     * Returns the specific Observable from this pattern.
     * @param index the index
     * @return the observable
     */
    Observable<? extends Object> get(int index) {
    	return observables.get(index);
    }

    /**
     * Creates a pattern that matches when all previous observable sequences have an available element.
     * 
     * @param other
     *            Observable sequence to match with the previous sequences.
     * @return Pattern object that matches when all observable sequences have an available element.
     */
    public PatternN and(Observable<? extends Object> other) {
        if (other == null) {
            throw new NullPointerException();
        }
        return new PatternN(observables, other);
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
    public <R> Plan0<R> then(FuncN<R> selector) {
        if (selector == null) {
            throw new NullPointerException();
        }
        return new PlanN<R>(this, selector);
    }
}
