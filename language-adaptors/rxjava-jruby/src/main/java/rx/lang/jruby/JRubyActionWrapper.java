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

import rx.functions.Action;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Action3;

/**
 * Concrete wrapper that accepts a {@link RubyProc} and produces any needed Rx {@link Action}.
 *
 * @param <T1>
 * @param <T2>
 * @param <T3>
 * @param <T4>
 */
public class JRubyActionWrapper<T1, T2, T3, T4> implements Action, Action0, Action1<T1>, Action2<T1, T2>, Action3<T1, T2, T3> {

    private final RubyProc proc;
    private final ThreadContext context;
    private final Ruby runtime;

    public JRubyActionWrapper(ThreadContext context, RubyProc proc) {
        this.proc = proc;
        this.context = context;
        this.runtime = context.getRuntime();
    }

    @Override
    public void call() {
        IRubyObject[] array = new IRubyObject[0];
        proc.call(context, array);
    }

    @Override
    public void call(T1 t1) {
        IRubyObject[] array = {JavaUtil.convertJavaToRuby(runtime, t1)};
        proc.call(context, array);
    }

    @Override
    public void call(T1 t1, T2 t2) {
        IRubyObject[] array = {JavaUtil.convertJavaToRuby(runtime, t1),
                               JavaUtil.convertJavaToRuby(runtime, t2)};
        proc.call(context, array);
    }

    @Override
    public void call(T1 t1, T2 t2, T3 t3) {
        IRubyObject[] array = {JavaUtil.convertJavaToRuby(runtime, t1),
                               JavaUtil.convertJavaToRuby(runtime, t2),
                               JavaUtil.convertJavaToRuby(runtime, t3)};
        proc.call(context, array);
    }

}
