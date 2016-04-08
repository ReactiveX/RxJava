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

import rx.*;
import rx.Observable.OnSubscribe;
import rx.observers.Subscribers;
import rx.plugins.*;
import rx.subscriptions.*;

/**
 * Delays the subscription to the main source until the other
 * observable fires an event or completes.
 * @param <T> the main type
 * @param <U> the other value type, ignored
 */
public final class OnSubscribeDelaySubscriptionOther<T, U> implements OnSubscribe<T> {
    final Observable<? extends T> main;
    final Observable<U> other;
    
    public OnSubscribeDelaySubscriptionOther(Observable<? extends T> main, Observable<U> other) {
        this.main = main;
        this.other = other;
    }
    
    @Override
    public void call(Subscriber<? super T> t) {
        final SerialSubscription serial = new SerialSubscription();
        
        t.add(serial);
        
        final Subscriber<T> child = Subscribers.wrap(t);
        
        
        Subscriber<U> otherSubscriber = new Subscriber<U>() {
            boolean done;
            @Override
            public void onNext(U t) {
                onCompleted();
            }
            
            @Override
            public void onError(Throwable e) {
                if (done) {
                    RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
                    return;
                }
                done = true;
                child.onError(e);
            }
            
            @Override
            public void onCompleted() {
                if (done) {
                    return;
                }
                done = true;
                serial.set(Subscriptions.unsubscribed());
                
                main.unsafeSubscribe(child);
            }
        };
        
        serial.set(otherSubscriber);
        
        other.unsafeSubscribe(otherSubscriber);
    }
}
