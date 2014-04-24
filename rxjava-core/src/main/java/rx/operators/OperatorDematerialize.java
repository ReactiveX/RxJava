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
package rx.operators;

import rx.Notification;
import rx.Observable.Operator;
import rx.Subscriber;

/**
 * Reverses the effect of {@link OperatorMaterialize} by transforming the Notification objects
 * emitted by a source Observable into the items or notifications they represent.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/dematerialize.png">
 * <p>
 * See <a href="http://msdn.microsoft.com/en-us/library/hh229047(v=vs.103).aspx">here</a> for the
 * Microsoft Rx equivalent.
 * 
 * @param <T> the wrapped value type
 */
public final class OperatorDematerialize<T> implements Operator<T, Notification<T>> {

    @Override
    public Subscriber<? super Notification<T>> call(final Subscriber<? super T> child) {
        return new Subscriber<Notification<T>>(child) {
            /** Do not send two onCompleted events. */
            boolean terminated;
            @Override
            public void onNext(Notification<T> t) {
                switch (t.getKind()) {
                case OnNext:
                    if (!terminated) {
                        child.onNext(t.getValue());
                    }
                    break;
                case OnError:
                    onError(t.getThrowable());
                    break;
                case OnCompleted:
                    onCompleted();
                    break;
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!terminated) {
                    terminated = true;
                    child.onError(e);
                }
            }

            @Override
            public void onCompleted() {
                if (!terminated) {
                    terminated = true;
                    child.onCompleted();
                }
            }
            
        };
    }
    
}
