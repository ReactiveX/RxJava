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

import rx.Observable;
import rx.Subscriber;
import rx.util.functions.Action0;

/**
 * Repeats the source observable limited or unlimited times.
 * @param <T> the element type of the repeated observable
 */
public class OperatorRepeat<T> implements Operator<T, Observable<T>> {
    /** The repeat count, -1 means indefinitely. */
    final long count;
    /**
     * Constructor with repeat count.
     * @param count the repeat count, -1 indicates indefinitely
     */
    public OperatorRepeat(long count) {
        this.count = count;
    }
    @Override
    public Subscriber<? super Observable<T>> call(final Subscriber<? super T> t1) {
        return new Subscriber<Observable<T>>(t1) {
            /** Queue to avoid reentrancy and asynchronous anomalies. */
            final QueueDrain qd = new QueueDrain(t1);
            /** Repeat count. */
            long idx = count - 1;
            @Override
            public void onNext(final Observable<T> t) {
                if (count == 0) {
                    t1.onCompleted();
                    return;
                }
                qd.enqueue(new Action0() {

                    @Override
                    public void call() {
                        final Subscriber<T> s2 = new Subscriber<T>(t1) {
                            
                            @Override
                            public void onNext(T v) {
                                t1.onNext(v);
                            }

                            @Override
                            public void onError(Throwable e) {
                                t1.onError(e);
                            }

                            @Override
                            public void onCompleted() {
                                if (!isUnsubscribed()) {
                                    if (count < 0) {
                                        innerNext(t);
                                    } else
                                    if (idx > 0) {
                                        idx--;
                                        innerNext(t);
                                    } else {
                                        t1.onCompleted();
                                    }
                                }
                            }
                            
                        };
                        t.subscribe(s2);
                    }

                });
                qd.tryDrain();
            }
            /** Trigger resubscription from inner. */
            void innerNext(Observable<T> iargs) {
                onNext(iargs);
            }
            @Override
            public void onError(Throwable e) {
                t1.onError(e);
            }

            @Override
            public void onCompleted() {
                // from(this) will send this onCompleted, which can be safely ignored.
            }
        };
    }
    
}
