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
package rx.operators;

import java.util.Map;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func0;

/**
 * Select an observable from a map based on a case key returned by a selector
 * function when an observer subscribes.
 * 
 * @param <K>
 *            the case key type
 * @param <R>
 *            the result value type
 */
public final class OperatorSwitchCase<K, R> implements OnSubscribe<R> {
    final Func0<? extends K> caseSelector;
    final Map<? super K, ? extends Observable<? extends R>> mapOfCases;
    final Observable<? extends R> defaultCase;

    public OperatorSwitchCase(Func0<? extends K> caseSelector,
            Map<? super K, ? extends Observable<? extends R>> mapOfCases,
            Observable<? extends R> defaultCase) {
        this.caseSelector = caseSelector;
        this.mapOfCases = mapOfCases;
        this.defaultCase = defaultCase;
    }

    @Override
    public void call(Subscriber<? super R> t1) {
        Observable<? extends R> target;
        try {
            K caseKey = caseSelector.call();
            if (mapOfCases.containsKey(caseKey)) {
                target = mapOfCases.get(caseKey);
            } else {
                target = defaultCase;
            }
        } catch (Throwable t) {
            t1.onError(t);
            return;
        }
        target.subscribe(t1);
    }
}