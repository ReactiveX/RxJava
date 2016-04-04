/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;
import rx.plugins.RxJavaPlugins;

/**
 * Registers an action to be called after an Observable invokes {@code onComplete} or {@code onError}.
 * <p>
 * <img width="640" src="https://github.com/ReactiveX/RxJava/wiki/images/rx-operators/finallyDo.png" alt="">
 * <p>
 * See also the <a href="http://msdn.microsoft.com/en-us/library/hh212133.aspx">MSDN Observable.Finally
 * method</a>
 * 
 * @param <T> the value type
 */
public final class OperatorDoAfterTerminate<T> implements Operator<T, T> {
    final Action0 action;

    public OperatorDoAfterTerminate(Action0 action) {
        if (action == null) {
            throw new NullPointerException("Action can not be null");
        }
        this.action = action;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new Subscriber<T>(child) {

            @Override
            public void onNext(T t) {
                child.onNext(t);
            }

            @Override
            public void onError(Throwable e) {
                try {
                    child.onError(e);
                } finally {
                    callAction();
                }
            }

            @Override
            public void onCompleted() {
                try {
                    child.onCompleted();
                } finally {
                    callAction();
                }
            }
            
            void callAction() {
                try {
                    action.call();
                } catch (Throwable ex) {
                    Exceptions.throwIfFatal(ex);
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(ex);
                }
            }
        };
    }
    
}
