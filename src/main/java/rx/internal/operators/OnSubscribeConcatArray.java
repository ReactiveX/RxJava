/**
 * Copyright 2016 Netflix, Inc.
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
import rx.functions.Func1;
import rx.internal.operators.OnSubscribeConcatMap.ConcatMapSubscriber;

public final class OnSubscribeConcatArray<T> implements OnSubscribe<T>, Func1<Observable<T>, Observable<T>> {
    final Observable<T>[] sources;
    
    public OnSubscribeConcatArray(Observable<T>[] sources) {
        this.sources = sources;
    }

    @SuppressWarnings("unchecked")
    public OnSubscribeConcatArray<T> startWith(Observable<? extends T> source) {
        Observable<T>[] oldSources = sources;
        
        int oldLen = oldSources.length;
        Observable<T>[] newSources = new Observable[oldLen + 1];
        newSources[0] = (Observable<T>)source;
        System.arraycopy(oldSources, 0, newSources, 1, oldLen);
        
        return new OnSubscribeConcatArray<T>(newSources);
    }

    @SuppressWarnings("unchecked")
    public OnSubscribeConcatArray<T> endWith(Observable<? extends T> source) {
        Observable<T>[] oldSources = sources;
        
        int oldLen = oldSources.length;
        Observable<T>[] newSources = new Observable[oldLen + 1];
        System.arraycopy(oldSources, 0, newSources, 0, oldLen);
        newSources[oldLen] = (Observable<T>)source;
        
        return new OnSubscribeConcatArray<T>(newSources);
    }
    
    @Override
    public void call(final Subscriber<? super T> s) {
        ConcatMapSubscriber<Observable<T>, T> parent = OnSubscribeConcatMap.prepare(s, this, 2, OnSubscribeConcatMap.IMMEDIATE);
        if (!s.isUnsubscribed()) {
            parent.setProducer(new OnSubscribeFromArray.FromArrayProducer<Observable<T>>(parent, sources));
        }
    }

    @Override
    public Observable<T> call(Observable<T> t) {
        return t;
    }
}
