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

import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.exceptions.AssemblyStackTraceException;

/**
 * Captures the current stack when it is instantiated, makes
 * it available through a field and attaches it to all
 * passing exception.
 *
 * @param <T> the value type
 */
public final class OnSubscribeOnAssembly<T> implements OnSubscribe<T> {

    final OnSubscribe<T> source;

    final String stacktrace;

    /**
     * If set to true, the creation of PublisherOnAssembly will capture the raw
     * stacktrace instead of the sanitized version.
     */
    public static volatile boolean fullStackTrace;

    public OnSubscribeOnAssembly(OnSubscribe<T> source) {
        this.source = source;
        this.stacktrace = createStacktrace();
    }

    static String createStacktrace() {
        StackTraceElement[] stacktraceElements = Thread.currentThread().getStackTrace();

        StringBuilder sb = new StringBuilder("Assembly trace:");

        for (StackTraceElement e : stacktraceElements) {
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
            sb.append("\n at ").append(row);
        }

        return sb.append("\nOriginal exception:").toString();
    }

    @Override
    public void call(Subscriber<? super T> t) {
        source.call(new OnAssemblySubscriber<T>(t, stacktrace));
    }

    static final class OnAssemblySubscriber<T> extends Subscriber<T> {

        final Subscriber<? super T> actual;

        final String stacktrace;

        public OnAssemblySubscriber(Subscriber<? super T> actual, String stacktrace) {
            super(actual);
            this.actual = actual;
            this.stacktrace = stacktrace;
        }

        @Override
        public void onCompleted() {
            actual.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            new AssemblyStackTraceException(stacktrace).attachTo(e);
            actual.onError(e);
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

    }
}
