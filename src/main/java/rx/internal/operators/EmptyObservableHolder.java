/**
 * Copyright 2016 Netflix, Inc.
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

package rx.internal.operators;

import rx.*;
import rx.Observable.OnSubscribe;

/**
 * Holds a singleton instance of an empty Observable which is stateless and completes
 * the child subscriber immediately.
 */
public enum EmptyObservableHolder implements OnSubscribe<Object> {
    INSTANCE
    ;
    
    /**
     * Returns a type-corrected singleton instance of the empty Observable.
     * @param <T> the value type
     * @return a type-corrected singleton instance of the empty Observable.
     */
    @SuppressWarnings("unchecked")
    public static <T> Observable<T> instance() {
        return (Observable<T>)EMPTY;
    }
    
    /** The singleton instance. */
    static final Observable<Object> EMPTY = Observable.create(INSTANCE);
    
    @Override
    public void call(Subscriber<? super Object> child) {
        child.onCompleted();
    }
}
