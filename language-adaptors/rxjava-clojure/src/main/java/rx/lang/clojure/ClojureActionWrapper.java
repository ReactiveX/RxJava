/**
 * Copyright 2013 Netflix, Inc.
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
package rx.lang.clojure;

import rx.util.functions.Action0;
import rx.util.functions.Action1;

import clojure.lang.IFn;
import clojure.lang.RT;
import clojure.lang.Var;

/**
 * Concrete wrapper that accepts an {@code IFn} and produces any needed Rx {@code Action}.
 * @param <T1>
 */
public class ClojureActionWrapper<T1> extends ClojureArityChecker implements Action0, Action1<T1> {
    public ClojureActionWrapper(IFn ifn) {
        this.ifn = ifn;
    }

    @Override
    public void call() {
        ifn.invoke();
    }

    @Override
    public void call(T1 t1) {
        ifn.invoke(t1);
    }
}