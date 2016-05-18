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
 * Holds a singleton instance of a never Observable which is stateless doesn't
 * call any of the Subscriber's methods.
 */
public enum NeverObservableHolder implements OnSubscribe<Object> {
    INSTANCE
    ;
    
    /**
     * Returns a type-corrected singleton instance of the never Observable.
     * @param <T> the value type
     * @return a type-corrected singleton instance of the never Observable.
     */
    @SuppressWarnings("unchecked")
    public static <T> Observable<T> instance() {
        return (Observable<T>)NEVER;
    }
    
    /** The singleton instance. */
    static final Observable<Object> NEVER = Observable.create(INSTANCE);
    
    @Override
    public void call(Subscriber<? super Object> child) {
    }
}
