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

package io.reactivex.internal.operators;

import java.util.concurrent.*;

import org.reactivestreams.*;

import io.reactivex.internal.subscriptions.ScalarAsyncSubscription;

public final class PublisherFutureSource<T> implements Publisher<T> {
    final Future<? extends T> future;
    final long timeout;
    final TimeUnit unit;

    public PublisherFutureSource(Future<? extends T> future, long timeout, TimeUnit unit) {
        this.future = future;
        this.timeout = timeout;
        this.unit = unit;
    }
    
    @Override
    public void subscribe(Subscriber<? super T> s) {
        ScalarAsyncSubscription<T> sas = new ScalarAsyncSubscription<>(s);
        s.onSubscribe(sas);
        if (!sas.isComplete()) {
            T v;
            try {
                v = unit != null ? future.get(timeout, unit) : future.get();
            } catch (Throwable ex) {
                if (!sas.isComplete()) {
                    s.onError(ex);
                }
                return;
            } finally {
                future.cancel(true); // TODO ?? not sure about this
            }
            sas.setValue(v);
        }
    }
}
