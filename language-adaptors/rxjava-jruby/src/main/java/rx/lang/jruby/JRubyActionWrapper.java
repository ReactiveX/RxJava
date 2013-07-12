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
package rx.lang.jruby;

import org.jruby.Ruby;
import org.jruby.RubyProc;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

import rx.util.functions.Action0;
import rx.util.functions.Action1;
import rx.util.functions.FunctionLanguageAdaptor;

public class JRubyActionWrapper<T1> implements Action0, Action1<T1> {
    private RubyProc proc;
    
    public JRubyActionWrapper(RubyProc proc) {
        this.proc = proc;
    }
    
    @Override
        public void call() {
        Ruby ruby = proc.getRuntime();
        IRubyObject[] rubyArgs = new IRubyObject[0];
        proc.getBlock().call(ruby.getCurrentContext(), rubyArgs);
    }

    @Override
        public void call(T1 t1) {
        Ruby ruby = proc.getRuntime();
        IRubyObject[] rubyArgs = new IRubyObject[1];
        rubyArgs[0] = JavaEmbedUtils.javaToRuby(ruby, t1);
        proc.getBlock().call(ruby.getCurrentContext(), rubyArgs);
    }
}