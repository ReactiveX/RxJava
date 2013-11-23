/**
 * Copyright 2013 Netflix, Inc.
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
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.subscriptions.MultipleAssignmentSubscription;
import rx.util.functions.Func2;

public final class OperationRepeat {

    public static <T> Observable.OnSubscribeFunc<T> repeat(Observable<? extends T> source) {
        return new RepeatObservable<T>(source);
    }

    static class RepeatObservable<T> implements Observable.OnSubscribeFunc<T> {

        RepeatObservable(Observable<? extends T> source) {
            this.source = source;
        }

        private Observable<? extends T> source;
        private Observer<? super T> observer;
        private MultipleAssignmentSubscription subscription =  new MultipleAssignmentSubscription();

        @Override
        public Subscription onSubscribe(Observer observer) {
            this.observer = observer;
            Loop();
            return subscription;
        }

        void Loop() {
            subscription.setSubscription(Schedulers.currentThread().schedule(0, new Func2<Scheduler, Integer, Subscription>() {
                @Override
                public Subscription call(Scheduler s, Integer n) {
                    return source.subscribe(new Observer<T>() {
                        @Override
                        public void onCompleted() { Loop();  }

                        @Override
                        public void onError(Throwable error) { observer.onError(error); }

                        @Override
                        public void onNext(T value) { observer.onNext(value); }
                    });
                }
            }));
        }
    }


}
