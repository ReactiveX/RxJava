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
package rx.joins;

import java.util.Map;

import rx.Observable;
import rx.Observer;
import rx.functions.Action1;

/**
 * Represents an execution plan for join patterns.
 */
public abstract class Plan0<R> {
    public abstract ActivePlan0 activate(Map<Object, JoinObserver> externalSubscriptions,
            Observer<R> observer, Action1<ActivePlan0> deactivate);

    @SuppressWarnings("unchecked")
    public static <T> JoinObserver1<T> createObserver(
            Map<Object, JoinObserver> externalSubscriptions,
            Observable<T> observable,
            Action1<Throwable> onError
            ) {
        JoinObserver1<T> observer;
        JoinObserver nonGeneric = externalSubscriptions.get(observable);
        if (nonGeneric == null) {
            observer = new JoinObserver1<T>(observable, onError);
            externalSubscriptions.put(observable, observer);
        } else {
            observer = (JoinObserver1<T>) nonGeneric;
        }
        return observer;
    }
}
