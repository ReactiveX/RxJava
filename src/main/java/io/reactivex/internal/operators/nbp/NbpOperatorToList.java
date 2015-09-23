/**
 * Copyright 2015 Netflix, Inc.
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

package io.reactivex.internal.operators.nbp;

import java.util.*;
import java.util.function.Supplier;

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.subscribers.nbp.NbpCancelledSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.plugins.RxJavaPlugins;

public final class NbpOperatorToList<T, U extends Collection<? super T>> implements NbpOperator<U, T> {
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    static final NbpOperatorToList DEFAULT = new NbpOperatorToList(ArrayList::new);
    
    @SuppressWarnings("unchecked")
    public static <T> NbpOperatorToList<T, List<T>> defaultInstance() {
        return DEFAULT;
    }
    
    final Supplier<U> collectionSupplier;
    
    public NbpOperatorToList(Supplier<U> collectionSupplier) {
        this.collectionSupplier = collectionSupplier;
    }
    
    @Override
    public NbpSubscriber<? super T> apply(NbpSubscriber<? super U> t) {
        U coll;
        try {
            coll = collectionSupplier.get();
        } catch (Throwable e) {
            EmptyDisposable.error(e, t);
            return NbpCancelledSubscriber.INSTANCE;
        }
        return new ToListSubscriber<>(t, coll);
    }
    
    static final class ToListSubscriber<T, U extends Collection<? super T>> implements NbpSubscriber<T> {
        U collection;
        final NbpSubscriber<? super U> actual;
        
        Disposable s;
        
        public ToListSubscriber(NbpSubscriber<? super U> actual, U collection) {
            this.actual = actual;
            this.collection = collection;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (SubscriptionHelper.validateDisposable(this.s, s)) {
                s.dispose();
                RxJavaPlugins.onError(new IllegalStateException("Subscription already set!"));
                return;
            }
            this.s = s;
            actual.onSubscribe(s);
        }
        
        @Override
        public void onNext(T t) {
            collection.add(t);
        }
        
        @Override
        public void onError(Throwable t) {
            collection = null;
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            U c = collection;
            collection = null;
            actual.onNext(c);
            actual.onComplete();
        }
    }
}
