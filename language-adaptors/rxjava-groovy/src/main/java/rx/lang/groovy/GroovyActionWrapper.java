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
import rx.functions.Action;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;

/**
 * Concrete wrapper that accepts a {@link Closure} and produces any needed Rx {@link Action}.
 * 
 * @param <T1>
 * @param <T2>
 * @param <T3>
 * @param <T4>
 */
public class GroovyActionWrapper<T1, T2, T3, T4> implements Action, Action0, Action1<T1>, Action2<T1, T2>, Action3<T1, T2, T3> {

    private final Closure<Void> closure;

    public GroovyActionWrapper(Closure<Void> closure) {
        this.closure = closure;
    }

    @Override
    public void call() {
        closure.call();
    }

    @Override
    public void call(T1 t1) {
        closure.call(t1);
    }

    @Override
    public void call(T1 t1, T2 t2) {
        closure.call(t1, t2);
    }

    @Override
    public void call(T1 t1, T2 t2, T3 t3) {
        closure.call(t1, t2, t3);
    }

}