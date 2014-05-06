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
package rx.lang.groovy;

import groovy.lang.Closure;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;

/**
 * Concrete wrapper that accepts a {@link Closure} and produces a {@link OnSubscribeFunc}.
 * 
 * @param <T>
 */
public class GroovyOnSubscribeFuncWrapper<T> implements OnSubscribeFunc<T> {

    private final Closure<Subscription> closure;

    public GroovyOnSubscribeFuncWrapper(Closure<Subscription> closure) {
        this.closure = closure;
    }

    @Override
    public Subscription onSubscribe(Observer<? super T> observer) {
        return closure.call(observer);
    }

}