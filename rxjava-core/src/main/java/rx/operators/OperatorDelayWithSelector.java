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

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observers.SerializedSubscriber;
import rx.operators.OperatorDelay.Emitter;
import rx.subscriptions.CompositeSubscription;

/**
 * Delay the subscription and emission of the source items by a per-item observable that fires its first element.
 * @param <T> the item type
 * @param <U> the value type of the subscription-delaying observable
 * @param <V> the value type of the item-delaying observable
 */
public final class OperatorDelayWithSelector<T, U, V> implements OnSubscribe<T> {
    final Observable<? extends T> source;
    final Func0<? extends Observable<U>> subscriptionDelay;
    final Func1<? super T, ? extends Observable<V>> itemDelay;

    public OperatorDelayWithSelector(Observable<? extends T> source, Func1<? super T, ? extends Observable<V>> itemDelay) {
        this.source = source;
        this.subscriptionDelay = new Func0<Observable<U>>() {
            @Override
            public Observable<U> call() {
                return Observable.just(null);
            }
        };
        this.itemDelay = itemDelay;
    }

    public OperatorDelayWithSelector(Observable<? extends T> source, Func0<? extends Observable<U>> subscriptionDelay, Func1<? super T, ? extends Observable<V>> itemDelay) {
        this.source = source;
        this.subscriptionDelay = subscriptionDelay;
        this.itemDelay = itemDelay;
    }

    @Override
    public void call(Subscriber<? super T> child) {
        final SerializedSubscriber<T> s = new SerializedSubscriber<T>(child);
        final CompositeSubscription csub = new CompositeSubscription();
        child.add(csub);
        
        Observable<U> osub;
        try {
            osub = subscriptionDelay.call();
        } catch (Throwable e) {
            s.onError(e);
            return;
        }
        
        Observable<Observable<T>> seqs = source.map(new Func1<T, Observable<T>>() {
            @Override
            public Observable<T> call(final T x) {
                final Emitter<T> e = new Emitter<T>(x);
                Observable<V> itemObs = itemDelay.call(x);

                Subscriber<V> itemSub = new Subscriber<V>() {
                    boolean once = true;
                    @Override
                    public void onNext(V t) {
                        emit();
                    }

                    @Override
                    public void onError(Throwable e) {
                        s.onError(e);
                        s.unsubscribe();
                    }

                    @Override
                    public void onCompleted() {
                        emit();
                    }
                    void emit() {
                        if (once) {
                            once = false;
                            e.call();
                            csub.remove(this);
                        }
                    }
                };
                csub.add(itemSub);
                itemObs.unsafeSubscribe(itemSub);
                
                return Observable.create(e);
            }
        });
        final Observable<T> delayed = Observable.merge(seqs);
        
        Subscriber<U> osubSub = new Subscriber<U>(child) {
            boolean subscribed;
            @Override
            public void onNext(U ignored) {
                onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                if (!subscribed) {
                    s.onError(e);
                    unsubscribe();
                }
            }

            @Override
            public void onCompleted() {
                if (!subscribed) {
                    subscribed = true;
                    delayed.unsafeSubscribe(s);
                }
            }
        };
        
        osub.unsafeSubscribe(osubSub);
    }
}
