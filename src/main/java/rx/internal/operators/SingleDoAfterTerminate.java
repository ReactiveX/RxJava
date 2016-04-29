/**
 * Copyright 2015 Netflix, Inc.
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
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.internal.util.RxJavaPluginUtils;

/**
 * Execute an action after onSuccess or onError has been delivered.
 *
 * @param <T> the value type
 */
public final class SingleDoAfterTerminate<T> implements Single.OnSubscribe<T> {
    final Single<T> source;
    
    final Action0 action;

    public SingleDoAfterTerminate(Single<T> source, Action0 action) {
        this.source = source;
        this.action = action;
    }
    
    @Override
    public void call(SingleSubscriber<? super T> t) {
        SingleDoAfterTerminateSubscriber<T> parent = new SingleDoAfterTerminateSubscriber<T>(t, action);
        t.add(parent);
        source.subscribe(parent);
    }
    
    static final class SingleDoAfterTerminateSubscriber<T> extends SingleSubscriber<T> {
        final SingleSubscriber<? super T> actual;

        final Action0 action;
        
        public SingleDoAfterTerminateSubscriber(SingleSubscriber<? super T> actual, Action0 action) {
            this.actual = actual;
            this.action = action;
        }
        
        @Override
        public void onSuccess(T value) {
            try {
                actual.onSuccess(value);
            } finally {
                doAction();
            }
        }
        
        @Override
        public void onError(Throwable error) {
            try {
                actual.onError(error);
            } finally {
                doAction();
            }
        }
        
        void doAction() {
            try {
                action.call();
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                RxJavaPluginUtils.handleException(ex);
            }
        }
    }
    
    
}
