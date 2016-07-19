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

import java.util.ArrayList;
import java.util.List;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

/**
 * Captures the current stack when it is instantiated, makes
 * it available through a field and attaches it to all
 * passing exception.
 *
 * @param <T> the value type
 */
public final class OnSubscribeOnAssembly<T> implements OnSubscribe<T> {

    final OnSubscribe<T> source;
    
    final StackTraceElement[] assemblyStacktrace;

    /**
     * If set to true, the creation of PublisherOnAssembly will capture the raw
     * stacktrace instead of the sanitized version.
     */
    public static volatile boolean fullStackTrace;
    
    public OnSubscribeOnAssembly(OnSubscribe<T> source) {
        this.source = source;
        this.assemblyStacktrace = assemblyStacktrace();
    }
    
    static StackTraceElement[] assemblyStacktrace() {
        final StackTraceElement[] in = new Exception().getStackTrace();
        final List<StackTraceElement> out = new ArrayList<StackTraceElement>(in.length);

        for (StackTraceElement e : in) {
            String row = e.toString();
            if (!fullStackTrace) {
                if (e.getLineNumber() <= 1) {
                    continue;
                }
                if (row.contains("RxJavaHooks.")) {
                    continue;
                }
                if (row.contains("OnSubscribeOnAssembly")) {
                    continue;
                }
                if (row.contains(".junit.runner")) {
                    continue;
                }
                if (row.contains(".junit4.runner")) {
                    continue;
                }
                if (row.contains(".junit.internal")) {
                    continue;
                }
                if (row.contains("sun.reflect")) {
                    continue;
                }
                if (row.contains("java.lang.Thread.")) {
                    continue;
                }
                if (row.contains("ThreadPoolExecutor")) {
                    continue;
                }
                if (row.contains("org.apache.catalina.")) {
                    continue;
                }
                if (row.contains("org.apache.tomcat.")) {
                    continue;
                }
            }
            out.add(e);
        }

        return out.toArray(new StackTraceElement[out.size()]);
    }

    static Throwable addAssembly(Throwable original, StackTraceElement[] assemblyStacktrace) {
        final StackTraceElement[] originalStacktrace = original.getStackTrace();
        final StackTraceElement[] resultingStacktrace = new StackTraceElement[originalStacktrace.length + assemblyStacktrace.length];

        System.arraycopy(originalStacktrace, 0, resultingStacktrace, 0, originalStacktrace.length);

        System.arraycopy(assemblyStacktrace, 0, resultingStacktrace,
            originalStacktrace.length, assemblyStacktrace.length);

        original.setStackTrace(resultingStacktrace);

        return original;
    }

    @Override
    public void call(Subscriber<? super T> t) {
        source.call(new OnAssemblySubscriber<T>(t, assemblyStacktrace));
    }
    
    static final class OnAssemblySubscriber<T> extends Subscriber<T> {

        final Subscriber<? super T> actual;
        
        final StackTraceElement[] assemblyStacktrace;
        
        public OnAssemblySubscriber(Subscriber<? super T> actual, StackTraceElement[] assemblyStacktrace) {
            super(actual);
            this.actual = actual;
            this.assemblyStacktrace = assemblyStacktrace;
        }

        @Override
        public void onCompleted() {
            actual.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            actual.onError(addAssembly(e, assemblyStacktrace));
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }
        
    }
}
