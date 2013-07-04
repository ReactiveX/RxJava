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
package rx.lang.dynamic;

import rx.util.functions.Func0;
import rx.util.functions.Func1;
import rx.util.functions.Func2;
import rx.util.functions.Func3;
import rx.util.functions.Func4;
import rx.util.functions.FunctionLanguageAdaptor;
import rx.util.functions.Functions;

public class DynamicFunctionWrapper<T1, T2, T3, T4, R> implements Func0<R>, Func1<T1, R>, Func2<T1, T2, R>, Func3<T1, T2, T3, R>, Func4<T1, T2, T3, T4, R> {
    private Object object;

    public DynamicFunctionWrapper(Object object) {
        this.object = object;
    }

    @Override
    public R call() {
        return (R) Functions.from(object).call();
    }

    @Override
    public R call(T1 t1) {
        return (R) Functions.from(object).call(t1);
    }

    @Override
    public R call(T1 t1, T2 t2) {
        return (R) Functions.from(object).call(t1, t2);
    }

    @Override
    public R call(T1 t1, T2 t2, T3 t3) {
        return (R) Functions.from(object).call(t1, t2, t3);
    }

    @Override
    public R call(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (R) Functions.from(object).call(t1, t2, t3, t4);
    }
}