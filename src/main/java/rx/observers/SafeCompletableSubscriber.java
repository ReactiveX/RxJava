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
package rx.observers;

import rx.Completable.CompletableSubscriber;
import rx.Subscription;
import rx.annotations.Experimental;
import rx.exceptions.*;
import rx.internal.util.RxJavaPluginUtils;

/**
 * Wraps another CompletableSubscriber and handles exceptions thrown
 * from onError and onCompleted.
 * 
 * @since (if this graduates from Experimental/Beta to supported, replace this parenthetical with the release number)
 */
@Experimental
public final class SafeCompletableSubscriber implements CompletableSubscriber, Subscription {
    final CompletableSubscriber actual;

    Subscription s;
    
    boolean done;
    
    public SafeCompletableSubscriber(CompletableSubscriber actual) {
        this.actual = actual;
    }

    @Override
    public void onCompleted() {
        if (done) {
            return;
        }
        done = true;
        try {
            actual.onCompleted();
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            
            throw new OnCompletedFailedException(ex);
        }
    }

    @Override
    public void onError(Throwable e) {
        RxJavaPluginUtils.handleException(e);
        if (done) {
            return;
        }
        done = true;
        try {
            actual.onError(e);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            
            throw new OnErrorFailedException(new CompositeException(e, ex));
        }
    }

    @Override
    public void onSubscribe(Subscription d) {
        this.s = d;
        try {
            actual.onSubscribe(this);
        } catch (Throwable ex) {
            Exceptions.throwIfFatal(ex);
            d.unsubscribe();
            onError(ex);
        }
    }
    
    @Override
    public void unsubscribe() {
        s.unsubscribe();
    }
    
    @Override
    public boolean isUnsubscribed() {
        return done || s.isUnsubscribed();
    }
}
