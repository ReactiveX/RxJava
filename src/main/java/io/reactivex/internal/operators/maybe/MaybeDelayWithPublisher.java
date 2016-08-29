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

package io.reactivex.internal.operators.maybe;

import java.util.concurrent.atomic.AtomicReference;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;
import io.reactivex.internal.operators.maybe.MaybeDelayWithCompletable.DelayWithMainObserver;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class MaybeDelayWithPublisher<T, U> extends Maybe<T> {

    final MaybeSource<T> source;
    
    final Publisher<U> other;
    
    public MaybeDelayWithPublisher(MaybeSource<T> source, Publisher<U> other) {
        this.source = source;
        this.other = other;
    }
    
    @Override
    protected void subscribeActual(MaybeObserver<? super T> subscriber) {
        other.subscribe(new OtherSubscriber<T, U>(subscriber, source));
    }
    
    static final class OtherSubscriber<T, U> 
    extends AtomicReference<Disposable>
    implements Subscriber<U>, Disposable {
        
        /** */
        private static final long serialVersionUID = -8565274649390031272L;

        final MaybeObserver<? super T> actual;
        
        final MaybeSource<T> source;

        boolean done;
        
        Subscription s;
        
        public OtherSubscriber(MaybeObserver<? super T> actual, MaybeSource<T> source) {
            this.actual = actual;
            this.source = source;
        }
        
        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.validate(this.s, s)) {
                this.s = s;
                
                actual.onSubscribe(this);
                
                s.request(Long.MAX_VALUE);
            }
        }
        
        @Override
        public void onNext(U value) {
            get().dispose();
            onComplete();
        }
        
        @Override
        public void onError(Throwable e) {
            if (done) {
                RxJavaPlugins.onError(e);
                return;
            }
            done = true;
            actual.onError(e);
        }
        
        @Override
        public void onComplete() {
            if (done) {
                return;
            }
            done = true;
            source.subscribe(new DelayWithMainObserver<T>(this, actual));
        }
        
        @Override
        public void dispose() {
            s.cancel();
            DisposableHelper.dispose(this);
        }
        
        @Override
        public boolean isDisposed() {
            return DisposableHelper.isDisposed(get());
        }
    }
}
