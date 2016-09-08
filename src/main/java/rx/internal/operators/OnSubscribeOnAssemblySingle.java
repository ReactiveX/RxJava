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
import rx.exceptions.AssemblyStackTraceException;

/**
 * Captures the current stack when it is instantiated, makes
 * it available through a field and attaches it to all
 * passing exception.
 *
 * @param <T> the value type
 */
public final class OnSubscribeOnAssemblySingle<T> implements Single.OnSubscribe<T> {

    final Single.OnSubscribe<T> source;

    final String stacktrace;

    /**
     * If set to true, the creation of PublisherOnAssembly will capture the raw
     * stacktrace instead of the sanitized version.
     */
    public static volatile boolean fullStackTrace;

    public OnSubscribeOnAssemblySingle(Single.OnSubscribe<T> source) {
        this.source = source;
        this.stacktrace = OnSubscribeOnAssembly.createStacktrace();
    }

    @Override
    public void call(SingleSubscriber<? super T> t) {
        source.call(new OnAssemblySingleSubscriber<T>(t, stacktrace));
    }

    static final class OnAssemblySingleSubscriber<T> extends SingleSubscriber<T> {

        final SingleSubscriber<? super T> actual;

        final String stacktrace;

        public OnAssemblySingleSubscriber(SingleSubscriber<? super T> actual, String stacktrace) {
            this.actual = actual;
            this.stacktrace = stacktrace;
            actual.add(this);
        }

        @Override
        public void onError(Throwable e) {
            new AssemblyStackTraceException(stacktrace).attachTo(e);
            actual.onError(e);
        }

        @Override
        public void onSuccess(T t) {
            actual.onSuccess(t);
        }

    }
}
