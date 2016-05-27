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
import rx.functions.*;
import rx.internal.producers.SingleProducer;

public final class OnSubscribeZipArray<T, R> implements OnSubscribe<R> {
    final Observable<? extends T>[] sources;
    
    final PairwiseZipper zipper;

    @SuppressWarnings("rawtypes") 
    public OnSubscribeZipArray(Observable<? extends T>[] sources, Func2 zipper) {
        this.sources = sources;
        this.zipper = new PairwiseZipper(new Func2[] { zipper });
    }

    OnSubscribeZipArray(Observable<? extends T>[] sources, PairwiseZipper zipper) {
        this.sources = sources;
        this.zipper = zipper;
    }

    @SuppressWarnings("unchecked")
    public <U> OnSubscribeZipArray<T, U> zipWith(Observable<? extends T> source, Func2<R, T, U> zipper) {
        Observable<? extends T>[] oldSources = sources;
        
        int oldLen = oldSources.length;
        Observable<? extends T>[] newSources = new Observable[oldLen + 1];
        System.arraycopy(oldSources, 0, newSources, 0, oldLen);
        newSources[oldLen] = source;
        
        return new OnSubscribeZipArray<T, U>(newSources, this.zipper.then(zipper));
    }
    
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void call(final Subscriber<? super R> s) {
        OperatorZip<R> op = new OperatorZip<R>(zipper);
        Subscriber<? super Observable[]> call = op.call(s);
        
        if (!s.isUnsubscribed()) {
            call.setProducer(new SingleProducer<Observable[]>(call, sources));
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static final class PairwiseZipper implements FuncN {
        final Func2[] zippers;
        
        public PairwiseZipper(Func2[] zippers) {
            this.zippers = zippers;
        }
        
        @Override
        public Object call(Object... args) {
            Object o = zippers[0].call(args[0], args[1]);
            for (int i = 1; i < zippers.length; i++) {
                o = zippers[i].call(o, args[i + 1]);
            }
            return o;
        }
        
        public PairwiseZipper then(Func2 zipper) {
            Func2[] zippers = this.zippers;
            int n = zippers.length;
            Func2[] newZippers = new Func2[n + 1];
            System.arraycopy(zippers, 0, newZippers, 0, n);
            newZippers[n] = zipper;
            
            return new PairwiseZipper(newZippers);
        }
    }
}
