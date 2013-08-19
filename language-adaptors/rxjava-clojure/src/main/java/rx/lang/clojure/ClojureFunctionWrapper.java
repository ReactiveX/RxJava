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

import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.FunctionLanguageAdaptor;

import clojure.lang.IFn;

/**
 * Concrete wrapper that accepts an {@code IFn} and produces any needed Rx {@code Function}.
 * @param <T1>
 * @param <T2>
 * @param <T3>
 * @param <T4>
 * @param <R>
 */
public class ClojureFunctionWrapper<T1, T2, T3, T4, R> extends ClojureArityChecker implements Func0<R>, Func1<T1, R>, Func2<T1, T2, R>, Func3<T1, T2, T3, R>, Func4<T1, T2, T3, T4, R> {
    public ClojureFunctionWrapper(IFn ifn) {
        this.ifn = ifn;
    }

    @Override
    public R call() {
        return (R) ifn.invoke();
    }

    @Override
    public R call(T1 t1) {
        return (R) ifn.invoke(t1);
    }

    @Override
    public R call(T1 t1, T2 t2) {
        return (R) ifn.invoke(t1, t2);
    }

    @Override
    public R call(T1 t1, T2 t2, T3 t3) {
        return (R) ifn.invoke(t1, t2, t3);
    }

    @Override
    public R call(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (R) ifn.invoke(t1, t2, t3, t4);
    }
}