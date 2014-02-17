/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import java.util.concurrent.atomic.AtomicInteger;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler.Inner;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.util.functions.Action1;

/**
 * Resubscribe to the source observable if an error occurs.
 * @param <T> the 
 */
public class OperatorRetry<T> implements OnSubscribe<T> {
    final Observable<T> source;
    final int maxRetry;

    public OperatorRetry(Observable<T> source, int maxRetry) {
        this.source = source;
        this.maxRetry = maxRetry;
    }
    public OperatorRetry(Observable<T> source) {
        this.source = source;
        this.maxRetry = -1;
    }

    @Override
    public void call(final Subscriber<? super T> sub) {
        final MultipleAssignmentSubscription mas = new MultipleAssignmentSubscription();
        sub.add(mas);
        sub.add(Schedulers.trampoline().schedule(new Action1<Inner>() {
            final Action1<Inner> self = this;
            final AtomicInteger retryCount = new AtomicInteger();
            @Override
            public void call(final Inner inner) {
                mas.set(source.subscribe(new Subscriber<T>() {

                    @Override
                    public void onNext(T t) {
                        sub.onNext(t);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (maxRetry == -1 || retryCount.getAndIncrement() < maxRetry) {
                            inner.schedule(self);
                        } else {
                            sub.onError(e);
                        }
                    }

                    @Override
                    public void onCompleted() {
                        sub.onCompleted();
                    }
                }));
            }
        }));
    }
    
}
