/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */
package io.reactivex.observers;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.CompositeException;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Wraps another Subscriber and ensures all onXXX methods conform the protocol
 * (except the requirement for serialized access).
 *
 * @param <T> the value type
 */
public final class SafeObserver<T> implements Observer<T> {
    /** The actual Subscriber. */
    final Observer<? super T> actual;
    /** The subscription. */
    Disposable subscription;
    /** Indicates a terminal state. */
    boolean done;
    
    public SafeObserver(Observer<? super T> actual) {
        this.actual = actual;
    }
    
    @Override
    public void onSubscribe(Disposable s) {
        if (done) {
            return;
        }
        if (this.subscription != null) {
            Throwable ise = new IllegalStateException("Disposable already set!");
            try {
                s.dispose();
            } catch (Throwable e) {
                ise = new CompositeException(e, ise);
            }
            onError(ise);
            return;
        }
        if (s == null) {
            subscription = EmptyDisposable.INSTANCE;
            onError(new NullPointerException("Subscription is null!"));
            return;
        }
        this.subscription = s;
        try {
            actual.onSubscribe(s);
        } catch (Throwable e) {
            done = true;
            // can't call onError because the actual's state may be corrupt at this point
            try {
                s.dispose();
            } catch (Throwable e1) {
                RxJavaPlugins.onError(e1);
            }
            RxJavaPlugins.onError(e);
        }
    }
    
    @Override
    public void onNext(T t) {
        if (done) {
            return;
        }
        if (t == null) {
            onError(new NullPointerException());
            return;
        }
        if (subscription == null) {
            onError(null); // null is okay here, onError checks for subscription == null first
            return;
        }
        try {
            actual.onNext(t);
        } catch (Throwable e) {
            onError(e);
        }
    }
    
    @Override
    public void onError(Throwable t) {
        if (done) {
            return;
        }
        done = true;
        
        if (subscription == null) {
            CompositeException t2 = new CompositeException(t, new NullPointerException("Subscription not set!"));
            
            try {
                actual.onSubscribe(EmptyDisposable.INSTANCE);
            } catch (Throwable e) {
                // can't call onError because the actual's state may be corrupt at this point
                t2.suppress(e);
                
                RxJavaPlugins.onError(t2);
                return;
            }
            try {
                actual.onError(t2);
            } catch (Throwable e) {
                // if onError failed, all that's left is to report the error to plugins
                t2.suppress(e);
                
                RxJavaPlugins.onError(t2);
            }
            return;
        }
        
        CompositeException t2 = null;
        if (t == null) {
            t = new NullPointerException();
        }

        try {
            subscription.dispose();
        } catch (Throwable e) {
            t2 = new CompositeException(e, t);
        }

        try {
            if (t2 != null) {
                actual.onError(t2);
            } else {
                actual.onError(t);
            }
        } catch (Throwable e) {
            t2.suppress(e);
            
            RxJavaPlugins.onError(t2);
        }
    }
    
    @Override
    public void onComplete() {
        if (done) {
            return;
        }
        if (subscription == null) {
            onError(null); // null is okay here, onError checks for subscription == null first
            return;
        }

        done = true;

        try {
            subscription.dispose();
        } catch (Throwable e) {
            try {
                actual.onError(e);
            } catch (Throwable e1) {
                RxJavaPlugins.onError(new CompositeException(e1, e));
            }
            return;
        }
        
        try {
            actual.onComplete();
        } catch (Throwable e) {
            RxJavaPlugins.onError(e);
        }
    }
    
    /* test */ Observer<? super T> actual() {
        return actual;
    }
}
