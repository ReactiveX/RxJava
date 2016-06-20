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

import java.util.NoSuchElementException;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposables;

public final class SingleFromPublisher<T> extends Single<T> {

    final Publisher<? extends T> publisher;
    
    public SingleFromPublisher(Publisher<? extends T> publisher) {
        this.publisher = publisher;
    }

    @Override
    protected void subscribeActual(final SingleSubscriber<? super T> s) {

        publisher.subscribe(new Subscriber<T>() {
            T value;
            @Override
            public void onComplete() {
                T v = value;
                value = null;
                if (v != null) {
                    s.onSuccess(v);
                } else {
                    s.onError(new NoSuchElementException());
                }
            }

            @Override
            public void onError(Throwable t) {
                value = null;
                s.onError(t);
            }

            @Override
            public void onNext(T t) {
                value = t;
            }

            @Override
            public void onSubscribe(Subscription inner) {
                s.onSubscribe(Disposables.from(inner));
                inner.request(Long.MAX_VALUE);
            }
            
        });
    }

}
