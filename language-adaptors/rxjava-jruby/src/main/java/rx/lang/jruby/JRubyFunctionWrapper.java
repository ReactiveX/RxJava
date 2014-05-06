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
package rx.lang.jruby;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

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
 * Concrete wrapper that accepts a {@link RubyProc} and produces any needed Rx {@link Function}.
 *
 * @param <T1>
 * @param <T2>
 * @param <T3>
 * @param <T4>
 * @param <T5>
 * @param <T6>
 * @param <T7>
 * @param <T8>
 * @param <T9>
 * @param <R>
 */
public class JRubyFunctionWrapper<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> implements
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

    private final RubyProc proc;
    private final ThreadContext context;
    private final Ruby runtime;

    public JRubyFunctionWrapper(ThreadContext context, RubyProc proc) {
        this.proc = proc;
        this.context = context;
        this.runtime = context.getRuntime();
    }

    @Override
    @SuppressWarnings("unchecked")
    public R call() {
        IRubyObject[] array = new IRubyObject[0];
        return (R) proc.call(context, array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R call(T1 t1) {
        IRubyObject[] array = {JavaUtil.convertJavaToRuby(runtime, t1)};
        return (R) proc.call(context, array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R call(T1 t1, T2 t2) {
        IRubyObject[] array = {JavaUtil.convertJavaToRuby(runtime, t1),
                               JavaUtil.convertJavaToRuby(runtime, t2)};
        return (R) proc.call(context, array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R call(T1 t1, T2 t2, T3 t3) {
        IRubyObject[] array = {JavaUtil.convertJavaToRuby(runtime, t1),
                               JavaUtil.convertJavaToRuby(runtime, t2),
                               JavaUtil.convertJavaToRuby(runtime, t3)};
        return (R) proc.call(context, array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R call(T1 t1, T2 t2, T3 t3, T4 t4) {
        IRubyObject[] array = {JavaUtil.convertJavaToRuby(runtime, t1),
                               JavaUtil.convertJavaToRuby(runtime, t2),
                               JavaUtil.convertJavaToRuby(runtime, t3),
                               JavaUtil.convertJavaToRuby(runtime, t4)};
        return (R) proc.call(context, array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        IRubyObject[] array = {JavaUtil.convertJavaToRuby(runtime, t1),
                               JavaUtil.convertJavaToRuby(runtime, t2),
                               JavaUtil.convertJavaToRuby(runtime, t3),
                               JavaUtil.convertJavaToRuby(runtime, t4),
                               JavaUtil.convertJavaToRuby(runtime, t5)};
        return (R) proc.call(context, array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        IRubyObject[] array = {JavaUtil.convertJavaToRuby(runtime, t1),
                               JavaUtil.convertJavaToRuby(runtime, t2),
                               JavaUtil.convertJavaToRuby(runtime, t3),
                               JavaUtil.convertJavaToRuby(runtime, t4),
                               JavaUtil.convertJavaToRuby(runtime, t5),
                               JavaUtil.convertJavaToRuby(runtime, t6)};
        return (R) proc.call(context, array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
        IRubyObject[] array = {JavaUtil.convertJavaToRuby(runtime, t1),
                               JavaUtil.convertJavaToRuby(runtime, t2),
                               JavaUtil.convertJavaToRuby(runtime, t3),
                               JavaUtil.convertJavaToRuby(runtime, t4),
                               JavaUtil.convertJavaToRuby(runtime, t5),
                               JavaUtil.convertJavaToRuby(runtime, t6),
                               JavaUtil.convertJavaToRuby(runtime, t7)};
        return (R) proc.call(context, array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
        IRubyObject[] array = {JavaUtil.convertJavaToRuby(runtime, t1),
                               JavaUtil.convertJavaToRuby(runtime, t2),
                               JavaUtil.convertJavaToRuby(runtime, t3),
                               JavaUtil.convertJavaToRuby(runtime, t4),
                               JavaUtil.convertJavaToRuby(runtime, t5),
                               JavaUtil.convertJavaToRuby(runtime, t6),
                               JavaUtil.convertJavaToRuby(runtime, t7),
                               JavaUtil.convertJavaToRuby(runtime, t8)};
        return (R) proc.call(context, array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R call(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
        IRubyObject[] array = {JavaUtil.convertJavaToRuby(runtime, t1),
                               JavaUtil.convertJavaToRuby(runtime, t2),
                               JavaUtil.convertJavaToRuby(runtime, t3),
                               JavaUtil.convertJavaToRuby(runtime, t4),
                               JavaUtil.convertJavaToRuby(runtime, t5),
                               JavaUtil.convertJavaToRuby(runtime, t6),
                               JavaUtil.convertJavaToRuby(runtime, t7),
                               JavaUtil.convertJavaToRuby(runtime, t8),
                               JavaUtil.convertJavaToRuby(runtime, t9)};
        return (R) proc.call(context, array);
    }

    @Override
    @SuppressWarnings("unchecked")
    public R call(Object... args) {
        IRubyObject[] array = new IRubyObject[args.length];
        for (int i = 0; i < args.length; i++) {
          array[i] = JavaUtil.convertJavaToRuby(runtime, args[i]);
        }
        return (R) proc.call(context, array);
    }
}
