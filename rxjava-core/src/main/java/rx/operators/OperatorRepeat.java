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
import rx.Scheduler;
import rx.Scheduler.Inner;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import rx.util.functions.Action1;

public class OperatorRepeat<T> implements Operator<T, Observable<T>> {

    private final Scheduler scheduler;

    public OperatorRepeat(Scheduler scheduler) {
        this.scheduler = scheduler;

    }

    public OperatorRepeat() {
        this(Schedulers.trampoline());
    }

    @Override
    public Subscriber<? super Observable<T>> call(final Subscriber<? super T> child) {
        return new Subscriber<Observable<T>>(child) {

            @Override
            public void onCompleted() {
                // ignore as we will keep repeating
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(final Observable<T> t) {
                scheduler.schedule(new Action1<Inner>() {

                    final Action1<Inner> self = this;

                    @Override
                    public void call(final Inner inner) {

                        t.subscribe(new Subscriber<T>(child) {

                            @Override
                            public void onCompleted() {
                                inner.schedule(self);
                            }

                            @Override
                            public void onError(Throwable e) {
                                child.onError(e);
                            }

                            @Override
                            public void onNext(T t) {
                                child.onNext(t);
                            }

                        });
                    }

                });
            }

        };
    }
}