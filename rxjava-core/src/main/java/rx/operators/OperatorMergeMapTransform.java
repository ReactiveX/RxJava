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

import java.util.concurrent.atomic.AtomicInteger;
import rx.Observable;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observers.SerializedSubscriber;
import rx.subscriptions.CompositeSubscription;

/**
 * Projects the notification of an observable sequence to an observable
 * sequence and merges the results into one.
 * 
 * @param <T> the input value type
 * @param <R> the output value type
 */
public final class OperatorMergeMapTransform<T, R> implements Operator<R, T> {
    final Func1<? super T, ? extends Observable<? extends R>> onNext;
    final Func1<? super Throwable, ? extends Observable<? extends R>> onError;
    final Func0<? extends Observable<? extends R>> onCompleted;

    public OperatorMergeMapTransform(Func1<? super T, ? extends Observable<? extends R>> onNext, 
            Func1<? super Throwable, ? extends Observable<? extends R>> onError, 
            Func0<? extends Observable<? extends R>> onCompleted) {
        this.onNext = onNext;
        this.onError = onError;
        this.onCompleted = onCompleted;
    }

    @Override
    public Subscriber<? super T> call(Subscriber<? super R> child) {
        final SerializedSubscriber<R> s = new SerializedSubscriber<R>(child);
        final CompositeSubscription csub = new CompositeSubscription();
        child.add(csub);
        
        return new Subscriber<T>(child) {
            final AtomicInteger wip = new AtomicInteger(1);
            @Override
            public void onNext(T t) {
                Observable<? extends R> o;
                try {
                    o = onNext.call(t);
                } catch (Throwable e) {
                    error(e);
                    return;
                }
                subscribeTo(o);
            }

            @Override
            public void onError(Throwable e) {
                Observable<? extends R> o;
                try {
                    o = onError.call(e);
                } catch (Throwable t) {
                    error(t);
                    return;
                }
                subscribeTo(o);
                finish();
            }

            @Override
            public void onCompleted() {
                Observable<? extends R> o;
                try {
                    o = onCompleted.call();
                } catch (Throwable e) {
                    error(e);
                    return;
                }
                subscribeTo(o);
                finish();
            }
            void finish() {
                if (wip.decrementAndGet() == 0) {
                    s.onCompleted();
                }
            }
            void error(Throwable t) {
                s.onError(t);
                unsubscribe();
            }
            void subscribeTo(Observable<? extends R> o) {
                Subscriber<R> oSub = new Subscriber<R>() {

                    @Override
                    public void onNext(R t) {
                        s.onNext(t);
                    }

                    @Override
                    public void onError(Throwable e) {
                        error(e);
                    }

                    @Override
                    public void onCompleted() {
                        try {
                            finish();
                        } finally {
                            csub.remove(this);
                        }
                    }
                };
                csub.add(oSub);
                wip.incrementAndGet();

                o.unsafeSubscribe(oSub);
            }
        };
    }
    
}
