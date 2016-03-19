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

package io.reactivex.internal.operators.flowable;

import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.*;

import io.reactivex.internal.subscribers.flowable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;

public final class PublisherArraySource<T> implements Publisher<T> {
    final T[] array;
    public PublisherArraySource(T[] array) {
        this.array = array;
    }
    public T[] array() {
        return array;
    }
    @Override
    public void subscribe(Subscriber<? super T> s) {
        if (s instanceof ConditionalSubscriber) {
            ConditionalSubscriber<? super T> cs = (ConditionalSubscriber<? super T>) s;
            s.onSubscribe(new ConditionalArraySourceSubscription<T>(array, cs));
        } else {
            s.onSubscribe(new ArraySourceSubscription<T>(array, s));
        }
    }
    
    static final class ArraySourceSubscription<T> extends AtomicLong implements Subscription {
        /** */
        private static final long serialVersionUID = -225561973532207332L;
        
        final T[] array;
        final Subscriber<? super T> subscriber;
        
        int index;
        volatile boolean cancelled;
        
        public ArraySourceSubscription(T[] array, Subscriber<? super T> subscriber) {
            this.array = array;
            this.subscriber = subscriber;
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            if (BackpressureHelper.add(this, n) == 0L) {
                long r = n;
                final Subscriber<? super T> s = subscriber;
                for (;;) {
                    int i = index;
                    T[] a = array;
                    int len = a.length;
                    if (i + r >= len) {
                        if (cancelled) {
                            return;
                        }
                        for (int j = i; j < len; j++) {
                            T t = a[j];
                            if (t == null) {
                                s.onError(new NullPointerException("The " + j + "th array element is null"));
                                return;
                            }
                            s.onNext(t);
                            if (cancelled) {
                                return;
                            }
                        }
                        s.onComplete();
                        return;
                    }
                    long e = 0;
                    if (cancelled) {
                        return;
                    }
                    while (r != 0 && i < len) {
                        T t = a[i];
                        if (t == null) {
                            s.onError(new NullPointerException("The " + i + "th array element is null"));
                            return;
                        }
                        s.onNext(t);
                        if (cancelled) {
                            return;
                        }
                        if (++i == len) {
                            s.onComplete();
                            return;
                        }
                        r--;
                        e--;
                    }
                    index = i;
                    r = addAndGet(e);
                    if (r == 0L) {
                        return;
                    }
                }
            }
        }
        @Override
        public void cancel() {
            cancelled = true;
        }
    }
    
    static final class ConditionalArraySourceSubscription<T> extends AtomicLong implements Subscription {
        /** */
        private static final long serialVersionUID = -225561973532207332L;
        
        final T[] array;
        final ConditionalSubscriber<? super T> subscriber;
        
        int index;
        volatile boolean cancelled;
        
        public ConditionalArraySourceSubscription(T[] array, ConditionalSubscriber<? super T> subscriber) {
            this.array = array;
            this.subscriber = subscriber;
        }
        
        @Override
        public void request(long n) {
            if (SubscriptionHelper.validateRequest(n)) {
                return;
            }
            if (BackpressureHelper.add(this, n) == 0L) {
                long r = n;
                final ConditionalSubscriber<? super T> s = subscriber;
                for (;;) {
                    int i = index;
                    T[] a = array;
                    int len = a.length;
                    if (i + r >= len) {
                        if (cancelled) {
                            return;
                        }
                        for (int j = i; j < len; j++) {
                            T t = a[j];
                            if (t == null) {
                                s.onError(new NullPointerException("The " + j + "th array element is null"));
                                return;
                            }
                            s.onNext(t);
                            if (cancelled) {
                                return;
                            }
                        }
                        s.onComplete();
                        return;
                    }
                    long e = 0;
                    if (cancelled) {
                        return;
                    }
                    while (r != 0 && i < len) {
                        boolean b = s.onNextIf(a[i]);
                        if (cancelled) {
                            return;
                        }
                        if (++i == len) {
                            s.onComplete();
                            return;
                        }
                        if (b) {
                            r--;
                            e--;
                        }
                    }
                    index = i;
                    r = addAndGet(e);
                    if (r == 0L) {
                        return;
                    }
                }
            }
        }
        @Override
        public void cancel() {
            cancelled = true;
        }
    }
}
