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

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;

public final class OperatorDelay {
    private OperatorDelay() { throw new IllegalStateException("No instances!"); }
    
    public static <T> Observable<T> delay(Observable<T> observable, final long delay, final TimeUnit unit, final Scheduler scheduler) {
        // observable.map(x => Observable.timer(t).map(_ => x).startItAlreadyNow()).concat()
        final Observable<Long> delayTimer = Observable.timer(delay, unit, scheduler);

        Observable<Observable<T>> seqs = observable.map(new Func1<T, Observable<T>>() {
            @Override
            public Observable<T> call(final T x) {
                ConnectableObservable<T> co = delayTimer.map(new Func1<Long, T>() {
                    @Override
                    public T call(Long ignored) {
                        return x;
                    }
                }).replay();
                co.connect();
                return co;
            }
        });
        return Observable.concat(seqs);
    }
}
