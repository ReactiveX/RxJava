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

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.Operator;
import rx.Scheduler.Inner;
import rx.Scheduler.Recurse;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class OperatorRetry<T> implements Operator<T, Observable<T>> {

    private static final int INFINITE_RETRY = -1;

    private final int retryCount;

    public OperatorRetry(int retryCount) {
        this.retryCount = retryCount;
    }

    public OperatorRetry() {
        this(INFINITE_RETRY);
    }

    @Override
    public Subscriber<? super Observable<T>> call(final Subscriber<? super T> s) {
        final Inner innerScheduler = Schedulers.trampoline().createInner();
        return new Subscriber<Observable<T>>(s) {
            final AtomicInteger attempts = new AtomicInteger(0);

            @Override
            public void onCompleted() {
                // ignore as we expect a single nested Observable<T>
            }

            @Override
            public void onError(Throwable e) {
                s.onError(e);
            }

            @Override
            public void onNext(final Observable<T> o) {
                innerScheduler.schedule(new Action1<Recurse>() {

                    @Override
                    public void call(final Recurse re) {
                        attempts.incrementAndGet();
                        o.unsafeSubscribe(new Subscriber<T>(s) {

                            @Override
                            public void onCompleted() {
                                s.onCompleted();
                            }

                            @Override
                            public void onError(Throwable e) {
                                if ((retryCount == INFINITE_RETRY || attempts.get() <= retryCount) && !re.isUnsubscribed()) {
                                    // retry again
                                    re.schedule();
                                } else {
                                    // give up and pass the failure
                                    s.onError(e);
                                }
                            }

                            @Override
                            public void onNext(T v) {
                                s.onNext(v);
                            }

                        });
                    }
                });
            }

        };
    }
}
