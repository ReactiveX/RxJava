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
package rx.functions;

/**
 * Implements the negation of a predicate.
 * 
 * @param <T>
 *            The type of the single input parameter.
 */
public class Not<T> implements Func1<T, Boolean> {
    private final Func1<? super T, Boolean> predicate;

    /**
     * Constructs a predicate that returns true for each input that the source
     * predicate returns false for and vice versa.
     * 
     * @param predicate
     *            The source predicate to negate.
     */
    public Not(Func1<? super T, Boolean> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Boolean call(T param) {
        return !predicate.call(param);
    }
}
