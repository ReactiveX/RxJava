 /**
  * Copyright 2014 Netflix, Inc.
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
package rx.operators;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Action0;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.SerialSubscription;
import rx.subscriptions.Subscriptions;

/**
 * Returns an Observable that emits the items emitted by two or more Observables, one after the
 * other.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/concat.png">
 */
public final class OperatorConcat<T> implements Operator<T, Observable<? extends T>> {
    final NotificationLite<Observable<? extends T>> nl = NotificationLite.instance();
    @Override
    public Subscriber<? super Observable<? extends T>> call(final Subscriber<? super T> child) {
        final SerializedSubscriber<T> s = new SerializedSubscriber<T>(child);
        final SerialSubscription current = new SerialSubscription();
        child.add(current);
        return new ConcatSubscriber(s, current);
    }
    
    final class ConcatSubscriber extends Subscriber<Observable<? extends T>> {
        
        private final Subscriber<T> s;
        private final SerialSubscription current;
        final ConcurrentLinkedQueue<Object> queue;
        final AtomicInteger wip;
        
        public ConcatSubscriber(Subscriber<T> s, SerialSubscription current) {
            super(s);
            this.s = s;
            this.current = current;
            this.queue = new ConcurrentLinkedQueue<Object>();
            this.wip = new AtomicInteger();
            add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    queue.clear();
                }
            }));
        }
        
        @Override
        public void onNext(Observable<? extends T> t) {
            queue.add(nl.next(t));
            if (wip.getAndIncrement() == 0) {
                subscribeNext();
            }
        }
        
        @Override
        public void onError(Throwable e) {
            s.onError(e);
            unsubscribe();
        }
        
        @Override
        public void onCompleted() {
            queue.add(nl.completed());
            if (wip.getAndIncrement() == 0) {
                subscribeNext();
            }
        }
        
        void subscribeNext() {
            Object o = queue.poll();
            if (nl.isCompleted(o)) {
                s.onCompleted();
            } else 
            if (o != null) {
                Observable<? extends T> obs = nl.getValue(o);
                Subscriber<T> sourceSub = new Subscriber<T>() {

                    @Override
                    public void onNext(T t) {
                        s.onNext(t);
                    }

                    @Override
                    public void onError(Throwable e) {
                        ConcatSubscriber.this.onError(e);
                    }

                    @Override
                    public void onCompleted() {
                        if (wip.decrementAndGet() > 0) {
                            subscribeNext();
                        }
                    }

                };
                current.set(sourceSub);
                obs.unsafeSubscribe(sourceSub);
            }
        }
    }
}
