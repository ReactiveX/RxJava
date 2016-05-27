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

public final class OnSubscribeMergeArray<T> implements OnSubscribe<T> {
    final Observable<? extends T>[] sources;
    
    public OnSubscribeMergeArray(Observable<? extends T>[] sources) {
        this.sources = sources;
    }

    @SuppressWarnings("unchecked")
    public OnSubscribeMergeArray<T> mergeWith(Observable<? extends T> source) {
        Observable<? extends T>[] oldSources = sources;
        
        int oldLen = oldSources.length;
        Observable<? extends T>[] newSources = new Observable[oldLen + 1];
        System.arraycopy(oldSources, 0, newSources, 0, oldLen);
        newSources[oldLen] = source;
        
        return new OnSubscribeMergeArray<T>(newSources);
    }
    
    @Override
    public void call(final Subscriber<? super T> s) {
        
        OperatorMerge<T> op = OperatorMerge.instance(false);
        
        Subscriber<Observable<? extends T>> parent = op.call(s);
        
        if (!s.isUnsubscribed()) {
            parent.setProducer(new OnSubscribeFromArray.FromArrayProducer<Observable<? extends T>>(parent, sources));
        }
    }
}
