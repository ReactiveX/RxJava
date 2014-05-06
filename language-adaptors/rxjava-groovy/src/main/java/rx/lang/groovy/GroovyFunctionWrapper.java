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
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.functions.Func4;
import rx.functions.Func5;
import rx.functions.Func6;
import rx.functions.Func7;
import rx.functions.Func8;
import rx.functions.Func9;
import rx.functions.FuncN;
import rx.functions.Function;

/**
 * Concrete wrapper that accepts a {@link Closure} and produces any needed Rx {@link Function}.
 * 
 * @param <T1>
 * @param <T2>
 * @param <T3>
 * @param <T4>
 * @param <R>
 */
public class GroovyFunctionWrapper<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> implements
        Func0<R>,
        Func1<T1, R>,
        Func2<T1, T2, R>,
        Func3<T1, T2, T3, R>,
        Func4<T1, T2, T3, T4, R>,
        Func5<T1, T2, T3, T4, T5, R>,
        Func6<T1, T2, T3, T4, T5, T6, R>,
        Func7<T1, T2, T3, T4, T5, T6, T7, R>,
        Func8<T1, T2, T3, T4, T5, T6, T7, T8, R>,
        Func9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R>,
        FuncN<R> {

    private final Closure<R> closure;

    public GroovyFunctionWrapper(Closure<R> closure) {
        this.closure = closure;
    }

    @Override
    public R call() {
        return (R) closure.call();
    }

    @Override
    public R call(T1 t1) {
        return (R) closure.call(t1);
    }

    @Override
    public R call(T1 t1, T2 t2) {
        return (R) closure.call(t1, t2);
    }

    @Override
    public R call(T1 t1, T2 t2, T3 t3) {
        return (R) closure.call(t1, t2, t3);
    }

    @Override
    public R call(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (R) closure.call(t1, t2, t3, t4);
    }

    @Override
    public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return (R) closure.call(t1, t2, t3, t4, t5);
    }

    @Override
    public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        return (R) closure.call(t1, t2, t3, t4, t5, t6);
    }

    @Override
    public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
        return (R) closure.call(t1, t2, t3, t4, t5, t6, t7);
    }

    @Override
    public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
        return (R) closure.call(t1, t2, t3, t4, t5, t6, t7, t8);
    }

    @Override
    public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
        return (R) closure.call(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Override
    public R call(Object... args) {
        return (R) closure.call(args);
    }
}