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

import java.util.*;
import java.util.concurrent.atomic.*;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.EmptyDisposable;
import io.reactivex.internal.util.NotificationLite;

public final class SingleCache<T> extends Single<T> {

    final SingleConsumable<? extends T> source;
    
    final AtomicInteger wip;
    final AtomicReference<Object> notification;
    final List<SingleSubscriber<? super T>> subscribers;

    public SingleCache(SingleConsumable<? extends T> source) {
        this.source = source;
        this.wip = new AtomicInteger();
        this.notification = new AtomicReference<Object>();
        this.subscribers = new ArrayList<SingleSubscriber<? super T>>();
    }

    @Override
    protected void subscribeActual(SingleSubscriber<? super T> s) {

        Object o = notification.get();
        if (o != null) {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            if (NotificationLite.isError(o)) {
                s.onError(NotificationLite.getError(o));
            } else {
                s.onSuccess(NotificationLite.<T>getValue(o));
            }
            return;
        }
        
        synchronized (subscribers) {
            o = notification.get();
            if (o == null) {
                subscribers.add(s);
            }
        }
        if (o != null) {
            s.onSubscribe(EmptyDisposable.INSTANCE);
            if (NotificationLite.isError(o)) {
                s.onError(NotificationLite.getError(o));
            } else {
                s.onSuccess(NotificationLite.<T>getValue(o));
            }
            return;
        }
        
        if (wip.getAndIncrement() != 0) {
            return;
        }
        
        source.subscribe(new SingleSubscriber<T>() {

            @Override
            public void onSubscribe(Disposable d) {
                
            }

            @Override
            public void onSuccess(T value) {
                notification.set(NotificationLite.next(value));
                List<SingleSubscriber<? super T>> list;
                synchronized (subscribers) {
                    list = new ArrayList<SingleSubscriber<? super T>>(subscribers);
                    subscribers.clear();
                }
                for (SingleSubscriber<? super T> s1 : list) {
                    s1.onSuccess(value);
                }
            }

            @Override
            public void onError(Throwable e) {
                notification.set(NotificationLite.error(e));
                List<SingleSubscriber<? super T>> list;
                synchronized (subscribers) {
                    list = new ArrayList<SingleSubscriber<? super T>>(subscribers);
                    subscribers.clear();
                }
                for (SingleSubscriber<? super T> s1 : list) {
                    s1.onError(e);
                }
            }
            
        });
    }

}
