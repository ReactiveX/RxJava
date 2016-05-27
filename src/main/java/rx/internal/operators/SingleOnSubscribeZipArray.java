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
import rx.functions.Func2;
import rx.internal.operators.OnSubscribeZipArray.PairwiseZipper;

public final class SingleOnSubscribeZipArray<T, R> implements Single.OnSubscribe<R> {
    final Single<? extends T>[] sources;
    
    final PairwiseZipper zipper;

    @SuppressWarnings("rawtypes") 
    public SingleOnSubscribeZipArray(Single<? extends T>[] sources, Func2 zipper) {
        this.sources = sources;
        this.zipper = new PairwiseZipper(new Func2[] { zipper });
    }

    SingleOnSubscribeZipArray(Single<? extends T>[] sources, PairwiseZipper zipper) {
        this.sources = sources;
        this.zipper = zipper;
    }

    @SuppressWarnings("unchecked")
    public <U> SingleOnSubscribeZipArray<T, U> zipWith(Single<? extends T> source, Func2<R, T, U> zipper) {
        Single<? extends T>[] oldSources = sources;
        
        int oldLen = oldSources.length;
        Single<? extends T>[] newSources = new Single[oldLen + 1];
        System.arraycopy(oldSources, 0, newSources, 0, oldLen);
        newSources[oldLen] = source;
        
        return new SingleOnSubscribeZipArray<T, U>(newSources, this.zipper.then(zipper));
    }
    
    @Override
    @SuppressWarnings({ "unchecked" })
    public void call(final SingleSubscriber<? super R> s) {
        SingleOperatorZip.zip(sources, zipper).call(s);
    }
}
