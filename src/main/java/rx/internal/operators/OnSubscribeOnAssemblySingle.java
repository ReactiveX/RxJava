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

/**
 * Captures the current stack when it is instantiated, makes
 * it available through a field and attaches it to all
 * passing exception.
 *
 * @param <T> the value type
 */
public final class OnSubscribeOnAssemblySingle<T> implements Single.OnSubscribe<T> {

    final Single.OnSubscribe<T> source;
    
    final StackTraceElement[] assemblyStacktrace;

    /**
     * If set to true, the creation of PublisherOnAssembly will capture the raw
     * stacktrace instead of the sanitized version.
     */
    public static volatile boolean fullStackTrace;
    
    public OnSubscribeOnAssemblySingle(Single.OnSubscribe<T> source) {
        this.source = source;
        this.assemblyStacktrace = OnSubscribeOnAssembly.assemblyStacktrace();
    }
    
    @Override
    public void call(SingleSubscriber<? super T> t) {
        source.call(new OnAssemblySingleSubscriber<T>(t, assemblyStacktrace));
    }
    
    static final class OnAssemblySingleSubscriber<T> extends SingleSubscriber<T> {

        final SingleSubscriber<? super T> actual;
        
        final StackTraceElement[] assemblyStacktrace;
        
        public OnAssemblySingleSubscriber(SingleSubscriber<? super T> actual, StackTraceElement[] assemblyStacktrace) {
            this.actual = actual;
            this.assemblyStacktrace = assemblyStacktrace;
            actual.add(this);
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(OnSubscribeOnAssembly.addAssembly(e, assemblyStacktrace));
        }

        @Override
        public void onSuccess(T t) {
            actual.onSuccess(t);
        }
        
    }
}
