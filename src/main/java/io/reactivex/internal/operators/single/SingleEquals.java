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

package io.reactivex.internal.operators.single;

import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.internal.functions.Objects;
import io.reactivex.plugins.RxJavaPlugins;

public final class SingleEquals<T> extends Single<Boolean> {

    final SingleConsumable<? extends T> first;
    final SingleConsumable<? extends T> second;
    
    public SingleEquals(SingleConsumable<? extends T> first, SingleConsumable<? extends T> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    protected void subscribeActual(final SingleSubscriber<? super Boolean> s) {

        final AtomicInteger count = new AtomicInteger();
        final Object[] values = { null, null };
        
        final CompositeDisposable set = new CompositeDisposable();
        s.onSubscribe(set);
        
        class InnerSubscriber implements SingleSubscriber<T> {
            final int index;
            public InnerSubscriber(int index) {
                this.index = index;
            }
            @Override
            public void onSubscribe(Disposable d) {
                set.add(d);
            }

            @Override
            public void onSuccess(T value) {
                values[index] = value;
                
                if (count.incrementAndGet() == 2) {
                    s.onSuccess(Objects.equals(values[0], values[1]));
                }
            }

            @Override
            public void onError(Throwable e) {
                for (;;) {
                    int state = count.get();
                    if (state >= 2) {
                        RxJavaPlugins.onError(e);
                        return;
                    }
                    if (count.compareAndSet(state, 2)) {
                        s.onError(e);
                        return;
                    }
                }
            }
            
        }
        
        first.subscribe(new InnerSubscriber(0));
        second.subscribe(new InnerSubscriber(1));
    }

}
