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

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import io.reactivex.NbpObservable.*;
import io.reactivex.disposables.BooleanDisposable;
import io.reactivex.internal.disposables.EmptyDisposable;

/**
 *
 */
public final class NbpOnSubscribeStreamSource<T> extends AtomicBoolean implements NbpOnSubscribe<T> {
    /** */
    private static final long serialVersionUID = 9051303031779816842L;
    
    final Stream<? extends T> stream;
    public NbpOnSubscribeStreamSource(Stream<? extends T> stream) {
        this.stream = stream;
    }
    @Override
    public void accept(NbpSubscriber<? super T> s) {
        if (compareAndSet(false, true)) {
            Iterator<? extends T> it;
            try {
                it = stream.iterator();
            } catch (Throwable e) {
                EmptyDisposable.error(e, s);
                return;
            }
            
            BooleanDisposable bd = new BooleanDisposable();

            s.onSubscribe(bd);
            if (bd.isDisposed()) {
                return;
            }
            
            boolean hasNext;
            
            try {
                hasNext = it.hasNext();
            } catch (Throwable e) {
                s.onError(e);
                return;
            }
            
            
            do {
                T v;
                
                try {
                    v = it.next();
                } catch (Throwable e) {
                    s.onError(e);
                    return;
                }
                
                if (v == null) {
                    s.onError(new NullPointerException("The stream returned a null value"));
                    return;
                }
                
                if (bd.isDisposed()) {
                    return;
                }

                try {
                    hasNext = it.hasNext();
                } catch (Throwable e) {
                    s.onError(e);
                    return;
                }
                
                if (bd.isDisposed()) {
                    return;
                }
            } while (hasNext);
            if (bd.isDisposed()) {
                return;
            }
            s.onComplete();
            return;
        }
        EmptyDisposable.error(new IllegalStateException("Contents already consumed"), s);
    }
}
